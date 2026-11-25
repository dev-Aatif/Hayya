package dev.hayya.core.system.audio

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dev.hayya.core.R
import dev.hayya.core.data.preferences.UserPreferencesRepository
import dev.hayya.core.domain.model.AutoMuteTargetMode
import dev.hayya.core.system.alarm.PrayerAlarmScheduler
import dev.hayya.core.system.receivers.PrayerAlarmReceiver
import dev.hayya.core.ui.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Manages automated system ringer muting during congregational prayer (Salah)
 * in the mosque, and guarantees exact restoration of previous volume and ringer mode.
 *
 * Execution contract:
 * 1. Captures existing system ringer mode and stream volumes (STREAM_RING, STREAM_NOTIFICATION).
 * 2. Toggles system ringer to [AutoMuteTargetMode.SILENT] or [AutoMuteTargetMode.VIBRATE].
 * 3. Handles Android Do Not Disturb (DND) / Notification Policy permissions gracefully.
 * 4. Displays an ongoing status notification indicating active mute and scheduled restore time.
 * 5. Restores exact previous ringer mode and volume levels upon window completion.
 */
object AutoMuteManager {

    private const val TAG = "AutoMuteManager"
    const val CHANNEL_ID = "channel_salah_auto_mute"
    const val NOTIFICATION_ID = 88002

    /**
     * Checks if the application has been granted Do Not Disturb (Notification Policy) access.
     */
    fun hasNotificationPolicyAccess(context: Context): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return notificationManager?.isNotificationPolicyAccessGranted == true
    }

    /**
     * Opens system settings allowing the user to grant Do Not Disturb access.
     */
    fun openNotificationPolicySettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Notification Policy Access settings", e)
        }
    }

    /**
     * Activates Auto-Mute: records current ringer mode and volume levels,
     * switches ringer mode to target (Silent or Vibrate), and posts status notification.
     */
    suspend fun activateAutoMute(
        context: Context,
        durationMinutes: Int,
        targetMode: AutoMuteTargetMode,
        repository: UserPreferencesRepository,
        isFridayJumuah: Boolean = false
    ): Boolean {
        if (durationMinutes <= 0) return false
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        val endEpochMillis = System.currentTimeMillis() + (durationMinutes * 60_000L)
        val existingState = runCatching { repository.savedRingerStateFlow.firstOrNull() }.getOrNull()

        // Preserve original pre-mute ringer state if already auto-muted so re-triggering
        // (e.g. overlapping prayers or repeated Khutbah mute taps) never overwrites normal volumes.
        val ringerModeToSave = if (existingState?.isAutoMuted == true) {
            existingState.ringerMode
        } else {
            audioManager.ringerMode
        }
        val ringVolumeToSave = if (existingState?.isAutoMuted == true) {
            existingState.ringVolume
        } else {
            audioManager.getStreamVolume(AudioManager.STREAM_RING)
        }
        val notifVolumeToSave = if (existingState?.isAutoMuted == true) {
            existingState.notificationVolume
        } else {
            audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        }

        // Save state to DataStore
        repository.saveRingerState(
            ringerMode = ringerModeToSave,
            ringVolume = ringVolumeToSave,
            notificationVolume = notifVolumeToSave,
            endEpochMillis = endEpochMillis
        )

        // Switch to target ringer mode
        try {
            when (targetMode) {
                AutoMuteTargetMode.SILENT -> {
                    if (hasNotificationPolicyAccess(context)) {
                        audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    } else {
                        // Fallback to vibrate if DND permission is missing
                        audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                    }
                }
                AutoMuteTargetMode.VIBRATE -> {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Missing notification policy access for silent mode, falling back to vibrate", e)
            runCatching { audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error setting ringer mode", e)
        }

        // Post status notification
        postAutoMuteNotification(context, notificationManager, endEpochMillis, isFridayJumuah)
        return true
    }

    /**
     * Restores the exact previous system ringer mode and stream volumes upon window completion.
     */
    suspend fun restoreRingerMode(
        context: Context,
        repository: UserPreferencesRepository
    ): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        val savedState = repository.savedRingerStateFlow.first()
        if (!savedState.isAutoMuted) {
            Log.d(TAG, "No active auto-mute state to restore")
            return false
        }

        try {
            audioManager.ringerMode = savedState.ringerMode
            if (savedState.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                audioManager.setStreamVolume(AudioManager.STREAM_RING, savedState.ringVolume, 0)
                audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, savedState.notificationVolume, 0)
            }
            Log.i(TAG, "Successfully restored ringer mode: ${savedState.ringerMode}, ringVolume: ${savedState.ringVolume}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore ringer mode or volumes", e)
        }

        // Dismiss status notification
        notificationManager?.cancel(NOTIFICATION_ID)

        // Clear stored auto-mute state
        repository.clearAutoMuteState()
        return true
    }

    private fun postAutoMuteNotification(
        context: Context,
        notificationManager: NotificationManager?,
        endEpochMillis: Long,
        isFridayJumuah: Boolean = false
    ) {
        if (notificationManager == null) return
        ensureNotificationChannel(context, notificationManager)

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val formattedEndTime = timeFormat.format(Date(endEpochMillis))

        val restoreIntent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            action = PrayerAlarmScheduler.ACTION_RESTORE_RINGER_MODE
        }
        val restorePendingIntent = PendingIntent.getBroadcast(
            context,
            PrayerAlarmScheduler.NOTIFICATION_ID_AUTO_UNMUTE,
            restoreIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainActivityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (isFridayJumuah) "Jumu'ah Khutbah Auto-Mute Active" else "Salah Auto-Mute Active"
        val message = if (isFridayJumuah) {
            "Phone silenced until $formattedEndTime • Restoring normal mode after Jumu'ah"
        } else {
            "Phone silenced until $formattedEndTime • Restoring normal mode"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_hayya)
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(mainPendingIntent)
            .addAction(0, "Restore Normal Mode Now", restorePendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Skipping auto-mute notification: POST_NOTIFICATIONS permission not granted")
            return
        }

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun ensureNotificationChannel(context: Context, notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Congregational Prayer Auto-Mute",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows active auto-mute status with quick restore action during congregational Salah"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        notificationManager.createNotificationChannel(channel)
    }
}
