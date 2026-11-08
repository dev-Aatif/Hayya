package dev.hayya.core.domain.ramadan

import kotlinx.datetime.LocalDate
import java.time.chrono.HijrahChronology
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Status of daily fast during Ramadan or voluntary fasting throughout the year.
 */
enum class FastStatus(val displayName: String, val shortLabel: String) {
    FASTED("Fasted", "Fasted"),
    MISSED("Missed", "Missed"),
    EXEMPT("Exempt", "Exempt"),
    NOT_RECORDED("Pending", "Pending")
}

/**
 * Record of fasting completion for a specific calendar date and Hijri day.
 */
data class DailyFastRecord(
    val date: LocalDate,
    val hijriDay: Int = 1,
    val status: FastStatus = FastStatus.NOT_RECORDED,
    val note: String = ""
) {
    val isFasted: Boolean get() = status == FastStatus.FASTED
    val isMissed: Boolean get() = status == FastStatus.MISSED
    val isExempt: Boolean get() = status == FastStatus.EXEMPT
}

/**
 * Summary progress for the entire month of Ramadan.
 */
data class RamadanMonthProgress(
    val hijriYear: Int = 1447,
    val dailyRecords: Map<Int, FastStatus> = emptyMap(),
    val qadhaMadeUpCount: Int = 0,
    val totalDays: Int = 30
) {
    val fastedCount: Int get() = dailyRecords.values.count { it == FastStatus.FASTED }
    val missedCount: Int get() = dailyRecords.values.count { it == FastStatus.MISSED }
    val exemptCount: Int get() = dailyRecords.values.count { it == FastStatus.EXEMPT }
    val pendingCount: Int get() = (totalDays - (fastedCount + missedCount + exemptCount)).coerceAtLeast(0)

    val qadhaRemainingCount: Int
        get() = (missedCount - qadhaMadeUpCount).coerceAtLeast(0)

    val completionPercentage: Float
        get() = if (totalDays <= 0) 0f else (fastedCount.toFloat() / totalDays).coerceIn(0f, 1f)
}

/**
 * Special Islamic observances and celebratory events.
 */
enum class IslamicEvent(val title: String, val description: String) {
    NONE("Regular Day", ""),
    RAMADAN("Ramadan Mubarak", "The holy month of fasting, reflection, and the Quran"),
    EID_AL_FITR("Eid al-Fitr Mubarak", "The festival of breaking the fast (1st Shawwal)"),
    DAY_OF_ARAFAH("Day of Arafah", "The pinnacle of Hajj; fasting expiates sins of previous and coming year"),
    EID_AL_ADHA("Eid al-Adha Mubarak", "The festival of the sacrifice (10th Dhul-Hijjah)"),
    ASHURA("Day of Ashura", "10th of Muharram; fast commemorating Musa (AS)'s deliverance")
}

/**
 * Resolved Islamic calendar details for a specific date.
 */
data class IslamicDateInfo(
    val gregorianDate: LocalDate,
    val hijriDay: Int,
    val hijriMonth: Int,
    val hijriYear: Int,
    val hijriMonthName: String,
    val formattedHijriDate: String,
    val activeEvent: IslamicEvent = IslamicEvent.NONE,
    val daysUntilRamadan: Int? = null
) {
    val isRamadan: Boolean get() = hijriMonth == 9
    val isEidAlFitr: Boolean get() = hijriMonth == 10 && hijriDay == 1
    val isEidAlAdha: Boolean get() = hijriMonth == 12 && hijriDay == 10
    val isArafah: Boolean get() = hijriMonth == 12 && hijriDay == 9
}

/**
 * Schedule for a single day of Ramadan containing computed Suhoor & Iftar times.
 */
data class RamadanDaySchedule(
    val dayNumber: Int,
    val gregorianDate: LocalDate,
    val suhoorCutoffMillis: Long,
    val sunriseMillis: Long,
    val iftarMillis: Long,
    val formattedDate: String,
    val formattedSuhoor: String,
    val formattedIftar: String,
    val formattedFastDuration: String
)

/**
 * Configuration preferences for Ramadan alerts and Eid greetings.
 */
data class RamadanSettings(
    val suhoorAlarmEnabled: Boolean = false,
    val suhoorAlarmMinutesBefore: Int = 40,
    val iftarNotificationEnabled: Boolean = true,
    val eidNotificationEnabled: Boolean = true
)

/**
 * Categories of authentic Ramadan and Eid supplications.
 */
enum class RamadanDuaCategory(val displayName: String) {
    ALL("All Duas"),
    IFTAR("Iftar"),
    SUHOOR("Suhoor"),
    LAYLAT_AL_QADR("Laylat al-Qadr"),
    ASHRAS("Three Ashras"),
    EID("Eid & Takbirat")
}

/**
 * Authentic verified supplication with Arabic script, transliteration, translation, and source.
 */
data class RamadanDua(
    val id: String,
    val title: String,
    val category: RamadanDuaCategory,
    val arabicText: String,
    val transliteration: String,
    val translation: String,
    val reference: String,
    val contextNote: String = ""
)

/**
 * Repository of authentic Sunnah supplications for Ramadan, fasting, and Eid.
 */
object RamadanDuasStore {

    val allDuas: List<RamadanDua> = listOf(
        RamadanDua(
            id = "iftar_primary",
            title = "Breaking Fast (Iftar) — Primary Hadith",
            category = RamadanDuaCategory.IFTAR,
            arabicText = "ذَهَبَ الظَّمَأُ وَابْتَلَّتِ الْعُرُوقُ وَثَبَتَ الأَجْرُ إِنْ شَاءَ اللَّهُ",
            transliteration = "Dhahaba az-zama'u wabtallatil-'urooqu wa thabatal-ajru in sha'Allah",
            translation = "The thirst is gone, the veins are moistened, and the reward is confirmed, if Allah wills.",
            reference = "Sunan Abi Dawud 2357 • Hasan",
            contextNote = "Recited directly upon breaking the fast with dates or water."
        ),
        RamadanDua(
            id = "iftar_traditional",
            title = "Breaking Fast (Iftar) — Supplication of Gratitude",
            category = RamadanDuaCategory.IFTAR,
            arabicText = "اللَّهُمَّ إِنِّي لَكَ صُمْتُ وَبِكَ آمَنْتُ وَعَلَى رِزْقِكَ أَفْطَرْتُ",
            transliteration = "Allahumma inni laka sumtu wa bika aamantu wa 'ala rizqika aftartu",
            translation = "O Allah, for You I have fasted, in You I have believed, and upon Your provision I break my fast.",
            reference = "Sunan Abi Dawud 2358",
            contextNote = "Recited at the moment of Iftar before taking the first morsel."
        ),
        RamadanDua(
            id = "suhoor_niyyah",
            title = "Intention for Fasting (Niyyah)",
            category = RamadanDuaCategory.SUHOOR,
            arabicText = "وَبِصَوْمِ غَدٍ نَّوَيْتُ مِنْ شَهْرِ رَمَضَانَ",
            transliteration = "Wa bisawmi ghadin nawaytu min shahri Ramadan",
            translation = "And I intend to fast tomorrow in the month of Ramadan.",
            reference = "Consensus of Jurists (Intention of the Heart)",
            contextNote = "Made before Fajr dawn each night of Ramadan."
        ),
        RamadanDua(
            id = "laylat_al_qadr",
            title = "Night of Decree (Laylat al-Qadr)",
            category = RamadanDuaCategory.LAYLAT_AL_QADR,
            arabicText = "اللَّهُمَّ إِنَّكَ عَفُوٌّ تُحِبُّ الْعَفْوَ فَاعْفُ عَنِّي",
            transliteration = "Allahumma innaka 'afuwwun tuhibbul-'afwa fa'fu 'anni",
            translation = "O Allah, You are Most Forgiving, You love forgiveness, so forgive me.",
            reference = "Jami` at-Tirmidhi 3513 • Sahih",
            contextNote = "Taught by the Prophet ﷺ to Aisha (RA) for the last ten nights of Ramadan."
        ),
        RamadanDua(
            id = "ashra_first",
            title = "First 10 Days (Days 1–10) — Days of Mercy (Rahmah)",
            category = RamadanDuaCategory.ASHRAS,
            arabicText = "رَبِّ اغْفِرْ وَارْحَمْ وَأَنْتَ خَيْرُ الرَّاحِمِينَ",
            transliteration = "Rabbighfir warham wa anta khayrur-rahimeen",
            translation = "My Lord! Forgive and have mercy, for You are the Best of those who show mercy.",
            reference = "Surah Al-Mu'minun 23:118",
            contextNote = "Abundant recitation during the first Ashra of Ramadan."
        ),
        RamadanDua(
            id = "ashra_second",
            title = "Second 10 Days (Days 11–20) — Days of Forgiveness (Maghfirah)",
            category = RamadanDuaCategory.ASHRAS,
            arabicText = "أَسْتَغْفِرُ اللَّهَ رَبِّي مِنْ كُلِّ ذَنْبٍ وَأَتُوبُ إِلَيْهِ",
            transliteration = "Astaghfirullaha Rabbi min kulli dhambin wa atoobu ilayh",
            translation = "I ask forgiveness from Allah, my Lord, for every sin and I turn to Him in repentance.",
            reference = "Authentic Istighfar Supplication",
            contextNote = "Recited persistently during the middle Ashra of Ramadan."
        ),
        RamadanDua(
            id = "ashra_third",
            title = "Third 10 Days (Days 21–30) — Freedom from Hellfire (Nijat)",
            category = RamadanDuaCategory.ASHRAS,
            arabicText = "اللَّهُمَّ أَجِرْنِي مِنَ النَّارِ",
            transliteration = "Allahumma ajirni minan-naar",
            translation = "O Allah, grant me refuge from the Fire.",
            reference = "Sunan Abi Dawud 5079 • Sahih",
            contextNote = "Seeking salvation in the final ten nights of Ramadan."
        ),
        RamadanDua(
            id = "fasting_provocation",
            title = "When Provoked or Insulted While Fasting",
            category = RamadanDuaCategory.SUHOOR,
            arabicText = "إِنِّي صَائِمٌ ، إِنِّي صَائِمٌ",
            transliteration = "Inni saa'im, inni saa'im",
            translation = "I am fasting, indeed I am fasting.",
            reference = "Sahih al-Bukhari 1904 • Sahih Muslim 1151",
            contextNote = "Restraint reminder when facing anger or dispute while in a state of fasting."
        ),
        RamadanDua(
            id = "iftar_with_host",
            title = "When Breaking Fast at Another's Home",
            category = RamadanDuaCategory.IFTAR,
            arabicText = "أَفْطَرَ عِنْدَكُمُ الصَّائِمُونَ وَأَكَلَ طَعَامَكُمُ الأَبْرَارُ وَصَلَّتْ عَلَيْكُمُ الْمَلاَئِكَةُ",
            transliteration = "Aftara 'indakumus-saa'imoona wa akala ta'aamakumul-abraaru wa sallat 'alaykumul-malaa'ikah",
            translation = "May those who are fasting break their fast with you, may the dutiful eat your food, and may the angels send blessings upon you.",
            reference = "Sunan Abi Dawud 3854 • Sahih",
            contextNote = "Dua for the host after eating Iftar with family or community."
        ),
        RamadanDua(
            id = "eid_takbeerat",
            title = "The Takbirat of Eid",
            category = RamadanDuaCategory.EID,
            arabicText = "اللَّهُ أَكْبَرُ اللَّهُ أَكْبَرُ لَا إِلَهَ إِلَّا اللَّهُ، وَاللَّهُ أَكْبَرُ اللَّهُ أَكْبَرُ وَلِلَّهِ الْحَمْدُ",
            transliteration = "Allahu Akbar, Allahu Akbar, La ilaha illallah, Wallahu Akbar, Allahu Akbar, wa lillahil-hamd",
            translation = "Allah is the greatest, Allah is the greatest, there is no god but Allah. Allah is the greatest, Allah is the greatest, and to Allah belongs all praise.",
            reference = "Authentic Sunnah of Eid (Musannaf Ibn Abi Shaybah 5633)",
            contextNote = "Recited continuously on Eid al-Fitr (from Maghrib before Eid until prayer) and Eid al-Adha (throughout Days of Tashreeq)."
        )
    )
}

/**
 * Sunnah etiquette and guides for Eid al-Fitr and Eid al-Adha celebrations.
 */
object EidGuide {

    data class SunnahPractice(
        val title: String,
        val description: String,
        val hadithReference: String
    )

    val eidAlFitrSunnahs: List<SunnahPractice> = listOf(
        SunnahPractice(
            title = "Purification & Ghusl",
            description = "Perform the full ritual bath (Ghusl) on the morning of Eid before heading to the prayer ground.",
            hadithReference = "Sunan Ibn Majah 1315"
        ),
        SunnahPractice(
            title = "Eating Dates Before Leaving",
            description = "Eat an odd number of dates (e.g. 3, 5, or 7) before leaving for Eid al-Fitr prayer to symbolize that the month of fasting has concluded.",
            hadithReference = "Sahih al-Bukhari 953"
        ),
        SunnahPractice(
            title = "Wearing Best Clothes & Perfume",
            description = "Adorn yourself with your best and cleanest garments and apply pleasant fragrance (for men).",
            hadithReference = "Sahih al-Bukhari 948"
        ),
        SunnahPractice(
            title = "Paying Zakat al-Fitr",
            description = "Ensure Zakat al-Fitr is distributed to the needy before the Eid prayer begins so everyone may feast.",
            hadithReference = "Sahih al-Bukhari 1503"
        ),
        SunnahPractice(
            title = "Chanting Takbirat Openly",
            description = "Recite the Takbirat aloud on the way to the Musalla until the Imam arrives to begin prayer.",
            hadithReference = "Silsilat al-Ahaadeeth as-Saheehah 171"
        ),
        SunnahPractice(
            title = "Taking Alternate Routes",
            description = "Walk to the prayer ground using one path and return home via a different route to maximize good deeds and greetings.",
            hadithReference = "Sahih al-Bukhari 986"
        )
    )

    val eidAlAdhaSunnahs: List<SunnahPractice> = listOf(
        SunnahPractice(
            title = "Takbirat al-Tashreeq",
            description = "Recite the Takbirat after every obligatory prayer starting from Fajr on the Day of Arafah (9th Dhul-Hijjah) until Asr on the 13th.",
            hadithReference = "Irwaa' al-Ghaleel 654"
        ),
        SunnahPractice(
            title = "Fast Until the Eid Prayer",
            description = "Unlike Eid al-Fitr, it is Sunnah to refrain from eating on Eid al-Adha morning until returning from prayer to eat from the sacrifice (Udhiyah).",
            hadithReference = "Jami` at-Tirmidhi 542"
        ),
        SunnahPractice(
            title = "Performing the Qurbani (Udhiyah)",
            description = "Slaughter the sacrificial animal with the intention of pleasing Allah following the Eid prayer.",
            hadithReference = "Sahih al-Bukhari 5558"
        ),
        SunnahPractice(
            title = "Greeting One Another",
            description = "Say 'Taqabbal Allahu Minna wa Minkum' (May Allah accept good deeds from us and you).",
            hadithReference = "Fath al-Bari (2/446)"
        )
    )
}

/**
 * Islamic calendar conversion helper utilizing [java.time.chrono.HijrahChronology].
 */
object IslamicCalendarHelper {

    private val HIJRI_MONTH_NAMES = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhu al-Qi'dah", "Dhu al-Hijjah"
    )

    fun getMonthName(monthNumber: Int): String {
        return if (monthNumber in 1..12) HIJRI_MONTH_NAMES[monthNumber - 1] else "Month $monthNumber"
    }

    /**
     * Resolves the full [IslamicDateInfo] for a given Gregorian [LocalDate].
     */
    fun resolveIslamicDate(date: LocalDate): IslamicDateInfo {
        return try {
            val javaDate = java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
            val hijrahDate = HijrahDate.from(javaDate)

            val hijriDay = hijrahDate.get(ChronoField.DAY_OF_MONTH)
            val hijriMonth = hijrahDate.get(ChronoField.MONTH_OF_YEAR)
            val hijriYear = hijrahDate.get(ChronoField.YEAR)

            val monthName = getMonthName(hijriMonth)
            val formatted = "$hijriDay $monthName $hijriYear AH"

            val event = when {
                hijriMonth == 9 -> IslamicEvent.RAMADAN
                hijriMonth == 10 && hijriDay == 1 -> IslamicEvent.EID_AL_FITR
                hijriMonth == 12 && hijriDay == 9 -> IslamicEvent.DAY_OF_ARAFAH
                hijriMonth == 12 && hijriDay == 10 -> IslamicEvent.EID_AL_ADHA
                hijriMonth == 1 && hijriDay == 10 -> IslamicEvent.ASHURA
                else -> IslamicEvent.NONE
            }

            val daysUntilRamadan = if (hijriMonth == 9) {
                0
            } else {
                estimateDaysUntilRamadan(javaDate, hijriYear, hijriMonth)
            }

            IslamicDateInfo(
                gregorianDate = date,
                hijriDay = hijriDay,
                hijriMonth = hijriMonth,
                hijriYear = hijriYear,
                hijriMonthName = monthName,
                formattedHijriDate = formatted,
                activeEvent = event,
                daysUntilRamadan = daysUntilRamadan
            )
        } catch (e: Exception) {
            // Graceful fallback for unexpected chronologic errors
            IslamicDateInfo(
                gregorianDate = date,
                hijriDay = 1,
                hijriMonth = 9,
                hijriYear = 1447,
                hijriMonthName = "Ramadan",
                formattedHijriDate = "Ramadan 1447 AH",
                activeEvent = IslamicEvent.RAMADAN,
                daysUntilRamadan = 0
            )
        }
    }

    /**
     * Finds the Gregorian [LocalDate] for the 1st of Ramadan of the given [hijriYear].
     */
    fun getRamadanStartDate(hijriYear: Int): LocalDate {
        return try {
            val ramadan1 = HijrahChronology.INSTANCE.date(hijriYear, 9, 1)
            val gregorian = java.time.LocalDate.from(ramadan1)
            LocalDate(gregorian.year, gregorian.monthValue, gregorian.dayOfMonth)
        } catch (e: Exception) {
            LocalDate(2026, 2, 18) // Safe fallback
        }
    }

    private fun estimateDaysUntilRamadan(
        currentDate: java.time.LocalDate,
        currentHijriYear: Int,
        currentHijriMonth: Int
    ): Int {
        return try {
            val targetHijriYear = if (currentHijriMonth > 9) currentHijriYear + 1 else currentHijriYear
            val targetRamadan1 = HijrahChronology.INSTANCE.date(targetHijriYear, 9, 1)
            val targetGregorian = java.time.LocalDate.from(targetRamadan1)
            val diff = ChronoUnit.DAYS.between(currentDate, targetGregorian)
            diff.toInt().coerceAtLeast(0)
        } catch (e: Exception) {
            0
        }
    }
}
