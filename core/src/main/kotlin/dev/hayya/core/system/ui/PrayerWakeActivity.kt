package dev.hayya.core.system.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.content.ContextCompat
import dev.hayya.core.system.audio.PrayerAudioService
import dev.hayya.core.ui.screens.PrayerWakeScreen
import dev.hayya.core.ui.theme.HayyaTheme

/**
 * Lockscreen FullScreenIntent activity hosting [PrayerWakeScreen].
 *
 * Configured with Android window flags and lifecycle APIs to:
 * 1. Wake the display hardware (`setTurnScreenOn(true)`).
 * 2. Display over secure lockscreens without requiring device unlock (`setShowWhenLocked(true)`).
 * 3. Provide tactile dismissal to stop [PrayerAudioService] and dismiss the lockscreen.
 * 4. Automatically finish when audio playback completes or auto-silence expires so the
 *    display never remains awake unattended.
 */
class PrayerWakeActivity : ComponentActivity() {

    private var receiverRegistered = false
    private val playbackFinishedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == PrayerAudioService.ACTION_PLAYBACK_FINISHED) {
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        configureLockscreenFlags()
        super.onCreate(savedInstanceState)

        ContextCompat.registerReceiver(
            this,
            playbackFinishedReceiver,
            IntentFilter(PrayerAudioService.ACTION_PLAYBACK_FINISHED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        receiverRegistered = true

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val prayerType = intent.getStringExtra(EXTRA_PRAYER_TYPE) ?: "Prayer"
        val alertTier = intent.getStringExtra(EXTRA_ALERT_TIER) ?: "MAIN_CALL"
        val autoSilenceSeconds = intent.getIntExtra(
            EXTRA_AUTO_SILENCE_SECONDS,
            PrayerAudioService.DEFAULT_TIMEOUT_SECONDS
        )
        val showPostAdhanDua = intent.getBooleanExtra(EXTRA_SHOW_POST_ADHAN_DUA, true)

        setContent {
            HayyaTheme {
                PrayerWakeScreen(
                    prayerName = prayerType,
                    windowExpiryText = if (prayerType.equals("JUMU'AH", ignoreCase = true)) {
                        "Calling to Jumu'ah prayer"
                    } else {
                        "Calling to $prayerType prayer"
                    },
                    autoSilenceSeconds = autoSilenceSeconds,
                    showPostAdhanDua = showPostAdhanDua,
                    onDismiss = { stopAlarm() },
                    onMute = { muteAlarm() }
                )
            }
        }
    }

    override fun onDestroy() {
        if (receiverRegistered) {
            runCatching { unregisterReceiver(playbackFinishedReceiver) }
            receiverRegistered = false
        }
        super.onDestroy()
    }

    /**
     * Mutes or stops the currently active prayer audio service and finishes this activity.
     */
    fun stopAlarm() {
        val stopIntent = Intent(this, PrayerAudioService::class.java).apply {
            action = PrayerAudioService.ACTION_STOP
        }
        startService(stopIntent)
        finish()
    }

    /**
     * Mutes ongoing prayer audio playback without terminating the screen.
     */
    fun muteAlarm() {
        val muteIntent = Intent(this, PrayerAudioService::class.java).apply {
            action = PrayerAudioService.ACTION_MUTE
        }
        startService(muteIntent)
    }

    private fun configureLockscreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        }

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
        )
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_PRAYER_TYPE = "extra_prayer_type"
        const val EXTRA_ALERT_TIER = "extra_alert_tier"
        const val EXTRA_AUTO_SILENCE_SECONDS = "extra_auto_silence_seconds"
        const val EXTRA_SHOW_POST_ADHAN_DUA = "extra_show_post_adhan_dua"
    }
}
