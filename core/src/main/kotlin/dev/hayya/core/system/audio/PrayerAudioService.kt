package dev.hayya.core.system.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import dev.hayya.core.R
import dev.hayya.core.system.ui.PrayerWakeActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service managing playback of prayer calls (Adhan).
 *
 * Audio is strictly routed through [AudioAttributes.USAGE_ALARM] and
 * [AudioAttributes.CONTENT_TYPE_SONIFICATION], bypassing hardware silent switches
 * and standard Android Do Not Disturb (DND) profiles.
 *
 * Implements an automated coroutine fade-out loop: when `timeoutSeconds` expires,
 * volume smoothly decreases over 2000ms before self-terminating.
 */
class PrayerAudioService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var fadeOutJob: Job? = null
    private var escalationJob: Job? = null

    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ensureNotificationChannel()

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "hayya:PrayerAudioServiceWakeLock"
        ).apply {
            setReferenceCounted(false)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_ALARM

        when (action) {
            ACTION_START_ALARM -> {
                val soundUriString = intent?.getStringExtra(EXTRA_SOUND_URI)
                val timeoutSeconds = intent?.getIntExtra(EXTRA_TIMEOUT_SECONDS, DEFAULT_TIMEOUT_SECONDS)
                    ?: DEFAULT_TIMEOUT_SECONDS
                val escalationSeconds = intent?.getIntExtra(EXTRA_VOLUME_ESCALATION_SECONDS, DEFAULT_ESCALATION_SECONDS)
                    ?: DEFAULT_ESCALATION_SECONDS
                val prayerName = intent?.getStringExtra(EXTRA_PRAYER_NAME) ?: "Prayer"
                val notificationId = intent?.getIntExtra(EXTRA_NOTIFICATION_ID, DEFAULT_NOTIFICATION_ID)
                    ?: DEFAULT_NOTIFICATION_ID

                startForegroundServiceWithNotification(notificationId, prayerName)
                startPlayback(soundUriString, timeoutSeconds, escalationSeconds)
            }
            ACTION_MUTE -> {
                mutePlayback()
            }
            ACTION_STOP -> {
                stopPlaybackAndSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startPlayback(soundUriString: String?, timeoutSeconds: Int, escalationSeconds: Int = 0) {
        stopPlaybackOnly()

        // Acquire WakeLock for continuous audio playback through device deep sleep
        wakeLock?.acquire(300_000L) // 5-minute safety threshold

        val soundUri = soundUriString?.let { Uri.parse(it) }
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: Settings.System.DEFAULT_ALARM_ALERT_URI

        if (soundUri == null) {
            Log.e(TAG, "No default alarm or notification sound URI available on device")
            stopPlaybackAndSelf()
            return
        }

        try {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(audioAttributes)
                setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
                setDataSource(applicationContext, soundUri)
                // Loop short system alarm chimes while an auto-silence timeout is active so
                // playback does not terminate prematurely and close PrayerWakeActivity.
                isLooping = timeoutSeconds > 0
                setOnCompletionListener {
                    Log.i(TAG, "Audio playback completed naturally")
                    stopPlaybackAndSelf()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what, extra=$extra")
                    stopPlaybackAndSelf()
                    true
                }
                setOnPreparedListener { player ->
                    if (escalationSeconds > 0) {
                        player.setVolume(0.10f, 0.10f)
                        player.start()
                        scheduleVolumeEscalation(escalationSeconds)
                    } else {
                        player.setVolume(1.0f, 1.0f)
                        player.start()
                    }
                    scheduleFadeOut(timeoutSeconds)
                    Log.i(TAG, "Started prayer audio playback with timeout ${timeoutSeconds}s, escalation ${escalationSeconds}s")
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize MediaPlayer for URI: $soundUri", e)
            stopPlaybackAndSelf()
        }
    }

    private fun scheduleVolumeEscalation(escalationSeconds: Int) {
        escalationJob?.cancel()
        if (escalationSeconds <= 0) return

        escalationJob = serviceScope.launch {
            val initialVolume = 0.10f
            val targetVolume = 1.0f
            val stepIntervalMs = 1500L
            val totalDurationMs = escalationSeconds * 1000L
            val totalSteps = (totalDurationMs / stepIntervalMs).toInt().coerceAtLeast(1)
            val volumeStep = (targetVolume - initialVolume) / totalSteps

            for (step in 1..totalSteps) {
                delay(stepIntervalMs)
                val currentVol = (initialVolume + step * volumeStep).coerceAtMost(targetVolume)
                try {
                    mediaPlayer?.setVolume(currentVol, currentVol)
                } catch (e: Exception) {
                    break
                }
            }
            try {
                mediaPlayer?.setVolume(targetVolume, targetVolume)
            } catch (_: Exception) {}
        }
    }

    private fun scheduleFadeOut(timeoutSeconds: Int) {
        fadeOutJob?.cancel()

        // If timeout is <= 0 or null, playback runs until full audio completes
        if (timeoutSeconds <= 0) return

        fadeOutJob = serviceScope.launch {
            // Play at full volume until 2 seconds before the timeout
            val fullVolumeDurationMs = ((timeoutSeconds.coerceAtLeast(3) - 2) * 1000L)
            delay(fullVolumeDurationMs)

            // Smooth 20-step volume fade-out over 2000 milliseconds
            val fadeSteps = 20
            val stepDelayMs = 100L
            for (step in 1..fadeSteps) {
                val currentVol = 1.0f - (step.toFloat() / fadeSteps)
                try {
                    mediaPlayer?.setVolume(currentVol, currentVol)
                } catch (e: Exception) {
                    break
                }
                delay(stepDelayMs)
            }

            Log.i(TAG, "Automated fade-out completed; terminating service")
            stopPlaybackAndSelf()
        }
    }

    private fun mutePlayback() {
        escalationJob?.cancel()
        escalationJob = null
        try {
            mediaPlayer?.setVolume(0.0f, 0.0f)
            Log.i(TAG, "Playback muted by user request")
        } catch (e: Exception) {
            Log.e(TAG, "Error muting playback", e)
        }
    }

    private fun stopPlaybackOnly() {
        escalationJob?.cancel()
        escalationJob = null
        fadeOutJob?.cancel()
        fadeOutJob = null

        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.reset()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing MediaPlayer", e)
        } finally {
            mediaPlayer = null
        }
    }

    private fun stopPlaybackAndSelf() {
        stopPlaybackOnly()

        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }

        val finishedBroadcast = Intent(ACTION_PLAYBACK_FINISHED).apply {
            setPackage(packageName)
        }
        sendBroadcast(finishedBroadcast)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundServiceWithNotification(notificationId: Int, prayerName: String) {
        val notification = buildForegroundNotification(notificationId, prayerName)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                notificationId,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(notificationId, notification)
        }
    }

    private fun buildForegroundNotification(notificationId: Int, prayerName: String): Notification {
        val stopIntent = Intent(this, PrayerAudioService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            notificationId + 1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val muteIntent = Intent(this, PrayerAudioService::class.java).apply {
            action = ACTION_MUTE
        }
        val mutePendingIntent = PendingIntent.getService(
            this,
            notificationId + 2,
            muteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(this, PrayerWakeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(PrayerWakeActivity.EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(PrayerWakeActivity.EXTRA_PRAYER_TYPE, prayerName)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (prayerName.equals("Suhoor", ignoreCase = true)) {
            "Suhoor Wake-Up"
        } else {
            "Adhan: $prayerName"
        }
        val text = if (prayerName.equals("Suhoor", ignoreCase = true)) {
            "Eat Suhoor & make Niyyah before Fajr"
        } else {
            "Calling to $prayerName prayer"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_hayya)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .addAction(android.R.drawable.ic_lock_silent_mode, "Mute", mutePendingIntent)
            .build()
    }

    private fun ensureNotificationChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Adhan Audio Playback",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Foreground playback service for active prayer call"
            setSound(null, null) // Audio is managed directly by MediaPlayer
            enableVibration(false)
        }
        notificationManager.createNotificationChannel(channel)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlaybackOnly()
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
        }
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "PrayerAudioService"

        const val CHANNEL_ID = "channel_prayer_audio_service"

        const val ACTION_START_ALARM = "dev.hayya.audio.ACTION_START_ALARM"
        const val ACTION_MUTE = "dev.hayya.audio.ACTION_MUTE"
        const val ACTION_STOP = "dev.hayya.audio.ACTION_STOP"
        const val ACTION_PLAYBACK_FINISHED = "dev.hayya.audio.ACTION_PLAYBACK_FINISHED"

        const val EXTRA_SOUND_URI = "extra_sound_uri"
        const val EXTRA_TIMEOUT_SECONDS = "extra_timeout_seconds"
        const val EXTRA_VOLUME_ESCALATION_SECONDS = "extra_volume_escalation_seconds"
        const val EXTRA_PRAYER_NAME = "extra_prayer_name"
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

        const val DEFAULT_TIMEOUT_SECONDS = 60
        const val DEFAULT_ESCALATION_SECONDS = 0
        const val DEFAULT_NOTIFICATION_ID = 99999
    }
}
