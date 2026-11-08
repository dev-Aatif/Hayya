package dev.hayya.core.domain.friday

import com.google.common.truth.Truth.assertThat
import dev.hayya.core.domain.model.AutoMuteDuration
import dev.hayya.core.domain.model.FridayAutoMuteDuration
import dev.hayya.core.domain.model.PrayerType
import kotlinx.datetime.LocalDate
import org.junit.Test

class FridayRemindersTest {

    @Test
    fun isFriday_correctlyIdentifiesFridaysAndNonFridays() {
        val thursday = LocalDate(2026, 11, 5)
        val friday = LocalDate(2026, 11, 6)
        val saturday = LocalDate(2026, 11, 7)
        val sunday = LocalDate(2026, 11, 8)

        assertThat(FridayReminders.isFriday(thursday)).isFalse()
        assertThat(FridayReminders.isFriday(friday)).isTrue()
        assertThat(FridayReminders.isFriday(saturday)).isFalse()
        assertThat(FridayReminders.isFriday(sunday)).isFalse()
    }

    @Test
    fun resolvePrayerDisplayName_returnsJumuahForDhuhrOnFridayOnly() {
        assertThat(FridayReminders.resolvePrayerDisplayName(PrayerType.DHUHR, isFriday = true))
            .isEqualTo("JUMU'AH")

        assertThat(FridayReminders.resolvePrayerDisplayName(PrayerType.DHUHR, isFriday = false))
            .isEqualTo("DHUHR")
    }

    @Test
    fun resolvePrayerDisplayName_preservesAllOtherPrayersOnFriday() {
        listOf(
            PrayerType.FAJR,
            PrayerType.SUNRISE,
            PrayerType.ASR,
            PrayerType.MAGHRIB,
            PrayerType.ISHA
        ).forEach { prayer ->
            assertThat(FridayReminders.resolvePrayerDisplayName(prayer, isFriday = true))
                .isEqualTo(prayer.name)
            assertThat(FridayReminders.resolvePrayerDisplayName(prayer, isFriday = false))
                .isEqualTo(prayer.name)
        }
    }

    @Test
    fun prayerTypeDisplayNameExtension_behavesIdenticallyToHelper() {
        assertThat(PrayerType.DHUHR.displayName(isFriday = true)).isEqualTo("JUMU'AH")
        assertThat(PrayerType.DHUHR.displayName(isFriday = false)).isEqualTo("DHUHR")
        assertThat(PrayerType.FAJR.displayName(isFriday = true)).isEqualTo("FAJR")
    }

    @Test
    fun fridayAutoMuteDuration_resolvesCorrectMinutes() {
        // MATCH_STANDARD uses the standard duration
        assertThat(FridayAutoMuteDuration.MATCH_STANDARD.resolveMinutes(AutoMuteDuration.OFF))
            .isEqualTo(0)
        assertThat(FridayAutoMuteDuration.MATCH_STANDARD.resolveMinutes(AutoMuteDuration.MINUTES_20))
            .isEqualTo(20)
        assertThat(FridayAutoMuteDuration.MATCH_STANDARD.resolveMinutes(AutoMuteDuration.MINUTES_30))
            .isEqualTo(30)

        // MINUTES_45 always resolves to 45 minutes regardless of standard duration
        assertThat(FridayAutoMuteDuration.MINUTES_45.resolveMinutes(AutoMuteDuration.OFF))
            .isEqualTo(45)
        assertThat(FridayAutoMuteDuration.MINUTES_45.resolveMinutes(AutoMuteDuration.MINUTES_20))
            .isEqualTo(45)

        // MINUTES_60 always resolves to 60 minutes
        assertThat(FridayAutoMuteDuration.MINUTES_60.resolveMinutes(AutoMuteDuration.OFF))
            .isEqualTo(60)
        assertThat(FridayAutoMuteDuration.MINUTES_60.resolveMinutes(AutoMuteDuration.MINUTES_25))
            .isEqualTo(60)

        // OFF always resolves to 0
        assertThat(FridayAutoMuteDuration.OFF.resolveMinutes(AutoMuteDuration.MINUTES_30))
            .isEqualTo(0)
    }

    @Test
    fun authenticHadithReferences_surahKahfAndSalawat_containCanonicalTexts() {
        // Surah Al-Kahf authentic Hadith metadata
        assertThat(FridaySurahKahfReminder.TITLE).isEqualTo("SURAH AL-KAHF")
        assertThat(FridaySurahKahfReminder.REFERENCE).contains("Sahih al-Jami' 6470")
        assertThat(FridaySurahKahfReminder.ARABIC_HADITH).contains("مَنْ قَرَأَ سُورَةَ الْكَهْفِ")
        assertThat(FridaySurahKahfReminder.TRANSLATION).contains("light will illuminate for him")

        // Salawat authentic Hadith metadata
        assertThat(FridaySalawatReminder.TITLE).isEqualTo("BLESSINGS UPON THE PROPHET ﷺ")
        assertThat(FridaySalawatReminder.REFERENCE).contains("Sunan Abi Dawood 1047")
        assertThat(FridaySalawatReminder.ARABIC_DUROOD).contains("اللَّهُمَّ صَلِّ وَسَلِّمْ")
        assertThat(FridaySalawatReminder.HADITH_QUOTATION).contains("most excellent of your days is Friday")
    }
}
