package dev.hayya.core.domain.hadith

/**
 * Filters and maps remote HadeethEnc category trees onto Hayya's prioritized
 * spiritual, worship, creed, character, and tafsir pillars while excluding
 * judicial, penal, and commercial jurisprudence categories.
 */
object PriorityTopicFilter {

    private val excludedKeywords = listOf(
        "punishment",
        "hudud",
        "crime",
        "blood money",
        "judiciary",
        "judgment",
        "judge",
        "litigation",
        "witness",
        "transaction",
        "commercial",
        "sales",
        "trade",
        "loan",
        "hiring",
        "usury",
        "lease",
        "mortgage",
        "inheritance",
        "bequest",
        "wills",
        "slave",
        "manumission",
        "emancipation",
        "jihad",
        "warfare",
        "fighting",
        "expedition",
        "booty",
        "khumus",
        "jizyah",
        "divorce",
        "oaths and vows",
        "principles of islamic jurisprudence",
        "foods and drinks",
        "hunting"
    )

    fun isExcludedCategory(title: String): Boolean {
        val normalized = title.trim().lowercase()
        return excludedKeywords.any { keyword -> normalized.contains(keyword) }
    }

    /**
     * Classifies an accepted HadeethEnc category into a [HadithPillar], or returns `null`
     * if the category belongs to excluded legal/judicial topics.
     */
    fun classifyCategory(
        categoryId: String,
        title: String,
        parentId: String? = null
    ): HadithPillar? {
        if (isExcludedCategory(title)) return null
        val normalized = title.trim().lowercase()

        if (categoryId == "1" || parentId == "1" ||
            normalized.contains("qur'an") ||
            normalized.contains("quran") ||
            normalized.contains("tafsir") ||
            normalized.contains("commentary") ||
            normalized.contains("exegesis")
        ) {
            return HadithPillar.TAFSIR
        }

        if (categoryId == "3" || parentId == "3" ||
            normalized.contains("creed") ||
            normalized.contains("belief") ||
            normalized.contains("aqeedah") ||
            normalized.contains("tawhid") ||
            normalized.contains("oneness") ||
            normalized.contains("revelation") ||
            normalized.contains("faith") ||
            normalized.contains("iman") ||
            normalized.contains("hereafter")
        ) {
            return HadithPillar.CREED
        }

        if (normalized.contains("worship") ||
            normalized.contains("prayer") ||
            normalized.contains("salah") ||
            normalized.contains("ablution") ||
            normalized.contains("purification") ||
            normalized.contains("zakat") ||
            normalized.contains("zakah") ||
            normalized.contains("fasting") ||
            normalized.contains("ramadan") ||
            normalized.contains("tahajjud") ||
            normalized.contains("friday") ||
            normalized.contains("hajj") ||
            normalized.contains("supplication") ||
            normalized.contains("invocation") ||
            normalized.contains("dhikr") ||
            normalized.contains("remembrance")
        ) {
            return HadithPillar.WORSHIP
        }

        if (normalized.contains("manner") ||
            normalized.contains("ethic") ||
            normalized.contains("adab") ||
            normalized.contains("character") ||
            normalized.contains("family") ||
            normalized.contains("parent") ||
            normalized.contains("neighbor")
        ) {
            return HadithPillar.MANNERS
        }

        if (categoryId == "5" || parentId == "5" ||
            normalized.contains("virtue") ||
            normalized.contains("merit") ||
            normalized.contains("heart") ||
            normalized.contains("softening") ||
            normalized.contains("spirit") ||
            normalized.contains("charity") ||
            normalized.contains("good deed") ||
            normalized.contains("repentance")
        ) {
            return HadithPillar.VIRTUES
        }

        return null
    }

    /**
     * Classifies a canonical book's section title into a [HadithPillar] and whether it belongs
     * in the prioritized spiritual/worship feed (`isPriority = true`) vs legal-only chapters (`false`).
     */
    fun classifyBookSection(sectionTitle: String): Pair<HadithPillar, Boolean> {
        if (sectionTitle.isBlank()) return HadithPillar.VIRTUES to true
        if (isExcludedCategory(sectionTitle)) return HadithPillar.VIRTUES to false
        val mapped = classifyCategory(categoryId = "", title = sectionTitle, parentId = null)
        return (mapped ?: HadithPillar.VIRTUES) to true
    }
}
