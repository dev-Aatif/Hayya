package dev.hayya.core.domain.calculation

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.model.PrayerWindow
import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import org.junit.Before
import org.junit.Test

class EdgeCasesTest {

    private lateinit var engine: PrayerCalculationEngine

    @Before
    fun setUp() {
        engine = PrayerCalculationEngine(SolarAstronomicalCalculator())
    }

    /**
     * Edge Case 1: Midnight Crossing between Isha and Fajr.
     * When local time is between 00:00:00 and today's Fajr (e.g. 02:30 AM),
     * the active prayer must be yesterday's Isha, and the next prayer is today's Fajr.
     */
    @Test
    fun midnightCrossing_postMidnightBeforeFajr_resolvesYesterdayIshaToTodayFajr() {
        val coordinates = Coordinates(21.4225, 39.8262) // Makkah
        val timeZone = TimeZone.of("Asia/Riyadh")
        val params = engine.createParameters(CalculationMethod.UMM_AL_QURA)

        val targetDate = LocalDate(2026, 7, 10)
        val todayTimes = engine.computeDailyTimes(targetDate, coordinates, params)
        val yesterdayTimes = engine.computeDailyTimes(targetDate.minus(1, DateTimeUnit.DAY), coordinates, params)

        // Simulate 02:30 AM local time on targetDate (before Fajr)
        val localDateTime = LocalDateTime(2026, 7, 10, 2, 30, 0)
        val testEpochMillis = localDateTime.toInstant(timeZone).toEpochMilliseconds()

        assertThat(testEpochMillis).isLessThan(todayTimes.fajr)
        assertThat(testEpochMillis).isGreaterThan(yesterdayTimes.isha)

        val window = engine.getPrayerWindow(
            timestampEpochMillis = testEpochMillis,
            coordinates = coordinates,
            parameters = params,
            timeZone = timeZone
        )

        assertThat(window).isInstanceOf(PrayerWindow.Active::class.java)
        val active = window as PrayerWindow.Active
        assertThat(active.currentPrayer).isEqualTo(PrayerType.ISHA)
        assertThat(active.nextPrayer).isEqualTo(PrayerType.FAJR)
        assertThat(active.startEpochMillis).isEqualTo(yesterdayTimes.isha)
        assertThat(active.endEpochMillis).isEqualTo(todayTimes.fajr)

        // Progress must be strictly inside (0.0, 1.0)
        assertThat(active.elapsedProgress).isGreaterThan(0.0f)
        assertThat(active.elapsedProgress).isLessThan(1.0f)
    }

    /**
     * Edge Case 1b: Late night post-Isha before midnight (e.g. 23:30 PM).
     * Active prayer must be today's Isha, and the next prayer is tomorrow's Fajr.
     */
    @Test
    fun midnightCrossing_postIshaBeforeMidnight_resolvesTodayIshaToTomorrowFajr() {
        val coordinates = Coordinates(21.4225, 39.8262) // Makkah
        val timeZone = TimeZone.of("Asia/Riyadh")
        val params = engine.createParameters(CalculationMethod.UMM_AL_QURA)

        val targetDate = LocalDate(2026, 7, 10)
        val todayTimes = engine.computeDailyTimes(targetDate, coordinates, params)

        // Simulate 23:30 PM local time on targetDate
        val localDateTime = LocalDateTime(2026, 7, 10, 23, 30, 0)
        val testEpochMillis = localDateTime.toInstant(timeZone).toEpochMilliseconds()

        assertThat(testEpochMillis).isGreaterThan(todayTimes.isha)

        val window = engine.getPrayerWindow(
            timestampEpochMillis = testEpochMillis,
            coordinates = coordinates,
            parameters = params,
            timeZone = timeZone
        )

        assertThat(window).isInstanceOf(PrayerWindow.Active::class.java)
        val active = window as PrayerWindow.Active
        assertThat(active.currentPrayer).isEqualTo(PrayerType.ISHA)
        assertThat(active.nextPrayer).isEqualTo(PrayerType.FAJR)
        assertThat(active.startEpochMillis).isEqualTo(todayTimes.isha)
        assertThat(active.endEpochMillis).isEqualTo(todayTimes.nextDayFajr)
        assertThat(active.elapsedProgress).isGreaterThan(0.0f)
        assertThat(active.elapsedProgress).isLessThan(1.0f)
    }

    /**
     * Edge Case 2: Daylight Saving Time (DST) Transitions.
     * London UK switches GMT -> BST on March 29, 2026 (clocks spring forward 1 hour),
     * and BST -> GMT on October 25, 2026 (clocks fall back 1 hour).
     *
     * In UTC epoch milliseconds, astronomical triggers must remain strictly monotonic
     * and continuous with zero duplicate or skipped notifications.
     */
    @Test
    fun daylightSaving_springForwardAndFallBack_triggersAreMonotonic() {
        val londonCoords = Coordinates(51.5074, -0.1278)
        val params = engine.createParameters(CalculationMethod.MUSLIM_WORLD_LEAGUE)

        // Spring forward transition: March 29, 2026
        val springStartDate = LocalDate(2026, 3, 28)
        val springQueue = engine.computeRollingQueue(
            startDate = springStartDate,
            coordinates = londonCoords,
            parameters = params,
            daysAhead = 4,
            currentEpochMillis = 0L // Keep all triggers
        )

        // Verify strictly monotonic order across DST transition
        for (i in 0 until springQueue.size - 1) {
            assertThat(springQueue[i].triggerEpochMillis)
                .isLessThan(springQueue[i + 1].triggerEpochMillis)
        }

        // Fall back transition: October 25, 2026
        val fallStartDate = LocalDate(2026, 10, 24)
        val fallQueue = engine.computeRollingQueue(
            startDate = fallStartDate,
            coordinates = londonCoords,
            parameters = params,
            daysAhead = 4,
            currentEpochMillis = 0L
        )

        // Verify strictly monotonic order across DST fall-back
        for (i in 0 until fallQueue.size - 1) {
            assertThat(fallQueue[i].triggerEpochMillis)
                .isLessThan(fallQueue[i + 1].triggerEpochMillis)
        }
    }

    /**
     * Edge Case 3: High-Latitude Summer Solstice (Oslo, Norway / Reykjavik, Iceland).
     * At high latitudes during summer solstice (June 21), true astronomical twilight does not reach -18°.
     * High latitude rules (Middle of Night / Seventh of Night) must adjust Fajr and Isha so that
     * all prayers maintain strict chronological order without throwing exceptions or generating NaN.
     */
    @Test
    fun highLatitude_summerSolstice_computesValidChronologicalTimesWithRule() {
        // Oslo, Norway (approx 60° North)
        val osloCoords = Coordinates(59.9139, 10.7522)
        val summerSolstice = LocalDate(2026, 6, 21)

        // Test with Middle of the Night rule
        val middleOfNightTimes = engine.computeDailyTimes(
            date = summerSolstice,
            coordinates = osloCoords,
            method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT
        )

        assertThat(middleOfNightTimes.fajr).isLessThan(middleOfNightTimes.sunrise)
        assertThat(middleOfNightTimes.sunrise).isLessThan(middleOfNightTimes.dhuhr)
        assertThat(middleOfNightTimes.dhuhr).isLessThan(middleOfNightTimes.asr)
        assertThat(middleOfNightTimes.asr).isLessThan(middleOfNightTimes.maghrib)
        assertThat(middleOfNightTimes.maghrib).isLessThan(middleOfNightTimes.isha)
        assertThat(middleOfNightTimes.isha).isLessThan(middleOfNightTimes.nextDayFajr)

        // Test with Seventh of the Night rule
        val seventhOfNightTimes = engine.computeDailyTimes(
            date = summerSolstice,
            coordinates = osloCoords,
            method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            highLatitudeRule = HighLatitudeRule.SEVENTH_OF_THE_NIGHT
        )

        assertThat(seventhOfNightTimes.fajr).isLessThan(seventhOfNightTimes.sunrise)
        assertThat(seventhOfNightTimes.sunrise).isLessThan(seventhOfNightTimes.dhuhr)
        assertThat(seventhOfNightTimes.dhuhr).isLessThan(seventhOfNightTimes.asr)
        assertThat(seventhOfNightTimes.asr).isLessThan(seventhOfNightTimes.maghrib)
        assertThat(seventhOfNightTimes.maghrib).isLessThan(seventhOfNightTimes.isha)
        assertThat(seventhOfNightTimes.isha).isLessThan(seventhOfNightTimes.nextDayFajr)

        // Verify alert triggers can be scheduled for all 5 prayers without exceptions
        val alerts = engine.calculateDailyAlerts(middleOfNightTimes)
        assertThat(alerts).hasSize(15) // 5 prayers * 3 tiers
        for (alert in alerts) {
            assertThat(alert.triggerEpochMillis).isGreaterThan(0L)
            assertThat(alert.notificationId).isGreaterThan(0)
        }
    }
}
