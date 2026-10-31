package dev.hayya.core.domain.calculation

import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import kotlinx.datetime.LocalDate

/**
 * Astronomical calculator implementation wrapping Batoul Apps adhan-kotlin library.
 */
class AdhanAstronomicalCalculator : AstronomicalCalculator {

    override fun calculatePrayerTimes(
        coordinates: Coordinates,
        date: LocalDate,
        parameters: CalculationParameters
    ): RawPrayerTimes {
        val dateComponents = DateComponents(
            date.year,
            date.monthNumber,
            date.dayOfMonth
        )
        val prayerTimes = PrayerTimes(coordinates, dateComponents, parameters)

        return RawPrayerTimes(
            fajrEpochMillis = prayerTimes.fajr?.time ?: 0L,
            sunriseEpochMillis = prayerTimes.sunrise?.time ?: 0L,
            dhuhrEpochMillis = prayerTimes.dhuhr?.time ?: 0L,
            asrEpochMillis = prayerTimes.asr?.time ?: 0L,
            maghribEpochMillis = prayerTimes.maghrib?.time ?: 0L,
            ishaEpochMillis = prayerTimes.isha?.time ?: 0L
        )
    }
}
