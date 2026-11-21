package dev.hayya.core.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.viewModelScope
import com.google.common.truth.Truth.assertThat
import dev.hayya.core.data.prayerlog.PrayerLogRepository
import dev.hayya.core.data.preferences.UserPreferencesRepository
import dev.hayya.core.data.ramadan.FastTrackerRepository
import dev.hayya.core.domain.calculation.PrayerCalculationEngine
import dev.hayya.core.domain.calculation.SolarAstronomicalCalculator
import dev.hayya.core.domain.model.AlertMode
import dev.hayya.core.domain.model.AutoMuteDuration
import dev.hayya.core.domain.model.AutoMuteTargetMode
import dev.hayya.core.domain.model.AutoSilenceTimeout
import dev.hayya.core.domain.model.CalculationMethodOption
import dev.hayya.core.domain.model.CityCatalog
import dev.hayya.core.domain.model.FridayAutoMuteDuration
import dev.hayya.core.domain.model.MadhabOption
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.model.ThemeModeOption
import dev.hayya.core.domain.model.VolumeEscalationOption
import dev.hayya.core.domain.ramadan.*
import dev.hayya.core.domain.tasbih.TasbihTargetMode
import dev.hayya.core.system.alarm.PrayerAlarmScheduler
import dev.hayya.core.ui.components.AppNavTab
import io.mockk.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
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
    private lateinit var prayerLogRepository: PrayerLogRepository
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
        prayerLogRepository = PrayerLogRepository(dataStore)
        val fastTrackerRepository = FastTrackerRepository(dataStore)
        viewModel = PrayerViewModel(
            calculationEngine = calculationEngine,
            preferencesRepository = repository,
            alarmScheduler = alarmScheduler,
            prayerLogRepository = prayerLogRepository,
            fastTrackerRepository = fastTrackerRepository
        )
    }

    @After
    fun tearDown() {
        viewModel.viewModelScope.cancel()
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
            io.mockk.coVerify(atLeast = 1) {
                alarmScheduler.scheduleRollingAlarms(
                    preferences = match { it.autoSilenceTimeout == AutoSilenceTimeout.SECONDS_120 },
                    startDate = any(),
                    daysAhead = any(),
                    currentEpochMillis = any(),
                    ramadanSettings = any()
                )
            }
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun togglePinnedNotification_updatesPreferences() = runTest(testDispatcher) {
        try {
            testDispatcher.scheduler.runCurrent()

            viewModel.togglePinnedNotification(true)
            testDispatcher.scheduler.runCurrent()

            val updated = repository.userPreferencesFlow.first()
            assertThat(updated.pinnedNotificationEnabled).isTrue()
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun updateCalculationMethod_synchronizesCanonicalCityAndCoordinates() = runTest(testDispatcher) {
        try {
            testDispatcher.scheduler.runCurrent()

            // Start from canonical MWL reference city so switching method synchronizes coordinates
            repository.updateCalculationMethod(CalculationMethodOption.MUSLIM_WORLD_LEAGUE)
            repository.updateCoordinates(
                latitude = CalculationMethodOption.MUSLIM_WORLD_LEAGUE.defaultLatitude,
                longitude = CalculationMethodOption.MUSLIM_WORLD_LEAGUE.defaultLongitude,
                locationName = CalculationMethodOption.MUSLIM_WORLD_LEAGUE.defaultCity
            )
            testDispatcher.scheduler.runCurrent()

            viewModel.updateCalculationMethod(CalculationMethodOption.KARACHI)
            testDispatcher.scheduler.runCurrent()

            val updated = repository.userPreferencesFlow.first()
            assertThat(updated.calculationMethod).isEqualTo(CalculationMethodOption.KARACHI)
            assertThat(updated.location.resolvedLocationName).isEqualTo("Karachi, Pakistan")
            assertThat(updated.location.latitude).isEqualTo(24.8607)
            assertThat(updated.location.longitude).isEqualTo(67.0011)

            val uiState = viewModel.uiState.value
            assertThat(uiState.locationName).isEqualTo("Karachi, Pakistan")
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun updateCalculationMethod_preservesCustomUserLocation() = runTest(testDispatcher) {
        try {
            testDispatcher.scheduler.runCurrent()

            viewModel.updateLocation(31.5204, 74.3587, "Lahore, Pakistan")
            testDispatcher.scheduler.runCurrent()

            viewModel.updateCalculationMethod(CalculationMethodOption.KARACHI)
            testDispatcher.scheduler.runCurrent()

            val updated = repository.userPreferencesFlow.first()
            assertThat(updated.calculationMethod).isEqualTo(CalculationMethodOption.KARACHI)
            assertThat(updated.location.resolvedLocationName).isEqualTo("Lahore, Pakistan")
            assertThat(updated.location.latitude).isEqualTo(31.5204)
            assertThat(updated.location.longitude).isEqualTo(74.3587)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun countdownTicker_isIdleWithoutUiSubscribers_andResumesWhenCollected() = runTest(testDispatcher) {
        val fakeClock = MutableClock(Instant.parse("2026-06-15T06:00:00Z").toEpochMilliseconds())
        val gatedViewModel = PrayerViewModel(
            calculationEngine = calculationEngine,
            preferencesRepository = repository,
            alarmScheduler = alarmScheduler,
            clock = fakeClock
        )
        try {
            testDispatcher.scheduler.runCurrent()
            val initialCountdown = gatedViewModel.uiState.value.liveCountdownString

            // App in background: nobody collects uiState -> ticker must not run
            fakeClock.nowMillis += 10_000L
            testDispatcher.scheduler.advanceTimeBy(10_000L)
            testDispatcher.scheduler.runCurrent()
            assertThat(gatedViewModel.uiState.value.liveCountdownString).isEqualTo(initialCountdown)

            // App visible again: UI subscribes -> ticker resumes immediately
            backgroundScope.launch(testDispatcher) { gatedViewModel.uiState.collect { } }
            testDispatcher.scheduler.runCurrent()
            assertThat(gatedViewModel.uiState.value.liveCountdownString).isNotEqualTo(initialCountdown)
        } finally {
            gatedViewModel.viewModelScope.cancel()
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun countdownTicker_refreshesDateAndDailyLogWhenCrossingMidnightBeforeFajr() = runTest(testDispatcher) {
        val tz = TimeZone.currentSystemDefault()
        val beforeMidnight = LocalDateTime(2026, 6, 15, 23, 59, 59).toInstant(tz).toEpochMilliseconds()
        val afterMidnight = LocalDateTime(2026, 6, 16, 0, 0, 5).toInstant(tz).toEpochMilliseconds()
        val fakeClock = MutableClock(beforeMidnight)
        val midnightViewModel = PrayerViewModel(
            calculationEngine = calculationEngine,
            preferencesRepository = repository,
            alarmScheduler = alarmScheduler,
            prayerLogRepository = prayerLogRepository,
            clock = fakeClock
        )
        try {
            testDispatcher.scheduler.runCurrent()
            // Mark a prayer completed on June 15
            midnightViewModel.togglePrayerCompletion(PrayerType.FAJR)
            testDispatcher.scheduler.runCurrent()
            assertThat(midnightViewModel.uiState.value.todayPrayerLog.date).isEqualTo(LocalDate(2026, 6, 15))
            assertThat(midnightViewModel.uiState.value.todayPrayerLog.completedCount).isEqualTo(1)

            val job = backgroundScope.launch(testDispatcher) { midnightViewModel.uiState.collect { } }
            testDispatcher.scheduler.runCurrent()

            // Advance across midnight (before Fajr, so nextPrayerTimestampMillis has NOT elapsed yet)
            fakeClock.nowMillis = afterMidnight
            testDispatcher.scheduler.advanceTimeBy(1_000L)
            testDispatcher.scheduler.runCurrent()

            assertThat(midnightViewModel.uiState.value.todayPrayerLog.date).isEqualTo(LocalDate(2026, 6, 16))
            assertThat(midnightViewModel.uiState.value.todayPrayerLog.completedCount).isEqualTo(0)
            assertThat(midnightViewModel.uiState.value.islamicDateInfo.gregorianDate).isEqualTo(LocalDate(2026, 6, 16))
            job.cancel()
        } finally {
            midnightViewModel.viewModelScope.cancel()
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

    @Test
    fun tasbihState_incrementsResetsAndUpdatesTargetMode() {
        try {
            assertThat(viewModel.uiState.value.tasbihCount).isEqualTo(0)
            assertThat(viewModel.uiState.value.tasbihMode).isEqualTo(TasbihTargetMode.FATIMA_100)
            assertThat(viewModel.uiState.value.isTasbihOpen).isFalse()

            viewModel.incrementTasbih()
            viewModel.incrementTasbih()
            viewModel.incrementTasbih()
            assertThat(viewModel.uiState.value.tasbihCount).isEqualTo(3)

            viewModel.setTasbihMode(TasbihTargetMode.TARGET_33)
            assertThat(viewModel.uiState.value.tasbihMode).isEqualTo(TasbihTargetMode.TARGET_33)

            viewModel.resetTasbih()
            assertThat(viewModel.uiState.value.tasbihCount).isEqualTo(0)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun setTasbihOpen_updatesNavigationState() {
        try {
            assertThat(viewModel.uiState.value.isTasbihOpen).isFalse()

            viewModel.setTasbihOpen(true)
            assertThat(viewModel.uiState.value.isTasbihOpen).isTrue()

            viewModel.setTasbihOpen(false)
            assertThat(viewModel.uiState.value.isTasbihOpen).isFalse()
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun togglePostAdhanDua_persistsInRepository() = runTest(testDispatcher) {
        try {
            val initial = repository.userPreferencesFlow.first()
            assertThat(initial.showPostAdhanDua).isTrue()

            viewModel.togglePostAdhanDua(false)
            testDispatcher.scheduler.runCurrent()

            val updated = repository.userPreferencesFlow.first()
            assertThat(updated.showPostAdhanDua).isFalse()
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun qiblaState_updatesBearingAndNavigationState() = runTest(testDispatcher) {
        try {
            assertThat(viewModel.uiState.value.isQiblaOpen).isFalse()

            // Update Qibla screen visibility
            viewModel.setQiblaOpen(true)
            assertThat(viewModel.uiState.value.isQiblaOpen).isTrue()

            viewModel.setQiblaOpen(false)
            assertThat(viewModel.uiState.value.isQiblaOpen).isFalse()

            // Update location to London
            viewModel.updateLocation(51.5074, -0.1278, "London, UK")
            testDispatcher.scheduler.runCurrent()

            val state = viewModel.uiState.value
            assertThat(state.qiblaBearing).isWithin(0.5).of(118.98)
            assertThat(state.qiblaReadout).isEqualTo("119° ESE")
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun togglePrayerCompletion_updatesTodayPrayerLogAndRollingHistory() = runTest(testDispatcher) {
        try {
            testDispatcher.scheduler.runCurrent()

            // Initially no prayers completed
            val initialLog = viewModel.uiState.value.todayPrayerLog
            assertThat(initialLog.completedPrayers).isEmpty()

            // Toggle Fajr
            viewModel.togglePrayerCompletion(PrayerType.FAJR)
            testDispatcher.scheduler.runCurrent()

            val updatedLog = viewModel.uiState.value.todayPrayerLog
            assertThat(updatedLog.completedPrayers).containsExactly(PrayerType.FAJR)
            assertThat(viewModel.uiState.value.rollingPrayerHistory.todayCompletedCount).isEqualTo(1)

            // Toggle Dhuhr
            viewModel.togglePrayerCompletion(PrayerType.DHUHR)
            testDispatcher.scheduler.runCurrent()

            val updatedLog2 = viewModel.uiState.value.todayPrayerLog
            assertThat(updatedLog2.completedPrayers).containsExactly(PrayerType.FAJR, PrayerType.DHUHR)
            assertThat(viewModel.uiState.value.rollingPrayerHistory.todayCompletedCount).isEqualTo(2)

            // Toggle Fajr again (uncheck)
            viewModel.togglePrayerCompletion(PrayerType.FAJR)
            testDispatcher.scheduler.runCurrent()

            val uncheckLog = viewModel.uiState.value.todayPrayerLog
            assertThat(uncheckLog.completedPrayers).containsExactly(PrayerType.DHUHR)
            assertThat(viewModel.uiState.value.rollingPrayerHistory.todayCompletedCount).isEqualTo(1)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun selectNavTab_updatesCurrentTabAndSettingsVisibility() = runTest(testDispatcher) {
        try {
            assertThat(viewModel.uiState.value.currentTab).isEqualTo(AppNavTab.DASHBOARD)
            assertThat(viewModel.uiState.value.isSettingsOpen).isFalse()

            // Select Tools
            viewModel.selectNavTab(AppNavTab.TOOLS)
            assertThat(viewModel.uiState.value.currentTab).isEqualTo(AppNavTab.TOOLS)
            assertThat(viewModel.uiState.value.isSettingsOpen).isFalse()

            // Select Settings
            viewModel.selectNavTab(AppNavTab.SETTINGS)
            assertThat(viewModel.uiState.value.currentTab).isEqualTo(AppNavTab.SETTINGS)
            assertThat(viewModel.uiState.value.isSettingsOpen).isTrue()

            // Select Dashboard
            viewModel.selectNavTab(AppNavTab.DASHBOARD)
            assertThat(viewModel.uiState.value.currentTab).isEqualTo(AppNavTab.DASHBOARD)
            assertThat(viewModel.uiState.value.isSettingsOpen).isFalse()
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun updateThemeModeAndDynamicColor_updatesPreferences() = runTest(testDispatcher) {
        try {
            testDispatcher.scheduler.runCurrent()
            assertThat(viewModel.uiState.value.userPreferences.themeMode).isEqualTo(ThemeModeOption.DARK)
            assertThat(viewModel.uiState.value.userPreferences.dynamicColorEnabled).isFalse()

            viewModel.updateThemeMode(ThemeModeOption.LIGHT)
            testDispatcher.scheduler.runCurrent()
            assertThat(repository.userPreferencesFlow.first().themeMode).isEqualTo(ThemeModeOption.LIGHT)

            viewModel.updateDynamicColorEnabled(true)
            testDispatcher.scheduler.runCurrent()
            assertThat(repository.userPreferencesFlow.first().dynamicColorEnabled).isTrue()
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun scheduleTestAlarm_delegatesToAlarmScheduler() = runTest(testDispatcher) {
        try {
            testDispatcher.scheduler.runCurrent()
            viewModel.scheduleTestAlarm(delaySeconds = 10)

            verify(atLeast = 1) {
                alarmScheduler.scheduleTestAlarm(
                    delaySeconds = 10,
                    prayerType = any(),
                    alertMode = any(),
                    autoSilenceSeconds = any(),
                    showPostAdhanDua = any(),
                    volumeEscalationSeconds = VolumeEscalationOption.SECONDS_20.seconds
                )
            }
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun updateAutoMuteDurationAndTargetMode_updatesPreferences() = runTest(testDispatcher) {
        try {
            testDispatcher.scheduler.runCurrent()
            assertThat(viewModel.uiState.value.userPreferences.autoMuteDuration).isEqualTo(AutoMuteDuration.OFF)
            assertThat(viewModel.uiState.value.userPreferences.autoMuteTargetMode).isEqualTo(AutoMuteTargetMode.SILENT)

            viewModel.updateAutoMuteDuration(AutoMuteDuration.MINUTES_25)
            testDispatcher.scheduler.runCurrent()
            assertThat(repository.userPreferencesFlow.first().autoMuteDuration).isEqualTo(AutoMuteDuration.MINUTES_25)

            viewModel.updateAutoMuteTargetMode(AutoMuteTargetMode.VIBRATE)
            testDispatcher.scheduler.runCurrent()
            assertThat(repository.userPreferencesFlow.first().autoMuteTargetMode).isEqualTo(AutoMuteTargetMode.VIBRATE)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun ramadanHub_stateAndToggleOperations() = runTest(testDispatcher) {
        try {
            testDispatcher.scheduler.runCurrent()
            assertThat(viewModel.uiState.value.isRamadanOpen).isFalse()
            assertThat(viewModel.uiState.value.suhoorCutoffFormatted).isNotEmpty()
            assertThat(viewModel.uiState.value.iftarFormatted).isNotEmpty()
            assertThat(viewModel.uiState.value.fastingCountdown).isNotEmpty()

            // Open Ramadan Hub
            viewModel.setRamadanOpen(true)
            assertThat(viewModel.uiState.value.isRamadanOpen).isTrue()

            // Toggle fast
            viewModel.toggleTodayFast()
            testDispatcher.scheduler.runCurrent()

            // Close Ramadan Hub
            viewModel.setRamadanOpen(false)
            assertThat(viewModel.uiState.value.isRamadanOpen).isFalse()
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun tahajjudAndNightFasting_stateAndCardExpansion_behaveCorrectly() = runTest(testDispatcher) {
        try {
            testDispatcher.scheduler.runCurrent()
            val state = viewModel.uiState.value

            assertThat(state.tahajjudCountdown).isNotEmpty()
            assertThat(state.tahajjudStartTimeFormatted).isNotEmpty()
            assertThat(state.islamicMidnightFormatted).isNotEmpty()
            assertThat(state.isNightFastingCardExpanded).isTrue()

            // Toggle card expansion
            viewModel.toggleNightFastingCardExpanded()
            assertThat(viewModel.uiState.value.isNightFastingCardExpanded).isFalse()

            viewModel.toggleNightFastingCardExpanded()
            assertThat(viewModel.uiState.value.isNightFastingCardExpanded).isTrue()
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun volumeEscalation_updatesPreferenceAndReschedulesAlarms() = runTest(testDispatcher) {
        try {
            testDispatcher.scheduler.runCurrent()

            viewModel.updateVolumeEscalation(VolumeEscalationOption.SECONDS_30)
            testDispatcher.scheduler.runCurrent()

            assertThat(repository.userPreferencesFlow.first().volumeEscalation)
                .isEqualTo(VolumeEscalationOption.SECONDS_30)

            io.mockk.coVerify(atLeast = 1) {
                alarmScheduler.scheduleRollingAlarms(
                    preferences = match { it.volumeEscalation == VolumeEscalationOption.SECONDS_30 },
                    startDate = any(),
                    daysAhead = any(),
                    currentEpochMillis = any(),
                    ramadanSettings = any()
                )
            }
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun tahajjudAlarm_updatesPreferenceAndReschedulesAlarms() = runTest(testDispatcher) {
        try {
            testDispatcher.scheduler.runCurrent()

            viewModel.toggleTahajjudAlarm(true)
            testDispatcher.scheduler.runCurrent()

            assertThat(repository.userPreferencesFlow.first().tahajjudAlarmEnabled).isTrue()

            viewModel.updateTahajjudMinutesOffset(20)
            testDispatcher.scheduler.runCurrent()

            assertThat(repository.userPreferencesFlow.first().tahajjudAlarmMinutesOffset).isEqualTo(20)

            io.mockk.coVerify(atLeast = 1) {
                alarmScheduler.scheduleRollingAlarms(
                    preferences = match { it.tahajjudAlarmMinutesOffset == 20 },
                    startDate = any(),
                    daysAhead = any(),
                    currentEpochMillis = any(),
                    ramadanSettings = any()
                )
            }
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun dashboardUiState_providesCompleteCleanDashboardProperties() = runTest(testDispatcher) {
        try {
            testDispatcher.scheduler.runCurrent()
            val state = viewModel.uiState.value

            assertThat(state.locationName).isNotEmpty()
            assertThat(state.hijriDateString).isNotEmpty()
            assertThat(state.nextPrayerName).isNotEmpty()
            assertThat(state.liveCountdownString).isNotEmpty()
            assertThat(state.nextPrayerTimeFormatted).isNotEmpty()
            assertThat(state.dailyPrayers).hasSize(6)
            assertThat(state.todayPrayerLog.totalCount).isEqualTo(5)
            assertThat(state.todayPrayerLog.completedCount).isIn(com.google.common.collect.Range.closed(0, 5))
            assertThat(state.suhoorCutoffFormatted).isNotEmpty()
            assertThat(state.iftarFormatted).isNotEmpty()
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun fridayJumuahMode_onFriday_displaysJumuahInsteadOfDhuhr() = runTest(testDispatcher) {
        // Friday Nov 6, 2026, 11:30:00 UTC (14:30:00 Makkah time or midday)
        // Instant for 2026-11-06T08:00:00Z
        val fridayInstant = Instant.parse("2026-11-06T08:00:00Z")
        val fridayClock = MutableClock(fridayInstant.toEpochMilliseconds())

        val fridayVm = PrayerViewModel(
            calculationEngine = calculationEngine,
            preferencesRepository = repository,
            alarmScheduler = alarmScheduler,
            prayerLogRepository = prayerLogRepository,
            fastTrackerRepository = FastTrackerRepository(dataStore),
            clock = fridayClock
        )

        try {
            testDispatcher.scheduler.runCurrent()
            val state = fridayVm.uiState.value

            assertThat(state.isFriday).isTrue()

            // Verify Dhuhr row display name is JUMU'AH
            val dhuhrRow = state.dailyPrayers.firstOrNull { it.prayerType == PrayerType.DHUHR }
            assertThat(dhuhrRow).isNotNull()
            assertThat(dhuhrRow?.name).isEqualTo("JUMU'AH")

            // If next prayer is Dhuhr, nextPrayerName should be JUMU'AH
            if (state.nextPrayerType == PrayerType.DHUHR) {
                assertThat(state.nextPrayerName).isEqualTo("JUMU'AH")
            }
        } finally {
            fridayVm.viewModelScope.cancel()
        }
    }

    @Test
    fun fridayJumuahMode_onNonFriday_displaysStandardDhuhr() = runTest(testDispatcher) {
        // Thursday Nov 5, 2026
        val thursdayInstant = Instant.parse("2026-11-05T08:00:00Z")
        val thursdayClock = MutableClock(thursdayInstant.toEpochMilliseconds())

        val thursdayVm = PrayerViewModel(
            calculationEngine = calculationEngine,
            preferencesRepository = repository,
            alarmScheduler = alarmScheduler,
            prayerLogRepository = prayerLogRepository,
            fastTrackerRepository = FastTrackerRepository(dataStore),
            clock = thursdayClock
        )

        try {
            testDispatcher.scheduler.runCurrent()
            val state = thursdayVm.uiState.value

            assertThat(state.isFriday).isFalse()

            val dhuhrRow = state.dailyPrayers.firstOrNull { it.prayerType == PrayerType.DHUHR }
            assertThat(dhuhrRow).isNotNull()
            assertThat(dhuhrRow?.name).isEqualTo("DHUHR")

            if (state.nextPrayerType == PrayerType.DHUHR) {
                assertThat(state.nextPrayerName).isEqualTo("DHUHR")
            }
        } finally {
            thursdayVm.viewModelScope.cancel()
        }
    }

    @Test
    fun toggleSurahKahfRead_updatesUiStateAndPersists() = runTest(testDispatcher) {
        val fridayInstant = Instant.parse("2026-11-06T08:00:00Z")
        val fridayClock = MutableClock(fridayInstant.toEpochMilliseconds())

        val fridayVm = PrayerViewModel(
            calculationEngine = calculationEngine,
            preferencesRepository = repository,
            alarmScheduler = alarmScheduler,
            prayerLogRepository = prayerLogRepository,
            fastTrackerRepository = FastTrackerRepository(dataStore),
            clock = fridayClock
        )

        try {
            testDispatcher.scheduler.runCurrent()
            assertThat(fridayVm.uiState.value.isSurahKahfRead).isFalse()

            // Toggle to read
            fridayVm.toggleSurahKahfRead()
            testDispatcher.scheduler.runCurrent()
            assertThat(fridayVm.uiState.value.isSurahKahfRead).isTrue()

            // Toggle again to unread
            fridayVm.toggleSurahKahfRead()
            testDispatcher.scheduler.runCurrent()
            assertThat(fridayVm.uiState.value.isSurahKahfRead).isFalse()
        } finally {
            fridayVm.viewModelScope.cancel()
        }
    }

    @Test
    fun updateFridayAutoMuteDuration_updatesPreferences() = runTest(testDispatcher) {
        try {
            testDispatcher.scheduler.runCurrent()

            viewModel.updateFridayAutoMuteDuration(FridayAutoMuteDuration.MINUTES_45)
            testDispatcher.scheduler.runCurrent()

            assertThat(repository.userPreferencesFlow.first().fridayAutoMuteDuration)
                .isEqualTo(FridayAutoMuteDuration.MINUTES_45)

            viewModel.updateFridayAutoMuteDuration(FridayAutoMuteDuration.MINUTES_60)
            testDispatcher.scheduler.runCurrent()

            assertThat(repository.userPreferencesFlow.first().fridayAutoMuteDuration)
                .isEqualTo(FridayAutoMuteDuration.MINUTES_60)
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    @Test
    fun subPages_salawat_citySelection_andOnboarding_updateStateCleanly() = runTest(testDispatcher) {
        try {
            testDispatcher.scheduler.runCurrent()

            viewModel.setTrackerOpen(true)
            assertThat(viewModel.uiState.value.isTrackerOpen).isTrue()

            viewModel.setTahajjudOpen(true)
            assertThat(viewModel.uiState.value.isTahajjudOpen).isTrue()

            viewModel.setFridayGuideOpen(true)
            assertThat(viewModel.uiState.value.isFridayGuideOpen).isTrue()

            viewModel.selectNavTab(AppNavTab.TOOLS)
            assertThat(viewModel.uiState.value.isTrackerOpen).isFalse()
            assertThat(viewModel.uiState.value.isTahajjudOpen).isFalse()
            assertThat(viewModel.uiState.value.isFridayGuideOpen).isFalse()

            viewModel.incrementSalawat()
            viewModel.incrementSalawat()
            testDispatcher.scheduler.runCurrent()
            assertThat(viewModel.uiState.value.salawatCount).isEqualTo(2)
            assertThat(repository.userPreferencesFlow.first().salawatCount).isEqualTo(2)
            viewModel.resetSalawat()
            testDispatcher.scheduler.runCurrent()
            assertThat(viewModel.uiState.value.salawatCount).isEqualTo(0)
            assertThat(repository.userPreferencesFlow.first().salawatCount).isEqualTo(0)

            viewModel.incrementTasbih()
            viewModel.setTasbihMode(dev.hayya.core.domain.tasbih.TasbihTargetMode.TARGET_33)
            testDispatcher.scheduler.runCurrent()
            assertThat(repository.userPreferencesFlow.first().tasbihCount).isEqualTo(1)
            assertThat(repository.userPreferencesFlow.first().tasbihMode)
                .isEqualTo(dev.hayya.core.domain.tasbih.TasbihTargetMode.TARGET_33)

            val london = CityCatalog.search("London").first()
            viewModel.selectCity(london)
            testDispatcher.scheduler.runCurrent()
            assertThat(viewModel.uiState.value.locationName).isEqualTo(london.displayTitle)

            val larkana = CityCatalog.search("Larkana", district = "Larkana").first()
            viewModel.selectCity(larkana)
            testDispatcher.scheduler.runCurrent()
            assertThat(viewModel.uiState.value.locationName).isEqualTo(larkana.displayTitle)
            assertThat(viewModel.uiState.value.sehriIhtiyatFormatted).isNotEmpty()
            assertThat(viewModel.uiState.value.ishraqTimeFormatted).isNotEmpty()
            assertThat(viewModel.uiState.value.zawalWindowFormatted).isNotEmpty()
            assertThat(viewModel.uiState.value.mosqueSeasonLabel).isNotEmpty()
            val fajrRow = viewModel.uiState.value.dailyPrayers.first { it.prayerType == PrayerType.FAJR }
            assertThat(fajrRow.jamaatTimeFormatted).isNotEmpty()

            viewModel.openOnboarding()
            assertThat(viewModel.uiState.value.isOnboardingForced).isTrue()
            viewModel.completeOnboarding()
            testDispatcher.scheduler.runCurrent()
            assertThat(viewModel.uiState.value.isOnboardingForced).isFalse()
            assertThat(repository.userPreferencesFlow.first().hasCompletedOnboarding).isTrue()
            assertThat(viewModel.uiState.value.isPreferencesLoaded).isTrue()
        } finally {
            viewModel.viewModelScope.cancel()
        }
    }

    private class MutableClock(var nowMillis: Long) : Clock {
        override fun now(): Instant = Instant.fromEpochMilliseconds(nowMillis)
    }
}
