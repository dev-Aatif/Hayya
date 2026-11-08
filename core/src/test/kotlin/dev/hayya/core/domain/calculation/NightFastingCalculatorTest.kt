package dev.hayya.core.domain.calculation

import com.batoulapps.adhan.Coordinates
import com.google.common.truth.Truth.assertThat
import dev.hayya.core.domain.ramadan.IslamicCalendarHelper
import dev.hayya.core.domain.ramadan.IslamicDateInfo
import dev.hayya.core.domain.ramadan.IslamicEvent
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Test

class NightFastingCalculatorTest {

    private val fixedDate = LocalDate(2026, 6, 15)
    private val makkahCoords = Coordinates(21.4225, 39.8262)

    @Test
    fun computeNightWindows_exactMathematicalDivision() {
        // Night from Maghrib (18:00) to Fajr (06:00 next day) = 12 hours = 43,200,000 ms
        val maghribMillis = 100_000_000L
        val fajrMillis = maghribMillis + 43_200_000L

        val (midnight, lastThird) = NightFastingCalculator.computeNightWindows(maghribMillis, fajrMillis)

        // Islamic Midnight = Maghrib + 6 hours = 100M + 21.6M = 121,600,000 ms
        assertThat(midnight).isEqualTo(maghribMillis + 21_600_000L)

        // Last Third = Maghrib + 8 hours (2/3 of 12h) = 100M + 28.8M = 128,800,000 ms
        assertThat(lastThird).isEqualTo(maghribMillis + 28_800_000L)

        // Verify chronological progression
        assertThat(maghribMillis).isLessThan(midnight)
        assertThat(midnight).isLessThan(lastThird)
        assertThat(lastThird).isLessThan(fajrMillis)

        // Verify Last Third duration equals exactly one third of the night
        val lastThirdDuration = fajrMillis - lastThird
        val nightDuration = fajrMillis - maghribMillis
        assertThat(lastThirdDuration).isEqualTo(nightDuration / 3L)
    }

    @Test
    fun resolveTahajjudSchedule_earlyMorningBeforeLastThird_targetsLastThird() {
        // Simulate night from 18:00 to 06:00 (12 hours)
        // Last third starts at 02:00
        val maghribMillis = 100_000_000L
        val fajrMillis = maghribMillis + 43_200_000L // 06:00
        val lastThirdStart = maghribMillis + 28_800_000L // 02:00

        // Current time: 01:00 (before last third)
        val nowMillis = maghribMillis + 25_200_000L

        val dailyTimes = DailyPrayerTimes(
            date = fixedDate,
            coordinates = makkahCoords,
            fajr = fajrMillis,
            sunrise = fajrMillis + 5_000_000L,
            dhuhr = fajrMillis + 20_000_000L,
            asr = fajrMillis + 30_000_000L,
            maghrib = fajrMillis + 40_000_000L,
            isha = fajrMillis + 45_000_000L,
            nextDayFajr = fajrMillis + 86_400_000L,
            islamicMidnight = maghribMillis + 21_600_000L,
            lastThirdOfTheNight = lastThirdStart
        )

        val schedule = NightFastingCalculator.resolveTahajjudSchedule(
            dailyTimes = dailyTimes,
            nowEpochMillis = nowMillis,
            yesterdayMaghribMillis = maghribMillis
        )

        assertThat(schedule.isTahajjudActiveNow).isFalse()
        assertThat(schedule.targetEpochMillis).isEqualTo(lastThirdStart)
        assertThat(schedule.remainingMillis).isEqualTo(lastThirdStart - nowMillis)
    }

    @Test
    fun resolveTahajjudSchedule_insideLastThird_marksActiveNowAndTargetsFajr() {
        val maghribMillis = 100_000_000L
        val fajrMillis = maghribMillis + 43_200_000L // 06:00
        val lastThirdStart = maghribMillis + 28_800_000L // 02:00

        // Current time: 03:30 (inside last third window)
        val nowMillis = maghribMillis + 34_200_000L

        val dailyTimes = DailyPrayerTimes(
            date = fixedDate,
            coordinates = makkahCoords,
            fajr = fajrMillis,
            sunrise = fajrMillis + 5_000_000L,
            dhuhr = fajrMillis + 20_000_000L,
            asr = fajrMillis + 30_000_000L,
            maghrib = fajrMillis + 40_000_000L,
            isha = fajrMillis + 45_000_000L,
            nextDayFajr = fajrMillis + 86_400_000L,
            islamicMidnight = maghribMillis + 21_600_000L,
            lastThirdOfTheNight = lastThirdStart
        )

        val schedule = NightFastingCalculator.resolveTahajjudSchedule(
            dailyTimes = dailyTimes,
            nowEpochMillis = nowMillis,
            yesterdayMaghribMillis = maghribMillis
        )

        assertThat(schedule.isTahajjudActiveNow).isTrue()
        assertThat(schedule.targetEpochMillis).isEqualTo(fajrMillis)
        assertThat(schedule.remainingMillis).isEqualTo(fajrMillis - nowMillis)
    }

    @Test
    fun resolveTahajjudSchedule_duringDaytime_targetsTonightLastThird() {
        val todayFajr = 100_000_000L
        val todayMaghrib = todayFajr + 40_000_000L
        val tomorrowFajr = todayMaghrib + 30_000_000L

        // Current time: 14:00 (after today Fajr, before today Maghrib)
        val nowMillis = todayFajr + 25_000_000L

        val tonightNightDuration = tomorrowFajr - todayMaghrib
        val tonightLastThird = todayMaghrib + (2L * tonightNightDuration / 3L)

        val dailyTimes = DailyPrayerTimes(
            date = fixedDate,
            coordinates = makkahCoords,
            fajr = todayFajr,
            sunrise = todayFajr + 5_000_000L,
            dhuhr = todayFajr + 20_000_000L,
            asr = todayFajr + 28_000_000L,
            maghrib = todayMaghrib,
            isha = todayMaghrib + 5_000_000L,
            nextDayFajr = tomorrowFajr,
            islamicMidnight = todayMaghrib + (tonightNightDuration / 2L),
            lastThirdOfTheNight = tonightLastThird
        )

        val schedule = NightFastingCalculator.resolveTahajjudSchedule(
            dailyTimes = dailyTimes,
            nowEpochMillis = nowMillis
        )

        assertThat(schedule.isTahajjudActiveNow).isFalse()
        assertThat(schedule.targetEpochMillis).isEqualTo(tonightLastThird)
        assertThat(schedule.remainingMillis).isEqualTo(tonightLastThird - nowMillis)
    }

    @Test
    fun determineFastingType_identifiesRamadanCorrectly() {
        val ramadanDate = IslamicDateInfo(
            gregorianDate = LocalDate(2026, 2, 20),
            hijriDay = 3,
            hijriMonth = 9,
            hijriYear = 1447,
            hijriMonthName = "Ramadan",
            formattedHijriDate = "3 Ramadan 1447 AH",
            activeEvent = IslamicEvent.RAMADAN
        )

        val type = NightFastingCalculator.determineFastingType(ramadanDate.gregorianDate, ramadanDate)
        assertThat(type).isEqualTo(FastingType.RAMADAN)
    }

    @Test
    fun determineFastingType_identifiesAyyamAlBeedCorrectly() {
        listOf(13, 14, 15).forEach { day ->
            val ayyamDate = IslamicDateInfo(
                gregorianDate = LocalDate(2026, 5, 1),
                hijriDay = day,
                hijriMonth = 11, // Dhu al-Qi'dah
                hijriYear = 1447,
                hijriMonthName = "Dhu al-Qi'dah",
                formattedHijriDate = "$day Dhu al-Qi'dah 1447 AH"
            )

            val type = NightFastingCalculator.determineFastingType(ayyamDate.gregorianDate, ayyamDate)
            assertThat(type).isEqualTo(FastingType.AYYAM_AL_BEED)
        }
    }

    @Test
    fun determineFastingType_identifiesMondaysAndThursdaysCorrectly() {
        // 2026-06-15 is a Monday
        val mondayDate = LocalDate(2026, 6, 15)
        assertThat(mondayDate.dayOfWeek).isEqualTo(DayOfWeek.MONDAY)

        val regularIslamicDate = IslamicDateInfo(
            gregorianDate = mondayDate,
            hijriDay = 1,
            hijriMonth = 1,
            hijriYear = 1448,
            hijriMonthName = "Muharram",
            formattedHijriDate = "1 Muharram 1448 AH"
        )

        val mondayType = NightFastingCalculator.determineFastingType(mondayDate, regularIslamicDate)
        assertThat(mondayType).isEqualTo(FastingType.MONDAY_THURSDAY)

        // 2026-06-18 is a Thursday
        val thursdayDate = LocalDate(2026, 6, 18)
        assertThat(thursdayDate.dayOfWeek).isEqualTo(DayOfWeek.THURSDAY)

        val thursdayType = NightFastingCalculator.determineFastingType(thursdayDate, regularIslamicDate.copy(gregorianDate = thursdayDate))
        assertThat(thursdayType).isEqualTo(FastingType.MONDAY_THURSDAY)
    }

    @Test
    fun determineFastingType_eidDaysAreProhibited() {
        val eidFitr = IslamicDateInfo(
            gregorianDate = LocalDate(2026, 3, 20),
            hijriDay = 1,
            hijriMonth = 10,
            hijriYear = 1447,
            hijriMonthName = "Shawwal",
            formattedHijriDate = "1 Shawwal 1447 AH",
            activeEvent = IslamicEvent.EID_AL_FITR
        )
        assertThat(NightFastingCalculator.determineFastingType(eidFitr.gregorianDate, eidFitr)).isEqualTo(FastingType.NONE)

        val eidAdha = IslamicDateInfo(
            gregorianDate = LocalDate(2026, 5, 27),
            hijriDay = 10,
            hijriMonth = 12,
            hijriYear = 1447,
            hijriMonthName = "Dhu al-Hijjah",
            formattedHijriDate = "10 Dhu al-Hijjah 1447 AH",
            activeEvent = IslamicEvent.EID_AL_ADHA
        )
        assertThat(NightFastingCalculator.determineFastingType(eidAdha.gregorianDate, eidAdha)).isEqualTo(FastingType.NONE)
    }

    @Test
    fun resolveVoluntaryFasting_tracksSuhurCutoffAndIftarAccurately() {
        val fajr = 100_000_000L
        val maghrib = fajr + 45_000_000L
        val nextDayFajr = maghrib + 35_000_000L

        val dailyTimes = DailyPrayerTimes(
            date = fixedDate,
            coordinates = makkahCoords,
            fajr = fajr,
            sunrise = fajr + 5_000_000L,
            dhuhr = fajr + 20_000_000L,
            asr = fajr + 30_000_000L,
            maghrib = maghrib,
            isha = maghrib + 5_000_000L,
            nextDayFajr = nextDayFajr,
            islamicMidnight = maghrib + 17_500_000L,
            lastThirdOfTheNight = maghrib + (2L * 35_000_000L / 3L)
        )

        // Daytime test: 12:00 (between Fajr and Maghrib)
        val daytimeNow = fajr + 20_000_000L
        val mondayDate = LocalDate(2026, 6, 15) // Monday
        val islamicDate = IslamicDateInfo(
            gregorianDate = mondayDate,
            hijriDay = 1,
            hijriMonth = 1,
            hijriYear = 1448,
            hijriMonthName = "Muharram",
            formattedHijriDate = "1 Muharram 1448 AH"
        )

        val fastingInfo = NightFastingCalculator.resolveVoluntaryFasting(
            localDate = mondayDate,
            islamicDate = islamicDate,
            dailyTimes = dailyTimes,
            nowEpochMillis = daytimeNow
        )

        assertThat(fastingInfo.isFastingDayToday).isTrue()
        assertThat(fastingInfo.todayFastingType).isEqualTo(FastingType.MONDAY_THURSDAY)
        assertThat(fastingInfo.isDaytimeFasting).isTrue()
        assertThat(fastingInfo.fastingTargetMillis).isEqualTo(maghrib)
        assertThat(fastingInfo.remainingFastingMillis).isEqualTo(maghrib - daytimeNow)
    }
}
