package dev.hayya.core.domain.qibla

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

class QiblaCalculatorTest {

    @Test
    fun calculateBearing_londonCoordinates_yieldsApproximately119DegreesESE() {
        // London, UK
        val latitude = 51.5074
        val longitude = -0.1278

        val bearing = QiblaCalculator.calculateBearing(latitude, longitude)
        val compassPoint = QiblaCalculator.getCompassDirection(bearing)
        val formatted = QiblaCalculator.formatBearingReadout(bearing)

        // Spherical great circle bearing to Kaaba from London is ~118.98° (rounds to 119° ESE)
        assertThat(bearing).isWithin(0.5).of(118.98)
        assertThat(compassPoint).isEqualTo("ESE")
        assertThat(formatted).isEqualTo("119° ESE")
    }

    @Test
    fun calculateBearing_newYorkCoordinates_yieldsApproximately58DegreesENE() {
        // New York City, USA
        val latitude = 40.7128
        val longitude = -74.0060

        val bearing = QiblaCalculator.calculateBearing(latitude, longitude)
        val compassPoint = QiblaCalculator.getCompassDirection(bearing)

        assertThat(bearing).isWithin(0.5).of(58.48)
        assertThat(compassPoint).isEqualTo("ENE")
    }

    @Test
    fun calculateBearing_cairoCoordinates_yieldsApproximately136DegreesSE() {
        // Cairo, Egypt
        val latitude = 30.0444
        val longitude = 31.2357

        val bearing = QiblaCalculator.calculateBearing(latitude, longitude)
        val compassPoint = QiblaCalculator.getCompassDirection(bearing)

        assertThat(bearing).isWithin(0.5).of(136.54)
        assertThat(compassPoint).isEqualTo("SE")
    }

    @Test
    fun calculateBearing_jakartaCoordinates_yieldsApproximately295DegreesWNW() {
        // Jakarta, Indonesia
        val latitude = -6.2088
        val longitude = 106.8456

        val bearing = QiblaCalculator.calculateBearing(latitude, longitude)
        val compassPoint = QiblaCalculator.getCompassDirection(bearing)

        assertThat(bearing).isWithin(0.5).of(295.19)
        assertThat(compassPoint).isEqualTo("WNW")
    }

    @Test
    fun calculateBearing_atKaaba_yieldsZero() {
        val bearing = QiblaCalculator.calculateBearing(
            QiblaCalculator.KAABA_LATITUDE,
            QiblaCalculator.KAABA_LONGITUDE
        )
        assertThat(bearing).isEqualTo(0.0)
    }

    @Test
    fun calculateDistanceKm_calculatesAccurateGreatCircleDistance() {
        // London to Kaaba is ~4,788 km
        val distanceLondon = QiblaCalculator.calculateDistanceKm(51.5074, -0.1278)
        assertThat(distanceLondon).isWithin(20.0).of(4788.0)

        // Cairo to Kaaba is ~1,288 km
        val distanceCairo = QiblaCalculator.calculateDistanceKm(30.0444, 31.2357)
        assertThat(distanceCairo).isWithin(20.0).of(1288.0)

        // At Kaaba itself
        val distanceZero = QiblaCalculator.calculateDistanceKm(
            QiblaCalculator.KAABA_LATITUDE,
            QiblaCalculator.KAABA_LONGITUDE
        )
        assertThat(distanceZero).isWithin(0.1).of(0.0)
    }

    @Test
    fun getCompassDirection_mapsAll16CardinalPointsAccurately() {
        assertThat(QiblaCalculator.getCompassDirection(0.0)).isEqualTo("N")
        assertThat(QiblaCalculator.getCompassDirection(22.5)).isEqualTo("NNE")
        assertThat(QiblaCalculator.getCompassDirection(45.0)).isEqualTo("NE")
        assertThat(QiblaCalculator.getCompassDirection(67.5)).isEqualTo("ENE")
        assertThat(QiblaCalculator.getCompassDirection(90.0)).isEqualTo("E")
        assertThat(QiblaCalculator.getCompassDirection(112.5)).isEqualTo("ESE")
        assertThat(QiblaCalculator.getCompassDirection(118.0)).isEqualTo("ESE")
        assertThat(QiblaCalculator.getCompassDirection(135.0)).isEqualTo("SE")
        assertThat(QiblaCalculator.getCompassDirection(157.5)).isEqualTo("SSE")
        assertThat(QiblaCalculator.getCompassDirection(180.0)).isEqualTo("S")
        assertThat(QiblaCalculator.getCompassDirection(202.5)).isEqualTo("SSW")
        assertThat(QiblaCalculator.getCompassDirection(225.0)).isEqualTo("SW")
        assertThat(QiblaCalculator.getCompassDirection(247.5)).isEqualTo("WSW")
        assertThat(QiblaCalculator.getCompassDirection(270.0)).isEqualTo("W")
        assertThat(QiblaCalculator.getCompassDirection(292.5)).isEqualTo("WNW")
        assertThat(QiblaCalculator.getCompassDirection(315.0)).isEqualTo("NW")
        assertThat(QiblaCalculator.getCompassDirection(337.5)).isEqualTo("NNW")
        assertThat(QiblaCalculator.getCompassDirection(355.0)).isEqualTo("N")
    }

    @Test
    fun calculateDeviation_computesSignedShortestAngularDifference() {
        // Clockwise turn needed (positive)
        assertThat(QiblaCalculator.calculateDeviation(100.0, 118.0)).isEqualTo(18.0)

        // Counter-clockwise turn needed (negative)
        assertThat(QiblaCalculator.calculateDeviation(130.0, 118.0)).isEqualTo(-12.0)

        // Wrap-around across 0°/360°
        // Device at 350°, target at 10° -> turn right 20°
        assertThat(QiblaCalculator.calculateDeviation(350.0, 10.0)).isEqualTo(20.0)

        // Device at 10°, target at 350° -> turn left 20°
        assertThat(QiblaCalculator.calculateDeviation(10.0, 350.0)).isEqualTo(-20.0)
    }

    @Test
    fun isAligned_within2Degrees_detectsAccurateAlignmentAndWrapAround() {
        val qibla = 118.0

        // Exact match
        assertThat(QiblaCalculator.isAligned(118.0, qibla, toleranceDegrees = 2.0)).isTrue()

        // Within +2.0° and -2.0°
        assertThat(QiblaCalculator.isAligned(116.0, qibla, toleranceDegrees = 2.0)).isTrue()
        assertThat(QiblaCalculator.isAligned(120.0, qibla, toleranceDegrees = 2.0)).isTrue()
        assertThat(QiblaCalculator.isAligned(117.5, qibla, toleranceDegrees = 2.0)).isTrue()
        assertThat(QiblaCalculator.isAligned(118.8, qibla, toleranceDegrees = 2.0)).isTrue()

        // Outside ±2.0°
        assertThat(QiblaCalculator.isAligned(115.9, qibla, toleranceDegrees = 2.0)).isFalse()
        assertThat(QiblaCalculator.isAligned(120.1, qibla, toleranceDegrees = 2.0)).isFalse()
        assertThat(QiblaCalculator.isAligned(45.0, qibla, toleranceDegrees = 2.0)).isFalse()

        // Wrap-around boundary tests
        // Target: 1.0°, Heading: 359.5° -> diff = +1.5° (within ±2°)
        assertThat(QiblaCalculator.isAligned(359.5, 1.0, toleranceDegrees = 2.0)).isTrue()

        // Target: 359.0°, Heading: 0.5° -> diff = -1.5° (within ±2°)
        assertThat(QiblaCalculator.isAligned(0.5, 359.0, toleranceDegrees = 2.0)).isTrue()

        // Target: 1.0°, Heading: 358.0° -> diff = +3.0° (outside ±2°)
        assertThat(QiblaCalculator.isAligned(358.0, 1.0, toleranceDegrees = 2.0)).isFalse()
    }

    @Test
    fun smoothAngle_handlesContinuousCircularSmoothingAcrossZeroDegreesBoundary() {
        // Smoothing between 358° and 2° with alpha = 0.5 should result in 0.0° (or 360.0°)
        val smoothed = QiblaCalculator.smoothAngle(358f, 2f, alpha = 0.5f)
        assertThat(smoothed).isWithin(0.1f).of(0.0f)

        // Smoothing between 10° and 30° with alpha = 0.5 should result in 20.0°
        val smoothedStandard = QiblaCalculator.smoothAngle(10f, 30f, alpha = 0.5f)
        assertThat(smoothedStandard).isWithin(0.1f).of(20.0f)
    }
}
