package dev.hayya.core.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.hayya.core.data.hadith.HadithRepository
import dev.hayya.core.domain.hadith.CanonicalHadithBooks
import dev.hayya.core.domain.hadith.DailyHadithSelector
import dev.hayya.core.domain.hadith.HadithBookInfo
import dev.hayya.core.domain.hadith.HadithCategorySyncInfo
import dev.hayya.core.domain.hadith.HadithEntry
import dev.hayya.core.domain.hadith.HadithFeedFilter
import dev.hayya.core.domain.hadith.HadithLanguage
import dev.hayya.core.domain.hadith.HadithUserPreferences
import dev.hayya.core.system.alarm.PrayerAlarmScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * UI state for the dedicated Hadith section (daily card, topic feed, search, reader, and sync packs).
 */
data class HadithUiState(
    val isLoaded: Boolean = false,
    val preferences: HadithUserPreferences = HadithUserPreferences(),
    val activeFilter: HadithFeedFilter = HadithFeedFilter.ALL,
    val searchQuery: String = "",
    val dailyHadith: HadithEntry? = null,
    val entries: List<HadithEntry> = emptyList(),
    val totalStoredCount: Int = 0,
    val selectedHadith: HadithEntry? = null,
    val shareTargetHadith: HadithEntry? = null,
    val categories: List<HadithCategorySyncInfo> = emptyList(),
    val books: List<HadithBookInfo> = CanonicalHadithBooks.catalog,
    val selectedBookId: String? = null,
    val isSyncSheetOpen: Boolean = false,
    val isLoadingCategories: Boolean = false,
    val syncingCategoryId: String? = null,
    val downloadingBookId: String? = null,
    val syncProgressText: String? = null,
    val syncError: String? = null
)

/**
 * Dedicated ViewModel isolating Hadith reading, offline SQLite search, HadeethEnc
 * multilingual synchronization, and WhatsApp Status card preparation from prayer alarms.
 */
class HadithViewModel(
    private val repository: HadithRepository,
    private val clock: Clock = Clock.System,
    private val alarmScheduler: PrayerAlarmScheduler? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HadithUiState())
    val uiState: StateFlow<HadithUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initialize()
            refreshFeed()
        }

        viewModelScope.launch {
            repository.preferencesFlow.collectLatest { prefs ->
                _uiState.update { it.copy(preferences = prefs) }
            }
        }

        viewModelScope.launch {
            repository.databaseChanges.collectLatest {
                refreshFeed()
            }
        }
    }

    fun setLanguage(language: HadithLanguage) {
        viewModelScope.launch {
            repository.updateLanguage(language)
            val openEntry = _uiState.value.selectedHadith
            if (openEntry != null) {
                val updated = repository.ensureTranslationIfSyncedEntry(openEntry, language)
                _uiState.update { it.copy(selectedHadith = updated) }
            }
        }
    }

    fun toggleShowArabic() {
        val next = !_uiState.value.preferences.showArabic
        viewModelScope.launch {
            repository.updateShowArabic(next)
        }
    }

    fun adjustFontScale(delta: Float) {
        val next = (_uiState.value.preferences.fontScale + delta).coerceIn(0.85f, 1.35f)
        viewModelScope.launch {
            repository.updateFontScale(next)
        }
    }

    fun toggleMorningNotification() {
        val next = !_uiState.value.preferences.morningNotificationEnabled
        viewModelScope.launch {
            repository.updateMorningNotificationEnabled(next)
            alarmScheduler?.scheduleRollingAlarms()
        }
    }

    fun openHadithById(hadithId: String) {
        viewModelScope.launch {
            repository.initialize()
            val entry = repository.getHadithById(hadithId) ?: return@launch
            openHadith(entry)
        }
    }

    fun setFilter(filter: HadithFeedFilter) {
        _uiState.update { it.copy(activeFilter = filter) }
        viewModelScope.launch {
            refreshFeed()
        }
    }

    fun selectBook(bookId: String?) {
        _uiState.update {
            it.copy(
                selectedBookId = bookId,
                activeFilter = if (bookId != null) HadithFeedFilter.ALL else it.activeFilter
            )
        }
        viewModelScope.launch {
            refreshFeed()
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        viewModelScope.launch {
            refreshFeed()
        }
    }

    fun openHadith(entry: HadithEntry) {
        _uiState.update { it.copy(selectedHadith = entry) }
        viewModelScope.launch {
            repository.markAsRead(entry.id, true)
            val lang = _uiState.value.preferences.preferredLanguage
            val updated = repository.ensureTranslationIfSyncedEntry(entry, lang)
            _uiState.update { state ->
                if (state.selectedHadith?.id == entry.id) {
                    state.copy(selectedHadith = updated.copy(isRead = true))
                } else {
                    state
                }
            }
        }
    }

    fun closeHadith() {
        _uiState.update { it.copy(selectedHadith = null) }
    }

    fun toggleBookmark(hadithId: String) {
        viewModelScope.launch {
            val isNowBookmarked = repository.toggleBookmark(hadithId)
            _uiState.update { state ->
                val updatedSelected = state.selectedHadith?.let {
                    if (it.id == hadithId) it.copy(isBookmarked = isNowBookmarked) else it
                }
                val updatedDaily = state.dailyHadith?.let {
                    if (it.id == hadithId) it.copy(isBookmarked = isNowBookmarked) else it
                }
                state.copy(
                    selectedHadith = updatedSelected,
                    dailyHadith = updatedDaily
                )
            }
        }
    }

    fun openShareSheet(entry: HadithEntry) {
        _uiState.update { it.copy(shareTargetHadith = entry) }
    }

    fun closeShareSheet() {
        _uiState.update { it.copy(shareTargetHadith = null) }
    }

    fun openSyncSheet() {
        _uiState.update { it.copy(isSyncSheetOpen = true, syncError = null) }
        if (_uiState.value.categories.isEmpty()) {
            refreshRemoteCategories()
        }
    }

    fun closeSyncSheet() {
        _uiState.update { it.copy(isSyncSheetOpen = false, syncError = null) }
    }

    fun refreshRemoteCategories() {
        if (_uiState.value.isLoadingCategories) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingCategories = true, syncError = null) }
            val result = repository.refreshCategories()
            result.fold(
                onSuccess = { categories ->
                    _uiState.update {
                        it.copy(
                            isLoadingCategories = false,
                            categories = categories,
                            syncError = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoadingCategories = false,
                            syncError = error.message ?: "Unable to fetch Hadith topics"
                        )
                    }
                }
            )
        }
    }

    fun syncCategory(category: HadithCategorySyncInfo) {
        if (_uiState.value.syncingCategoryId != null || _uiState.value.downloadingBookId != null) return
        viewModelScope.launch {
            val nextPage = (category.syncedCount / 20) + 1
            _uiState.update {
                it.copy(
                    syncingCategoryId = category.id,
                    syncProgressText = "Preparing ${category.title}…",
                    syncError = null
                )
            }

            val result = repository.syncCategoryBatch(
                category = category,
                page = nextPage,
                perPage = 20
            ) { completed, total ->
                _uiState.update {
                    it.copy(syncProgressText = "Downloading ${category.title} ($completed/$total)…")
                }
            }

            result.fold(
                onSuccess = {
                    val refreshedCategories = repository.getCategories()
                    _uiState.update {
                        it.copy(
                            syncingCategoryId = null,
                            syncProgressText = null,
                            categories = refreshedCategories,
                            syncError = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            syncingCategoryId = null,
                            syncProgressText = null,
                            syncError = error.message ?: "Failed to download category"
                        )
                    }
                }
            )
        }
    }

    fun downloadBook(book: HadithBookInfo) {
        if (_uiState.value.downloadingBookId != null || _uiState.value.syncingCategoryId != null) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    downloadingBookId = book.bookId,
                    syncProgressText = "Starting ${book.title}…",
                    syncError = null
                )
            }

            val result = repository.downloadCanonicalBook(book) { stageText ->
                _uiState.update { it.copy(syncProgressText = stageText) }
            }

            result.fold(
                onSuccess = {
                    val refreshedBooks = repository.getBooksCatalog()
                    _uiState.update {
                        it.copy(
                            downloadingBookId = null,
                            syncProgressText = null,
                            books = refreshedBooks,
                            syncError = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            downloadingBookId = null,
                            syncProgressText = null,
                            syncError = error.message ?: "Failed to download ${book.title}"
                        )
                    }
                }
            )
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            repository.deleteCanonicalBook(bookId)
            val refreshedBooks = repository.getBooksCatalog()
            _uiState.update { state ->
                state.copy(
                    books = refreshedBooks,
                    selectedBookId = if (state.selectedBookId == bookId) null else state.selectedBookId
                )
            }
            refreshFeed()
        }
    }

    private suspend fun refreshFeed() {
        val state = _uiState.value
        val filter = state.activeFilter
        val entries = repository.queryHadiths(
            pillar = filter.toPillarOrNull(),
            onlyBookmarked = (filter == HadithFeedFilter.SAVED),
            searchQuery = state.searchQuery,
            bookId = state.selectedBookId
        )
        val priorityPool = repository.getPriorityPoolForDailySelection()
        val totalCount = repository.getTotalCount()
        val categories = repository.getCategories()
        val books = repository.getBooksCatalog()
        val today = Instant.fromEpochMilliseconds(clock.now().toEpochMilliseconds())
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
        val daily = DailyHadithSelector.selectForDate(priorityPool, today)

        _uiState.update { current ->
            val updatedSelected = current.selectedHadith?.let { sel ->
                entries.firstOrNull { it.id == sel.id } ?: sel
            }
            current.copy(
                isLoaded = true,
                dailyHadith = daily,
                entries = entries,
                totalStoredCount = totalCount,
                categories = categories,
                books = books,
                selectedHadith = updatedSelected
            )
        }
    }
}
