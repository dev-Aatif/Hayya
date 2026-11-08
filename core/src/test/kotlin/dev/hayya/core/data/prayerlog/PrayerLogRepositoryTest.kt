package dev.hayya.core.data.prayerlog

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import dev.hayya.core.domain.model.PrayerType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PrayerLogRepositoryTest {

    private val dataStoreScope = CoroutineScope(UnconfinedTestDispatcher() + Job())

    private lateinit var tempFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: PrayerLogRepository

    private val today = LocalDate(2026, 11, 2)
    private val yesterday = LocalDate(2026, 11, 1)

    @Before
    fun setUp() {
        tempFile = File.createTempFile("test_prayer_log", ".preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { tempFile }
        )
        repository = PrayerLogRepository(dataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        if (tempFile.exists()) {
            tempFile.delete()
        }
    }

    @Test
    fun observeDailyLog_defaultsToEmptySetForNewDate() = runTest {
        val log = repository.observeDailyLog(today).first()
        assertThat(log.date).isEqualTo(today)
        assertThat(log.completedPrayers).isEmpty()
        assertThat(log.completedCount).isEqualTo(0)
    }

    @Test
    fun togglePrayer_persistsAndEmitsUpdatedPrayerLog() = runTest {
        // 1. Toggle Fajr ON
        val log1 = repository.togglePrayer(today, PrayerType.FAJR)
        assertThat(log1.isPrayerCompleted(PrayerType.FAJR)).isTrue()

        val observed1 = repository.observeDailyLog(today).first()
        assertThat(observed1.isPrayerCompleted(PrayerType.FAJR)).isTrue()
        assertThat(observed1.completedCount).isEqualTo(1)

        // 2. Toggle Asr ON
        val log2 = repository.togglePrayer(today, PrayerType.ASR)
        assertThat(log2.isPrayerCompleted(PrayerType.ASR)).isTrue()

        val observed2 = repository.observeDailyLog(today).first()
        assertThat(observed2.isPrayerCompleted(PrayerType.FAJR)).isTrue()
        assertThat(observed2.isPrayerCompleted(PrayerType.ASR)).isTrue()
        assertThat(observed2.completedCount).isEqualTo(2)

        // 3. Toggle Fajr OFF
        val log3 = repository.togglePrayer(today, PrayerType.FAJR)
        assertThat(log3.isPrayerCompleted(PrayerType.FAJR)).isFalse()

        val observed3 = repository.observeDailyLog(today).first()
        assertThat(observed3.isPrayerCompleted(PrayerType.FAJR)).isFalse()
        assertThat(observed3.isPrayerCompleted(PrayerType.ASR)).isTrue()
        assertThat(observed3.completedCount).isEqualTo(1)
    }

    @Test
    fun setPrayerCompleted_explicitlyUpdatesPrayerState() = runTest {
        repository.setPrayerCompleted(today, PrayerType.MAGHRIB, true)
        val log1 = repository.observeDailyLog(today).first()
        assertThat(log1.isPrayerCompleted(PrayerType.MAGHRIB)).isTrue()

        repository.setPrayerCompleted(today, PrayerType.MAGHRIB, false)
        val log2 = repository.observeDailyLog(today).first()
        assertThat(log2.isPrayerCompleted(PrayerType.MAGHRIB)).isFalse()
    }

    @Test
    fun observeRollingHistory_tracksChronologicalSevenDaysWindow() = runTest {
        // Mark prayers across yesterday and today
        repository.togglePrayer(yesterday, PrayerType.FAJR)
        repository.togglePrayer(yesterday, PrayerType.DHUHR)
        repository.togglePrayer(yesterday, PrayerType.ASR)
        repository.togglePrayer(yesterday, PrayerType.MAGHRIB)
        repository.togglePrayer(yesterday, PrayerType.ISHA) // Yesterday 5/5

        repository.togglePrayer(today, PrayerType.FAJR)
        repository.togglePrayer(today, PrayerType.DHUHR) // Today 2/5

        val history = repository.observeRollingHistory(today, daysCount = 7).first()

        assertThat(history.dailyLogs).hasSize(7)
        assertThat(history.dailyLogs.last().date).isEqualTo(today)
        assertThat(history.dailyLogs.last().completedCount).isEqualTo(2)

        val yesterdayLog = history.dailyLogs[history.dailyLogs.lastIndex - 1]
        assertThat(yesterdayLog.date).isEqualTo(yesterday)
        assertThat(yesterdayLog.completedCount).isEqualTo(5)
        assertThat(yesterdayLog.isComplete).isTrue()

        // 5 + 2 = 7 completed out of 35
        assertThat(history.totalCompleted).isEqualTo(7)
        assertThat(history.totalPossible).isEqualTo(35)
    }

    @Test
    fun datePartitioning_automaticallyArchivesHistoricalDaysIndependently() = runTest {
        repository.togglePrayer(yesterday, PrayerType.FAJR)

        // Mutating today does not alter yesterday's archived log
        repository.togglePrayer(today, PrayerType.ISHA)

        val yesterdayLog = repository.observeDailyLog(yesterday).first()
        val todayLog = repository.observeDailyLog(today).first()

        assertThat(yesterdayLog.completedPrayers).containsExactly(PrayerType.FAJR)
        assertThat(todayLog.completedPrayers).containsExactly(PrayerType.ISHA)
    }
}
