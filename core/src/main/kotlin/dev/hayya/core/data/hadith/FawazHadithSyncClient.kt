package dev.hayya.core.data.hadith

import dev.hayya.core.domain.hadith.FawazEditionDescriptor
import dev.hayya.core.domain.hadith.FawazHadithJsonParser
import dev.hayya.core.domain.hadith.FawazParsedEdition
import dev.hayya.core.domain.hadith.FawazRawHadith
import dev.hayya.core.domain.hadith.HadithBookInfo
import dev.hayya.core.domain.hadith.HadithEntry
import dev.hayya.core.domain.hadith.HadithLanguage
import dev.hayya.core.domain.hadith.HadithTextVariant
import dev.hayya.core.domain.hadith.PriorityTopicFilter
import dev.hayya.core.system.update.AppUpdateEngine
import dev.hayya.core.system.update.HttpUrlConnectionUpdateTransport
import dev.hayya.core.system.update.UpdateHttpTransport

/**
 * Result of downloading and merging a canonical book from fawazahmed0/hadith-api.
 */
data class FawazBookDownloadResult(
    val bookTitle: String,
    val downloadedLanguages: List<String>,
    val entries: List<HadithEntry>
)

/**
 * Downloads full canonical Hadith books (Sahih al-Bukhari, Sahih Muslim, Sunan, 40 Nawawi, Qudsi)
 * in Arabic, English, and Urdu from the free rate-limit-free jsDelivr CDN.
 */
class FawazHadithSyncClient(
    private val httpTransport: UpdateHttpTransport = HttpUrlConnectionUpdateTransport(
        urlValidator = ::isAllowedCdnUrl
    ),
    private val editionsIndexUrl: String = DEFAULT_EDITIONS_URL
) {

    private var cachedIndex: Map<String, List<FawazEditionDescriptor>>? = null

    suspend fun downloadBook(
        book: HadithBookInfo,
        onStageProgress: (String) -> Unit = {}
    ): Result<FawazBookDownloadResult> {
        onStageProgress("Fetching edition index for ${book.title}…")
        val index = getOrFetchIndex().getOrElse { return Result.failure(it) }
        val descriptors = index[book.bookId]
            ?: return Result.failure(IllegalStateException("Book '${book.bookId}' not found in CDN index"))

        val arabicDesc = pickEdition(descriptors, "ara", book.bookId)
        val englishDesc = pickEdition(descriptors, "eng", book.bookId)
        val urduDesc = pickEdition(descriptors, "urd", book.bookId)

        if (arabicDesc == null && englishDesc == null && urduDesc == null) {
            return Result.failure(IllegalStateException("No Arabic, English, or Urdu editions found for ${book.title}"))
        }

        val downloadedLangs = mutableListOf<String>()
        var arMap: Map<String, FawazRawHadith> = emptyMap()
        var enMap: Map<String, FawazRawHadith> = emptyMap()
        var urMap: Map<String, FawazRawHadith> = emptyMap()
        var sectionsAr: Map<String, String> = emptyMap()
        var sectionsEn: Map<String, String> = emptyMap()
        var sectionsUr: Map<String, String> = emptyMap()
        var arOrder: List<String> = emptyList()
        var enOrder: List<String> = emptyList()
        var urOrder: List<String> = emptyList()

        suspend fun fetchAndIndex(desc: FawazEditionDescriptor?, label: String, code: String): FawazParsedEdition? {
            if (desc == null) return null
            onStageProgress("Downloading ${book.title} ($label)…")
            val json = httpTransport.getText(desc.downloadUrl).getOrNull() ?: return null
            val parsed = FawazHadithJsonParser.parseEditionBook(json) ?: return null
            downloadedLangs.add(code.uppercase())
            return parsed
        }

        // Fetch, index, and drop each edition sequentially (preserving AR -> EN -> UR order)
        fetchAndIndex(arabicDesc, "Arabic", "ar")?.let { arEd ->
            sectionsAr = arEd.sections
            val map = HashMap<String, FawazRawHadith>(arEd.hadiths.size)
            val order = ArrayList<String>(arEd.hadiths.size)
            arEd.hadiths.forEach {
                order.add(it.hadithNumber)
                map[it.hadithNumber] = it
            }
            arOrder = order
            arMap = map
        }
        fetchAndIndex(englishDesc, "English", "en")?.let { enEd ->
            sectionsEn = enEd.sections
            val map = HashMap<String, FawazRawHadith>(enEd.hadiths.size)
            val order = ArrayList<String>(enEd.hadiths.size)
            enEd.hadiths.forEach {
                order.add(it.hadithNumber)
                map[it.hadithNumber] = it
            }
            enOrder = order
            enMap = map
        }
        fetchAndIndex(urduDesc, "Urdu", "ur")?.let { urEd ->
            sectionsUr = urEd.sections
            val map = HashMap<String, FawazRawHadith>(urEd.hadiths.size)
            val order = ArrayList<String>(urEd.hadiths.size)
            urEd.hadiths.forEach {
                order.add(it.hadithNumber)
                map[it.hadithNumber] = it
            }
            urOrder = order
            urMap = map
        }

        val orderedNumbers = LinkedHashSet<String>(enOrder.size + arOrder.size + urOrder.size).apply {
            addAll(enOrder)
            addAll(arOrder)
            addAll(urOrder)
        }

        if (orderedNumbers.isEmpty()) {
            return Result.failure(IllegalStateException("Unable to download editions for ${book.title}"))
        }

        onStageProgress("Indexing ${book.title} offline…")
        val entries = mergeEditionsFromMaps(
            book = book,
            orderedNumbers = orderedNumbers,
            arMap = arMap,
            enMap = enMap,
            urMap = urMap,
            sectionsAr = sectionsAr,
            sectionsEn = sectionsEn,
            sectionsUr = sectionsUr
        )
        if (entries.isEmpty()) {
            return Result.failure(IllegalStateException("Downloaded book '${book.title}' contained no Hadiths"))
        }

        return Result.success(
            FawazBookDownloadResult(
                bookTitle = book.title,
                downloadedLanguages = downloadedLangs,
                entries = entries
            )
        )
    }

    private suspend fun getOrFetchIndex(): Result<Map<String, List<FawazEditionDescriptor>>> {
        cachedIndex?.let { return Result.success(it) }
        val json = httpTransport.getText(editionsIndexUrl).getOrElse { return Result.failure(it) }
        val parsed = FawazHadithJsonParser.parseEditionsIndex(json)
        if (parsed.isEmpty()) {
            return Result.failure(IllegalStateException("Empty editions index from CDN"))
        }
        cachedIndex = parsed
        return Result.success(parsed)
    }

    private fun pickEdition(
        descriptors: List<FawazEditionDescriptor>,
        prefix: String,
        bookSlug: String
    ): FawazEditionDescriptor? {
        val exact = descriptors.firstOrNull { it.name.equals("$prefix-$bookSlug", ignoreCase = true) }
        if (exact != null) return exact
        return descriptors.firstOrNull { it.name.startsWith("$prefix-", ignoreCase = true) }
    }

    private fun mergeEditionsFromMaps(
        book: HadithBookInfo,
        orderedNumbers: Set<String>,
        arMap: Map<String, FawazRawHadith>,
        enMap: Map<String, FawazRawHadith>,
        urMap: Map<String, FawazRawHadith>,
        sectionsAr: Map<String, String>,
        sectionsEn: Map<String, String>,
        sectionsUr: Map<String, String>
    ): List<HadithEntry> {
        return orderedNumbers.mapNotNull { number ->
            val ar = arMap[number]
            val en = enMap[number]
            val ur = urMap[number]

            val sectionKey = en?.sectionKey?.ifBlank { null }
                ?: ar?.sectionKey?.ifBlank { null }
                ?: ur?.sectionKey.orEmpty()

            val sectionTitleEn = sectionsEn[sectionKey].orEmpty()
            val sectionTitleFallback = sectionTitleEn.ifBlank { sectionsAr[sectionKey] ?: sectionsUr[sectionKey].orEmpty() }
            val (pillar, isPriority) = PriorityTopicFilter.classifyBookSection(sectionTitleEn)

            val translations = LinkedHashMap<HadithLanguage, HadithTextVariant>()
            if (en != null && en.text.isNotBlank()) {
                translations[HadithLanguage.ENGLISH] = HadithTextVariant(
                    title = sectionTitleEn,
                    matn = en.text
                )
            }
            if (ur != null && ur.text.isNotBlank()) {
                translations[HadithLanguage.URDU] = HadithTextVariant(
                    title = sectionsUr[sectionKey].orEmpty(),
                    matn = ur.text
                )
            }
            if (translations.isEmpty() && (ar == null || ar.text.isBlank())) {
                return@mapNotNull null
            }

            val resolvedGrade = en?.grade?.ifBlank { null }
                ?: ar?.grade?.ifBlank { null }
                ?: ur?.grade?.ifBlank { null }
                ?: if (book.bookId == "bukhari" || book.bookId == "muslim") "Sahih" else ""

            HadithEntry(
                id = "fw_${book.bookId}_$number",
                pillar = pillar,
                categoryId = "${book.bookId}_s_$sectionKey",
                categoryTitle = sectionTitleFallback.ifBlank { book.title },
                attribution = "${book.title} $number",
                grade = resolvedGrade,
                arabic = HadithTextVariant(
                    title = sectionsAr[sectionKey].orEmpty(),
                    matn = ar?.text.orEmpty()
                ),
                translations = translations,
                bookId = book.bookId,
                bookName = book.title,
                isPriority = isPriority
            )
        }
    }

    companion object {
        const val DEFAULT_EDITIONS_URL =
            "https://cdn.jsdelivr.net/gh/fawazahmed0/hadith-api@1/editions.json"

        private val ALLOWED_GITHUB_HOSTS = setOf(
            "raw.githubusercontent.com",
            "github.com",
            "objects.githubusercontent.com"
        )

        fun isAllowedCdnUrl(rawUrl: String?): Boolean {
            return AppUpdateEngine.isAllowedHttpsUrl(rawUrl) { host ->
                host == "cdn.jsdelivr.net" ||
                    host.endsWith(".jsdelivr.net") ||
                    host in ALLOWED_GITHUB_HOSTS
            }
        }
    }
}
