package dev.hayya.core.domain.calculation

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.CalculationParameters
import com.batoulapps.adhan.Coordinates
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab
import dev.hayya.core.domain.model.AlertTier
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.model.PrayerWindow
import dev.hayya.core.domain.model.ScheduledAlert
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/**
 * Immutable value representation of computed prayer times for a single local date.
 * All timestamps are in UTC epoch milliseconds.
 */
data class DailyPrayerTimes(
    val date: LocalDate,
    val coordinates: Coordinates,
    val fajr: Long,
    val sunrise: Long,
    val dhuhr: Long,
    val asr: Long,
    val maghrib: Long,
    val isha: Long,
    val nextDayFajr: Long,
    val islamicMidnight: Long,
    val lastThirdOfTheNight: Long = 0L
) {
    fun timeFor(prayerType: PrayerType): Long = when (prayerType) {
        PrayerType.FAJR -> fajr
        PrayerType.SUNRISE -> sunrise
        PrayerType.DHUHR -> dhuhr
        PrayerType.ASR -> asr
        PrayerType.MAGHRIB -> maghrib
        PrayerType.ISHA -> isha
    }

    /**
     * Returns the next upcoming occurrence of [prayerType] relative to [nowMillis].
     *
     * During the evening Isha window (after Isha, before local midnight) the next prayer is
     * FAJR, but [fajr] refers to this date's (already elapsed) Fajr. In that case tomorrow's
     * Fajr ([nextDayFajr]) is returned so countdowns never target a past timestamp.
     */
    fun upcomingTimeFor(prayerType: PrayerType, nowMillis: Long): Long {
        val time = timeFor(prayerType)
        return if (prayerType == PrayerType.FAJR && time <= nowMillis) nextDayFajr else time
    }

    fun windowEndFor(prayerType: PrayerType, ishaExpiresAtMidnight: Boolean = false): Long = when (prayerType) {
        PrayerType.FAJR -> sunrise
        PrayerType.SUNRISE -> dhuhr
        PrayerType.DHUHR -> asr
        PrayerType.ASR -> maghrib
        PrayerType.MAGHRIB -> isha
        PrayerType.ISHA -> if (ishaExpiresAtMidnight) islamicMidnight else nextDayFajr
    }
}

/**
 * Pure calculation and scheduling engine for Islamic prayer times.
 * Wraps Batoul Apps adhan-kotlin and performs deterministic time calculations,
 * active window resolution, multi-tier alert triggers, and rolling queue generation.
 */
class PrayerCalculationEngine(
    private val astronomicalCalculator: AstronomicalCalculator = AdhanAstronomicalCalculator()
) {

    /**
     * Builds calculation parameters combining method, madhab, and high-latitude rule.
     */
    fun createParameters(
        method: CalculationMethod,
        madhab: Madhab = Madhab.SHAFI,
        highLatitudeRule: HighLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT
    ): CalculationParameters {
        return method.parameters.apply {
            this.madhab = madhab
            this.highLatitudeRule = highLatitudeRule
        }
    }

    /**
     * Pure function computing all prayer times for a given local date and coordinates,
     * applying per-prayer manual minute offsets (-30 to +30 min).
     */
    fun computeDailyTimes(
        date: LocalDate,
        coordinates: Coordinates,
        parameters: CalculationParameters,
        manualOffsets: Map<PrayerType, Int> = emptyMap()
    ): DailyPrayerTimes {
        val todayRaw = astronomicalCalculator.calculatePrayerTimes(coordinates, date, parameters)
        val tomorrowDate = date.plus(1, DateTimeUnit.DAY)
        val tomorrowRaw = astronomicalCalculator.calculatePrayerTimes(coordinates, tomorrowDate, parameters)

        fun applyOffset(rawEpochMillis: Long, prayer: PrayerType): Long {
            val offsetMinutes = manualOffsets[prayer] ?: 0
            val clampedOffset = offsetMinutes.coerceIn(-30, 30)
            return rawEpochMillis + (clampedOffset * 60_000L)
        }

        val fajr = applyOffset(todayRaw.fajrEpochMillis, PrayerType.FAJR)
        val sunrise = applyOffset(todayRaw.sunriseEpochMillis, PrayerType.SUNRISE)
        val dhuhr = applyOffset(todayRaw.dhuhrEpochMillis, PrayerType.DHUHR)
        val asr = applyOffset(todayRaw.asrEpochMillis, PrayerType.ASR)
        val maghrib = applyOffset(todayRaw.maghribEpochMillis, PrayerType.MAGHRIB)
        val isha = applyOffset(todayRaw.ishaEpochMillis, PrayerType.ISHA)
        val nextDayFajr = applyOffset(tomorrowRaw.fajrEpochMillis, PrayerType.FAJR)

        // Islamic Midnight (Nisf al-Layl) = Maghrib + (NextDayFajr - Maghrib) / 2
        // Last Third of the Night (Thuluth al-Akhir) = Maghrib + 2 * (NextDayFajr - Maghrib) / 3
        val nightDuration = (nextDayFajr - maghrib).coerceAtLeast(0L)
        val islamicMidnight = maghrib + (nightDuration / 2L)
        val lastThirdOfTheNight = maghrib + ((2L * nightDuration) / 3L)

        return DailyPrayerTimes(
            date = date,
            coordinates = coordinates,
            fajr = fajr,
            sunrise = sunrise,
            dhuhr = dhuhr,
            asr = asr,
            maghrib = maghrib,
            isha = isha,
            nextDayFajr = nextDayFajr,
            islamicMidnight = islamicMidnight,
            lastThirdOfTheNight = lastThirdOfTheNight
        )
    }

    /**
     * Overload accepting individual method, madhab, and highLatitudeRule parameters.
     */
    fun computeDailyTimes(
        date: LocalDate,
        coordinates: Coordinates,
        method: CalculationMethod,
        madhab: Madhab = Madhab.SHAFI,
        highLatitudeRule: HighLatitudeRule = HighLatitudeRule.MIDDLE_OF_THE_NIGHT,
        manualOffsets: Map<PrayerType, Int> = emptyMap()
    ): DailyPrayerTimes = computeDailyTimes(
        date = date,
        coordinates = coordinates,
        parameters = createParameters(method, madhab, highLatitudeRule),
        manualOffsets = manualOffsets
    )

    /**
     * Retrieves the current active prayer and impending upcoming prayer for an exact millisecond timestamp,
     * including elapsed window progress from 0.0f to 1.0f.
     *
     * Correctly resolves the edge case of midnight crossing (between 00:00 and Fajr), where the active
     * prayer is yesterday's Isha and the next impending prayer is today's Fajr.
     */
    fun getPrayerWindow(
        timestampEpochMillis: Long,
        coordinates: Coordinates,
        parameters: CalculationParameters,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        manualOffsets: Map<PrayerType, Int> = emptyMap(),
        ishaExpiresAtMidnight: Boolean = false
    ): PrayerWindow {
        val instant = Instant.fromEpochMilliseconds(timestampEpochMillis)
        val localDate = instant.toLocalDateTime(timeZone).date

        val todayTimes = computeDailyTimes(localDate, coordinates, parameters, manualOffsets)

        return when {
            // Case 1: Post-midnight before today's Fajr (Isha window from yesterday)
            timestampEpochMillis < todayTimes.fajr -> {
                val yesterdayDate = localDate.minus(1, DateTimeUnit.DAY)
                val yesterdayTimes = computeDailyTimes(yesterdayDate, coordinates, parameters, manualOffsets)
                val windowStart = yesterdayTimes.isha
                val windowEnd = if (ishaExpiresAtMidnight) yesterdayTimes.islamicMidnight else todayTimes.fajr
                val progress = PrayerWindow.calculateProgress(timestampEpochMillis, windowStart, windowEnd)
                PrayerWindow.Active(
                    currentPrayer = PrayerType.ISHA,
                    nextPrayer = PrayerType.FAJR,
                    startEpochMillis = windowStart,
                    endEpochMillis = windowEnd,
                    elapsedProgress = progress
                )
            }

            // Case 2: Fajr window (Fajr to Sunrise)
            timestampEpochMillis in todayTimes.fajr until todayTimes.sunrise -> {
                val progress = PrayerWindow.calculateProgress(
                    timestampEpochMillis,
                    todayTimes.fajr,
                    todayTimes.sunrise
                )
                PrayerWindow.Active(
                    currentPrayer = PrayerType.FAJR,
                    nextPrayer = PrayerType.SUNRISE,
                    startEpochMillis = todayTimes.fajr,
                    endEpochMillis = todayTimes.sunrise,
                    elapsedProgress = progress
                )
            }

            // Case 3: Sunrise interval (Sunrise to Dhuhr / Duha)
            timestampEpochMillis in todayTimes.sunrise until todayTimes.dhuhr -> {
                val progress = PrayerWindow.calculateProgress(
                    timestampEpochMillis,
                    todayTimes.sunrise,
                    todayTimes.dhuhr
                )
                PrayerWindow.SunriseInterval(
                    startEpochMillis = todayTimes.sunrise,
                    endEpochMillis = todayTimes.dhuhr,
                    elapsedProgress = progress
                )
            }

            // Case 4: Dhuhr window (Dhuhr to Asr)
            timestampEpochMillis in todayTimes.dhuhr until todayTimes.asr -> {
                val progress = PrayerWindow.calculateProgress(
                    timestampEpochMillis,
                    todayTimes.dhuhr,
                    todayTimes.asr
                )
                PrayerWindow.Active(
                    currentPrayer = PrayerType.DHUHR,
                    nextPrayer = PrayerType.ASR,
                    startEpochMillis = todayTimes.dhuhr,
                    endEpochMillis = todayTimes.asr,
                    elapsedProgress = progress
                )
            }

            // Case 5: Asr window (Asr to Maghrib)
            timestampEpochMillis in todayTimes.asr until todayTimes.maghrib -> {
                val progress = PrayerWindow.calculateProgress(
                    timestampEpochMillis,
                    todayTimes.asr,
                    todayTimes.maghrib
                )
                PrayerWindow.Active(
                    currentPrayer = PrayerType.ASR,
                    nextPrayer = PrayerType.MAGHRIB,
                    startEpochMillis = todayTimes.asr,
                    endEpochMillis = todayTimes.maghrib,
                    elapsedProgress = progress
                )
            }

            // Case 6: Maghrib window (Maghrib to Isha)
            timestampEpochMillis in todayTimes.maghrib until todayTimes.isha -> {
                val progress = PrayerWindow.calculateProgress(
                    timestampEpochMillis,
                    todayTimes.maghrib,
                    todayTimes.isha
                )
                PrayerWindow.Active(
                    currentPrayer = PrayerType.MAGHRIB,
                    nextPrayer = PrayerType.ISHA,
                    startEpochMillis = todayTimes.maghrib,
                    endEpochMillis = todayTimes.isha,
                    elapsedProgress = progress
                )
            }

            // Case 7: Isha window (Isha to tomorrow's Fajr / Islamic Midnight)
            else -> {
                val windowStart = todayTimes.isha
                val windowEnd = if (ishaExpiresAtMidnight) todayTimes.islamicMidnight else todayTimes.nextDayFajr
                val progress = PrayerWindow.calculateProgress(timestampEpochMillis, windowStart, windowEnd)
                PrayerWindow.Active(
                    currentPrayer = PrayerType.ISHA,
                    nextPrayer = PrayerType.FAJR,
                    startEpochMillis = windowStart,
                    endEpochMillis = windowEnd,
                    elapsedProgress = progress
                )
            }
        }
    }

    /**
     * Calculates the 3 scheduled triggers for a specific prayer:
     * - Tier 1: Exactly 15 minutes before prayer time (T - 15m)
     * - Tier 2: Exactly at prayer start (T0)
     * - Tier 3: Exactly 20 minutes before the prayer window expires (T_end - 20m)
     *   (Fajr expires at Sunrise; Dhuhr at Asr; Asr at Maghrib; Maghrib at Isha; Isha at Fajr/Islamic Midnight)
     */
    fun calculateTriggersForPrayer(
        prayerType: PrayerType,
        dailyTimes: DailyPrayerTimes,
        ishaExpiresAtMidnight: Boolean = false
    ): List<ScheduledAlert> {
        val t0 = dailyTimes.timeFor(prayerType)
        val tEnd = dailyTimes.windowEndFor(prayerType, ishaExpiresAtMidnight)
        val dayOfYear = dailyTimes.date.dayOfYear

        val tier1Trigger = t0 - (15 * 60_000L) // 15m before prayer start
        val tier2Trigger = t0                 // Prayer start (T0)
        val tier3Trigger = tEnd - (20 * 60_000L) // 20m before window expires

        return listOf(
            ScheduledAlert(
                dayOfYear = dayOfYear,
                prayerType = prayerType,
                tier = AlertTier.PRE_REMINDER,
                triggerEpochMillis = tier1Trigger
            ),
            ScheduledAlert(
                dayOfYear = dayOfYear,
                prayerType = prayerType,
                tier = AlertTier.MAIN_CALL,
                triggerEpochMillis = tier2Trigger
            ),
            ScheduledAlert(
                dayOfYear = dayOfYear,
                prayerType = prayerType,
                tier = AlertTier.LAST_CALL,
                triggerEpochMillis = tier3Trigger
            )
        )
    }

    /**
     * Calculates all alerts for the specified prayers on a single day.
     * Defaults to the five obligatory daily prayers (Fajr, Dhuhr, Asr, Maghrib, Isha).
     */
    fun calculateDailyAlerts(
        dailyTimes: DailyPrayerTimes,
        prayers: Set<PrayerType> = DEFAULT_OBLIGATORY_PRAYERS,
        ishaExpiresAtMidnight: Boolean = false
    ): List<ScheduledAlert> {
        return prayers.flatMap { prayer ->
            calculateTriggersForPrayer(prayer, dailyTimes, ishaExpiresAtMidnight)
        }.sorted()
    }

    /**
     * Filters out scheduled alert triggers that have already passed relative to [currentEpochMillis].
     */
    fun filterFutureTriggers(
        alerts: List<ScheduledAlert>,
        currentEpochMillis: Long
    ): List<ScheduledAlert> {
        return alerts.filter { it.triggerEpochMillis > currentEpochMillis }
    }

    /**
     * Computes a rolling queue of [ScheduledAlert] objects for the next [daysAhead] consecutive days (default 7),
     * automatically filtering out past triggers and sorting upcoming triggers chronologically.
     */
    fun computeRollingQueue(
        startDate: LocalDate,
        coordinates: Coordinates,
        parameters: CalculationParameters,
        manualOffsets: Map<PrayerType, Int> = emptyMap(),
        ishaExpiresAtMidnight: Boolean = false,
        daysAhead: Int = 7,
        currentEpochMillis: Long = Clock.System.now().toEpochMilliseconds(),
        prayers: Set<PrayerType> = DEFAULT_OBLIGATORY_PRAYERS
    ): List<ScheduledAlert> {
        require(daysAhead >= 1) { "daysAhead must be at least 1, was $daysAhead" }

        val allAlerts = ArrayList<ScheduledAlert>(daysAhead * prayers.size * 3)

        for (dayIndex in 0 until daysAhead) {
            val date = startDate.plus(dayIndex, DateTimeUnit.DAY)
            val dailyTimes = computeDailyTimes(date, coordinates, parameters, manualOffsets)
            val dailyAlerts = calculateDailyAlerts(dailyTimes, prayers, ishaExpiresAtMidnight)
            allAlerts.addAll(dailyAlerts)
        }

        return filterFutureTriggers(allAlerts, currentEpochMillis).sorted()
    }

    companion object {
        val DEFAULT_OBLIGATORY_PRAYERS: Set<PrayerType> = setOf(
            PrayerType.FAJR,
            PrayerType.DHUHR,
            PrayerType.ASR,
            PrayerType.MAGHRIB,
            PrayerType.ISHA
        )
    }
}
