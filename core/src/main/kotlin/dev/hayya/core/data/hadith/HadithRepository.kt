package dev.hayya.core.data.hadith

import dev.hayya.core.domain.hadith.HadithBookInfo
import dev.hayya.core.domain.hadith.HadithCategorySyncInfo
import dev.hayya.core.domain.hadith.HadithEntry
import dev.hayya.core.domain.hadith.HadithLanguage
import dev.hayya.core.domain.hadith.HadithPillar
import dev.hayya.core.domain.hadith.HadithUserPreferences
import kotlinx.coroutines.flow.Flow

/**
 * Coordinates offline SQLite storage, starter seeding, DataStore preferences,
 * HadeethEnc thematic synchronization, and canonical book downloads from fawazahmed0/hadith-api.
 */
class HadithRepository(
    private val database: HadithDatabase,
    private val preferencesRepository: HadithPreferencesRepository,
    private val syncClient: HadeethEncSyncClient = HadeethEncSyncClient(),
    private val bookSyncClient: FawazHadithSyncClient = FawazHadithSyncClient()
) {

    val preferencesFlow: Flow<HadithUserPreferences> = preferencesRepository.preferencesFlow

    val databaseChanges: Flow<Long> = database.changes

    suspend fun initialize() {
        database.ensureStarterSeeded()
    }

    suspend fun queryHadiths(
        pillar: HadithPillar? = null,
        onlyBookmarked: Boolean = false,
        searchQuery: String = "",
        bookId: String? = null
    ): List<HadithEntry> {
        database.ensureStarterSeeded()
        return database.queryHadiths(
            pillar = pillar,
            onlyBookmarked = onlyBookmarked,
            searchQuery = searchQuery,
            bookId = bookId
        )
    }

    suspend fun getPriorityPoolForDailySelection(): List<HadithEntry> {
        database.ensureStarterSeeded()
        return database.getPriorityPoolForDailySelection()
    }

    suspend fun getHadithById(hadithId: String): HadithEntry? {
        return database.getHadithById(hadithId)
    }

    suspend fun toggleBookmark(hadithId: String): Boolean {
        return database.toggleBookmark(hadithId)
    }

    suspend fun markAsRead(hadithId: String, isRead: Boolean = true) {
        database.setRead(hadithId, isRead)
    }

    suspend fun getCategories(): List<HadithCategorySyncInfo> {
        return database.getCategories()
    }

    suspend fun getBooksCatalog(): List<HadithBookInfo> {
        return database.getBooksCatalog()
    }

    suspend fun getTotalCount(): Int {
        return database.getTotalCount()
    }

    suspend fun refreshCategories(): Result<List<HadithCategorySyncInfo>> {
        val result = syncClient.fetchPrioritizedCategories()
        result.onSuccess { categories ->
            database.upsertCategories(categories)
        }
        return result.map { database.getCategories() }
    }

    suspend fun syncCategoryBatch(
        category: HadithCategorySyncInfo,
        page: Int = 1,
        perPage: Int = HadeethEncSyncClient.DEFAULT_BATCH_SIZE,
        onItemProgress: (completed: Int, total: Int) -> Unit = { _, _ -> }
    ): Result<Int> {
        val fetchResult = syncClient.fetchCategoryBatch(
            category = category,
            page = page,
            perPage = perPage,
            onItemProgress = onItemProgress
        )
        return fetchResult.map { entries ->
            database.upsertHadiths(entries)
            entries.size
        }
    }

    suspend fun downloadCanonicalBook(
        book: HadithBookInfo,
        onStageProgress: (String) -> Unit = {}
    ): Result<Int> {
        val result = bookSyncClient.downloadBook(book, onStageProgress)
        return result.map { downloaded ->
            database.saveDownloadedBook(
                bookId = book.bookId,
                bookTitle = downloaded.bookTitle,
                downloadedLanguages = downloaded.downloadedLanguages,
                entries = downloaded.entries
            )
            downloaded.entries.size
        }
    }

    suspend fun deleteCanonicalBook(bookId: String) {
        database.deleteBook(bookId)
    }

    suspend fun ensureTranslationIfSyncedEntry(
        entry: HadithEntry,
        language: HadithLanguage
    ): HadithEntry {
        if (!entry.id.startsWith("he_") || entry.translations.containsKey(language)) {
            return entry
        }
        val fetched = syncClient.fetchSingleTranslation(entry.id, language).getOrNull() ?: return entry
        database.upsertTranslationVariant(entry.id, language, fetched)
        return database.getHadithById(entry.id) ?: entry
    }

    suspend fun updateLanguage(language: HadithLanguage) {
        preferencesRepository.updateLanguage(language)
    }

    suspend fun updateShowArabic(showArabic: Boolean) {
        preferencesRepository.updateShowArabic(showArabic)
    }

    suspend fun updateFontScale(scale: Float) {
        preferencesRepository.updateFontScale(scale)
    }

    suspend fun updateMorningNotificationEnabled(enabled: Boolean) {
        preferencesRepository.updateMorningNotificationEnabled(enabled)
    }
}
