package dev.hayya.core.domain.prayerlog

import com.google.common.truth.Truth.assertThat
import dev.hayya.core.domain.model.PrayerType
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertThrows
import org.junit.Test

class PrayerLogModelsTest {

    private val testDate = LocalDate(2026, 11, 2)

    @Test
    fun obligatoryPrayers_containsExactlyFiveDailyPrayersExcludingSunrise() {
        assertThat(OBLIGATORY_PRAYERS).containsExactly(
            PrayerType.FAJR,
            PrayerType.DHUHR,
            PrayerType.ASR,
            PrayerType.MAGHRIB,
            PrayerType.ISHA
        ).inOrder()

        assertThat(OBLIGATORY_PRAYERS).doesNotContain(PrayerType.SUNRISE)
    }

    @Test
    fun withToggled_togglesCompletionStateAcrossObligatoryPrayers() {
        val initialLog = DailyPrayerLog(date = testDate)
        assertThat(initialLog.completedCount).isEqualTo(0)
        assertThat(initialLog.isPrayerCompleted(PrayerType.FAJR)).isFalse()
        assertThat(initialLog.isComplete).isFalse()

        // Toggle Fajr ON
        val log1 = initialLog.withToggled(PrayerType.FAJR)
        assertThat(log1.completedCount).isEqualTo(1)
        assertThat(log1.isPrayerCompleted(PrayerType.FAJR)).isTrue()
        assertThat(log1.completionRatio).isEqualTo(0.2f)
        assertThat(log1.isComplete).isFalse()

        // Toggle Dhuhr ON
        val log2 = log1.withToggled(PrayerType.DHUHR)
        assertThat(log2.completedCount).isEqualTo(2)
        assertThat(log2.isPrayerCompleted(PrayerType.DHUHR)).isTrue()
        assertThat(log2.completionRatio).isEqualTo(0.4f)

        // Toggle Fajr OFF
        val log3 = log2.withToggled(PrayerType.FAJR)
        assertThat(log3.completedCount).isEqualTo(1)
        assertThat(log3.isPrayerCompleted(PrayerType.FAJR)).isFalse()
        assertThat(log3.isPrayerCompleted(PrayerType.DHUHR)).isTrue()
    }

    @Test
    fun withToggled_nonObligatoryPrayer_throwsIllegalArgumentException() {
        val initialLog = DailyPrayerLog(date = testDate)
        assertThrows(IllegalArgumentException::class.java) {
            initialLog.withToggled(PrayerType.SUNRISE)
        }
    }

    @Test
    fun completionMetrics_reachFullCompletionAtFivePrayers() {
        val allDone = DailyPrayerLog(
            date = testDate,
            completedPrayers = OBLIGATORY_PRAYERS.toSet()
        )
        assertThat(allDone.completedCount).isEqualTo(5)
        assertThat(allDone.totalCount).isEqualTo(5)
        assertThat(allDone.completionRatio).isEqualTo(1.0f)
        assertThat(allDone.isComplete).isTrue()
    }

    @Test
    fun rollingPrayerHistory_aggregatesTotalCountsAndOverallRatio() {
        val day1 = DailyPrayerLog(LocalDate(2026, 10, 27), setOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB, PrayerType.ISHA)) // 5
        val day2 = DailyPrayerLog(LocalDate(2026, 10, 28), setOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB, PrayerType.ISHA)) // 5
        val day3 = DailyPrayerLog(LocalDate(2026, 10, 29), setOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB)) // 4
        val day4 = DailyPrayerLog(LocalDate(2026, 10, 30), setOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR)) // 3
        val day5 = DailyPrayerLog(LocalDate(2026, 10, 31), setOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB, PrayerType.ISHA)) // 5
        val day6 = DailyPrayerLog(LocalDate(2026, 11, 1), setOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB, PrayerType.ISHA)) // 5
        val day7 = DailyPrayerLog(LocalDate(2026, 11, 2), setOf(PrayerType.FAJR, PrayerType.DHUHR, PrayerType.ASR, PrayerType.MAGHRIB, PrayerType.ISHA)) // 5

        val history = RollingPrayerHistory(dailyLogs = listOf(day1, day2, day3, day4, day5, day6, day7))

        assertThat(history.dailyLogs).hasSize(7)
        assertThat(history.totalCompleted).isEqualTo(32)
        assertThat(history.totalPossible).isEqualTo(35)
        assertThat(history.overallRatio).isWithin(0.001f).of(32f / 35f)
        assertThat(history.todayLog).isEqualTo(day7)
        assertThat(history.todayCompletedCount).isEqualTo(5)
    }
}
