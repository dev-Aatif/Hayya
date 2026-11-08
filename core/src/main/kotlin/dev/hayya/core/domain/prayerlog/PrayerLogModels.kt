package dev.hayya.core.domain.prayerlog

import dev.hayya.core.domain.model.PrayerType
import kotlinx.datetime.LocalDate

/**
 * The five obligatory daily prayers in Islam tracked in Hayya.
 * (Sunrise is an astronomical boundary, not an obligatory prayer).
 */
val OBLIGATORY_PRAYERS = listOf(
    PrayerType.FAJR,
    PrayerType.DHUHR,
    PrayerType.ASR,
    PrayerType.MAGHRIB,
    PrayerType.ISHA
)

/**
 * Immutable domain model representing daily prayer completion for a specific calendar date.
 */
data class DailyPrayerLog(
    val date: LocalDate,
    val completedPrayers: Set<PrayerType> = emptySet()
) {
    val completedCount: Int get() = completedPrayers.size
    val totalCount: Int get() = OBLIGATORY_PRAYERS.size
    val completionRatio: Float get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount
    val isComplete: Boolean get() = completedCount == totalCount

    fun isPrayerCompleted(prayer: PrayerType): Boolean = completedPrayers.contains(prayer)

    fun withToggled(prayer: PrayerType): DailyPrayerLog {
        require(prayer in OBLIGATORY_PRAYERS) {
            "Only obligatory prayers (Fajr, Dhuhr, Asr, Maghrib, Isha) can be logged, was $prayer"
        }
        val updated = if (completedPrayers.contains(prayer)) {
            completedPrayers - prayer
        } else {
            completedPrayers + prayer
        }
        return copy(completedPrayers = updated)
    }
}

/**
 * Aggregated 7-day rolling completion history.
 */
data class RollingPrayerHistory(
    val dailyLogs: List<DailyPrayerLog> = emptyList()
) {
    val totalCompleted: Int get() = dailyLogs.sumOf { it.completedCount }
    val totalPossible: Int get() = dailyLogs.size * OBLIGATORY_PRAYERS.size
    val overallRatio: Float get() = if (totalPossible == 0) 0f else totalCompleted.toFloat() / totalPossible
    val todayLog: DailyPrayerLog? get() = dailyLogs.lastOrNull()
    val todayCompletedCount: Int get() = todayLog?.completedCount ?: 0
}
