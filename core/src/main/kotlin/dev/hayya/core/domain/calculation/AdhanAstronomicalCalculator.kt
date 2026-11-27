package dev.hayya.core.domain.calculation

import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.PrayerTimes
import com.batoulapps.adhan.data.DateComponents
import kotlinx.datetime.LocalDate

/**
 * Astronomical calculator implementation wrapping Batoul Apps adhan-kotlin library.
 */
class AdhanAstronomicalCalculator(
    private val solarFallback: SolarAstronomicalCalculator = SolarAstronomicalCalculator()
) : AstronomicalCalculator {

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

        val fajr = prayerTimes.fajr?.time
        val sunrise = prayerTimes.sunrise?.time
        val dhuhr = prayerTimes.dhuhr?.time
        val asr = prayerTimes.asr?.time
        val maghrib = prayerTimes.maghrib?.time
        val isha = prayerTimes.isha?.time

        val fallback by lazy(LazyThreadSafetyMode.NONE) {
            solarFallback.calculatePrayerTimes(coordinates, date, parameters)
        }

        return RawPrayerTimes(
            fajrEpochMillis = fajr ?: fallback.fajrEpochMillis,
            sunriseEpochMillis = sunrise ?: fallback.sunriseEpochMillis,
            dhuhrEpochMillis = dhuhr ?: fallback.dhuhrEpochMillis,
            asrEpochMillis = asr ?: fallback.asrEpochMillis,
            maghribEpochMillis = maghrib ?: fallback.maghribEpochMillis,
            ishaEpochMillis = isha ?: fallback.ishaEpochMillis
        )
    }
}
