package dev.hayya.core.domain.hadith

/**
 * Supported reading and share-card translation languages for the Hadith module.
 */
enum class HadithLanguage(
    val code: String,
    val displayName: String,
    val nativeLabel: String,
    val isRtl: Boolean
) {
    ENGLISH("en", "English", "EN", false),
    URDU("ur", "Urdu", "اردو", true),
    SINDHI("sd", "Sindhi", "سنڌي", true);

    companion object {
        fun fromCode(code: String?): HadithLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}

/**
 * Prioritized spiritual, worship, creed, character, and tafsir pillars.
 * Legal/penal jurisprudence chapters are intentionally excluded from default curated feeds.
 */
enum class HadithPillar(
    val id: String,
    val title: String,
    val subtitle: String
) {
    WORSHIP(
        id = "worship",
        title = "Prayer & Worship",
        subtitle = "Salah, Zakat, fasting, purification & dhikr"
    ),
    CREED(
        id = "creed",
        title = "Creed & Theology",
        subtitle = "Tawhid, Iman, pillars of faith & Akhirah"
    ),
    VIRTUES(
        id = "virtues",
        title = "Virtues & Spirituality",
        subtitle = "Good deeds, heart purification & gratitude"
    ),
    MANNERS(
        id = "manners",
        title = "Ethics & Manners",
        subtitle = "Character, family ties, honesty & kindness"
    ),
    TAFSIR(
        id = "tafsir",
        title = "Quran & Tafsir",
        subtitle = "Virtues & exegesis of the Noble Qur'an"
    );

    companion object {
        fun fromId(id: String?): HadithPillar {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: VIRTUES
        }
    }
}

/**
 * Filter chips displayed at the top of the Hadith hub.
 */
enum class HadithFeedFilter(val label: String) {
    ALL("All"),
    SAVED("Saved"),
    WORSHIP("Prayer & Worship"),
    CREED("Creed"),
    VIRTUES("Virtues"),
    MANNERS("Manners"),
    TAFSIR("Quran & Tafsir");

    fun toPillarOrNull(): HadithPillar? = when (this) {
        WORSHIP -> HadithPillar.WORSHIP
        CREED -> HadithPillar.CREED
        VIRTUES -> HadithPillar.VIRTUES
        MANNERS -> HadithPillar.MANNERS
        TAFSIR -> HadithPillar.TAFSIR
        ALL, SAVED -> null
    }
}

/**
 * Text content for a single language variant of a Hadith.
 */
data class HadithTextVariant(
    val title: String,
    val matn: String,
    val explanation: String = "",
    val hints: List<String> = emptyList()
)

/**
 * Resolved translation output with fallback tracking when a regional translation
 * (e.g., Sindhi) is not yet available for an individual entry.
 */
data class ResolvedHadithTranslation(
    val language: HadithLanguage,
    val variant: HadithTextVariant,
    val isFallback: Boolean
)

/**
 * Complete domain model for a stored Prophetic Hadith across Arabic and supported translations.
 */
data class HadithEntry(
    val id: String,
    val pillar: HadithPillar,
    val categoryId: String,
    val categoryTitle: String,
    val attribution: String,
    val grade: String,
    val arabic: HadithTextVariant,
    val translations: Map<HadithLanguage, HadithTextVariant>,
    val isBookmarked: Boolean = false,
    val isRead: Boolean = false,
    val bookId: String = "",
    val bookName: String = "",
    val isPriority: Boolean = true
) {
    fun resolveTranslation(preferred: HadithLanguage): ResolvedHadithTranslation {
        val direct = translations[preferred]?.takeIf { it.matn.isNotBlank() }
        if (direct != null) {
            return ResolvedHadithTranslation(preferred, direct, isFallback = false)
        }

        val fallbackOrder = when (preferred) {
            HadithLanguage.SINDHI -> listOf(HadithLanguage.URDU, HadithLanguage.ENGLISH)
            HadithLanguage.URDU -> listOf(HadithLanguage.ENGLISH)
            HadithLanguage.ENGLISH -> listOf(HadithLanguage.URDU, HadithLanguage.SINDHI)
        }
        for (lang in fallbackOrder) {
            val candidate = translations[lang]?.takeIf { it.matn.isNotBlank() }
            if (candidate != null) {
                return ResolvedHadithTranslation(lang, candidate, isFallback = true)
            }
        }
        return ResolvedHadithTranslation(HadithLanguage.ENGLISH, arabic, isFallback = true)
    }
}

/**
 * Sync status for a remote HadeethEnc thematic category.
 */
data class HadithCategorySyncInfo(
    val id: String,
    val title: String,
    val pillar: HadithPillar,
    val remoteCount: Int,
    val syncedCount: Int
)

/**
 * Metadata and offline download state for a canonical Hadith book (e.g., Sahih al-Bukhari).
 */
data class HadithBookInfo(
    val bookId: String,
    val title: String,
    val subtitle: String,
    val downloadedCount: Int = 0,
    val downloadedLanguages: List<String> = emptyList()
) {
    val isDownloaded: Boolean get() = downloadedCount > 0
}

/**
 * Curated canonical books available from the open-source fawazahmed0/hadith-api CDN.
 */
object CanonicalHadithBooks {
    val catalog: List<HadithBookInfo> = listOf(
        HadithBookInfo("nawawi", "40 Hadith an-Nawawi", "Essential foundational narrations • AR / EN"),
        HadithBookInfo("qudsi", "40 Hadith Qudsi", "Sacred narrations from Allah • AR / EN"),
        HadithBookInfo("bukhari", "Sahih al-Bukhari", "Full collection • AR / EN / UR"),
        HadithBookInfo("muslim", "Sahih Muslim", "Full collection • AR / EN / UR"),
        HadithBookInfo("tirmidhi", "Jami at-Tirmidhi", "Full collection • AR / EN / UR"),
        HadithBookInfo("abudawud", "Sunan Abu Dawud", "Full collection • AR / EN / UR"),
        HadithBookInfo("nasai", "Sunan an-Nasa'i", "Full collection • AR / EN / UR"),
        HadithBookInfo("ibnmajah", "Sunan Ibn Majah", "Full collection • AR / EN / UR"),
        HadithBookInfo("malik", "Muwatta Malik", "Full collection • AR / EN / UR")
    )
}

/**
 * Visual theme presets for generated WhatsApp Status share cards.
 */
enum class ShareCardTheme(val label: String) {
    OLED_GOLD("OLED Gold"),
    WARM_SAND("Warm Sand"),
    EMERALD_NIGHT("Emerald Night")
}

/**
 * Aspect ratio presets for generated shareable cards.
 */
enum class ShareCardAspect(
    val label: String,
    val widthPx: Int,
    val heightPx: Int
) {
    WHATSAPP_STATUS("Status 9:16", 1080, 1920),
    SQUARE("Square 1:1", 1080, 1080)
}

/**
 * User preferences specific to the Hadith section.
 */
data class HadithUserPreferences(
    val preferredLanguage: HadithLanguage = HadithLanguage.ENGLISH,
    val showArabic: Boolean = true,
    val fontScale: Float = 1.0f,
    val morningNotificationEnabled: Boolean = true,
    val morningNotificationOnSunrise: Boolean = true
)
