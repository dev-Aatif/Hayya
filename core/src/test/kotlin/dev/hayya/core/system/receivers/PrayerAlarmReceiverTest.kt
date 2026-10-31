package dev.hayya.core.system.receivers

import android.app.PendingIntent
import android.media.RingtoneManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.common.truth.Truth.assertThat
import dev.hayya.core.domain.model.AlertTier
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.system.alarm.PrayerAlarmScheduler
import dev.hayya.core.system.audio.PrayerAudioService
import io.mockk.*
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
        mockkConstructor(NotificationCompat.Builder::class)
        every { anyConstructed<NotificationCompat.Builder>().build() } returns mockk(relaxed = true)
        mockkStatic(NotificationManagerCompat::class)
        mockkStatic(ContextCompat::class)
        mockkStatic(PendingIntent::class)
        mockkStatic(RingtoneManager::class)
        every { NotificationManagerCompat.from(any()) } returns notificationManagerCompat
        every { ContextCompat.startForegroundService(any(), any()) } returns Unit
        every { PendingIntent.getActivity(any(), any(), any(), any()) } returns mockk(relaxed = true)
        every { RingtoneManager.getDefaultUri(any()) } returns mockk(relaxed = true)
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
        val intent = mockk<Intent>(relaxed = true) {
            every { action } returns PrayerAlarmScheduler.ACTION_TRIGGER_PRAYER_ALARM
            every { getIntExtra(PrayerAlarmScheduler.EXTRA_NOTIFICATION_ID, -1) } returns testId
            every { getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TYPE) } returns PrayerType.DHUHR.name
            every { getStringExtra(PrayerAlarmScheduler.EXTRA_ALERT_TIER) } returns AlertTier.PRE_REMINDER.name
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
        val intent = mockk<Intent>(relaxed = true) {
            every { action } returns PrayerAlarmScheduler.ACTION_TRIGGER_PRAYER_ALARM
            every { getIntExtra(PrayerAlarmScheduler.EXTRA_NOTIFICATION_ID, -1) } returns testId
            every { getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TYPE) } returns PrayerType.ASR.name
            every { getStringExtra(PrayerAlarmScheduler.EXTRA_ALERT_TIER) } returns AlertTier.LAST_CALL.name
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
        } returns Unit

        val intent = mockk<Intent>(relaxed = true) {
            every { action } returns PrayerAlarmScheduler.ACTION_TRIGGER_PRAYER_ALARM
            every { getIntExtra(PrayerAlarmScheduler.EXTRA_NOTIFICATION_ID, -1) } returns testId
            every { getStringExtra(PrayerAlarmScheduler.EXTRA_PRAYER_TYPE) } returns PrayerType.MAGHRIB.name
            every { getStringExtra(PrayerAlarmScheduler.EXTRA_ALERT_TIER) } returns AlertTier.MAIN_CALL.name
        }

        receiver.onReceive(context, intent)

        verify(atLeast = 1) {
            ContextCompat.startForegroundService(context, any())
        }

        verify(exactly = 1) {
            notificationManagerCompat.notify(testId, any())
        }
    }
}
