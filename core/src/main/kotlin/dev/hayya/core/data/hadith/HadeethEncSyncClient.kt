package dev.hayya.core.data.hadith

import dev.hayya.core.domain.hadith.HadeethEncDetailDto
import dev.hayya.core.domain.hadith.HadeethEncJsonParser
import dev.hayya.core.domain.hadith.HadithCategorySyncInfo
import dev.hayya.core.domain.hadith.HadithEntry
import dev.hayya.core.domain.hadith.HadithLanguage
import dev.hayya.core.domain.hadith.HadithPillar
import dev.hayya.core.domain.hadith.HadithTextVariant
import dev.hayya.core.domain.hadith.PriorityTopicFilter
import dev.hayya.core.system.update.AppUpdateEngine
import dev.hayya.core.system.update.HttpUrlConnectionUpdateTransport
import dev.hayya.core.system.update.UpdateHttpTransport
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Sync client for the free HadeethEnc.com v1 API.
 * Downloads prioritized thematic categories (Prayer/Worship, Creed, Virtues, Manners, Tafsir)
 * in Arabic, English, Urdu, and Sindhi for offline SQLite caching.
 */
class HadeethEncSyncClient(
    private val httpTransport: UpdateHttpTransport = HttpUrlConnectionUpdateTransport(
        urlValidator = ::isAllowedApiUrl
    ),
    private val baseUrl: String = DEFAULT_BASE_URL
) {

    suspend fun fetchPrioritizedCategories(): Result<List<HadithCategorySyncInfo>> {
        val url = "$baseUrl/categories/list/?language=en"
        val json = httpTransport.getText(url).getOrElse { return Result.failure(it) }
        val rawCategories = HadeethEncJsonParser.parseCategories(json)
        if (rawCategories.isEmpty()) {
            return Result.failure(IllegalStateException("No categories returned from HadeethEnc"))
        }

        val prioritized = rawCategories.mapNotNull { dto ->
            val pillar = PriorityTopicFilter.classifyCategory(
                categoryId = dto.id,
                title = dto.title,
                parentId = dto.parentId
            ) ?: return@mapNotNull null

            // Prefer leaf/subcategory items when count > 0 to keep sync batches focused
            if (dto.hadeethsCount <= 0) return@mapNotNull null

            HadithCategorySyncInfo(
                id = dto.id,
                title = dto.title,
                pillar = pillar,
                remoteCount = dto.hadeethsCount,
                syncedCount = 0
            )
        }

        return Result.success(prioritized)
    }

    suspend fun fetchCategoryBatch(
        category: HadithCategorySyncInfo,
        page: Int = 1,
        perPage: Int = DEFAULT_BATCH_SIZE,
        onItemProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<List<HadithEntry>> {
        val listUrl = "$baseUrl/hadeeths/list/?language=en&category_id=${category.id}&page=$page&per_page=$perPage"
        val listJson = httpTransport.getText(listUrl).getOrElse { return Result.failure(it) }
        val pageDto = HadeethEncJsonParser.parseListPage(listJson)
            ?: return Result.failure(IllegalStateException("Unable to parse category Hadith list"))

        if (pageDto.items.isEmpty()) {
            return Result.success(emptyList())
        }

        val total = pageDto.items.size
        val completed = AtomicInteger(0)
        val semaphore = Semaphore(MAX_CONCURRENT_REQUESTS)

        val entries = coroutineScope {
            pageDto.items.map { summary ->
                async {
                    val entry = semaphore.withPermit {
                        fetchMultilingualEntry(
                            hadithId = summary.id,
                            pillar = category.pillar,
                            categoryId = category.id,
                            categoryTitle = category.title,
                            availableCodes = summary.translations
                        )
                    }
                    onItemProgress(completed.incrementAndGet(), total)
                    entry
                }
            }.awaitAll().filterNotNull()
        }

        return Result.success(entries)
    }

    suspend fun fetchSingleTranslation(
        hadithId: String,
        language: HadithLanguage
    ): Result<HadithTextVariant> {
        val detail = fetchDetail(hadithId, language.code).getOrElse { return Result.failure(it) }
        return Result.success(detail.toVariant())
    }

    private suspend fun fetchMultilingualEntry(
        hadithId: String,
        pillar: HadithPillar,
        categoryId: String,
        categoryTitle: String,
        availableCodes: List<String>
    ): HadithEntry? {
        val arDetail = fetchDetail(hadithId, "ar").getOrNull() ?: return null
        val targetLanguages = HadithLanguage.entries.filter { lang ->
            availableCodes.isEmpty() || availableCodes.any { it.equals(lang.code, ignoreCase = true) }
        }

        val translations = LinkedHashMap<HadithLanguage, HadithTextVariant>()
        var attribution = arDetail.attribution
        var grade = arDetail.grade

        for (lang in targetLanguages) {
            val detail = fetchDetail(hadithId, lang.code).getOrNull() ?: continue
            translations[lang] = detail.toVariant()
            if (lang == HadithLanguage.ENGLISH) {
                if (detail.attribution.isNotBlank()) attribution = detail.attribution
                if (detail.grade.isNotBlank()) grade = detail.grade
            }
        }

        if (translations.isEmpty()) {
            return null
        }

        return HadithEntry(
            id = "he_$hadithId",
            pillar = pillar,
            categoryId = categoryId,
            categoryTitle = categoryTitle,
            attribution = attribution.ifBlank { "HadeethEnc.com" },
            grade = grade.ifBlank { "Authenticated" },
            arabic = arDetail.toVariant(),
            translations = translations
        )
    }

    private suspend fun fetchDetail(hadithId: String, languageCode: String): Result<HadeethEncDetailDto> {
        val rawId = hadithId.removePrefix("he_")
        val url = "$baseUrl/hadeeths/one/?id=$rawId&language=$languageCode"
        val json = httpTransport.getText(url).getOrElse { return Result.failure(it) }
        val parsed = HadeethEncJsonParser.parseHadithDetail(json)
            ?: return Result.failure(IllegalStateException("Malformed Hadith detail for $rawId ($languageCode)"))
        return Result.success(parsed)
    }

    private fun HadeethEncDetailDto.toVariant(): HadithTextVariant {
        return HadithTextVariant(
            title = title,
            matn = hadeeth,
            explanation = explanation,
            hints = hints
        )
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://hadeethenc.com/api/v1"
        const val DEFAULT_BATCH_SIZE = 20
        private const val MAX_CONCURRENT_REQUESTS = 4

        fun isAllowedApiUrl(rawUrl: String?): Boolean {
            return AppUpdateEngine.isAllowedHttpsUrl(rawUrl) { host ->
                host == "hadeethenc.com" || host.endsWith(".hadeethenc.com")
            }
        }
    }
}
