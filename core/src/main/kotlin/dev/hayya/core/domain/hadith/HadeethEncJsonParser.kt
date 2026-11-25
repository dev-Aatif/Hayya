package dev.hayya.core.domain.hadith

import dev.hayya.core.domain.update.SimpleJsonParser

/**
 * Lightweight descriptor returned by `/categories/list` or `/categories/roots`.
 */
data class HadeethEncCategoryDto(
    val id: String,
    val title: String,
    val parentId: String?,
    val hadeethsCount: Int
)

/**
 * Summary item returned inside `/hadeeths/list`.
 */
data class HadeethEncSummaryDto(
    val id: String,
    val title: String,
    val translations: List<String>
)

/**
 * Paginated list wrapper returned by `/hadeeths/list`.
 */
data class HadeethEncListPageDto(
    val items: List<HadeethEncSummaryDto>,
    val currentPage: Int,
    val lastPage: Int,
    val totalItems: Int
)

/**
 * Full localized Hadith detail returned by `/hadeeths/one`.
 */
data class HadeethEncDetailDto(
    val id: String,
    val title: String,
    val hadeeth: String,
    val attribution: String,
    val grade: String,
    val explanation: String,
    val hints: List<String>
)

/**
 * Parses HadeethEnc API v1 JSON payloads using [SimpleJsonParser].
 */
object HadeethEncJsonParser {

    fun parseCategories(jsonPayload: String): List<HadeethEncCategoryDto> {
        val array = runCatching { SimpleJsonParser.parseArray(jsonPayload) }.getOrNull() ?: return emptyList()
        return array.mapNotNull { element ->
            @Suppress("UNCHECKED_CAST")
            val obj = element as? Map<String, Any?> ?: return@mapNotNull null
            val id = obj["id"]?.toString()?.trim().orEmpty()
            val title = (obj["title"] as? String)?.trim().orEmpty()
            if (id.isEmpty() || title.isEmpty()) return@mapNotNull null
            val parentId = obj["parent_id"]?.toString()?.trim()?.takeIf { it.isNotEmpty() && it != "null" }
            val count = obj["hadeeths_count"]?.toString()?.toIntOrNull()
                ?: (obj["hadeeths_count"] as? Number)?.toInt()
                ?: 0
            HadeethEncCategoryDto(
                id = id,
                title = title,
                parentId = parentId,
                hadeethsCount = count
            )
        }
    }

    fun parseListPage(jsonPayload: String): HadeethEncListPageDto? {
        val root = runCatching { SimpleJsonParser.parseObject(jsonPayload) }.getOrNull() ?: return null
        @Suppress("UNCHECKED_CAST")
        val rawData = root["data"] as? List<Any?> ?: return null
        val items = rawData.mapNotNull { item ->
            @Suppress("UNCHECKED_CAST")
            val obj = item as? Map<String, Any?> ?: return@mapNotNull null
            val id = obj["id"]?.toString()?.trim().orEmpty()
            val title = (obj["title"] as? String)?.trim().orEmpty()
            if (id.isEmpty()) return@mapNotNull null
            val rawLangs = obj["translations"] as? List<*> ?: emptyList<Any?>()
            val langs = rawLangs.mapNotNull { it?.toString()?.trim()?.takeIf { code -> code.isNotEmpty() } }
            HadeethEncSummaryDto(id = id, title = title, translations = langs)
        }

        @Suppress("UNCHECKED_CAST")
        val meta = root["meta"] as? Map<String, Any?>
        val currentPage = meta?.get("current_page")?.toString()?.toIntOrNull() ?: 1
        val lastPage = meta?.get("last_page")?.toString()?.toIntOrNull()
            ?: (meta?.get("last_page") as? Number)?.toInt()
            ?: 1
        val totalItems = meta?.get("total_items")?.toString()?.toIntOrNull()
            ?: (meta?.get("total_items") as? Number)?.toInt()
            ?: items.size

        return HadeethEncListPageDto(
            items = items,
            currentPage = currentPage,
            lastPage = lastPage,
            totalItems = totalItems
        )
    }

    fun parseHadithDetail(jsonPayload: String): HadeethEncDetailDto? {
        val root = runCatching { SimpleJsonParser.parseObject(jsonPayload) }.getOrNull() ?: return null
        val id = root["id"]?.toString()?.trim().orEmpty()
        val hadeeth = (root["hadeeth"] as? String)?.trim().orEmpty()
        if (id.isEmpty() || hadeeth.isEmpty()) return null

        val title = (root["title"] as? String)?.trim().orEmpty()
        val attribution = (root["attribution"] as? String)?.trim().orEmpty()
        val grade = (root["grade"] as? String)?.trim().orEmpty()
        val explanation = (root["explanation"] as? String)?.trim().orEmpty()
        val rawHints = root["hints"] as? List<*> ?: emptyList<Any?>()
        val hints = rawHints.mapNotNull { it?.toString()?.trim()?.takeIf { h -> h.isNotEmpty() } }

        return HadeethEncDetailDto(
            id = id,
            title = title,
            hadeeth = hadeeth,
            attribution = attribution,
            grade = grade,
            explanation = explanation,
            hints = hints
        )
    }
}
