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
    private val httpTransport: UpdateHttpTransport = HttpUrlConnectionUpdateTransport(),
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

        val parsedByKey = LinkedHashMap<String, FawazParsedEdition>()
        val downloadedLangs = mutableListOf<String>()

        suspend fun fetchAndParse(desc: FawazEditionDescriptor?, label: String, code: String) {
            if (desc == null) return
            onStageProgress("Downloading ${book.title} ($label)…")
            val json = httpTransport.getText(desc.downloadUrl).getOrNull() ?: return
            val parsed = FawazHadithJsonParser.parseEditionBook(json) ?: return
            parsedByKey[code] = parsed
            downloadedLangs.add(code.uppercase())
        }

        fetchAndParse(arabicDesc, "Arabic", "ar")
        fetchAndParse(englishDesc, "English", "en")
        fetchAndParse(urduDesc, "Urdu", "ur")

        if (parsedByKey.isEmpty()) {
            return Result.failure(IllegalStateException("Unable to download editions for ${book.title}"))
        }

        onStageProgress("Indexing ${book.title} offline…")
        val entries = mergeEditions(
            book = book,
            arabicEdition = parsedByKey["ar"],
            englishEdition = parsedByKey["en"],
            urduEdition = parsedByKey["ur"]
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

    private fun mergeEditions(
        book: HadithBookInfo,
        arabicEdition: FawazParsedEdition?,
        englishEdition: FawazParsedEdition?,
        urduEdition: FawazParsedEdition?
    ): List<HadithEntry> {
        val arMap = arabicEdition?.hadiths?.associateBy { it.hadithNumber }.orEmpty()
        val enMap = englishEdition?.hadiths?.associateBy { it.hadithNumber }.orEmpty()
        val urMap = urduEdition?.hadiths?.associateBy { it.hadithNumber }.orEmpty()

        val allNumbers = LinkedHashSet<String>().apply {
            englishEdition?.hadiths?.forEach { add(it.hadithNumber) }
            arabicEdition?.hadiths?.forEach { add(it.hadithNumber) }
            urduEdition?.hadiths?.forEach { add(it.hadithNumber) }
        }

        val sectionsEn = englishEdition?.sections.orEmpty()
        val sectionsAr = arabicEdition?.sections.orEmpty()
        val sectionsUr = urduEdition?.sections.orEmpty()

        return allNumbers.mapNotNull { number ->
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
    }
}
