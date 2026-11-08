package dev.hayya.core.domain.calculation

import dev.hayya.core.domain.ramadan.IslamicCalendarHelper
import dev.hayya.core.domain.ramadan.IslamicDateInfo
import dev.hayya.core.domain.ramadan.IslamicEvent
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.util.Locale

/**
 * Types of Islamic fasting throughout the year.
 */
enum class FastingType(
    val title: String,
    val badge: String,
    val description: String
) {
    RAMADAN("Ramadan Fast", "Obligatory", "The holy month of fasting"),
    AYYAM_AL_BEED("Ayyam al-Beed", "Sunnah", "The White Days (13th, 14th, 15th)"),
    MONDAY_THURSDAY("Monday / Thursday", "Sunnah", "Prophetic weekly Sunnah fast"),
    DAY_OF_ARAFAH("Day of Arafah", "Virtuous", "9th Dhul-Hijjah fast expiating two years"),
    ASHURA("Day of Ashura", "Virtuous", "10th Muharram fast"),
    TASUAH("Day of Tasu'a", "Sunnah", "9th Muharram companion fast"),
    VOLUNTARY("Voluntary Fast", "Nafl", "General voluntary fast for the sake of Allah"),
    NONE("Regular Day", "", "")
}

/**
 * Detailed calculations for the Islamic Night and Tahajjud (Qiyam al-Layl).
 *
 * The Islamic night begins at Maghrib (sunset) and ends at Fajr (true dawn).
 * - Islamic Midnight (Nisf al-Layl): Maghrib + (NightDuration / 2)
 * - Last Third of the Night (Thuluth al-Akhir): Maghrib + 2 * (NightDuration / 3) = Fajr - (NightDuration / 3)
 */
data class TahajjudScheduleInfo(
    val islamicMidnightEpochMillis: Long,
    val lastThirdStartEpochMillis: Long,
    val fajrCutoffEpochMillis: Long,
    val isTahajjudActiveNow: Boolean,
    val targetEpochMillis: Long,
    val formattedIslamicMidnight: String,
    val formattedLastThirdStart: String,
    val formattedFajrCutoff: String,
    val remainingMillis: Long
)

/**
 * Summary of fasting observances and Suhur/Iftar times for today and tomorrow.
 */
data class VoluntaryFastingInfo(
    val isFastingDayToday: Boolean,
    val isFastingDayTomorrow: Boolean,
    val todayFastingType: FastingType,
    val tomorrowFastingType: FastingType,
    val todayReason: String,
    val tomorrowReason: String,
    val suhurCutoffEpochMillis: Long,
    val iftarEpochMillis: Long,
    val formattedSuhurCutoff: String,
    val formattedIftar: String,
    val isDaytimeFasting: Boolean,
    val fastingTargetMillis: Long,
    val remainingFastingMillis: Long
)

/**
 * Pure calculation engine for Islamic Midnight, Last Third of the Night (Tahajjud),
 * and Voluntary Sunnah Fasting (Ramadan, Mondays/Thursdays, Ayyam al-Beed 13th-15th).
 */
object NightFastingCalculator {

    /**
     * Computes the Last Third of the Night start and Islamic Midnight.
     *
     * @param maghribEpochMillis Maghrib sunset of the night in question.
     * @param fajrEpochMillis Fajr dawn terminating the night.
     */
    fun computeNightWindows(
        maghribEpochMillis: Long,
        fajrEpochMillis: Long
    ): Pair<Long, Long> {
        val nightDuration = (fajrEpochMillis - maghribEpochMillis).coerceAtLeast(0L)
        val islamicMidnight = maghribEpochMillis + (nightDuration / 2L)
        val lastThirdStart = maghribEpochMillis + ((2L * nightDuration) / 3L)
        return Pair(islamicMidnight, lastThirdStart)
    }

    /**
     * Resolves the current or upcoming Tahajjud schedule relative to [nowEpochMillis].
     *
     * Correctly handles all 3 phases of the 24-hour cycle:
     * 1. Early morning before today's Fajr (now < fajr):
     *    The night in progress started at yesterday's Maghrib and ends at today's Fajr.
     * 2. Daytime between today's Fajr and today's Maghrib (fajr <= now < maghrib):
     *    Tahajjud is upcoming tonight, beginning at today's Maghrib and ending at tomorrow's Fajr.
     * 3. Evening after today's Maghrib (now >= maghrib):
     *    Tonight's night is active, ending at tomorrow's Fajr.
     */
    fun resolveTahajjudSchedule(
        dailyTimes: DailyPrayerTimes,
        nowEpochMillis: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        yesterdayMaghribMillis: Long? = null
    ): TahajjudScheduleInfo {
        val todayFajr = dailyTimes.fajr
        val todayMaghrib = dailyTimes.maghrib
        val tomorrowFajr = dailyTimes.nextDayFajr

        val (midnightMillis, lastThirdStartMillis, fajrEndMillis, isActiveNow, targetMillis) = when {
            // Case 1: Early morning before today's Fajr
            nowEpochMillis < todayFajr -> {
                // If yesterday's Maghrib is not provided, approximate using typical night duration
                val effectiveYesterdayMaghrib = yesterdayMaghribMillis
                    ?: (todayFajr - (tomorrowFajr - todayMaghrib).coerceAtLeast(0L))
                val (midnight, lastThird) = computeNightWindows(effectiveYesterdayMaghrib, todayFajr)

                val active = nowEpochMillis in lastThird until todayFajr
                val target = if (active) todayFajr else if (nowEpochMillis < lastThird) lastThird else todayMaghrib
                NightResolution(midnight, lastThird, todayFajr, active, target)
            }
            // Case 2: Daytime or evening (now >= todayFajr)
            else -> {
                val (midnight, lastThird) = computeNightWindows(todayMaghrib, tomorrowFajr)
                val active = nowEpochMillis in lastThird until tomorrowFajr
                val target = if (active) tomorrowFajr else lastThird
                NightResolution(midnight, lastThird, tomorrowFajr, active, target)
            }
        }

        val remainingMillis = (targetMillis - nowEpochMillis).coerceAtLeast(0L)

        return TahajjudScheduleInfo(
            islamicMidnightEpochMillis = midnightMillis,
            lastThirdStartEpochMillis = lastThirdStartMillis,
            fajrCutoffEpochMillis = fajrEndMillis,
            isTahajjudActiveNow = isActiveNow,
            targetEpochMillis = targetMillis,
            formattedIslamicMidnight = formatTimeOfDay(midnightMillis, timeZone),
            formattedLastThirdStart = formatTimeOfDay(lastThirdStartMillis, timeZone),
            formattedFajrCutoff = formatTimeOfDay(fajrEndMillis, timeZone),
            remainingMillis = remainingMillis
        )
    }

    /**
     * Resolves today's and tomorrow's voluntary/obligatory fasting opportunities.
     */
    fun resolveVoluntaryFasting(
        localDate: LocalDate,
        islamicDate: IslamicDateInfo,
        dailyTimes: DailyPrayerTimes,
        nowEpochMillis: Long,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): VoluntaryFastingInfo {
        val tomorrowDate = localDate.plus(1, DateTimeUnit.DAY)
        val tomorrowIslamic = IslamicCalendarHelper.resolveIslamicDate(tomorrowDate)

        val todayType = determineFastingType(localDate, islamicDate)
        val tomorrowType = determineFastingType(tomorrowDate, tomorrowIslamic)

        val todayReason = formatFastingReason(todayType, islamicDate)
        val tomorrowReason = formatFastingReason(tomorrowType, tomorrowIslamic)

        val suhurCutoff = if (nowEpochMillis < dailyTimes.fajr) dailyTimes.fajr else dailyTimes.nextDayFajr
        val iftar = dailyTimes.maghrib

        val isDaytimeFasting = nowEpochMillis in dailyTimes.fajr until dailyTimes.maghrib
        val fastingTarget = if (isDaytimeFasting) iftar else suhurCutoff
        val remainingFasting = (fastingTarget - nowEpochMillis).coerceAtLeast(0L)

        return VoluntaryFastingInfo(
            isFastingDayToday = todayType != FastingType.NONE,
            isFastingDayTomorrow = tomorrowType != FastingType.NONE,
            todayFastingType = todayType,
            tomorrowFastingType = tomorrowType,
            todayReason = todayReason,
            tomorrowReason = tomorrowReason,
            suhurCutoffEpochMillis = suhurCutoff,
            iftarEpochMillis = iftar,
            formattedSuhurCutoff = formatTimeOfDay(suhurCutoff, timeZone),
            formattedIftar = formatTimeOfDay(iftar, timeZone),
            isDaytimeFasting = isDaytimeFasting,
            fastingTargetMillis = fastingTarget,
            remainingFastingMillis = remainingFasting
        )
    }

    /**
     * Evaluates whether a given calendar and Hijri date qualifies as an Islamic fasting day.
     */
    fun determineFastingType(
        date: LocalDate,
        islamicDate: IslamicDateInfo
    ): FastingType {
        // 1. Ramadan (Month 9): Obligatory fast every day
        if (islamicDate.isRamadan) {
            return FastingType.RAMADAN
        }

        // Days of Eid: Fasting is strictly prohibited
        if (islamicDate.isEidAlFitr || islamicDate.isEidAlAdha) {
            return FastingType.NONE
        }

        // Days of Tashreeq (11th, 12th, 13th Dhul-Hijjah): Fasting is prohibited
        if (islamicDate.hijriMonth == 12 && islamicDate.hijriDay in 11..13) {
            return FastingType.NONE
        }

        // 2. Day of Arafah (9th Dhul-Hijjah) for non-pilgrims
        if (islamicDate.isArafah) {
            return FastingType.DAY_OF_ARAFAH
        }

        // 3. Day of Ashura (10th Muharram) & Tasu'a (9th Muharram)
        if (islamicDate.hijriMonth == 1 && islamicDate.hijriDay == 10) {
            return FastingType.ASHURA
        }
        if (islamicDate.hijriMonth == 1 && islamicDate.hijriDay == 9) {
            return FastingType.TASUAH
        }

        // 4. Ayyam al-Beed (The White Days: 13th, 14th, 15th of each lunar month)
        if (islamicDate.hijriDay in 13..15) {
            return FastingType.AYYAM_AL_BEED
        }

        // 5. Sunnah Mondays & Thursdays
        if (date.dayOfWeek == DayOfWeek.MONDAY || date.dayOfWeek == DayOfWeek.THURSDAY) {
            return FastingType.MONDAY_THURSDAY
        }

        return FastingType.NONE
    }

    private fun formatFastingReason(type: FastingType, islamicDate: IslamicDateInfo): String = when (type) {
        FastingType.RAMADAN -> "Ramadan Fast (Day ${islamicDate.hijriDay})"
        FastingType.AYYAM_AL_BEED -> "Ayyam al-Beed (${islamicDate.hijriDay}th ${islamicDate.hijriMonthName})"
        FastingType.MONDAY_THURSDAY -> "Sunnah Fast (${if (islamicDate.gregorianDate.dayOfWeek == DayOfWeek.MONDAY) "Monday" else "Thursday"})"
        FastingType.DAY_OF_ARAFAH -> "Day of Arafah (Expiates 2 Years)"
        FastingType.ASHURA -> "Day of Ashura (10th Muharram)"
        FastingType.TASUAH -> "Day of Tasu'a (9th Muharram)"
        FastingType.VOLUNTARY -> "Voluntary Fast"
        FastingType.NONE -> ""
    }

    fun formatDuration(durationMillis: Long): String {
        val totalSeconds = (durationMillis / 1000L).coerceAtLeast(0L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    }

    fun formatTimeOfDay(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        val instant = Instant.fromEpochMilliseconds(epochMillis)
        val localTime = instant.toLocalDateTime(timeZone).time
        val hour = localTime.hour
        val minute = localTime.minute

        val period = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.US, "%02d:%02d %s", displayHour, minute, period)
    }

    private data class NightResolution(
        val midnightMillis: Long,
        val lastThirdStartMillis: Long,
        val fajrEndMillis: Long,
        val isActiveNow: Boolean,
        val targetMillis: Long
    )
}
