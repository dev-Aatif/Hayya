package dev.hayya.core.domain.hadith

import dev.hayya.core.domain.update.SimpleJsonParser

/**
 * Edition entry inside fawazahmed0/hadith-api's `editions.json`.
 */
data class FawazEditionDescriptor(
    val name: String,
    val bookSlug: String,
    val language: String,
    val downloadUrl: String
)

/**
 * Single Hadith item parsed from a fawazahmed0/hadith-api edition JSON file.
 */
data class FawazRawHadith(
    val hadithNumber: String,
    val sectionKey: String,
    val text: String,
    val grade: String
)

/**
 * Complete parsed book edition from fawazahmed0/hadith-api.
 */
data class FawazParsedEdition(
    val bookName: String,
    val sections: Map<String, String>,
    val hadiths: List<FawazRawHadith>
)

/**
 * Parses `fawazahmed0/hadith-api` CDN JSON payloads (`editions.json` and individual book editions).
 */
object FawazHadithJsonParser {

    fun parseEditionsIndex(jsonPayload: String): Map<String, List<FawazEditionDescriptor>> {
        val root = runCatching { SimpleJsonParser.parseObject(jsonPayload) }.getOrNull() ?: return emptyMap()
        val result = LinkedHashMap<String, List<FawazEditionDescriptor>>()

        for ((bookSlug, rawBookValue) in root) {
            @Suppress("UNCHECKED_CAST")
            val bookObj = rawBookValue as? Map<String, Any?> ?: continue
            @Suppress("UNCHECKED_CAST")
            val collection = bookObj["collection"] as? List<Any?> ?: continue

            val descriptors = collection.mapNotNull { item ->
                @Suppress("UNCHECKED_CAST")
                val obj = item as? Map<String, Any?> ?: return@mapNotNull null
                val name = (obj["name"] as? String)?.trim().orEmpty()
                val language = (obj["language"] as? String)?.trim().orEmpty()
                val linkMin = (obj["linkmin"] as? String)?.trim().orEmpty()
                val linkFull = (obj["link"] as? String)?.trim().orEmpty()
                val url = linkMin.takeIf { it.startsWith("https://", ignoreCase = true) }
                    ?: linkFull.takeIf { it.startsWith("https://", ignoreCase = true) }
                    ?: return@mapNotNull null
                if (name.isEmpty()) return@mapNotNull null
                FawazEditionDescriptor(
                    name = name,
                    bookSlug = bookSlug,
                    language = language,
                    downloadUrl = url
                )
            }
            if (descriptors.isNotEmpty()) {
                result[bookSlug] = descriptors
            }
        }
        return result
    }

    fun parseEditionBook(jsonPayload: String): FawazParsedEdition? {
        val root = runCatching { SimpleJsonParser.parseObject(jsonPayload) }.getOrNull() ?: return null
        @Suppress("UNCHECKED_CAST")
        val metadata = root["metadata"] as? Map<String, Any?>
        val bookName = (metadata?.get("name") as? String)?.trim().orEmpty()

        @Suppress("UNCHECKED_CAST")
        val rawSections = metadata?.get("sections") as? Map<String, Any?> ?: emptyMap()
        val sections = LinkedHashMap<String, String>()
        for ((k, v) in rawSections) {
            val title = v?.toString()?.trim().orEmpty()
            if (title.isNotEmpty()) {
                sections[k] = title
            }
        }

        @Suppress("UNCHECKED_CAST")
        val rawHadiths = root["hadiths"] as? List<Any?> ?: return null
        val hadiths = rawHadiths.mapNotNull { item ->
            @Suppress("UNCHECKED_CAST")
            val obj = item as? Map<String, Any?> ?: return@mapNotNull null
            val numberStr = formatHadithNumber(obj["hadithnumber"]) ?: return@mapNotNull null
            val text = (obj["text"] as? String)?.trim().orEmpty()
            if (text.isEmpty()) return@mapNotNull null

            @Suppress("UNCHECKED_CAST")
            val ref = obj["reference"] as? Map<String, Any?>
            val sectionKey = formatHadithNumber(ref?.get("book")) ?: ""

            @Suppress("UNCHECKED_CAST")
            val grades = obj["grades"] as? List<Any?> ?: emptyList()
            val grade = grades.firstNotNullOfOrNull { gItem ->
                @Suppress("UNCHECKED_CAST")
                val gObj = gItem as? Map<String, Any?>
                (gObj?.get("grade") as? String)?.trim()?.takeIf { it.isNotEmpty() }
            }.orEmpty()

            FawazRawHadith(
                hadithNumber = numberStr,
                sectionKey = sectionKey,
                text = text,
                grade = grade
            )
        }

        return FawazParsedEdition(
            bookName = bookName,
            sections = sections,
            hadiths = hadiths
        )
    }

    private fun formatHadithNumber(raw: Any?): String? {
        return when (raw) {
            is Number -> {
                val d = raw.toDouble()
                if (d == d.toLong().toDouble()) raw.toLong().toString() else raw.toString()
            }
            is String -> raw.trim().takeIf { it.isNotEmpty() }
            else -> null
        }
    }
}
