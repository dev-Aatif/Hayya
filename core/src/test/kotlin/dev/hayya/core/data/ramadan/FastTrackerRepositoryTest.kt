package dev.hayya.core.data.ramadan

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.google.common.truth.Truth.assertThat
import dev.hayya.core.domain.ramadan.FastStatus
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
class FastTrackerRepositoryTest {

    private val testScope = CoroutineScope(UnconfinedTestDispatcher() + Job())
    private lateinit var tempFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: FastTrackerRepository

    @Before
    fun setUp() {
        tempFile = File.createTempFile("test_fast_prefs", ".preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFile }
        )
        repository = FastTrackerRepository(dataStore)
    }

    @After
    fun tearDown() {
        testScope.cancel()
        tempFile.delete()
    }

    @Test
    fun setFastStatus_persistsAndEmitsUpdatedRecord() = runTest {
        val date = LocalDate(2026, 3, 5)
        val initial = repository.observeDailyFast(date).first()
        assertThat(initial.status).isEqualTo(FastStatus.NOT_RECORDED)

        repository.setFastStatus(date, FastStatus.FASTED, note = "First 10 days")
        val updated = repository.observeDailyFast(date).first()
        assertThat(updated.status).isEqualTo(FastStatus.FASTED)
        assertThat(updated.note).isEqualTo("First 10 days")
    }

    @Test
    fun cycleFastStatus_transitionsThroughCompleteCycle() = runTest {
        val date = LocalDate(2026, 3, 6)

        // 1. Initial NOT_RECORDED -> FASTED
        val step1 = repository.cycleFastStatus(date)
        assertThat(step1).isEqualTo(FastStatus.FASTED)

        // 2. FASTED -> MISSED
        val step2 = repository.cycleFastStatus(date)
        assertThat(step2).isEqualTo(FastStatus.MISSED)

        // 3. MISSED -> EXEMPT
        val step3 = repository.cycleFastStatus(date)
        assertThat(step3).isEqualTo(FastStatus.EXEMPT)

        // 4. EXEMPT -> NOT_RECORDED
        val step4 = repository.cycleFastStatus(date)
        assertThat(step4).isEqualTo(FastStatus.NOT_RECORDED)
    }

    @Test
    fun updateQadhaMadeUpCount_persistsAndObservesCorrectly() = runTest {
        val initialProgress = repository.observeRamadanProgress(1447).first()
        assertThat(initialProgress.qadhaMadeUpCount).isEqualTo(0)

        repository.updateQadhaMadeUpCount(3)
        val updatedProgress = repository.observeRamadanProgress(1447).first()
        assertThat(updatedProgress.qadhaMadeUpCount).isEqualTo(3)
    }

    @Test
    fun ramadanSettings_updatesAlertPreferences() = runTest {
        val initial = repository.observeRamadanSettings().first()
        assertThat(initial.suhoorAlarmEnabled).isFalse()
        assertThat(initial.suhoorAlarmMinutesBefore).isEqualTo(40)
        assertThat(initial.iftarNotificationEnabled).isTrue()
        assertThat(initial.eidNotificationEnabled).isTrue()

        repository.updateSuhoorAlarmEnabled(true)
        repository.updateSuhoorAlarmMinutesBefore(50)
        repository.updateIftarNotificationEnabled(false)
        repository.updateEidNotificationEnabled(false)

        val updated = repository.observeRamadanSettings().first()
        assertThat(updated.suhoorAlarmEnabled).isTrue()
        assertThat(updated.suhoorAlarmMinutesBefore).isEqualTo(50)
        assertThat(updated.iftarNotificationEnabled).isFalse()
        assertThat(updated.eidNotificationEnabled).isFalse()
    }
}
