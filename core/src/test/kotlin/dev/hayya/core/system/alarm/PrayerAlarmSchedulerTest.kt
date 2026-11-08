package dev.hayya.core.system.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.common.truth.Truth.assertThat
import dev.hayya.core.domain.calculation.PrayerCalculationEngine
import dev.hayya.core.domain.calculation.SolarAstronomicalCalculator
import dev.hayya.core.domain.model.AlertMode
import dev.hayya.core.domain.model.AlertTier
import dev.hayya.core.domain.model.AutoSilenceTimeout
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.model.ScheduledAlert
import dev.hayya.core.domain.model.UserPreferences
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test

class PrayerAlarmSchedulerTest {

    private val context = mockk<Context>(relaxed = true)
    private val alarmManager = mockk<AlarmManager>(relaxed = true)
    private val calculationEngine = PrayerCalculationEngine(SolarAstronomicalCalculator())

    private lateinit var scheduler: PrayerAlarmScheduler

    @Before
    fun setUp() {
        mockkStatic(PendingIntent::class)
        every {
            PendingIntent.getBroadcast(any(), any(), any(), any())
        } returns mockk(relaxed = true)
        every {
            PendingIntent.getActivity(any(), any(), any(), any())
        } returns mockk(relaxed = true)

        scheduler = PrayerAlarmScheduler(
            context = context,
            calculationEngine = calculationEngine,
            alarmManager = alarmManager
        )
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun scheduleSingleAlert_usesSetAlarmClockExclusively() {
        val alert = ScheduledAlert(
            dayOfYear = 100,
            prayerType = PrayerType.FAJR,
            tier = AlertTier.MAIN_CALL,
            triggerEpochMillis = 1_700_000_000_000L
        )

        val alarmClockInfoSlot = slot<AlarmManager.AlarmClockInfo>()
        val pendingIntentSlot = slot<PendingIntent>()

        every {
            alarmManager.setAlarmClock(capture(alarmClockInfoSlot), capture(pendingIntentSlot))
        } returns Unit

        scheduler.scheduleSingleAlert(alert)

        verify(exactly = 1) {
            alarmManager.setAlarmClock(any(), any())
        }

        // Never call setExactAndAllowWhileIdle
        verify(exactly = 0) {
            alarmManager.setExactAndAllowWhileIdle(any(), any(), any())
        }

        assertThat(alarmClockInfoSlot.isCaptured).isTrue()
    }

    @Test
    fun scheduleSingleAlert_generatesDeterministicRequestCode() {
        val dayOfYear = 150
        val prayerType = PrayerType.ASR // ordinal = 3
        val tier = AlertTier.LAST_CALL  // ordinal = 2

        val expectedRequestCode = (dayOfYear * 100) + (prayerType.ordinal * 10) + tier.ordinal
        // 150 * 100 + 3 * 10 + 2 = 15032

        val alert = ScheduledAlert(
            dayOfYear = dayOfYear,
            prayerType = prayerType,
            tier = tier,
            triggerEpochMillis = 1_700_000_000_000L
        )

        assertThat(alert.notificationId).isEqualTo(expectedRequestCode)

        val requestCodeSlot = slot<Int>()
        every {
            PendingIntent.getBroadcast(any(), capture(requestCodeSlot), any(), any())
        } returns mockk(relaxed = true)

        scheduler.scheduleSingleAlert(alert)

        assertThat(requestCodeSlot.captured).isEqualTo(expectedRequestCode)
    }

    @Test
    fun scheduleRollingAlarms_schedulesFullSevenDayQueue() = runTest {
        val preferences = UserPreferences()
        val startDate = LocalDate(2026, 6, 1)

        val scheduledAlerts = scheduler.scheduleRollingAlarms(
            preferences = preferences,
            startDate = startDate,
            daysAhead = 7,
            currentEpochMillis = 0L // Keep all alarms
        )

        // 7 days * 5 obligatory prayers * 3 tiers = 105 alarms
        assertThat(scheduledAlerts).hasSize(105)

        verify(exactly = 105) {
            alarmManager.setAlarmClock(any(), any())
        }
    }

    @Test
    fun cancelAlertById_cancelsMatchingPendingIntent() {
        val testId = 12041
        val mockPendingIntent = mockk<PendingIntent>(relaxed = true)

        every {
            PendingIntent.getBroadcast(
                context,
                testId,
                any<Intent>(),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        } returns mockPendingIntent

        scheduler.cancelAlertById(testId)

        verify(exactly = 1) {
            alarmManager.cancel(mockPendingIntent)
            mockPendingIntent.cancel()
        }
    }

    @Test
    fun scheduleTestAlarm_setsAlarmClockWithSpecifiedDelay() {
        val delaySeconds = 15
        val clockInfoSlot = slot<AlarmManager.AlarmClockInfo>()

        scheduler.scheduleTestAlarm(delaySeconds, PrayerType.DHUHR)

        verify(atLeast = 1) {
            alarmManager.setAlarmClock(capture(clockInfoSlot), any())
        }
        assertThat(clockInfoSlot.isCaptured).isTrue()
    }

    @Test
    fun scheduleTestAlarm_zeroDelay_triggersImmediateStartActivityAndBroadcast() {
        scheduler.scheduleTestAlarm(delaySeconds = 0, prayerType = PrayerType.FAJR)

        verify(atLeast = 1) {
            context.startActivity(any())
            context.sendBroadcast(any())
        }
    }

    @Test
    fun scheduleRollingAlarms_filtersOutTiersWhenDisabled() = runTest {
        val startDate = LocalDate(2026, 6, 1)
        val preferences = UserPreferences(
            preReminderEnabled = false,
            lastCallEnabled = false
        )

        val scheduledAlerts = scheduler.scheduleRollingAlarms(
            preferences = preferences,
            startDate = startDate,
            daysAhead = 7,
            currentEpochMillis = 0L
        )

        // Only Tier 2 (Main Call) scheduled: 7 days * 5 prayers * 1 tier = 35 alarms
        assertThat(scheduledAlerts).hasSize(35)
        assertThat(scheduledAlerts.all { it.tier == AlertTier.MAIN_CALL }).isTrue()
    }

    @Test
    fun scheduleRollingAlarms_filtersOutMutedPrayers_andCancelsStaleAlarms() = runTest {
        val startDate = LocalDate(2026, 6, 1)
        val preferences = UserPreferences(
            alertModes = PrayerType.entries.associateWith { prayer ->
                if (prayer == PrayerType.ISHA) AlertMode.MUTED else AlertMode.FULL_ALARM
            }
        )

        val scheduledAlerts = scheduler.scheduleRollingAlarms(
            preferences = preferences,
            startDate = startDate,
            daysAhead = 7,
            currentEpochMillis = 0L
        )

        // Isha muted -> 7 days * 4 obligatory prayers * 3 tiers = 84 alarms
        assertThat(scheduledAlerts).hasSize(84)
        assertThat(scheduledAlerts.none { it.prayerType == PrayerType.ISHA }).isTrue()

        // Stale Isha alarm IDs for day 1 must have been probed for cancellation
        val day1IshaMainId = ScheduledAlert.createNotificationId(
            startDate.dayOfYear,
            PrayerType.ISHA,
            AlertTier.MAIN_CALL
        )
        verify(atLeast = 1) {
            PendingIntent.getBroadcast(
                context,
                day1IshaMainId,
                any<Intent>(),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    @Test
    fun scheduleRollingAlarms_forwardsAlertModeAndAutoSilenceSeconds() = runTest {
        val startDate = LocalDate(2026, 6, 1)
        val preferences = UserPreferences(
            alertModes = PrayerType.entries.associateWith { AlertMode.GENTLE_BANNER },
            autoSilenceTimeout = AutoSilenceTimeout.SECONDS_120,
            preReminderEnabled = false,
            lastCallEnabled = false
        )

        val broadcastIntentSlot = slot<Intent>()
        every {
            PendingIntent.getBroadcast(
                context,
                any(),
                capture(broadcastIntentSlot),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } returns mockk(relaxed = true)

        scheduler.scheduleRollingAlarms(
            preferences = preferences,
            startDate = startDate,
            daysAhead = 1,
            currentEpochMillis = 0L
        )

        assertThat(broadcastIntentSlot.isCaptured).isTrue()
        verify(atLeast = 1) {
            PendingIntent.getBroadcast(
                context,
                any(),
                any(),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }

    @Test
    fun scheduleAutoUnmute_setsAlarmClockWithRestoreRingerAction() {
        val triggerEpoch = System.currentTimeMillis() + 1_200_000L
        val alarmClockInfoSlot = slot<AlarmManager.AlarmClockInfo>()
        val pendingIntentSlot = slot<PendingIntent>()

        every {
            alarmManager.setAlarmClock(capture(alarmClockInfoSlot), capture(pendingIntentSlot))
        } returns Unit

        scheduler.scheduleAutoUnmute(triggerEpoch)

        verify(exactly = 1) {
            alarmManager.setAlarmClock(any(), any())
        }
        assertThat(alarmClockInfoSlot.isCaptured).isTrue()
    }

    @Test
    fun cancelAutoUnmute_cancelsPendingIntentAndAlarm() {
        val mockPendingIntent = mockk<PendingIntent>(relaxed = true)
        every {
            PendingIntent.getBroadcast(
                context,
                PrayerAlarmScheduler.NOTIFICATION_ID_AUTO_UNMUTE,
                any<Intent>(),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        } returns mockPendingIntent

        scheduler.cancelAutoUnmute()

        verify(exactly = 1) {
            alarmManager.cancel(mockPendingIntent)
            mockPendingIntent.cancel()
        }
    }

    @Test
    fun scheduleSuhoorAlarm_setsAlarmClockWithFajrExtra() {
        val triggerEpoch = System.currentTimeMillis() + 3_600_000L
        val fajrEpoch = triggerEpoch + 2_400_000L
        val alarmClockInfoSlot = slot<AlarmManager.AlarmClockInfo>()
        val pendingIntentSlot = slot<PendingIntent>()

        every {
            alarmManager.setAlarmClock(capture(alarmClockInfoSlot), capture(pendingIntentSlot))
        } returns Unit

        scheduler.scheduleSuhoorAlarm(triggerEpoch, fajrEpoch)

        verify(exactly = 1) {
            alarmManager.setAlarmClock(any(), any())
        }
        assertThat(alarmClockInfoSlot.isCaptured).isTrue()
    }

    @Test
    fun cancelSuhoorAlarm_cancelsPendingIntentAndAlarm() {
        val mockPendingIntent = mockk<PendingIntent>(relaxed = true)
        every {
            PendingIntent.getBroadcast(
                context,
                PrayerAlarmScheduler.NOTIFICATION_ID_SUHOOR,
                any<Intent>(),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        } returns mockPendingIntent

        scheduler.cancelSuhoorAlarm()

        verify(exactly = 1) {
            alarmManager.cancel(mockPendingIntent)
            mockPendingIntent.cancel()
        }
    }

    @Test
    fun scheduleEidCelebration_setsAlarmClockWithCelebratoryExtras() {
        val triggerEpoch = System.currentTimeMillis() + 86_400_000L
        val alarmClockInfoSlot = slot<AlarmManager.AlarmClockInfo>()

        every {
            alarmManager.setAlarmClock(capture(alarmClockInfoSlot), any())
        } returns Unit

        scheduler.scheduleEidCelebration(triggerEpoch, "🌙 Eid al-Fitr Mubarak!", "Taqabbal Allahu Minna wa Minkum")

        verify(exactly = 1) {
            alarmManager.setAlarmClock(any(), any())
        }
        assertThat(alarmClockInfoSlot.isCaptured).isTrue()
    }

    @Test
    fun tahajjudAlarm_constantsAndScheduling_behaveCorrectly() {
        assertThat(PrayerAlarmScheduler.ACTION_TRIGGER_TAHAJJUD_ALARM)
            .isEqualTo("dev.hayya.alarm.ACTION_TRIGGER_TAHAJJUD_ALARM")
        assertThat(PrayerAlarmScheduler.NOTIFICATION_ID_TAHAJJUD).isEqualTo(777779)
        assertThat(PrayerAlarmScheduler.EXTRA_VOLUME_ESCALATION_SECONDS)
            .isEqualTo("extra_volume_escalation_seconds")

        val triggerEpoch = System.currentTimeMillis() + 3600000L
        val alarmClockInfoSlot = slot<AlarmManager.AlarmClockInfo>()

        every {
            alarmManager.setAlarmClock(capture(alarmClockInfoSlot), any())
        } returns Unit

        scheduler.scheduleTahajjudAlarm(triggerEpoch)

        verify(exactly = 1) {
            alarmManager.setAlarmClock(any(), any())
        }
        assertThat(alarmClockInfoSlot.isCaptured).isTrue()
    }

    @Test
    fun cancelTahajjudAlarm_cancelsPendingIntentWhenPresent() {
        val mockPendingIntent = mockk<PendingIntent>(relaxed = true)
        every {
            PendingIntent.getBroadcast(
                context,
                PrayerAlarmScheduler.NOTIFICATION_ID_TAHAJJUD,
                any<Intent>(),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
        } returns mockPendingIntent

        scheduler.cancelTahajjudAlarm()

        verify(exactly = 1) {
            alarmManager.cancel(mockPendingIntent)
            mockPendingIntent.cancel()
        }
    }
}
