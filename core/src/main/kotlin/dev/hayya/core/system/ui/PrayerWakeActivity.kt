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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
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

    private var currentNotificationId by mutableIntStateOf(-1)
    private var prayerTypeState by mutableStateOf("Prayer")
    private var subtitleState by mutableStateOf("")
    private var autoSilenceSecondsState by mutableIntStateOf(PrayerAudioService.DEFAULT_TIMEOUT_SECONDS)
    private var showPostAdhanDuaState by mutableStateOf(true)
    private var isDuaSheetOpen = false
    private var pendingFinishAfterDua = false

    private var receiverRegistered = false
    private val playbackFinishedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == PrayerAudioService.ACTION_PLAYBACK_FINISHED) {
                val finishedNotificationId = intent.getIntExtra(
                    PrayerAudioService.EXTRA_NOTIFICATION_ID,
                    -1
                )
                if (finishedNotificationId != -1 &&
                    currentNotificationId != -1 &&
                    finishedNotificationId != currentNotificationId
                ) {
                    return
                }
                cancelWakeNotification()
                if (isDuaSheetOpen) {
                    pendingFinishAfterDua = true
                } else {
                    finish()
                }
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

        applyIntentExtras(intent)

        setContent {
            HayyaTheme {
                val prayerType = prayerTypeState
                val resolvedExpiryText = subtitleState.ifBlank {
                    when {
                        prayerType.equals("JUMU'AH", ignoreCase = true) -> "Calling to Jumu'ah prayer"
                        prayerType.equals("SUHOOR", ignoreCase = true) -> "Eat Suhoor & make Niyyah before Fajr"
                        else -> "Calling to $prayerType prayer"
                    }
                }
                androidx.compose.runtime.key(currentNotificationId, prayerType) {
                    PrayerWakeScreen(
                        prayerName = prayerType,
                        windowExpiryText = resolvedExpiryText,
                        autoSilenceSeconds = autoSilenceSecondsState,
                        showPostAdhanDua = showPostAdhanDuaState,
                        onDismiss = { stopAlarm() },
                        onMute = { muteAlarm() },
                        onDuaSheetVisibilityChange = { isOpen ->
                            isDuaSheetOpen = isOpen
                            if (!isOpen && pendingFinishAfterDua) {
                                pendingFinishAfterDua = false
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        applyIntentExtras(intent)
    }

    private fun applyIntentExtras(intent: Intent?) {
        if (intent == null) return
        currentNotificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        prayerTypeState = intent.getStringExtra(EXTRA_PRAYER_TYPE) ?: "Prayer"
        subtitleState = intent.getStringExtra(EXTRA_SUBTITLE).orEmpty()
        autoSilenceSecondsState = intent.getIntExtra(
            EXTRA_AUTO_SILENCE_SECONDS,
            PrayerAudioService.DEFAULT_TIMEOUT_SECONDS
        )
        showPostAdhanDuaState = intent.getBooleanExtra(EXTRA_SHOW_POST_ADHAN_DUA, true)
    }

    private fun cancelWakeNotification() {
        if (currentNotificationId != -1) {
            runCatching {
                NotificationManagerCompat.from(this).cancel(currentNotificationId)
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
        cancelWakeNotification()
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
        }

        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )
    }

    companion object {
        const val EXTRA_NOTIFICATION_ID = "extra_notification_id"
        const val EXTRA_PRAYER_TYPE = "extra_prayer_type"
        const val EXTRA_SUBTITLE = "extra_subtitle"
        const val EXTRA_ALERT_TIER = "extra_alert_tier"
        const val EXTRA_AUTO_SILENCE_SECONDS = "extra_auto_silence_seconds"
        const val EXTRA_SHOW_POST_ADHAN_DUA = "extra_show_post_adhan_dua"
    }
}
