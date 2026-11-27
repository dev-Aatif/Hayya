package dev.hayya.core.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import dev.hayya.core.data.hadith.FawazBookDownloadResult
import dev.hayya.core.data.hadith.FawazHadithSyncClient
import dev.hayya.core.data.hadith.HadithDatabase
import dev.hayya.core.data.hadith.HadithPreferencesRepository
import dev.hayya.core.data.hadith.HadithRepository
import dev.hayya.core.data.hadith.HadithStarterPack
import dev.hayya.core.data.hadith.HadeethEncSyncClient
import dev.hayya.core.domain.hadith.CanonicalHadithBooks
import dev.hayya.core.domain.hadith.HadithCategorySyncInfo
import dev.hayya.core.domain.hadith.HadithFeedFilter
import dev.hayya.core.domain.hadith.HadithLanguage
import dev.hayya.core.domain.hadith.HadithPillar
import dev.hayya.core.system.alarm.PrayerAlarmScheduler
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class HadithViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dataStoreScope = CoroutineScope(UnconfinedTestDispatcher() + Job())
    private lateinit var tempFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefsRepo: HadithPreferencesRepository
    private lateinit var database: HadithDatabase
    private lateinit var syncClient: HadeethEncSyncClient
    private lateinit var bookSyncClient: FawazHadithSyncClient
    private val changesFlow = MutableStateFlow(0L)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        tempFile = File.createTempFile("test_hadith_prefs", ".preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { tempFile }
        )
        prefsRepo = HadithPreferencesRepository(dataStore)
        database = mockk(relaxed = true) {
            every { changes } returns changesFlow
            coEvery { queryHadiths(any(), any(), any(), any(), any()) } answers {
                val pillar = firstArg<HadithPillar?>()
                val onlyBookmarked = secondArg<Boolean>()
                val query = thirdArg<String>()
                val bookId = arg<String?>(3)
                HadithStarterPack.entries.filter { entry ->
                    (bookId == null || entry.bookId == bookId) &&
                        (pillar == null || entry.pillar == pillar) &&
                        (!onlyBookmarked || entry.isBookmarked) &&
                        (query.isBlank() || entry.attribution.contains(query, ignoreCase = true))
                }
            }
            coEvery { getPriorityPoolForDailySelection(any()) } returns HadithStarterPack.entries
            coEvery { getTotalCount() } returns HadithStarterPack.entries.size
            coEvery { getCategories() } returns emptyList()
            coEvery { getBooksCatalog() } returns CanonicalHadithBooks.catalog
            coEvery { toggleBookmark(any()) } returns true
        }
        syncClient = mockk(relaxed = true)
        bookSyncClient = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        Dispatchers.resetMain()
        tempFile.delete()
    }

    @Test
    fun init_loadsStarterEntriesAndDailyHadith() = runTest(testDispatcher) {
        val repository = HadithRepository(database, prefsRepo, syncClient, bookSyncClient)
        val vm = HadithViewModel(
            repository = repository,
            clock = object : Clock {
                override fun now(): Instant = Instant.fromEpochMilliseconds(1_764_100_000_000L)
            }
        )

        try {
            testDispatcher.scheduler.runCurrent()

            val state = vm.uiState.value
            assertThat(state.isLoaded).isTrue()
            assertThat(state.entries).hasSize(HadithStarterPack.entries.size)
            assertThat(state.dailyHadith).isNotNull()
            assertThat(state.totalStoredCount).isEqualTo(HadithStarterPack.entries.size)
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun languageFilterSearchAndReaderActions_updateStateAndPersist() = runTest(testDispatcher) {
        val repository = HadithRepository(database, prefsRepo, syncClient, bookSyncClient)
        val alarmScheduler = mockk<PrayerAlarmScheduler>(relaxed = true)
        val vm = HadithViewModel(repository = repository, alarmScheduler = alarmScheduler)

        try {
            testDispatcher.scheduler.runCurrent()

            vm.setLanguage(HadithLanguage.SINDHI)
            testDispatcher.scheduler.runCurrent()
            assertThat(prefsRepo.preferencesFlow.first().preferredLanguage).isEqualTo(HadithLanguage.SINDHI)
            assertThat(vm.uiState.value.preferences.preferredLanguage).isEqualTo(HadithLanguage.SINDHI)

            vm.setFilter(HadithFeedFilter.CREED)
            testDispatcher.scheduler.runCurrent()
            assertThat(vm.uiState.value.entries.all { it.pillar == HadithPillar.CREED }).isTrue()

            val first = vm.uiState.value.entries.first()
            vm.openHadith(first)
            testDispatcher.scheduler.runCurrent()
            assertThat(vm.uiState.value.selectedHadith?.id).isEqualTo(first.id)
            coVerify { database.setRead(first.id, true) }

            vm.toggleBookmark(first.id)
            testDispatcher.scheduler.runCurrent()
            assertThat(vm.uiState.value.selectedHadith?.isBookmarked).isTrue()

            val cat = HadithCategorySyncInfo("3", "The Creed", HadithPillar.CREED, 20, 0)
            coEvery { syncClient.fetchCategoryBatch(cat, any(), any(), any()) } returns Result.success(emptyList())
            vm.syncCategory(cat)
            testDispatcher.scheduler.runCurrent()
            assertThat(vm.uiState.value.syncingCategoryId).isNull()
            assertThat(vm.uiState.value.syncError).isNull()

            val nawawi = CanonicalHadithBooks.catalog.first { it.bookId == "nawawi" }
            coEvery { bookSyncClient.downloadBook(nawawi, any()) } returns Result.success(
                FawazBookDownloadResult(
                    bookTitle = nawawi.title,
                    downloadedLanguages = listOf("AR", "EN"),
                    entries = listOf(first.copy(id = "fw_nawawi_1", bookId = "nawawi", bookName = nawawi.title))
                )
            )
            vm.downloadBook(nawawi)
            testDispatcher.scheduler.runCurrent()
            assertThat(vm.uiState.value.downloadingBookId).isNull()
            coVerify { database.saveDownloadedBook("nawawi", nawawi.title, listOf("AR", "EN"), any()) }

            vm.selectBook("nawawi")
            testDispatcher.scheduler.runCurrent()
            assertThat(vm.uiState.value.selectedBookId).isEqualTo("nawawi")

            vm.deleteBook("nawawi")
            testDispatcher.scheduler.runCurrent()
            assertThat(vm.uiState.value.selectedBookId).isNull()
            coVerify { database.deleteBook("nawawi") }

            assertThat(vm.uiState.value.preferences.morningNotificationEnabled).isTrue()
            vm.toggleMorningNotification()
            testDispatcher.scheduler.runCurrent()
            assertThat(prefsRepo.preferencesFlow.first().morningNotificationEnabled).isFalse()
            coVerify(atLeast = 1) { alarmScheduler.scheduleRollingAlarms(any(), any(), any(), any(), any()) }
        } finally {
            vm.viewModelScope.cancel()
        }
    }

    @Test
    fun syncCategory_advancesUsingSyncedPages_evenWhenPartialBatchSavedFewerThanTwenty() = runTest(testDispatcher) {
        val partialCategory = HadithCategorySyncInfo(
            id = "40",
            title = "Virtues",
            pillar = HadithPillar.VIRTUES,
            remoteCount = 60,
            syncedCount = 17,
            syncedPages = 1
        )
        coEvery { syncClient.fetchCategoryBatch(partialCategory, page = 2, perPage = 20, any()) } returns Result.success(emptyList())

        val repository = HadithRepository(database, prefsRepo, syncClient, bookSyncClient)
        val vm = HadithViewModel(
            repository = repository,
            alarmScheduler = mockk(relaxed = true)
        )
        try {
            testDispatcher.scheduler.runCurrent()
            vm.syncCategory(partialCategory)
            testDispatcher.scheduler.runCurrent()

            coVerify(exactly = 1) {
                syncClient.fetchCategoryBatch(partialCategory, page = 2, perPage = 20, any())
            }
        } finally {
            vm.viewModelScope.cancel()
        }
    }
}
