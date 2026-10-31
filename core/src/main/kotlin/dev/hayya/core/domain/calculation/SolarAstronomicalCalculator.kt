package dev.hayya.core.domain.calculation

import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab
import kotlinx.datetime.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Pure Kotlin astronomical solar calculation engine implementing Jean Meeus'
 * "Astronomical Algorithms". Operates completely offline with zero native or network dependencies.
 */
class SolarAstronomicalCalculator : AstronomicalCalculator {

    override fun calculatePrayerTimes(
        coordinates: Coordinates,
        date: LocalDate,
        parameters: CalculationParameters
    ): RawPrayerTimes {
        val lat = coordinates.latitude
        val lng = coordinates.longitude

        val julianDay = computeJulianDay(date.year, date.monthNumber, date.dayOfMonth)

        // Solar transit at solar noon (approx 12:00 UTC adjusted for longitude)
        val noonUtcFraction = (12.0 - lng / 15.0) / 24.0
        val solarCoordsNoon = computeSolarCoordinates(julianDay + noonUtcFraction)
        val equationOfTimeMinutes = solarCoordsNoon.equationOfTimeMinutes
        val declinationDeg = solarCoordsNoon.declinationDegrees

        // Solar noon in UTC hours
        val noonHoursUtc = 12.0 - (lng / 15.0) - (equationOfTimeMinutes / 60.0)

        // Sunrise & Sunset angle accounting for atmospheric refraction and solar disc (-0.8333°)
        val sunriseSunsetAngle = -0.8333333
        val sunriseSunsetHourAngle = computeHourAngle(lat, declinationDeg, sunriseSunsetAngle)

        val sunriseHoursUtc = noonHoursUtc - (sunriseSunsetHourAngle / 15.0)
        val sunsetHoursUtc = noonHoursUtc + (sunriseSunsetHourAngle / 15.0)

        // Asr angle based on shadow length ratio (Shafi = 1x, Hanafi = 2x)
        val shadowFactor = if (parameters.madhab == Madhab.HANAFI) 2.0 else 1.0
        val asrAltitude = computeAsrAltitude(lat, declinationDeg, shadowFactor)
        val asrHourAngle = computeHourAngle(lat, declinationDeg, asrAltitude)
        val asrHoursUtc = noonHoursUtc + (asrHourAngle / 15.0)

        // Fajr & Isha raw hour angles
        val fajrAngle = -parameters.fajrAngle
        val rawFajrHourAngle = computeHourAngleNullable(lat, declinationDeg, fajrAngle)

        val rawIshaHourAngle = if (parameters.ishaInterval > 0) {
            null // Isha calculated as fixed interval after Maghrib
        } else {
            computeHourAngleNullable(lat, declinationDeg, -parameters.ishaAngle)
        }

        // Night length between sunset and next sunrise (approx 24h - day length)
        val dayLengthHours = (2.0 * sunriseSunsetHourAngle) / 15.0
        val nightLengthHours = 24.0 - dayLengthHours

        // High latitude adjustments if twilight does not disappear or angle calculation fails
        val fajrHoursUtc: Double
        val ishaHoursUtc: Double

        val highLatPortion = when (parameters.highLatitudeRule) {
            HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> 1.0 / 7.0
            HighLatitudeRule.TWILIGHT_ANGLE -> parameters.fajrAngle / 60.0
            else -> 0.5 // MIDDLE_OF_THE_NIGHT
        }
        val maxFajrAdjustmentHours = nightLengthHours * highLatPortion
        val maxIshaAdjustmentHours = when (parameters.highLatitudeRule) {
            HighLatitudeRule.SEVENTH_OF_THE_NIGHT -> nightLengthHours / 7.0
            HighLatitudeRule.TWILIGHT_ANGLE -> parameters.ishaAngle / 60.0
            else -> nightLengthHours * 0.5
        }

        if (rawFajrHourAngle != null) {
            val normalFajr = noonHoursUtc - (rawFajrHourAngle / 15.0)
            val durationBeforeSunrise = sunriseHoursUtc - normalFajr
            fajrHoursUtc = if (durationBeforeSunrise > maxFajrAdjustmentHours) {
                sunriseHoursUtc - maxFajrAdjustmentHours
            } else {
                normalFajr
            }
        } else {
            fajrHoursUtc = sunriseHoursUtc - maxFajrAdjustmentHours
        }

        if (parameters.ishaInterval > 0) {
            ishaHoursUtc = sunsetHoursUtc + (parameters.ishaInterval / 60.0)
        } else if (rawIshaHourAngle != null) {
            val normalIsha = noonHoursUtc + (rawIshaHourAngle / 15.0)
            val durationAfterSunset = normalIsha - sunsetHoursUtc
            ishaHoursUtc = if (durationAfterSunset > maxIshaAdjustmentHours) {
                sunsetHoursUtc + maxIshaAdjustmentHours
            } else {
                normalIsha
            }
        } else {
            ishaHoursUtc = sunsetHoursUtc + maxIshaAdjustmentHours
        }

        // Maghrib time (at sunset or custom angle/interval)
        val maghribHoursUtc = if (parameters.maghribAngle > 0.0) {
            val maghribHourAngle = computeHourAngle(lat, declinationDeg, -parameters.maghribAngle)
            noonHoursUtc + (maghribHourAngle / 15.0)
        } else {
            sunsetHoursUtc
        }

        // Convert UTC hours to Epoch Milliseconds for this calendar date
        val baseDateEpochMillis = computeMidnightEpochMillis(date.year, date.monthNumber, date.dayOfMonth)

        return RawPrayerTimes(
            fajrEpochMillis = baseDateEpochMillis + (fajrHoursUtc * 3600_000.0).toLong(),
            sunriseEpochMillis = baseDateEpochMillis + (sunriseHoursUtc * 3600_000.0).toLong(),
            dhuhrEpochMillis = baseDateEpochMillis + (noonHoursUtc * 3600_000.0).toLong(),
            asrEpochMillis = baseDateEpochMillis + (asrHoursUtc * 3600_000.0).toLong(),
            maghribEpochMillis = baseDateEpochMillis + (maghribHoursUtc * 3600_000.0).toLong(),
            ishaEpochMillis = baseDateEpochMillis + (ishaHoursUtc * 3600_000.0).toLong()
        )
    }

    private data class SolarCoordinates(
        val declinationDegrees: Double,
        val equationOfTimeMinutes: Double
    )

    private fun computeSolarCoordinates(julianDay: Double): SolarCoordinates {
        val t = (julianDay - 2451545.0) / 36525.0
        val l0 = 280.46646 + t * (36000.76983 + 0.0003032 * t)
        val m = 357.52911 + t * (35999.05029 - 0.0001537 * t)
        val mRad = Math.toRadians(m)

        val c = sin(mRad) * (1.914602 - t * (0.004817 + 0.000014 * t)) +
            sin(2.0 * mRad) * (0.019993 - 0.000101 * t) +
            sin(3.0 * mRad) * 0.000289
        val trueLong = l0 + c
        val apparentLong = trueLong - 0.00569 - 0.00478 * sin(Math.toRadians(125.04 - 1934.136 * t))

        val eps0 = 23.0 + (26.0 + (21.448 - t * (46.815 + t * (0.00059 - t * 0.001813))) / 60.0) / 60.0
        val eps = eps0 + 0.00256 * cos(Math.toRadians(125.04 - 1934.136 * t))
        val epsRad = Math.toRadians(eps)

        val declinationRad = asin(sin(epsRad) * sin(Math.toRadians(apparentLong)))
        val declinationDeg = Math.toDegrees(declinationRad)

        val y = tan(epsRad / 2.0) * tan(epsRad / 2.0)
        val l0Rad = Math.toRadians(l0)
        val e = 0.016708634 - t * (0.000042037 + 0.0000001267 * t)

        val eqTimeRad = y * sin(2.0 * l0Rad) - 2.0 * e * sin(mRad) +
            4.0 * e * y * sin(mRad) * cos(2.0 * l0Rad) -
            0.5 * y * y * sin(4.0 * l0Rad) - 1.25 * e * e * sin(2.0 * mRad)

        val eqTimeMinutes = Math.toDegrees(eqTimeRad) * 4.0

        return SolarCoordinates(declinationDeg, eqTimeMinutes)
    }

    private fun computeHourAngle(latitude: Double, declination: Double, altitudeAngle: Double): Double {
        val latRad = Math.toRadians(latitude)
        val decRad = Math.toRadians(declination)
        val altRad = Math.toRadians(altitudeAngle)

        val cosH = (sin(altRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        val clampedCosH = cosH.coerceIn(-1.0, 1.0)
        return Math.toDegrees(acos(clampedCosH))
    }

    private fun computeHourAngleNullable(latitude: Double, declination: Double, altitudeAngle: Double): Double? {
        val latRad = Math.toRadians(latitude)
        val decRad = Math.toRadians(declination)
        val altRad = Math.toRadians(altitudeAngle)

        val cosH = (sin(altRad) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
        return if (cosH in -1.0..1.0) {
            Math.toDegrees(acos(cosH))
        } else {
            null
        }
    }

    private fun computeAsrAltitude(latitude: Double, declination: Double, shadowFactor: Double): Double {
        val diff = abs(latitude - declination)
        val diffRad = Math.toRadians(diff)
        val tanDiff = tan(diffRad)
        val cotAngle = shadowFactor + tanDiff
        val altitudeRad = atan(1.0 / cotAngle)
        return Math.toDegrees(altitudeRad)
    }

    private fun computeJulianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2.0 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun computeMidnightEpochMillis(year: Int, month: Int, day: Int): Long {
        // Calculate UTC epoch millis at 00:00:00 UTC for the given Gregorian date
        val daysSinceEpoch = computeDaysSinceUnixEpoch(year, month, day)
        return daysSinceEpoch * 86_400_000L
    }

    private fun computeDaysSinceUnixEpoch(year: Int, month: Int, day: Int): Long {
        // Days from 1970-01-01 to year-month-day
        var y = year.toLong()
        var m = month.toLong()
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val era = if (y >= 0) y / 400 else (y - 399) / 400
        val yoe = y - era * 400
        val doy = (153 * (m + 9) + 2) / 5 + day - 1
        val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
        return era * 146097 + doe - 719468
    }
}
