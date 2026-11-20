package dev.hayya.core.system.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.batoulapps.adhan.Coordinates
import dev.hayya.core.R
import dev.hayya.core.data.preferences.UserPreferencesRepository
import dev.hayya.core.domain.calculation.PrayerCalculationEngine
import dev.hayya.core.domain.model.PrayerWindow
import dev.hayya.core.system.receivers.PreferenceDataStoreProvider
import dev.hayya.core.ui.MainActivity
import dev.hayya.core.ui.viewmodel.PrayerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Minimalist, battery-neutral foreground service displaying a persistent, ongoing
 * prayer status notification in the Android notification shade.
 *
 * Anchors the Hayya application process in memory, rendering it practically immune
 * to the Android Low Memory Killer (LMK) on aggressive OEM devices.
 */
class PinnedPrayerStatusService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var tickerJob: Job? = null
    private var lastTitle: String? = null
    private var lastSubtitle: String? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        ensureNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        if (action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForegroundWithServiceType()
        startTicker()

        return START_STICKY
    }

    private fun startForegroundWithServiceType() {
        val initialNotification = buildStatusNotification(
            title = "Hayya Prayer Times",
            subtitle = "Calculating upcoming prayer window..."
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                initialNotification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            val dataStore = PreferenceDataStoreProvider.getDataStore(this@PinnedPrayerStatusService)
            val repository = UserPreferencesRepository(dataStore)
            val calculationEngine = PrayerCalculationEngine()
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            var forceImmediateRender = true

            while (isActive) {
                try {
                    val userPrefs = repository.userPreferencesFlow.first()
                    if (!userPrefs.pinnedNotificationEnabled) {
                        stopSelf()
                        break
                    }

                    val isInteractive = powerManager?.isInteractive ?: true
                    if (isInteractive || forceImmediateRender) {
                        forceImmediateRender = false
                        val nowMillis = Clock.System.now().toEpochMilliseconds()
                        val timeZone = TimeZone.currentSystemDefault()
                        val localDate = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date

                        val coordinates = Coordinates(userPrefs.location.latitude, userPrefs.location.longitude)
                        val params = calculationEngine.createParameters(
                            method = userPrefs.calculationMethod.toAdhanMethod(),
                            madhab = userPrefs.madhab.toAdhanMadhab(),
                            highLatitudeRule = userPrefs.highLatitudeRule.toAdhanRule()
                        )

                        val dailyTimes = calculationEngine.computeDailyTimes(
                            date = localDate,
                            coordinates = coordinates,
                            parameters = params,
                            manualOffsets = userPrefs.manualMinuteOffsets
                        )

                        val window = calculationEngine.getPrayerWindow(
                            timestampEpochMillis = nowMillis,
                            coordinates = coordinates,
                            parameters = params,
                            timeZone = timeZone,
                            manualOffsets = userPrefs.manualMinuteOffsets,
                            ishaExpiresAtMidnight = userPrefs.ishaExpiresAtMidnight
                        )

                        val nextPrayer = window.nextPrayer
                        val nextPrayerTimeMillis = dailyTimes.upcomingTimeFor(nextPrayer, nowMillis)
                        val nextPrayerFormatted = PrayerViewModel.formatTimeOfDay(nextPrayerTimeMillis, timeZone)

                        val remainingMillis = (nextPrayerTimeMillis - nowMillis).coerceAtLeast(0L)
                        val remainingMinutes = remainingMillis / 60_000L
                        val countdownStr = if (remainingMinutes >= 60) {
                            val hours = remainingMinutes / 60
                            val mins = remainingMinutes % 60
                            "in ${hours}h ${mins}m"
                        } else {
                            "in ${remainingMinutes}m"
                        }

                        val title = "Next: ${nextPrayer.name} at $nextPrayerFormatted"
                        val subtitle = when (window) {
                            is PrayerWindow.Active -> "${window.currentPrayer.name} window closes $countdownStr"
                            is PrayerWindow.SunriseInterval -> "Sunrise period • Dhuhr $countdownStr"
                        }

                        if (title != lastTitle || subtitle != lastSubtitle) {
                            lastTitle = title
                            lastSubtitle = subtitle
                            val notification = buildStatusNotification(title, subtitle)
                            val notificationManager =
                                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.notify(NOTIFICATION_ID, notification)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating pinned status notification", e)
                }

                // Low-frequency battery-neutral update: once every 60 seconds
                delay(60_000L)
            }
        }
    }

    private fun buildStatusNotification(title: String, subtitle: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(subtitle)
            .setSmallIcon(R.drawable.ic_stat_hayya)
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pinned Prayer Status",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Minimalist ongoing status notification anchoring prayer alarms in memory"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        isRunning = false
        super.onDestroy()
        tickerJob?.cancel()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "PinnedPrayerService"
        const val NOTIFICATION_ID = 88001
        const val CHANNEL_ID = "channel_pinned_prayer_status"

        const val ACTION_START = "dev.hayya.notification.ACTION_START_PINNED_STATUS"
        const val ACTION_STOP = "dev.hayya.notification.ACTION_STOP_PINNED_STATUS"

        @Volatile
        var isRunning: Boolean = false
            private set

        fun refreshIfRunning(context: Context) {
            if (isRunning) {
                start(context)
            }
        }

        fun start(context: Context) {
            val intent = Intent(context, PinnedPrayerStatusService::class.java).apply {
                action = ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, PinnedPrayerStatusService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
