package dev.hayya.core.domain.qibla

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Astronomical spherical trigonometry engine for calculating great-circle
 * bearings and distances to the Holy Kaaba in Makkah Al-Mukarramah.
 */
object QiblaCalculator {

    /**
     * Exact WGS-84 geographic coordinates for the Holy Kaaba in Makkah.
     * Latitude: 21° 25' 20.95" N (21.422487)
     * Longitude: 39° 49' 34.34" E (39.826206)
     */
    const val KAABA_LATITUDE = 21.422487
    const val KAABA_LONGITUDE = 39.826206

    private const val EARTH_RADIUS_KM = 6371.0

    private val COMPASS_POINTS_16 = arrayOf(
        "N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW"
    )

    /**
     * Calculates the forward great-circle azimuth (initial bearing) from the given observer
     * coordinates to the Kaaba in degrees, normalized to [0.0, 360.0).
     */
    fun calculateBearing(latitude: Double, longitude: Double): Double {
        if (abs(latitude - KAABA_LATITUDE) < 0.0001 && abs(longitude - KAABA_LONGITUDE) < 0.0001) {
            return 0.0
        }

        val phi1 = Math.toRadians(latitude)
        val lambda1 = Math.toRadians(longitude)
        val phi2 = Math.toRadians(KAABA_LATITUDE)
        val lambda2 = Math.toRadians(KAABA_LONGITUDE)

        val deltaLambda = lambda2 - lambda1

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)

        val initialBearingRad = atan2(y, x)
        val initialBearingDeg = Math.toDegrees(initialBearingRad)

        return (initialBearingDeg % 360.0 + 360.0) % 360.0
    }

    /**
     * Calculates the great-circle surface distance to the Kaaba in kilometers using the Haversine formula.
     */
    fun calculateDistanceKm(latitude: Double, longitude: Double): Double {
        val phi1 = Math.toRadians(latitude)
        val phi2 = Math.toRadians(KAABA_LATITUDE)
        val deltaPhi = Math.toRadians(KAABA_LATITUDE - latitude)
        val deltaLambda = Math.toRadians(KAABA_LONGITUDE - longitude)

        val a = sin(deltaPhi / 2.0).pow(2) +
            cos(phi1) * cos(phi2) * sin(deltaLambda / 2.0).pow(2)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))

        return EARTH_RADIUS_KM * c
    }

    /**
     * Formats an azimuth in degrees into one of the 16 cardinal/intercardinal compass points.
     */
    fun getCompassDirection(bearing: Double): String {
        val normalized = (bearing % 360.0 + 360.0) % 360.0
        val index = (((normalized + 11.25) % 360.0) / 22.5).toInt()
        return COMPASS_POINTS_16[index]
    }

    /**
     * Formats an exact bearing into a clean, human-readable compass read-out (e.g. "118° ESE").
     */
    fun formatBearingReadout(bearing: Double): String {
        val deg = bearing.roundToInt() % 360
        val direction = getCompassDirection(bearing)
        return "$deg° $direction"
    }

    /**
     * Calculates the signed shortest angular deviation between the device's current heading
     * and the target Qibla bearing in degrees, in the range [-180.0, 180.0].
     * Positive means the user should turn clockwise (right), negative means turn counter-clockwise (left).
     */
    fun calculateDeviation(deviceHeading: Double, qiblaBearing: Double): Double {
        var diff = (qiblaBearing - deviceHeading) % 360.0
        if (diff > 180.0) diff -= 360.0
        if (diff < -180.0) diff += 360.0
        return diff
    }

    /**
     * Determines whether the device orientation is aligned with the Kaaba within a specified tolerance.
     * Default tolerance is ±2.0 degrees.
     */
    fun isAligned(deviceHeading: Double, qiblaBearing: Double, toleranceDegrees: Double = 2.0): Boolean {
        return abs(calculateDeviation(deviceHeading, qiblaBearing)) <= toleranceDegrees
    }

    /**
     * Continuous circular exponential low-pass filter for smooth angle rotation.
     * Smooths angles on the unit circle to eliminate wrap-around jump artifacts at 0°/360°.
     */
    fun smoothAngle(currentAngle: Float, newAngle: Float, alpha: Float = 0.2f): Float {
        val curRad = Math.toRadians(currentAngle.toDouble())
        val targetRad = Math.toRadians(newAngle.toDouble())

        val curX = cos(curRad)
        val curY = sin(curRad)
        val targetX = cos(targetRad)
        val targetY = sin(targetRad)

        val smoothedX = curX + alpha * (targetX - curX)
        val smoothedY = curY + alpha * (targetY - curY)

        val smoothedRad = atan2(smoothedY, smoothedX)
        val smoothedDeg = Math.toDegrees(smoothedRad)
        return ((smoothedDeg % 360.0 + 360.0) % 360.0).toFloat()
    }
}
