package dev.hayya.core.domain.friday

import dev.hayya.core.domain.model.PrayerType
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * Authentic Friday (Yawm al-Jumu'ah) Islamic Sunnah reminders and Hadith citations.
 *
 * In Islamic tradition, Friday is the master of days (Sayyid al-Ayyam).
 * The Friday prayer (Salat al-Jumu'ah) replaces Dhuhr prayer for congregants attending the mosque.
 */
object FridayReminders {

    /**
     * Helper to verify if the given Gregorian calendar date falls on Friday (Yawm al-Jumu'ah).
     */
    fun isFriday(date: LocalDate): Boolean = date.dayOfWeek == DayOfWeek.FRIDAY

    /**
     * Returns "JUMU'AH" if [prayerType] is Dhuhr and [isFriday] is true, otherwise standard prayer name.
     */
    fun resolvePrayerDisplayName(prayerType: PrayerType, isFriday: Boolean): String =
        prayerType.displayName(isFriday)
}

/**
 * Authentic Hadith reminder for reciting Surah Al-Kahf on Friday.
 *
 * Source: Al-Mustadrak by Al-Hakim (2/399, no. 3392), Sunan al-Kubra by Al-Bayhaqi (3/249, no. 5856).
 * Graded Sahih (authentic) by Shaykh al-Albani in Sahih al-Jami' (no. 6470).
 */
object FridaySurahKahfReminder {

    const val TITLE = "SURAH AL-KAHF"
    const val ARABIC_TITLE = "سُورَةُ الْكَهْفِ"

    /**
     * Authentic Arabic Hadith text with Tashkeel.
     */
    const val ARABIC_HADITH = "مَنْ قَرَأَ سُورَةَ الْكَهْفِ فِي يَوْمِ الْجُمُعَةِ أَضَاءَ لَهُ مِنَ النُّورِ مَا بَيْنَ الْجُمُعَتَيْنِ"

    /**
     * English translation of the Prophetic tradition.
     */
    const val TRANSLATION = "Whoever recites Surah Al-Kahf on Friday, a light will illuminate for him from between the two Fridays."

    /**
     * Canonical Hadith citation.
     */
    const val REFERENCE = "Al-Bayhaqi 5856 • Al-Hakim 3392 • Sahih al-Jami' 6470"

    /**
     * Primary virtue / benefit.
     */
    const val VIRTUE = "Illuminating light between two Fridays & protection from the trial of the False Messiah (Dajjal)."
}

/**
 * Authentic Hadith reminder for sending abundant blessings upon Prophet Muhammad ﷺ on Friday.
 *
 * Source: Sunan Abi Dawood 1047, Sunan an-Nasa'i 1374, Sunan Ibn Majah 1085.
 * Graded Sahih (authentic) by Shaykh al-Albani.
 */
object FridaySalawatReminder {

    const val TITLE = "BLESSINGS UPON THE PROPHET ﷺ"
    const val ARABIC_TITLE = "الصَّلَاةُ عَلَى النَّبِيِّ ﷺ"

    /**
     * Canonical Salawat formula.
     */
    const val ARABIC_DUROOD = "اللَّهُمَّ صَلِّ وَسَلِّمْ عَلَى نَبِيِّنَا مُحَمَّدٍ"

    /**
     * Standard phonetic English transliteration.
     */
    const val TRANSLITERATION = "Allāhumma ṣalli wa sallim ‘alā Nabiyyinā Muḥammad"

    /**
     * English translation.
     */
    const val TRANSLATION = "O Allah, send peace and blessings upon our Prophet Muhammad."

    /**
     * Hadith quotation on the excellence of Salawat on Friday.
     */
    const val HADITH_QUOTATION = "The Prophet Muhammad ﷺ said: 'Indeed, among the most excellent of your days is Friday... therefore send abundant blessings upon me therein, for your blessings are presented to me.'"

    /**
     * Canonical Hadith citation.
     */
    const val REFERENCE = "Sunan Abi Dawood 1047 • Sahih"
}
