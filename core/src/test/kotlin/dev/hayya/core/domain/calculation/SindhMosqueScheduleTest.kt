package dev.hayya.core.domain.calculation

import com.batoulapps.adhan.Coordinates
import com.google.common.truth.Truth.assertThat
import dev.hayya.core.domain.model.PrayerType
import kotlinx.datetime.LocalDate
import org.junit.Test

class SindhMosqueScheduleTest {

    private val minuteMs = 60_000L

    private fun sampleDailyTimes(
        fajr: Long = 1_700_000_000_000L,
        sunriseGapMinutes: Long = 80L
    ): DailyPrayerTimes = DailyPrayerTimes(
        date = LocalDate(2026, 6, 15),
        coordinates = Coordinates(27.5590, 68.2120),
        fajr = fajr,
        sunrise = fajr + sunriseGapMinutes * minuteMs,
        dhuhr = fajr + 450 * minuteMs,
        asr = fajr + 680 * minuteMs,
        maghrib = fajr + 840 * minuteMs,
        isha = fajr + 930 * minuteMs,
        nextDayFajr = fajr + 1440 * minuteMs,
        islamicMidnight = fajr + 1140 * minuteMs
    )

    @Test
    fun resolveSeason_distinguishesSummerAndWinterMonths() {
        assertThat(SindhMosqueSchedule.resolveSeason(LocalDate(2026, 4, 1))).isEqualTo(SindhSeason.SUMMER)
        assertThat(SindhMosqueSchedule.resolveSeason(LocalDate(2026, 7, 15))).isEqualTo(SindhSeason.SUMMER)
        assertThat(SindhMosqueSchedule.resolveSeason(LocalDate(2026, 9, 30))).isEqualTo(SindhSeason.SUMMER)

        assertThat(SindhMosqueSchedule.resolveSeason(LocalDate(2026, 10, 1))).isEqualTo(SindhSeason.WINTER)
        assertThat(SindhMosqueSchedule.resolveSeason(LocalDate(2026, 1, 10))).isEqualTo(SindhSeason.WINTER)
        assertThat(SindhMosqueSchedule.resolveSeason(LocalDate(2026, 3, 31))).isEqualTo(SindhSeason.WINTER)
    }

    @Test
    fun computeDailyInfo_calculatesMosquePrecautionAndNafilahWindows() {
        val daily = sampleDailyTimes()
        val info = SindhMosqueSchedule.computeDailyInfo(
            date = LocalDate(2026, 6, 15),
            dailyTimes = daily,
            isFriday = false
        )

        assertThat(info.season).isEqualTo(SindhSeason.SUMMER)
        assertThat(info.sehriIhtiyatEpochMillis).isEqualTo(daily.fajr - 10 * minuteMs)
        assertThat(info.ishraqStartEpochMillis).isEqualTo(daily.sunrise + 15 * minuteMs)
        assertThat(info.zawalStartEpochMillis).isEqualTo(daily.dhuhr - 10 * minuteMs)
        assertThat(info.dhuhrIhtiyatStartEpochMillis).isEqualTo(daily.dhuhr + 2 * minuteMs)
        assertThat(info.preMaghribMakroohEpochMillis).isEqualTo(daily.maghrib - 10 * minuteMs)
    }

    @Test
    fun computeDailyInfo_appliesSummerAndWinterJamaatOffsetsAndClampsFajr() {
        val daily = sampleDailyTimes(sunriseGapMinutes = 80L)
        val summer = SindhMosqueSchedule.computeDailyInfo(
            date = LocalDate(2026, 6, 15),
            dailyTimes = daily,
            isFriday = false
        )
        assertThat(summer.jamaatTimesMillis[PrayerType.FAJR]).isEqualTo(daily.fajr + 25 * minuteMs)
        assertThat(summer.jamaatTimesMillis[PrayerType.DHUHR]).isEqualTo(daily.dhuhr + 20 * minuteMs)
        assertThat(summer.jamaatTimesMillis[PrayerType.ASR]).isEqualTo(daily.asr + 15 * minuteMs)
        assertThat(summer.jamaatTimesMillis[PrayerType.MAGHRIB]).isEqualTo(daily.maghrib + 4 * minuteMs)
        assertThat(summer.jamaatTimesMillis[PrayerType.ISHA]).isEqualTo(daily.isha + 20 * minuteMs)

        val winterFriday = SindhMosqueSchedule.computeDailyInfo(
            date = LocalDate(2026, 12, 18),
            dailyTimes = daily,
            isFriday = true
        )
        assertThat(winterFriday.season).isEqualTo(SindhSeason.WINTER)
        assertThat(winterFriday.jamaatTimesMillis[PrayerType.FAJR]).isEqualTo(daily.fajr + 30 * minuteMs)
        assertThat(winterFriday.jamaatTimesMillis[PrayerType.DHUHR]).isEqualTo(daily.dhuhr + 30 * minuteMs)
        assertThat(winterFriday.jamaatTimesMillis[PrayerType.ASR]).isEqualTo(daily.asr + 15 * minuteMs)
        assertThat(winterFriday.jamaatTimesMillis[PrayerType.ISHA]).isEqualTo(daily.isha + 15 * minuteMs)

        // Tight Fajr-to-Sunrise gap should clamp Fajr Jama'at to 12 min before Sunrise
        val shortFajrWindow = sampleDailyTimes(sunriseGapMinutes = 30L)
        val clamped = SindhMosqueSchedule.computeDailyInfo(
            date = LocalDate(2026, 12, 18),
            dailyTimes = shortFajrWindow
        )
        assertThat(clamped.jamaatTimesMillis[PrayerType.FAJR])
            .isEqualTo(shortFajrWindow.sunrise - 12 * minuteMs)
    }
}
