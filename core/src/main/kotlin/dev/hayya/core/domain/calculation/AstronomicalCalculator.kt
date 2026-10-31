package dev.hayya.core.domain.calculation

import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import kotlinx.datetime.LocalDate

/**
 * Raw astronomical epoch millisecond timestamps computed for a single solar calendar day.
 */
data class RawPrayerTimes(
    val fajrEpochMillis: Long,
    val sunriseEpochMillis: Long,
    val dhuhrEpochMillis: Long,
    val asrEpochMillis: Long,
    val maghribEpochMillis: Long,
    val ishaEpochMillis: Long
)

/**
 * Contract for astronomical solar calculation backends.
 */
interface AstronomicalCalculator {
    /**
     * Computes raw prayer times in UTC epoch milliseconds for the given local date and coordinates.
     */
    fun calculatePrayerTimes(
        coordinates: Coordinates,
        date: LocalDate,
        parameters: CalculationParameters
    ): RawPrayerTimes
}
