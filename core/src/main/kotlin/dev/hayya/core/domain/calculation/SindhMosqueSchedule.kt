package dev.hayya.core.domain.calculation

import dev.hayya.core.domain.model.PrayerType
import kotlinx.datetime.LocalDate

enum class SindhSeason(val displayName: String) {
    SUMMER("Summer (Apr–Sep)"),
    WINTER("Winter (Oct–Mar)")
}

/**
 * Snapshot of how local mosques in Larkana Division (Sindh, Pakistan) observe
 * precautionary windows (Ihtiyat, Zawal, Makrooh) and seasonal Jama'at gaps.
 */
data class SindhMosqueDailyInfo(
    val season: SindhSeason,
    val sehriIhtiyatEpochMillis: Long,
    val ishraqStartEpochMillis: Long,
    val zawalStartEpochMillis: Long,
    val dhuhrIhtiyatStartEpochMillis: Long,
    val preMaghribMakroohEpochMillis: Long,
    val jamaatTimesMillis: Map<PrayerType, Long>
)

/**
 * Models year-round local mosque conventions in Pakistan > Sindh > Larkana Division:
 * - Standard 18°/18° Karachi / PMD twilight angles & Hanafi Asr (2x shadow)
 * - 10-minute Sehri Ihtiyat (Imsak) & 10-minute Zawal / Sunrise / Sunset Makrooh buffers
 * - Seasonal Summer vs Winter Adhan-to-Jama'at (Iqamah) intervals practiced by neighborhood masajid
 */
object SindhMosqueSchedule {

    const val SEHRI_IHTIYAT_MINUTES = 10L
    const val ZAWAL_WINDOW_MINUTES = 10L
    const val ISHRAQ_BUFFER_MINUTES = 15L
    const val SUNSET_MAKROOH_MINUTES = 10L

    fun resolveSeason(date: LocalDate): SindhSeason =
        if (date.monthNumber in 4..9) SindhSeason.SUMMER else SindhSeason.WINTER

    /**
     * Typical Adhan-to-Jama'at gap in minutes observed by local mosques in Larkana Division.
     */
    fun typicalJamaatGapMinutes(
        prayer: PrayerType,
        season: SindhSeason,
        isFriday: Boolean = false
    ): Int = when (prayer) {
        PrayerType.FAJR -> if (season == SindhSeason.SUMMER) 25 else 30
        PrayerType.SUNRISE -> 0
        PrayerType.DHUHR -> when {
            isFriday -> 30 // Khutbah + Jumu'ah Adhan (~1:00-1:15 PM Khutbah, ~1:30-2:00 PM Jama'at)
            season == SindhSeason.SUMMER -> 20
            else -> 15
        }
        PrayerType.ASR -> 15
        PrayerType.MAGHRIB -> 4
        PrayerType.ISHA -> if (season == SindhSeason.SUMMER) 20 else 15
    }

    /**
     * Computes all local mosque milestones and estimated Jama'at times for [dailyTimes].
     */
    fun computeDailyInfo(
        date: LocalDate,
        dailyTimes: DailyPrayerTimes,
        isFriday: Boolean = false
    ): SindhMosqueDailyInfo {
        val season = resolveSeason(date)

        val sehriIhtiyat = dailyTimes.fajr - (SEHRI_IHTIYAT_MINUTES * 60_000L)
        val ishraqStart = dailyTimes.sunrise + (ISHRAQ_BUFFER_MINUTES * 60_000L)
        val zawalStart = dailyTimes.dhuhr - (ZAWAL_WINDOW_MINUTES * 60_000L)
        val dhuhrSafeStart = dailyTimes.dhuhr + (2 * 60_000L) // +2m Auqat-e-Namaz zenith buffer
        val preMaghribMakrooh = dailyTimes.maghrib - (SUNSET_MAKROOH_MINUTES * 60_000L)

        // Ensure Fajr Jama'at never exceeds 12 minutes before Sunrise
        val rawFajrJamaat = dailyTimes.fajr + (typicalJamaatGapMinutes(PrayerType.FAJR, season, isFriday) * 60_000L)
        val maxFajrJamaat = dailyTimes.sunrise - (12 * 60_000L)
        val fajrJamaat = rawFajrJamaat.coerceAtMost(maxFajrJamaat.coerceAtLeast(dailyTimes.fajr))

        val jamaatMap = mapOf(
            PrayerType.FAJR to fajrJamaat,
            PrayerType.DHUHR to dailyTimes.dhuhr + (typicalJamaatGapMinutes(PrayerType.DHUHR, season, isFriday) * 60_000L),
            PrayerType.ASR to dailyTimes.asr + (typicalJamaatGapMinutes(PrayerType.ASR, season, isFriday) * 60_000L),
            PrayerType.MAGHRIB to dailyTimes.maghrib + (typicalJamaatGapMinutes(PrayerType.MAGHRIB, season, isFriday) * 60_000L),
            PrayerType.ISHA to dailyTimes.isha + (typicalJamaatGapMinutes(PrayerType.ISHA, season, isFriday) * 60_000L)
        )

        return SindhMosqueDailyInfo(
            season = season,
            sehriIhtiyatEpochMillis = sehriIhtiyat,
            ishraqStartEpochMillis = ishraqStart,
            zawalStartEpochMillis = zawalStart,
            dhuhrIhtiyatStartEpochMillis = dhuhrSafeStart,
            preMaghribMakroohEpochMillis = preMaghribMakrooh,
            jamaatTimesMillis = jamaatMap
        )
    }
}
