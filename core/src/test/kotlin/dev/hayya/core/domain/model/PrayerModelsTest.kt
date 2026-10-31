package dev.hayya.core.domain.model

import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertThrows
import org.junit.Test

class PrayerModelsTest {

    @Test
    fun prayerType_obligatoryFlagsAndSequence_areCorrect() {
        assertThat(PrayerType.FAJR.isObligatory).isTrue()
        assertThat(PrayerType.SUNRISE.isObligatory).isFalse()
        assertThat(PrayerType.DHUHR.isObligatory).isTrue()
        assertThat(PrayerType.ASR.isObligatory).isTrue()
        assertThat(PrayerType.MAGHRIB.isObligatory).isTrue()
        assertThat(PrayerType.ISHA.isObligatory).isTrue()

        assertThat(PrayerType.FAJR.nextInSequence).isEqualTo(PrayerType.SUNRISE)
        assertThat(PrayerType.SUNRISE.nextInSequence).isEqualTo(PrayerType.DHUHR)
        assertThat(PrayerType.DHUHR.nextInSequence).isEqualTo(PrayerType.ASR)
        assertThat(PrayerType.ASR.nextInSequence).isEqualTo(PrayerType.MAGHRIB)
        assertThat(PrayerType.MAGHRIB.nextInSequence).isEqualTo(PrayerType.ISHA)
        assertThat(PrayerType.ISHA.nextInSequence).isEqualTo(PrayerType.FAJR)
    }

    @Test
    fun alertTier_offsets_matchContract() {
        assertThat(AlertTier.PRE_REMINDER.offsetMinutesFromAnchor).isEqualTo(-15L)
        assertThat(AlertTier.PRE_REMINDER.offsetMillis).isEqualTo(-15L * 60_000L)

        assertThat(AlertTier.MAIN_CALL.offsetMinutesFromAnchor).isEqualTo(0L)
        assertThat(AlertTier.MAIN_CALL.offsetMillis).isEqualTo(0L)

        assertThat(AlertTier.LAST_CALL.offsetMinutesFromAnchor).isEqualTo(-20L)
        assertThat(AlertTier.LAST_CALL.offsetMillis).isEqualTo(-20L * 60_000L)
    }

    @Test
    fun notificationId_isDeterministicAndUniqueAcrossAllDaysAndTiers() {
        val seenIds = mutableSetOf<Int>()

        for (dayOfYear in 1..366) {
            for (prayer in PrayerType.entries) {
                for (tier in AlertTier.entries) {
                    val id = ScheduledAlert.createNotificationId(dayOfYear, prayer, tier)

                    // Must be strictly positive and fit in 32-bit Int
                    assertThat(id).isGreaterThan(0)

                    // Must be 100% collision-free
                    val isUnique = seenIds.add(id)
                    assertThat(isUnique).isTrue()

                    // Must be completely reversible
                    assertThat(ScheduledAlert.extractDayOfYear(id)).isEqualTo(dayOfYear)
                    assertThat(ScheduledAlert.extractPrayerType(id)).isEqualTo(prayer)
                    assertThat(ScheduledAlert.extractAlertTier(id)).isEqualTo(tier)
                }
            }
        }

        // 366 days * 6 prayers * 3 tiers = 6,588 unique notification IDs
        assertThat(seenIds.size).isEqualTo(366 * 6 * 3)
    }

    @Test
    fun notificationId_throwsForInvalidDayOfYear() {
        assertThrows(IllegalArgumentException::class.java) {
            ScheduledAlert.createNotificationId(0, PrayerType.FAJR, AlertTier.MAIN_CALL)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ScheduledAlert.createNotificationId(367, PrayerType.FAJR, AlertTier.MAIN_CALL)
        }
    }

    @Test
    fun scheduledAlert_sortsChronologicallyByTriggerTimestamp() {
        val alert1 = ScheduledAlert(1, PrayerType.FAJR, AlertTier.PRE_REMINDER, 1_000_000L)
        val alert2 = ScheduledAlert(1, PrayerType.FAJR, AlertTier.MAIN_CALL, 2_000_000L)
        val alert3 = ScheduledAlert(1, PrayerType.FAJR, AlertTier.LAST_CALL, 3_000_000L)

        val unsorted = listOf(alert3, alert1, alert2)
        val sorted = unsorted.sorted()

        assertThat(sorted).containsExactly(alert1, alert2, alert3).inOrder()
    }

    @Test
    fun prayerWindow_progressCalculation_clampsBetweenZeroAndOne() {
        val start = 1_000_000L
        val end = 2_000_000L

        // Exactly at start
        assertThat(PrayerWindow.calculateProgress(1_000_000L, start, end)).isEqualTo(0.0f)

        // Halfway
        assertThat(PrayerWindow.calculateProgress(1_500_000L, start, end)).isEqualTo(0.5f)

        // Exactly at end
        assertThat(PrayerWindow.calculateProgress(2_000_000L, start, end)).isEqualTo(1.0f)

        // Before start (clamped to 0.0)
        assertThat(PrayerWindow.calculateProgress(500_000L, start, end)).isEqualTo(0.0f)

        // After end (clamped to 1.0)
        assertThat(PrayerWindow.calculateProgress(2_500_000L, start, end)).isEqualTo(1.0f)

        // Degenerate interval (end <= start)
        assertThat(PrayerWindow.calculateProgress(1_000_000L, 2_000_000L, 1_000_000L)).isEqualTo(0.0f)
    }
}
