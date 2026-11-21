package dev.hayya.core.system.receivers

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.hayya.core.R
import dev.hayya.core.data.preferences.UserPreferencesRepository
import dev.hayya.core.data.ramadan.FastTrackerRepository
import dev.hayya.core.domain.calculation.PrayerCalculationEngine
import dev.hayya.core.domain.model.AlertMode
import dev.hayya.core.domain.model.AlertTier
import dev.hayya.core.domain.model.AutoMuteDuration
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.ramadan.IslamicCalendarHelper
import dev.hayya.core.domain.ramadan.IslamicEvent
import dev.hayya.core.system.alarm.PrayerAlarmScheduler
import dev.hayya.core.system.audio.AutoMuteManager
import dev.hayya.core.system.audio.PrayerAudioService
import dev.hayya.core.system.notifications.PinnedPrayerStatusService
import dev.hayya.core.system.ui.PrayerWakeActivity
import dev.hayya.core.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * High-priority BroadcastReceiver triggered by [android.app.AlarmManager.setAlarmClock].
 *
 * Execution contract:
 * 1. Immediately acquires a 10-second [PowerManager.PARTIAL_WAKE_LOCK] ensuring CPU retention.
 * 2. Evaluates the incoming alert tier:
 *    - Tier 1 (PRE_REMINDER) & Tier 3 (LAST_CALL): Dispatches high-priority heads-up notifications.
 *    - Tier 2 (MAIN_CALL): Hands off to [PrayerAudioService] with lockscreen [FullScreenIntent].
 * 3. Releases the WakeLock in a mandatory `finally` block.
 */
class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        if (intent.action == PrayerAlarmScheduler.ACTION_RESTORE_RINGER_MODE) {
            val pendingResult = goAsync()
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKELOCK_TAG
            ).apply {
                setReferenceCounted(false)
                acquire(5_000L)
            }

            CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
                try {
                    val dataStore = PreferenceDataStoreProvider.getDataStore(context)
                    val repository = UserPreferencesRepository(dataStore)
                    AutoMuteManager.restoreRingerMode(context, repository)
                } catch (e: Exception) {
                    Log.e(TAG, "Error restoring ringer mode upon window completion", e)
                } finally {
                    if (wakeLock.isHeld) wakeLock.release()
                    pendingResult?.finish()
                }
            }
            return
        }

        if (intent.action == PrayerAlarmScheduler.ACTION_TRIGGER_SUHOOR_ALARM) {
            val pendingResult = goAsync()
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG).apply {
                setReferenceCounted(false)
                acquire(10_000L)
            }
            try {
                ensureNotificationChannels(context)
                val fajrMillis = intent.getLongExtra(PrayerAlarmScheduler.EXTRA_FAJR_MILLIS, 0L)
                val autoSilenceSeconds = intent.getIntExtra(
                    PrayerAlarmScheduler.EXTRA_AUTO_SILENCE_SECONDS,
                    PrayerAudioService.DEFAULT_TIMEOUT_SECONDS
                )
                val volumeEscalationSeconds = intent.getIntExtra(
                    PrayerAlarmScheduler.EXTRA_VOLUME_ESCALATION_SECONDS,
                    0
                )
                val timeStr = if (fajrMillis > 0) {
                    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
                    sdf.format(Date(fajrMillis))
                } else "Fajr"
                val suhoorSubtitle = "Eat Suhoor & make Niyyah before Fajr ($timeStr)"

                val fullScreenIntent = Intent(context, PrayerWakeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(PrayerWakeActivity.EXTRA_NOTIFICATION_ID, PrayerAlarmScheduler.NOTIFICATION_ID_SUHOOR)
                    putExtra(PrayerWakeActivity.EXTRA_PRAYER_TYPE, "SUHOOR")
                    putExtra(PrayerWakeActivity.EXTRA_SUBTITLE, suhoorSubtitle)
                    putExtra(PrayerWakeActivity.EXTRA_ALERT_TIER, AlertTier.MAIN_CALL.name)
                    putExtra(PrayerWakeActivity.EXTRA_AUTO_SILENCE_SECONDS, autoSilenceSeconds)
                    putExtra(PrayerWakeActivity.EXTRA_SHOW_POST_ADHAN_DUA, false)
                }
                val fullScreenPendingIntent = PendingIntent.getActivity(
                    context,
                    PrayerAlarmScheduler.NOTIFICATION_ID_SUHOOR,
                    fullScreenIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val serviceIntent = Intent(context, PrayerAudioService::class.java).apply {
                    action = PrayerAudioService.ACTION_START_ALARM
                    putExtra(PrayerAudioService.EXTRA_NOTIFICATION_ID, PrayerAlarmScheduler.NOTIFICATION_ID_SUHOOR)
                    putExtra(PrayerAudioService.EXTRA_PRAYER_NAME, "Suhoor")
                    putExtra(PrayerAudioService.EXTRA_TIMEOUT_SECONDS, autoSilenceSeconds)
                    putExtra(PrayerAudioService.EXTRA_VOLUME_ESCALATION_SECONDS, volumeEscalationSeconds)
                }
                ContextCompat.startForegroundService(context, serviceIntent)

                val notification = NotificationCompat.Builder(context, CHANNEL_SUHOOR_REMINDER)
                    .setSmallIcon(R.drawable.ic_stat_hayya)
                    .setContentTitle("🍽️ Suhoor Wake-Up Alarm")
                    .setContentText("Eat Suhoor & make Niyyah. Fasting begins at Fajr ($timeStr).")
                    .setStyle(NotificationCompat.BigTextStyle().bigText("Eat Suhoor and hydrate before Fajr ($timeStr).\nIntention (Niyyah): 'Wa bisawmi ghadin nawaytu min shahri Ramadan'."))
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .setContentIntent(fullScreenPendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setSilent(true)
                    .setAutoCancel(true)
                    .build()

                notifySafely(context, PrayerAlarmScheduler.NOTIFICATION_ID_SUHOOR, notification)

                try {
                    context.startActivity(fullScreenIntent)
                } catch (e: Exception) {
                    Log.d(TAG, "Direct Suhoor wake launch deferred to fullScreenIntent: ${e.message}")
                }

                CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
                    try {
                        val dataStore = PreferenceDataStoreProvider.getDataStore(context)
                        val repository = UserPreferencesRepository(dataStore)
                        val fastTrackerRepository = FastTrackerRepository(dataStore)
                        val engine = PrayerCalculationEngine()
                        val scheduler = PrayerAlarmScheduler(context, engine, repository, fastTrackerRepository)
                        scheduler.scheduleRollingAlarms()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error rescheduling next Suhoor alarm", e)
                    } finally {
                        if (wakeLock.isHeld) wakeLock.release()
                        pendingResult?.finish()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling Suhoor alarm", e)
                if (wakeLock.isHeld) wakeLock.release()
                pendingResult?.finish()
            }
            return
        }

        if (intent.action == PrayerAlarmScheduler.ACTION_TRIGGER_EID_NOTIFICATION) {
            val pendingResult = goAsync()
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG).apply {
                setReferenceCounted(false)
                acquire(5_000L)
            }
            try {
                ensureNotificationChannels(context)
                val title = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_EID_TITLE) ?: "🌙 Eid Mubarak!"
                val message = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_EID_MESSAGE)
                    ?: "Taqabbal Allahu Minna wa Minkum! Wishing you and your family a blessed Eid."

                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pi = PendingIntent.getActivity(
                    context,
                    PrayerAlarmScheduler.NOTIFICATION_ID_EID,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, CHANNEL_EID_CELEBRATION)
                    .setSmallIcon(R.drawable.ic_stat_hayya)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText("$message\n\nاللَّهُ أَكْبَرُ اللَّهُ أَكْبَرُ لَا إِلَهَ إِلَّا اللَّهُ، وَاللَّهُ أَكْبَرُ اللَّهُ أَكْبَرُ وَلِلَّهِ الْحَمْدُ"))
                    .setContentIntent(pi)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .build()

                notifySafely(context, PrayerAlarmScheduler.NOTIFICATION_ID_EID, notification)
            } finally {
                if (wakeLock.isHeld) wakeLock.release()
                pendingResult?.finish()
            }
            return
        }

        if (intent.action == PrayerAlarmScheduler.ACTION_TRIGGER_TAHAJJUD_ALARM) {
            val pendingResult = goAsync()
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_TAG).apply {
                setReferenceCounted(false)
                acquire(5_000L)
            }
            try {
                ensureNotificationChannels(context)
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val pi = PendingIntent.getActivity(
                    context,
                    PrayerAlarmScheduler.NOTIFICATION_ID_TAHAJJUD,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, CHANNEL_TAHAJJUD_REMINDER)
                    .setSmallIcon(R.drawable.ic_stat_hayya)
                    .setContentTitle("Qiyam al-Layl — Tahajjud")
                    .setContentText("The Last Third of the Night has begun (Thuluth al-Akhir).")
                    .setStyle(NotificationCompat.BigTextStyle().bigText("The Last Third of the Night has begun.\n'Our Lord descends to the lowest heaven when the last third of the night remains...' (Sahih al-Bukhari 1145)"))
                    .setContentIntent(pi)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setAutoCancel(true)
                    .build()

                notifySafely(context, PrayerAlarmScheduler.NOTIFICATION_ID_TAHAJJUD, notification)

                CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
                    try {
                        val dataStore = PreferenceDataStoreProvider.getDataStore(context)
                        val repository = UserPreferencesRepository(dataStore)
                        val fastTrackerRepository = FastTrackerRepository(dataStore)
                        val engine = PrayerCalculationEngine()
                        val scheduler = PrayerAlarmScheduler(context, engine, repository, fastTrackerRepository)
                        scheduler.scheduleRollingAlarms()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error in post-Tahajjud alarm reschedule", e)
                    } finally {
                        if (wakeLock.isHeld) wakeLock.release()
                        pendingResult?.finish()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error handling Tahajjud alarm", e)
                if (wakeLock.isHeld) wakeLock.release()
                pendingResult?.finish()
            }
            return
        }

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKELOCK_TAG
        ).apply {
            setReferenceCounted(false)
            acquire(10_000L) // 10-second safety timeout
        }

        val pendingResult = goAsync()

        try {
            ensureNotificationChannels(context)

            val notificationId = intent.getIntExtra(PrayerAlarmScheduler.EXTRA_NOTIFICATION_ID, -1)
            val prayerTypeName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TYPE)
            val alertTierName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_ALERT_TIER)
            val alertModeName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_ALERT_MODE)
            val autoSilenceSeconds = intent.getIntExtra(
                PrayerAlarmScheduler.EXTRA_AUTO_SILENCE_SECONDS,
                PrayerAudioService.DEFAULT_TIMEOUT_SECONDS
            )
            val volumeEscalationSeconds = intent.getIntExtra(
                PrayerAlarmScheduler.EXTRA_VOLUME_ESCALATION_SECONDS,
                0
            )
            val showPostAdhanDua = intent.getBooleanExtra(
                PrayerWakeActivity.EXTRA_SHOW_POST_ADHAN_DUA,
                true
            )
            val iftarNotificationEnabled = intent.getBooleanExtra(
                PrayerAlarmScheduler.EXTRA_IFTAR_NOTIFICATION_ENABLED,
                true
            )

            val prayerType = prayerTypeName?.let {
                runCatching { PrayerType.valueOf(it) }.getOrNull()
            } ?: PrayerType.FAJR

            val alertTier = alertTierName?.let {
                runCatching { AlertTier.valueOf(it) }.getOrNull()
            } ?: AlertTier.MAIN_CALL

            val alertMode = alertModeName?.let {
                runCatching { AlertMode.valueOf(it) }.getOrNull()
            } ?: AlertMode.FULL_ALARM

            Log.i(TAG, "Triggered prayer alarm: $prayerType, Tier: $alertTier, Mode: $alertMode, ID: $notificationId")

            if (alertMode != AlertMode.MUTED) {
                when (alertTier) {
                    AlertTier.PRE_REMINDER -> {
                        handlePreReminder(context, notificationId, prayerType)
                    }
                    AlertTier.MAIN_CALL -> {
                        if (alertMode == AlertMode.GENTLE_BANNER) {
                            handleGentleMainCall(context, notificationId, prayerType, iftarNotificationEnabled)
                        } else {
                            handleMainCall(
                                context = context,
                                notificationId = notificationId,
                                prayerType = prayerType,
                                autoSilenceSeconds = autoSilenceSeconds,
                                showPostAdhanDua = showPostAdhanDua,
                                volumeEscalationSeconds = volumeEscalationSeconds,
                                iftarNotificationEnabled = iftarNotificationEnabled
                            )
                        }
                    }
                    AlertTier.LAST_CALL -> {
                        handleLastCall(context, notificationId, prayerType)
                    }
                }
            }

            // Refresh Glance AppWidgets, pinned status, auto-mute, and extend the 7-day rolling queue
            // inside the single WakeLock/goAsync()-guarded coroutine so Doze cannot interrupt them.
            val isFridayJumuah = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                .date.dayOfWeek == DayOfWeek.FRIDAY && prayerType == PrayerType.DHUHR
            val shouldActivateAutoMute = alertMode == AlertMode.FULL_ALARM && alertTier == AlertTier.MAIN_CALL

            CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
                try {
                    dev.hayya.core.widget.PrayerWidgetUpdater.updateAllWidgets(context)
                    PinnedPrayerStatusService.refreshIfRunning(context)

                    if (shouldActivateAutoMute || alertTier == AlertTier.MAIN_CALL) {
                        val dataStore = PreferenceDataStoreProvider.getDataStore(context)
                        val repository = UserPreferencesRepository(dataStore)
                        val fastTrackerRepository = FastTrackerRepository(dataStore)
                        val engine = PrayerCalculationEngine()
                        val scheduler = PrayerAlarmScheduler(context, engine, repository, fastTrackerRepository)

                        if (shouldActivateAutoMute) {
                            activatePrayerAutoMuteIfConfigured(context, repository, scheduler, isFridayJumuah)
                        }
                        if (alertTier == AlertTier.MAIN_CALL) {
                            scheduler.scheduleRollingAlarms()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in post-alarm background refresh", e)
                } finally {
                    if (wakeLock.isHeld) {
                        wakeLock.release()
                    }
                    pendingResult?.finish()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during prayer alarm dispatch", e)
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
            pendingResult?.finish()
        }
    }

    private fun handlePreReminder(context: Context, notificationId: Int, prayerType: PrayerType) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val isFriday = today.dayOfWeek == DayOfWeek.FRIDAY
        val displayName = prayerType.displayName(isFriday)
        val title = "$displayName in 15 Minutes"
        val message = "The window for $displayName begins shortly. Prepare for prayer."

        val notification = NotificationCompat.Builder(context, CHANNEL_PRE_REMINDER)
            .setSmallIcon(R.drawable.ic_stat_hayya)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()

        notifySafely(context, notificationId, notification)
    }

    private fun handleLastCall(context: Context, notificationId: Int, prayerType: PrayerType) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val isFriday = today.dayOfWeek == DayOfWeek.FRIDAY
        val displayName = prayerType.displayName(isFriday)
        val nextMilestone = prayerType.nextInSequence.displayName(isFriday)
        val title = "$displayName Window Ending Soon"
        val message = "20 minutes remaining before $displayName expires at $nextMilestone."

        val notification = NotificationCompat.Builder(context, CHANNEL_LAST_CALL)
            .setSmallIcon(R.drawable.ic_stat_hayya)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()

        notifySafely(context, notificationId, notification)
    }

    private fun handleGentleMainCall(
        context: Context,
        notificationId: Int,
        prayerType: PrayerType,
        iftarNotificationEnabled: Boolean = true
    ) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val isFriday = today.dayOfWeek == DayOfWeek.FRIDAY
        val displayName = prayerType.displayName(isFriday)
        val islamicDate = IslamicCalendarHelper.resolveIslamicDate(today)
        val isIftarAlert = prayerType == PrayerType.MAGHRIB && islamicDate.isRamadan && iftarNotificationEnabled

        val title = if (isIftarAlert) "🌙 Iftar Mubarak — Break Your Fast" else "Time for $displayName"
        val message = if (isIftarAlert) {
            "ذَهَبَ الظَّمَأُ وَابْتَلَّتِ الْعُرُوقُ وَثَبَتَ الأَجْرُ إِنْ شَاءَ اللَّهُ • Time to break your fast"
        } else {
            "The prayer window for $displayName has begun."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_GENTLE_MAIN_CALL)
            .setSmallIcon(R.drawable.ic_stat_hayya)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setSilent(true)
            .setAutoCancel(true)
            .build()

        notifySafely(context, notificationId, notification)
    }

    private fun handleMainCall(
        context: Context,
        notificationId: Int,
        prayerType: PrayerType,
        autoSilenceSeconds: Int,
        showPostAdhanDua: Boolean = true,
        volumeEscalationSeconds: Int = 0,
        iftarNotificationEnabled: Boolean = true
    ) {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val isFriday = today.dayOfWeek == DayOfWeek.FRIDAY
        val isFridayJumuah = isFriday && prayerType == PrayerType.DHUHR
        val displayName = prayerType.displayName(isFriday)
        val servicePrayerName = if (isFridayJumuah) "JUMU'AH" else prayerType.name

        // Build FullScreenIntent for lockscreen wake
        val fullScreenIntent = Intent(context, PrayerWakeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(PrayerWakeActivity.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(PrayerWakeActivity.EXTRA_PRAYER_TYPE, displayName)
            putExtra(PrayerWakeActivity.EXTRA_ALERT_TIER, AlertTier.MAIN_CALL.name)
            putExtra(PrayerWakeActivity.EXTRA_AUTO_SILENCE_SECONDS, autoSilenceSeconds)
            putExtra(PrayerWakeActivity.EXTRA_SHOW_POST_ADHAN_DUA, showPostAdhanDua)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Hand off to PrayerAudioService for full alarm sound stream and foreground lifecycle
        val serviceIntent = Intent(context, PrayerAudioService::class.java).apply {
            action = PrayerAudioService.ACTION_START_ALARM
            putExtra(PrayerAudioService.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(PrayerAudioService.EXTRA_PRAYER_NAME, servicePrayerName)
            putExtra(PrayerAudioService.EXTRA_TIMEOUT_SECONDS, autoSilenceSeconds)
            putExtra(PrayerAudioService.EXTRA_VOLUME_ESCALATION_SECONDS, volumeEscalationSeconds)
        }

        ContextCompat.startForegroundService(context, serviceIntent)

        val islamicDate = IslamicCalendarHelper.resolveIslamicDate(today)
        val isIftarAlert = prayerType == PrayerType.MAGHRIB && islamicDate.isRamadan && iftarNotificationEnabled

        val notificationTitle = if (isIftarAlert) {
            "🌙 Iftar Mubarak — Break Your Fast"
        } else if (isFridayJumuah) {
            "Adhan: JUMU'AH"
        } else {
            "Adhan: ${prayerType.name}"
        }

        val notificationText = if (isIftarAlert) {
            "ذَهَبَ الظَّمَأُ وَابْتَلَّتِ الْعُرُوقُ وَثَبَتَ الأَجْرُ إِنْ شَاءَ اللَّهُ • Time to break your fast"
        } else if (isFridayJumuah) {
            "Time for Jumu'ah prayer & Khutbah"
        } else {
            "Time for ${prayerType.name} prayer"
        }

        // Post immediate max-priority silent notification with fullScreenIntent
        // (Audio is played exclusively by PrayerAudioService to avoid double-playback)
        val notification = NotificationCompat.Builder(context, CHANNEL_MAIN_CALL)
            .setSmallIcon(R.drawable.ic_stat_hayya)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationText))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setSilent(true)
            .setAutoCancel(true)
            .build()

        notifySafely(context, notificationId, notification)

        // Attempt direct activity start for active/interactive app state
        try {
            context.startActivity(fullScreenIntent)
        } catch (e: Exception) {
            Log.d(TAG, "Direct wake activity launch deferred to fullScreenIntent: ${e.message}")
        }
    }

    private suspend fun activatePrayerAutoMuteIfConfigured(
        context: Context,
        repository: UserPreferencesRepository,
        scheduler: PrayerAlarmScheduler,
        isFridayJumuah: Boolean
    ) {
        try {
            val prefs = repository.userPreferencesFlow.first()
            val durationMinutes = if (isFridayJumuah) {
                prefs.fridayAutoMuteDuration.resolveMinutes(prefs.autoMuteDuration)
            } else {
                prefs.autoMuteDuration.minutes
            }

            if (durationMinutes > 0) {
                val endEpochMillis = System.currentTimeMillis() + (durationMinutes * 60_000L)
                val activated = AutoMuteManager.activateAutoMute(
                    context = context,
                    durationMinutes = durationMinutes,
                    targetMode = prefs.autoMuteTargetMode,
                    repository = repository,
                    isFridayJumuah = isFridayJumuah
                )
                if (activated) {
                    scheduler.scheduleAutoUnmute(endEpochMillis)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error activating auto-mute during main call", e)
        }
    }

    private fun notifySafely(context: Context, notificationId: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Skipping notification $notificationId: POST_NOTIFICATIONS permission not granted")
            return
        }
        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderAudioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val defaultNotificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            val preReminderChannel = NotificationChannel(
                CHANNEL_PRE_REMINDER,
                "Prayer Pre-Reminders (T - 15m)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Gentle heads-up reminders 15 minutes prior to prayer times"
                setSound(defaultNotificationSound, reminderAudioAttributes)
                enableVibration(true)
            }

            val mainCallChannel = NotificationChannel(
                CHANNEL_MAIN_CALL,
                "Prayer Main Call (Adhan)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Primary Adhan alarms and full-screen calls"
                setSound(null, null) // Audio handled exclusively by PrayerAudioService
                enableVibration(true)
                setBypassDnd(true)
            }

            val gentleMainCallChannel = NotificationChannel(
                CHANNEL_GENTLE_MAIN_CALL,
                "Prayer Gentle Banner (Silent)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Silent heads-up banner for prayers set to Gentle Banner mode"
                setSound(null, null)
                enableVibration(false)
            }

            val lastCallChannel = NotificationChannel(
                CHANNEL_LAST_CALL,
                "Prayer Last Call (T - 20m)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders 20 minutes before a prayer window concludes"
                setSound(defaultNotificationSound, reminderAudioAttributes)
                enableVibration(true)
            }

            val suhoorChannel = NotificationChannel(
                CHANNEL_SUHOOR_REMINDER,
                "Suhoor & Fasting Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Full-screen wake-up alarm and alerts before Fajr Suhoor cutoff"
                setSound(null, null) // Audio handled exclusively by PrayerAudioService
                enableVibration(true)
                setBypassDnd(true)
            }

            val eidChannel = NotificationChannel(
                CHANNEL_EID_CELEBRATION,
                "Eid Greetings & Takbirat",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Special morning celebrations and Takbirat for Eid al-Fitr and Eid al-Adha"
                setSound(defaultNotificationSound, reminderAudioAttributes)
                enableVibration(true)
            }

            val tahajjudChannel = NotificationChannel(
                CHANNEL_TAHAJJUD_REMINDER,
                "Qiyam al-Layl & Tahajjud",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Gentle pre-alarm and wake reminders for Tahajjud prayer at the Last Third of the Night"
                setSound(defaultNotificationSound, reminderAudioAttributes)
                enableVibration(true)
            }

            notificationManager.createNotificationChannels(
                listOf(preReminderChannel, mainCallChannel, gentleMainCallChannel, lastCallChannel, suhoorChannel, eidChannel, tahajjudChannel)
            )
        }
    }

    companion object {
        private const val TAG = "PrayerAlarmReceiver"
        private const val WAKELOCK_TAG = "hayya:PrayerAlarmReceiverWakeLock"

        const val CHANNEL_PRE_REMINDER = "channel_prayer_pre_reminder"
        const val CHANNEL_MAIN_CALL = "channel_prayer_main_call"
        const val CHANNEL_GENTLE_MAIN_CALL = "channel_prayer_gentle_main_call"
        const val CHANNEL_LAST_CALL = "channel_prayer_last_call"
        const val CHANNEL_SUHOOR_REMINDER = "channel_suhoor_reminder"
        const val CHANNEL_EID_CELEBRATION = "channel_eid_celebration"
        const val CHANNEL_TAHAJJUD_REMINDER = "channel_tahajjud_reminder"
    }
}
