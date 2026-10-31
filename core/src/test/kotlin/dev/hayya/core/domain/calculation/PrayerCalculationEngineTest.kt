package dev.hayya.core.domain.calculation

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab
import dev.hayya.core.domain.model.AlertTier
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.model.PrayerWindow
import com.google.common.truth.Truth.assertThat
import kotlinx.datetime.LocalDate
import org.junit.Before
import org.junit.Test

class PrayerCalculationEngineTest {

    private lateinit var engine: PrayerCalculationEngine

    // Deterministic test coordinates
    private val karachiCoords = Coordinates(24.8607, 67.0011)
    private val londonCoords = Coordinates(51.5074, -0.1278)
    private val newYorkCoords = Coordinates(40.7128, -74.0060)
    private val makkahCoords = Coordinates(21.4225, 39.8262)

    private val fixedDate = LocalDate(2026, 6, 15)

    @Before
    fun setUp() {
        // Use SolarAstronomicalCalculator for standalone offline determinism
        engine = PrayerCalculationEngine(SolarAstronomicalCalculator())
    }

    @Test
    fun computeDailyTimes_karachi_hasChronologicalOrder() {
        val times = engine.computeDailyTimes(
            date = fixedDate,
            coordinates = karachiCoords,
            method = CalculationMethod.KARACHI,
            madhab = Madhab.HANAFI
        )

        assertThat(times.fajr).isLessThan(times.sunrise)
        assertThat(times.sunrise).isLessThan(times.dhuhr)
        assertThat(times.dhuhr).isLessThan(times.asr)
        assertThat(times.asr).isLessThan(times.maghrib)
        assertThat(times.maghrib).isLessThan(times.isha)
        assertThat(times.isha).isLessThan(times.nextDayFajr)
    }

    @Test
    fun computeDailyTimes_london_hasChronologicalOrder() {
        val times = engine.computeDailyTimes(
            date = fixedDate,
            coordinates = londonCoords,
            method = CalculationMethod.MUSLIM_WORLD_LEAGUE,
            highLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT
        )

        assertThat(times.fajr).isLessThan(times.sunrise)
        assertThat(times.sunrise).isLessThan(times.dhuhr)
        assertThat(times.dhuhr).isLessThan(times.asr)
        assertThat(times.asr).isLessThan(times.maghrib)
        assertThat(times.maghrib).isLessThan(times.isha)
        assertThat(times.isha).isLessThan(times.nextDayFajr)
    }

    @Test
    fun computeDailyTimes_newYork_hasChronologicalOrder() {
        val times = engine.computeDailyTimes(
            date = fixedDate,
            coordinates = newYorkCoords,
            method = CalculationMethod.NORTH_AMERICA
        )

        assertThat(times.fajr).isLessThan(times.sunrise)
        assertThat(times.sunrise).isLessThan(times.dhuhr)
        assertThat(times.dhuhr).isLessThan(times.asr)
        assertThat(times.asr).isLessThan(times.maghrib)
        assertThat(times.maghrib).isLessThan(times.isha)
        assertThat(times.isha).isLessThan(times.nextDayFajr)
    }

    @Test
    fun computeDailyTimes_makkah_hasChronologicalOrder() {
        val times = engine.computeDailyTimes(
            date = fixedDate,
            coordinates = makkahCoords,
            method = CalculationMethod.UMM_AL_QURA
        )

        assertThat(times.fajr).isLessThan(times.sunrise)
        assertThat(times.sunrise).isLessThan(times.dhuhr)
        assertThat(times.dhuhr).isLessThan(times.asr)
        assertThat(times.asr).isLessThan(times.maghrib)
        assertThat(times.maghrib).isLessThan(times.isha)
        assertThat(times.isha).isLessThan(times.nextDayFajr)
    }

    @Test
    fun computeDailyTimes_hanafiAsr_isLaterThanStandardShafiAsr() {
        val shafiTimes = engine.computeDailyTimes(
            date = fixedDate,
            coordinates = karachiCoords,
            method = CalculationMethod.KARACHI,
            madhab = Madhab.SHAFI
        )

        val hanafiTimes = engine.computeDailyTimes(
            date = fixedDate,
            coordinates = karachiCoords,
            method = CalculationMethod.KARACHI,
            madhab = Madhab.HANAFI
        )

        // Hanafi shadow = 2x object length, Shafi shadow = 1x object length
        // Therefore, Hanafi Asr is strictly later than Shafi Asr
        assertThat(hanafiTimes.asr).isGreaterThan(shafiTimes.asr)
        // All other prayers must remain identical
        assertThat(hanafiTimes.fajr).isEqualTo(shafiTimes.fajr)
        assertThat(hanafiTimes.sunrise).isEqualTo(shafiTimes.sunrise)
        assertThat(hanafiTimes.dhuhr).isEqualTo(shafiTimes.dhuhr)
        assertThat(hanafiTimes.maghrib).isEqualTo(shafiTimes.maghrib)
        assertThat(hanafiTimes.isha).isEqualTo(shafiTimes.isha)
    }

    @Test
    fun calculateTriggersForPrayer_generatesExactThreeTiersAndExpirations() {
        val dailyTimes = engine.computeDailyTimes(
            date = fixedDate,
            coordinates = makkahCoords,
            method = CalculationMethod.UMM_AL_QURA
        )

        // Fajr (expires at Sunrise)
        val fajrAlerts = engine.calculateTriggersForPrayer(PrayerType.FAJR, dailyTimes)
        assertThat(fajrAlerts).hasSize(3)
        assertThat(fajrAlerts[0].tier).isEqualTo(AlertTier.PRE_REMINDER)
        assertThat(fajrAlerts[0].triggerEpochMillis).isEqualTo(dailyTimes.fajr - (15 * 60_000L))
        assertThat(fajrAlerts[1].tier).isEqualTo(AlertTier.MAIN_CALL)
        assertThat(fajrAlerts[1].triggerEpochMillis).isEqualTo(dailyTimes.fajr)
        assertThat(fajrAlerts[2].tier).isEqualTo(AlertTier.LAST_CALL)
        assertThat(fajrAlerts[2].triggerEpochMillis).isEqualTo(dailyTimes.sunrise - (20 * 60_000L))

        // Dhuhr (expires at Asr)
        val dhuhrAlerts = engine.calculateTriggersForPrayer(PrayerType.DHUHR, dailyTimes)
        assertThat(dhuhrAlerts[0].triggerEpochMillis).isEqualTo(dailyTimes.dhuhr - (15 * 60_000L))
        assertThat(dhuhrAlerts[1].triggerEpochMillis).isEqualTo(dailyTimes.dhuhr)
        assertThat(dhuhrAlerts[2].triggerEpochMillis).isEqualTo(dailyTimes.asr - (20 * 60_000L))

        // Asr (expires at Maghrib)
        val asrAlerts = engine.calculateTriggersForPrayer(PrayerType.ASR, dailyTimes)
        assertThat(asrAlerts[0].triggerEpochMillis).isEqualTo(dailyTimes.asr - (15 * 60_000L))
        assertThat(asrAlerts[1].triggerEpochMillis).isEqualTo(dailyTimes.asr)
        assertThat(asrAlerts[2].triggerEpochMillis).isEqualTo(dailyTimes.maghrib - (20 * 60_000L))

        // Maghrib (expires at Isha)
        val maghribAlerts = engine.calculateTriggersForPrayer(PrayerType.MAGHRIB, dailyTimes)
        assertThat(maghribAlerts[0].triggerEpochMillis).isEqualTo(dailyTimes.maghrib - (15 * 60_000L))
        assertThat(maghribAlerts[1].triggerEpochMillis).isEqualTo(dailyTimes.maghrib)
        assertThat(maghribAlerts[2].triggerEpochMillis).isEqualTo(dailyTimes.isha - (20 * 60_000L))

        // Isha (expires at Next Day Fajr by default)
        val ishaAlerts = engine.calculateTriggersForPrayer(PrayerType.ISHA, dailyTimes)
        assertThat(ishaAlerts[0].triggerEpochMillis).isEqualTo(dailyTimes.isha - (15 * 60_000L))
        assertThat(ishaAlerts[1].triggerEpochMillis).isEqualTo(dailyTimes.isha)
        assertThat(ishaAlerts[2].triggerEpochMillis).isEqualTo(dailyTimes.nextDayFajr - (20 * 60_000L))

        // Isha with Islamic Midnight expiration
        val ishaMidnightAlerts = engine.calculateTriggersForPrayer(
            PrayerType.ISHA,
            dailyTimes,
            ishaExpiresAtMidnight = true
        )
        assertThat(ishaMidnightAlerts[2].triggerEpochMillis).isEqualTo(dailyTimes.islamicMidnight - (20 * 60_000L))
    }

    @Test
    fun computeRollingQueue_sevenDays_generatesFilteredChronologicalQueue() {
        val params = engine.createParameters(CalculationMethod.MUSLIM_WORLD_LEAGUE)
        val dailyTimes = engine.computeDailyTimes(fixedDate, makkahCoords, params)

        // Set current time to mid-day (after Dhuhr, before Asr)
        val currentMillis = dailyTimes.dhuhr + 1000L

        val queue = engine.computeRollingQueue(
            startDate = fixedDate,
            coordinates = makkahCoords,
            parameters = params,
            daysAhead = 7,
            currentEpochMillis = currentMillis
        )

        // All triggers in the queue must strictly be in the future
        for (alert in queue) {
            assertThat(alert.triggerEpochMillis).isGreaterThan(currentMillis)
        }

        // Entire queue must be strictly monotonic
        for (i in 0 until queue.size - 1) {
            assertThat(queue[i].triggerEpochMillis).isAtMost(queue[i + 1].triggerEpochMillis)
        }

        // Verify notification IDs are valid and deterministic
        for (alert in queue) {
            assertThat(alert.notificationId).isGreaterThan(0)
        }
    }

    @Test
    fun manualMinuteOffsets_adjustPrayerTimesAccurately() {
        val baseTimes = engine.computeDailyTimes(
            date = fixedDate,
            coordinates = makkahCoords,
            method = CalculationMethod.UMM_AL_QURA
        )

        val offsets = mapOf(
            PrayerType.FAJR to 5,     // +5 minutes
            PrayerType.MAGHRIB to -10, // -10 minutes
            PrayerType.ISHA to 45     // exceeds 30m, should clamp to +30m
        )

        val adjustedTimes = engine.computeDailyTimes(
            date = fixedDate,
            coordinates = makkahCoords,
            method = CalculationMethod.UMM_AL_QURA,
            manualOffsets = offsets
        )

        assertThat(adjustedTimes.fajr).isEqualTo(baseTimes.fajr + (5 * 60_000L))
        assertThat(adjustedTimes.maghrib).isEqualTo(baseTimes.maghrib - (10 * 60_000L))
        // Clamped to +30
        assertThat(adjustedTimes.isha).isEqualTo(baseTimes.isha + (30 * 60_000L))
        // Dhuhr has 0 offset
        assertThat(adjustedTimes.dhuhr).isEqualTo(baseTimes.dhuhr)
    }

    @Test
    fun getPrayerWindow_duringDhuhr_reportsActiveDhuhrAndNextAsr() {
        val params = engine.createParameters(CalculationMethod.UMM_AL_QURA)
        val dailyTimes = engine.computeDailyTimes(fixedDate, makkahCoords, params)

        val halfwayDhuhr = dailyTimes.dhuhr + (dailyTimes.asr - dailyTimes.dhuhr) / 2

        val window = engine.getPrayerWindow(
            timestampEpochMillis = halfwayDhuhr,
            coordinates = makkahCoords,
            parameters = params
        )

        assertThat(window).isInstanceOf(PrayerWindow.Active::class.java)
        val active = window as PrayerWindow.Active
        assertThat(active.currentPrayer).isEqualTo(PrayerType.DHUHR)
        assertThat(active.nextPrayer).isEqualTo(PrayerType.ASR)
        assertThat(active.startEpochMillis).isEqualTo(dailyTimes.dhuhr)
        assertThat(active.endEpochMillis).isEqualTo(dailyTimes.asr)
        assertThat(active.elapsedProgress).isWithin(0.01f).of(0.5f)
    }

    @Test
    fun getPrayerWindow_duringSunriseInterval_reportsSunriseIntervalAndNextDhuhr() {
        val params = engine.createParameters(CalculationMethod.UMM_AL_QURA)
        val dailyTimes = engine.computeDailyTimes(fixedDate, makkahCoords, params)

        val halfwaySunrise = dailyTimes.sunrise + (dailyTimes.dhuhr - dailyTimes.sunrise) / 2

        val window = engine.getPrayerWindow(
            timestampEpochMillis = halfwaySunrise,
            coordinates = makkahCoords,
            parameters = params
        )

        assertThat(window).isInstanceOf(PrayerWindow.SunriseInterval::class.java)
        val sunriseWindow = window as PrayerWindow.SunriseInterval
        assertThat(sunriseWindow.currentPrayer).isEqualTo(PrayerType.SUNRISE)
        assertThat(sunriseWindow.nextPrayer).isEqualTo(PrayerType.DHUHR)
        assertThat(sunriseWindow.startEpochMillis).isEqualTo(dailyTimes.sunrise)
        assertThat(sunriseWindow.endEpochMillis).isEqualTo(dailyTimes.dhuhr)
        assertThat(sunriseWindow.elapsedProgress).isWithin(0.01f).of(0.5f)
    }
}
