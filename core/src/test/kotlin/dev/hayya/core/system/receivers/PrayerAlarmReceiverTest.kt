package dev.hayya.core.system.receivers

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.common.truth.Truth.assertThat
import dev.hayya.core.domain.model.AlertTier
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.system.alarm.PrayerAlarmScheduler
import dev.hayya.core.system.audio.PrayerAudioService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test

class PrayerAlarmReceiverTest {

    private val context = mockk<Context>(relaxed = true)
    private val powerManager = mockk<PowerManager>(relaxed = true)
    private val wakeLock = mockk<PowerManager.WakeLock>(relaxed = true)
    private val notificationManager = mockk<NotificationManager>(relaxed = true)
    private val notificationManagerCompat = mockk<NotificationManagerCompat>(relaxed = true)

    private lateinit var receiver: PrayerAlarmReceiver

    @Before
    fun setUp() {
        mockkStatic(NotificationManagerCompat::class)
        mockkStatic(ContextCompat::class)
        every { NotificationManagerCompat.from(any()) } returns notificationManagerCompat
        every { ContextCompat.startForegroundService(any(), any()) } returns null
        every { context.getSystemService(Context.POWER_SERVICE) } returns powerManager
        every { context.getSystemService(Context.NOTIFICATION_SERVICE) } returns notificationManager
        every { powerManager.newWakeLock(any(), any()) } returns wakeLock

        receiver = PrayerAlarmReceiver()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun onReceive_acquiresAndReleasesWakeLockSafely() {
        every { wakeLock.isHeld } returns true

        val intent = Intent(PrayerAlarmScheduler.ACTION_TRIGGER_PRAYER_ALARM).apply {
            putExtra(PrayerAlarmScheduler.EXTRA_NOTIFICATION_ID, 100)
            putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TYPE, PrayerType.FAJR.name)
            putExtra(PrayerAlarmScheduler.EXTRA_ALERT_TIER, AlertTier.PRE_REMINDER.name)
        }

        receiver.onReceive(context, intent)

        verify(exactly = 1) {
            wakeLock.acquire(10_000L) // 10-second safety timeout
        }
        verify(exactly = 1) {
            wakeLock.release()
        }
    }

    @Test
    fun onReceive_tier1PreReminder_postsHeadsUpNotification() {
        val testId = 10100
        val intent = Intent(PrayerAlarmScheduler.ACTION_TRIGGER_PRAYER_ALARM).apply {
            putExtra(PrayerAlarmScheduler.EXTRA_NOTIFICATION_ID, testId)
            putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TYPE, PrayerType.DHUHR.name)
            putExtra(PrayerAlarmScheduler.EXTRA_ALERT_TIER, AlertTier.PRE_REMINDER.name)
        }

        receiver.onReceive(context, intent)

        verify(exactly = 1) {
            notificationManagerCompat.notify(testId, any())
        }

        // Tier 1 does not start the audio service
        verify(exactly = 0) {
            context.startForegroundService(any())
        }
    }

    @Test
    fun onReceive_tier3LastCall_postsHeadsUpNotification() {
        val testId = 10102
        val intent = Intent(PrayerAlarmScheduler.ACTION_TRIGGER_PRAYER_ALARM).apply {
            putExtra(PrayerAlarmScheduler.EXTRA_NOTIFICATION_ID, testId)
            putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TYPE, PrayerType.ASR.name)
            putExtra(PrayerAlarmScheduler.EXTRA_ALERT_TIER, AlertTier.LAST_CALL.name)
        }

        receiver.onReceive(context, intent)

        verify(exactly = 1) {
            notificationManagerCompat.notify(testId, any())
        }

        // Tier 3 does not start the audio service
        verify(exactly = 0) {
            context.startForegroundService(any())
        }
    }

    @Test
    fun onReceive_tier2MainCall_startsPrayerAudioServiceAndFullScreenNotification() {
        val testId = 10101
        val serviceIntentSlot = slot<Intent>()

        every {
            ContextCompat.startForegroundService(context, capture(serviceIntentSlot))
        } returns null

        val intent = Intent(PrayerAlarmScheduler.ACTION_TRIGGER_PRAYER_ALARM).apply {
            putExtra(PrayerAlarmScheduler.EXTRA_NOTIFICATION_ID, testId)
            putExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TYPE, PrayerType.MAGHRIB.name)
            putExtra(PrayerAlarmScheduler.EXTRA_ALERT_TIER, AlertTier.MAIN_CALL.name)
        }

        receiver.onReceive(context, intent)

        verify(atLeast = 1) {
            ContextCompat.startForegroundService(context, any())
        }

        assertThat(serviceIntentSlot.captured.action).isEqualTo(PrayerAudioService.ACTION_START_ALARM)
        assertThat(serviceIntentSlot.captured.getIntExtra(PrayerAudioService.EXTRA_NOTIFICATION_ID, -1))
            .isEqualTo(testId)
        assertThat(serviceIntentSlot.captured.getStringExtra(PrayerAudioService.EXTRA_PRAYER_NAME))
            .isEqualTo(PrayerType.MAGHRIB.name)

        verify(exactly = 1) {
            notificationManagerCompat.notify(testId, any())
        }
    }
}
