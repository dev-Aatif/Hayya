package dev.hayya.core.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import dev.hayya.core.data.preferences.UserPreferencesRepository
import dev.hayya.core.domain.calculation.PrayerCalculationEngine
import dev.hayya.core.domain.calculation.SolarAstronomicalCalculator
import dev.hayya.core.domain.model.AlertMode
import dev.hayya.core.domain.model.AutoSilenceTimeout
import dev.hayya.core.domain.model.MadhabOption
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.system.alarm.PrayerAlarmScheduler
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PrayerViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val dataStoreScope = CoroutineScope(UnconfinedTestDispatcher() + Job())

    private lateinit var tempFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: UserPreferencesRepository
    private val calculationEngine = PrayerCalculationEngine(SolarAstronomicalCalculator())
    private val alarmScheduler = mockk<PrayerAlarmScheduler>(relaxed = true)

    private lateinit var viewModel: PrayerViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        tempFile = File.createTempFile("test_vm_prefs", ".preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { tempFile }
        )
        repository = UserPreferencesRepository(dataStore)
        viewModel = PrayerViewModel(calculationEngine, repository, alarmScheduler)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        Dispatchers.resetMain()
        tempFile.delete()
    }

    @Test
    fun initialState_populatesLocationAndAllSixDailyPrayers() = runTest(testDispatcher) {
        try {
            testDispatcher.scheduler.runCurrent()

            val state = viewModel.uiState.value

            assertThat(state.locationName).isNotEmpty()
            assertThat(state.nextPrayerName).isNotEmpty()
            assertThat(state.dailyPrayers).hasSize(6)

            val prayerTypes = state.dailyPrayers.map { it.prayerType }
            assertThat(prayerTypes).containsExactly(
                PrayerType.FAJR,
                PrayerType.SUNRISE,
                PrayerType.DHUHR,
                PrayerType.ASR,
                PrayerType.MAGHRIB,
                PrayerType.ISHA
            ).inOrder()

            assertThat(state.progressRatio).isIn(com.google.common.collect.Range.closed(0.0f, 1.0f))
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun togglePrayerAudioMode_cyclesThroughFullAlarmGentleAndMuted() = runTest(testDispatcher) {
        try {
            testDispatcher.scheduler.runCurrent()

            // Initial Fajr is FULL_ALARM by default
            val initialMode = viewModel.uiState.value.userPreferences.getAlertMode(PrayerType.FAJR)
            assertThat(initialMode).isEqualTo(AlertMode.FULL_ALARM)

            // 1. Toggle -> GENTLE_BANNER
            viewModel.togglePrayerAudioMode(PrayerType.FAJR)
            testDispatcher.scheduler.runCurrent()
            val mode1 = repository.userPreferencesFlow.first().getAlertMode(PrayerType.FAJR)
            assertThat(mode1).isEqualTo(AlertMode.GENTLE_BANNER)

            // 2. Toggle -> MUTED
            viewModel.togglePrayerAudioMode(PrayerType.FAJR)
            testDispatcher.scheduler.runCurrent()
            val mode2 = repository.userPreferencesFlow.first().getAlertMode(PrayerType.FAJR)
            assertThat(mode2).isEqualTo(AlertMode.MUTED)

            // 3. Toggle -> FULL_ALARM
            viewModel.togglePrayerAudioMode(PrayerType.FAJR)
            testDispatcher.scheduler.runCurrent()
            val mode3 = repository.userPreferencesFlow.first().getAlertMode(PrayerType.FAJR)
            assertThat(mode3).isEqualTo(AlertMode.FULL_ALARM)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun updateJuristicMethod_updatesPreferencesState() = runTest(testDispatcher) {
        try {
            testDispatcher.scheduler.runCurrent()

            viewModel.updateJuristicMethod(MadhabOption.HANAFI)
            testDispatcher.scheduler.runCurrent()

            val updated = repository.userPreferencesFlow.first()
            assertThat(updated.madhab).isEqualTo(MadhabOption.HANAFI)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun updateAutoSilence_updatesPreferencesState() = runTest(testDispatcher) {
        try {
            testDispatcher.scheduler.runCurrent()

            viewModel.updateAutoSilence(AutoSilenceTimeout.SECONDS_120)
            testDispatcher.scheduler.runCurrent()

            val updated = repository.userPreferencesFlow.first()
            assertThat(updated.autoSilenceTimeout).isEqualTo(AutoSilenceTimeout.SECONDS_120)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun formatDuration_correctlyFormatsTabularHoursMinutesSeconds() {
        // 0 ms -> 00:00:00
        assertThat(PrayerViewModel.formatDuration(0L)).isEqualTo("00:00:00")

        // 61 seconds -> 00:01:01
        assertThat(PrayerViewModel.formatDuration(61_000L)).isEqualTo("00:01:01")

        // 1 hour, 42 minutes, 18 seconds -> 01:42:18
        val duration = (1 * 3600 + 42 * 60 + 18) * 1000L
        assertThat(PrayerViewModel.formatDuration(duration)).isEqualTo("01:42:18")
    }

    @Test
    fun formatHijriDate_generatesNonEmptyIslamicDate() {
        val date = LocalDate(2026, 6, 15)
        val hijriString = PrayerViewModel.formatHijriDate(date)
        assertThat(hijriString).isNotEmpty()
        assertThat(hijriString).contains("AH")
    }
}
