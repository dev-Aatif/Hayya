package dev.hayya.core.system.ui

import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
 */
class PrayerWakeActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureLockscreenFlags()

        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val prayerType = intent.getStringExtra(EXTRA_PRAYER_TYPE) ?: "Prayer"
        val alertTier = intent.getStringExtra(EXTRA_ALERT_TIER) ?: "MAIN_CALL"

        setContent {
            HayyaTheme {
                PrayerWakeScreen(
                    prayerName = prayerType,
                    windowExpiryText = "Calling to $prayerType prayer",
                    autoSilenceSeconds = 60,
                    onDismiss = { stopAlarm() },
                    onMute = { muteAlarm() }
                )
            }
        }
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
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_PRAYER_TYPE = "extra_prayer_type"
        const val EXTRA_ALERT_TIER = "extra_alert_tier"
    }
}
