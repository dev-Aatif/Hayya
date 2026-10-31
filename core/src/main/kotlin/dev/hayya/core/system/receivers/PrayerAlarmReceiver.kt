package dev.hayya.core.system.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dev.hayya.core.domain.model.AlertTier
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.system.alarm.PrayerAlarmScheduler
import dev.hayya.core.system.audio.PrayerAudioService
import dev.hayya.core.system.ui.PrayerWakeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKELOCK_TAG
        ).apply {
            setReferenceCounted(false)
            acquire(10_000L) // 10-second safety timeout
        }

        try {
            ensureNotificationChannels(context)

            val notificationId = intent.getIntExtra(PrayerAlarmScheduler.EXTRA_NOTIFICATION_ID, -1)
            val prayerTypeName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TYPE)
            val alertTierName = intent.getStringExtra(PrayerAlarmScheduler.EXTRA_ALERT_TIER)

            val prayerType = prayerTypeName?.let {
                runCatching { PrayerType.valueOf(it) }.getOrNull()
            } ?: PrayerType.FAJR

            val alertTier = alertTierName?.let {
                runCatching { AlertTier.valueOf(it) }.getOrNull()
            } ?: AlertTier.MAIN_CALL

            Log.i(TAG, "Triggered prayer alarm: $prayerType, Tier: $alertTier, ID: $notificationId")

            when (alertTier) {
                AlertTier.PRE_REMINDER -> {
                    handlePreReminder(context, notificationId, prayerType)
                }
                AlertTier.MAIN_CALL -> {
                    handleMainCall(context, notificationId, prayerType)
                }
                AlertTier.LAST_CALL -> {
                    handleLastCall(context, notificationId, prayerType)
                }
            }

            // Instantly refresh Glance AppWidgets when any prayer transition occurs
            CoroutineScope(Dispatchers.Default).launch {
                dev.hayya.core.widget.PrayerWidgetUpdater.updateAllWidgets(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during prayer alarm dispatch", e)
        } finally {
            if (wakeLock.isHeld) {
                wakeLock.release()
            }
        }
    }

    private fun handlePreReminder(context: Context, notificationId: Int, prayerType: PrayerType) {
        val title = "${prayerType.name} in 15 Minutes"
        val message = "The window for ${prayerType.name} begins shortly. Prepare for prayer."

        val notification = NotificationCompat.Builder(context, CHANNEL_PRE_REMINDER)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun handleLastCall(context: Context, notificationId: Int, prayerType: PrayerType) {
        val nextMilestone = prayerType.nextInSequence.name
        val title = "${prayerType.name} Window Ending Soon"
        val message = "20 minutes remaining before ${prayerType.name} expires at $nextMilestone."

        val notification = NotificationCompat.Builder(context, CHANNEL_LAST_CALL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun handleMainCall(context: Context, notificationId: Int, prayerType: PrayerType) {
        // Build FullScreenIntent for lockscreen wake
        val fullScreenIntent = Intent(context, PrayerWakeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(PrayerWakeActivity.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(PrayerWakeActivity.EXTRA_PRAYER_TYPE, prayerType.name)
            putExtra(PrayerWakeActivity.EXTRA_ALERT_TIER, AlertTier.MAIN_CALL.name)
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
            putExtra(PrayerAudioService.EXTRA_PRAYER_NAME, prayerType.name)
        }

        ContextCompat.startForegroundService(context, serviceIntent)

        // Also post immediate max-priority notification with fullScreenIntent
        val notification = NotificationCompat.Builder(context, CHANNEL_MAIN_CALL)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Adhan: ${prayerType.name}")
            .setContentText("Time for ${prayerType.name} prayer")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(notificationId, notification)
    }

    private fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val reminderAudioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val alarmAudioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val defaultNotificationSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val defaultAlarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

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
                setSound(defaultAlarmSound, alarmAudioAttributes)
                enableVibration(true)
                setBypassDnd(true)
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

            notificationManager.createNotificationChannels(
                listOf(preReminderChannel, mainCallChannel, lastCallChannel)
            )
        }
    }

    companion object {
        private const val TAG = "PrayerAlarmReceiver"
        private const val WAKELOCK_TAG = "hayya:PrayerAlarmReceiverWakeLock"

        const val CHANNEL_PRE_REMINDER = "channel_prayer_pre_reminder"
        const val CHANNEL_MAIN_CALL = "channel_prayer_main_call"
        const val CHANNEL_LAST_CALL = "channel_prayer_last_call"
    }
}
