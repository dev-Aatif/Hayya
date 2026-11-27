package dev.hayya.core.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batoulapps.adhan.Coordinates
import dev.hayya.core.data.prayerlog.PrayerLogRepository
import dev.hayya.core.data.preferences.UserPreferencesRepository
import dev.hayya.core.data.ramadan.FastTrackerRepository
import dev.hayya.core.domain.calculation.DailyPrayerTimes
import dev.hayya.core.domain.calculation.FastingType
import dev.hayya.core.domain.calculation.NightFastingCalculator
import dev.hayya.core.domain.calculation.PrayerCalculationEngine
import dev.hayya.core.domain.calculation.SindhMosqueSchedule
import dev.hayya.core.domain.model.AlertMode
import dev.hayya.core.domain.model.AutoMuteDuration
import dev.hayya.core.domain.model.AutoMuteTargetMode
import dev.hayya.core.domain.model.AutoSilenceTimeout
import dev.hayya.core.domain.model.CalculationMethodOption
import dev.hayya.core.domain.model.CityPreset
import dev.hayya.core.domain.model.FridayAutoMuteDuration
import dev.hayya.core.domain.model.GeoLocation
import dev.hayya.core.domain.model.HighLatitudeRuleOption
import dev.hayya.core.domain.model.MadhabOption
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.model.PrayerWindow
import dev.hayya.core.domain.model.SavedRingerState
import dev.hayya.core.domain.model.ThemeModeOption
import dev.hayya.core.domain.model.UserPreferences
import dev.hayya.core.domain.model.VolumeEscalationOption
import dev.hayya.core.domain.prayerlog.DailyPrayerLog
import dev.hayya.core.domain.prayerlog.RollingPrayerHistory
import dev.hayya.core.domain.qibla.QiblaCalculator
import dev.hayya.core.domain.ramadan.*
import dev.hayya.core.domain.tasbih.TasbihTargetMode
import dev.hayya.core.domain.update.UpdateCheckResult
import dev.hayya.core.domain.update.UpdateUiState
import dev.hayya.core.system.alarm.PrayerAlarmScheduler
import dev.hayya.core.system.audio.AutoMuteManager
import dev.hayya.core.system.update.AppUpdateEngine
import dev.hayya.core.ui.components.AppNavTab
import java.io.File
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import java.time.chrono.HijrahDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * UI presentation model representing an individual row in the daily timetable.
 */
data class PrayerTimeRowUiState(
    val prayerType: PrayerType,
    val name: String,
    val timeMillis: Long,
    val timeFormatted: String,
    val isPast: Boolean,
    val isActive: Boolean,
    val audioMode: AlertMode,
    val jamaatTimeFormatted: String = ""
)

/**
 * Complete, unified immutable UI state for the ultra-minimalist prayer dashboard.
 */
data class PrayerDashboardUiState(
    val locationName: String = "Larkana, Pakistan",
    val nextPrayerType: PrayerType = PrayerType.DHUHR,
    val nextPrayerName: String = "DHUHR",
    val nextPrayerTimestampMillis: Long = 0L,
    val nextPrayerTimeFormatted: String = "12:00 PM",
    val liveCountdownString: String = "00:00:00",
    val progressRatio: Float = 0.0f,
    val dailyPrayers: List<PrayerTimeRowUiState> = emptyList(),
    val hijriDateString: String = "",
    val userPreferences: UserPreferences = UserPreferences(),
    val isSettingsOpen: Boolean = false,
    val isTasbihOpen: Boolean = false,
    val tasbihCount: Int = 0,
    val tasbihMode: TasbihTargetMode = TasbihTargetMode.FATIMA_100,
    val isQiblaOpen: Boolean = false,
    val qiblaBearing: Double = 0.0,
    val qiblaReadout: String = "0° N",
    val todayPrayerLog: DailyPrayerLog = DailyPrayerLog(LocalDate(2026, 1, 1)),
    val rollingPrayerHistory: RollingPrayerHistory = RollingPrayerHistory(),
    val savedRingerState: SavedRingerState = SavedRingerState(),
    val isRamadanOpen: Boolean = false,
    val islamicDateInfo: IslamicDateInfo = IslamicDateInfo(
        gregorianDate = LocalDate(2026, 1, 1),
        hijriDay = 1,
        hijriMonth = 9,
        hijriYear = 1447,
        hijriMonthName = "Ramadan",
        formattedHijriDate = "1 Ramadan 1447 AH"
    ),
    val todayFastRecord: DailyFastRecord? = null,
    val ramadanProgress: RamadanMonthProgress = RamadanMonthProgress(),
    val ramadanSettings: RamadanSettings = RamadanSettings(),
    val suhoorCutoffFormatted: String = "05:00 AM",
    val iftarFormatted: String = "06:30 PM",
    val fastingCountdown: String = "00:00:00",
    val isFastingCountdownToIftar: Boolean = true,
    val tahajjudCountdown: String = "00:00:00",
    val isTahajjudActiveNow: Boolean = false,
    val tahajjudStartTimeFormatted: String = "03:45 AM",
    val islamicMidnightFormatted: String = "11:45 PM",
    val tahajjudTargetMillis: Long = 0L,
    val isNightFastingCardExpanded: Boolean = true,
    val fastingDayType: FastingType = FastingType.NONE,
    val fastingDayTitle: String = "",
    val tomorrowFastingDayTitle: String = "",
    val isSunnahFastingToday: Boolean = false,
    val isSunnahFastingTomorrow: Boolean = false,
    val currentTab: AppNavTab = AppNavTab.DASHBOARD,
    val isFriday: Boolean = false,
    val isSurahKahfRead: Boolean = false,
    val isTrackerOpen: Boolean = false,
    val isTahajjudOpen: Boolean = false,
    val isFridayGuideOpen: Boolean = false,
    val isOnboardingForced: Boolean = false,
    val salawatCount: Int = 0,
    val sehriIhtiyatFormatted: String = "",
    val ishraqTimeFormatted: String = "",
    val zawalWindowFormatted: String = "",
    val mosqueSeasonLabel: String = "",
    val isPreferencesLoaded: Boolean = false,
    val updateState: UpdateUiState = UpdateUiState()
)

/**
 * Isolated 1-second ticker slice so countdown updates only recompose timer views
 * rather than re-evaluating the root Activity navigation shell every second.
 */
data class PrayerCountdownUiState(
    val liveCountdownString: String = "00:00:00",
    val fastingCountdown: String = "00:00:00",
    val isFastingCountdownToIftar: Boolean = true,
    val tahajjudCountdown: String = "00:00:00",
    val isTahajjudActiveNow: Boolean = false
)

/**
 * ViewModel orchestrating live prayer time calculations, 1-second countdown tickers,
 * and user preference updates with automated alarm rescheduling.
 */
class PrayerViewModel(
    private val calculationEngine: PrayerCalculationEngine,
    private val preferencesRepository: UserPreferencesRepository,
    private val alarmScheduler: PrayerAlarmScheduler? = null,
    private val prayerLogRepository: PrayerLogRepository? = null,
    private val fastTrackerRepository: FastTrackerRepository? = null,
    private val clock: Clock = Clock.System,
    private val updateEngine: AppUpdateEngine? = null
) : ViewModel() {

    private val initialSnapshot = PrayerDashboardUiState(
        updateState = UpdateUiState(
            currentVersionName = updateEngine?.currentVersionName() ?: "1.0.0"
        )
    )
    private val _uiState = MutableStateFlow(initialSnapshot)
    val uiState: StateFlow<PrayerDashboardUiState> = _uiState.asStateFlow()

    private val _staticUiState = MutableStateFlow(initialSnapshot)
    val staticUiState: StateFlow<PrayerDashboardUiState> = _staticUiState.asStateFlow()

    private val _countdownState = MutableStateFlow(PrayerCountdownUiState())
    val countdownState: StateFlow<PrayerCountdownUiState> = _countdownState.asStateFlow()

    private var currentPreferences = UserPreferences()
    private val currentDateState = MutableStateFlow<LocalDate?>(null)
    private var cachedDailyTimes: DailyPrayerTimes? = null
    private var cachedYesterdayMaghribMillis: Long? = null
    private var hasAttemptedLaunchUpdateCheck = false

    private inline fun updateCoreState(transform: (PrayerDashboardUiState) -> PrayerDashboardUiState) {
        _staticUiState.update(transform)
        _uiState.update(transform)
    }

    init {
        // Observe preferences and recalculate on changes
        viewModelScope.launch {
            preferencesRepository.userPreferencesFlow.collectLatest { prefs ->
                currentPreferences = prefs
                refreshCalculation(clock.now().toEpochMilliseconds())
            }
        }

        // Observe active congregational prayer auto-mute ringer state
        viewModelScope.launch {
            preferencesRepository.savedRingerStateFlow.collectLatest { savedState ->
                updateCoreState { it.copy(savedRingerState = savedState) }
            }
        }

        // Observe today's prayer log and 7-day rolling completion history
        prayerLogRepository?.let { repo ->
            viewModelScope.launch {
                currentDateState
                    .filterNotNull()
                    .distinctUntilChanged()
                    .flatMapLatest { date ->
                        repo.observeRollingHistory(date, daysCount = 7)
                    }
                    .collectLatest { history ->
                        val today = currentDateState.value
                        val todayLog = history.dailyLogs.find { it.date == today }
                            ?: today?.let { DailyPrayerLog(it) }
                            ?: DailyPrayerLog(LocalDate(2026, 1, 1))
                        updateCoreState {
                            it.copy(
                                todayPrayerLog = todayLog,
                                rollingPrayerHistory = history
                            )
                        }
                    }
            }
        }

        // Observe Ramadan & Fasting tracker state
        fastTrackerRepository?.let { repo ->
            viewModelScope.launch {
                currentDateState
                    .filterNotNull()
                    .distinctUntilChanged()
                    .flatMapLatest { date ->
                        repo.observeDailyFast(date)
                    }
                    .collectLatest { fastRecord ->
                        updateCoreState { it.copy(todayFastRecord = fastRecord) }
                    }
            }

            viewModelScope.launch {
                combine(
                    currentDateState.filterNotNull(),
                    preferencesRepository.userPreferencesFlow.map { it.hijriDayOffsetDays }.distinctUntilChanged()
                ) { date, offset ->
                    IslamicCalendarHelper.resolveIslamicDate(date, offset).hijriYear
                }
                    .distinctUntilChanged()
                    .flatMapLatest { year ->
                        repo.observeRamadanProgress(year)
                    }
                    .collectLatest { progress ->
                        updateCoreState { it.copy(ramadanProgress = progress) }
                    }
            }

            viewModelScope.launch {
                repo.observeRamadanSettings().collectLatest { settings ->
                    updateCoreState { it.copy(ramadanSettings = settings) }
                }
            }
        }

        // 1-second live countdown ticker, active ONLY while a UI screen or subscriber collects state.
        viewModelScope.launch {
            combine(_uiState.subscriptionCount, _countdownState.subscriptionCount) { uiSubs, tickSubs ->
                uiSubs > 0 || tickSubs > 0
            }
                .distinctUntilChanged()
                .collectLatest { hasSubscribers ->
                    if (!hasSubscribers) return@collectLatest
                    while (currentCoroutineContext().isActive) {
                        val nowMillis = clock.now().toEpochMilliseconds()
                        updateCountdownAndProgress(nowMillis)
                        delay(1000L)
                    }
                }
        }
    }

    /**
     * Cycles prayer alert audio mode: FULL_ALARM -> GENTLE_BANNER -> MUTED -> FULL_ALARM.
     */
    fun togglePrayerAudioMode(prayerType: PrayerType) {
        val currentMode = currentPreferences.getAlertMode(prayerType)
        val nextMode = when (currentMode) {
            AlertMode.FULL_ALARM -> AlertMode.GENTLE_BANNER
            AlertMode.GENTLE_BANNER -> AlertMode.MUTED
            AlertMode.MUTED -> AlertMode.FULL_ALARM
        }

        viewModelScope.launch {
            preferencesRepository.updateAlertMode(prayerType, nextMode)
            alarmScheduler?.scheduleRollingAlarms(preferences = currentPreferences.copy(
                alertModes = currentPreferences.alertModes + (prayerType to nextMode)
            ))
        }
    }

    /**
     * Updates juristic Madhab for Asr and reschedules rolling alarms.
     */
    fun updateJuristicMethod(madhab: MadhabOption) {
        viewModelScope.launch {
            preferencesRepository.updateMadhab(madhab)
            alarmScheduler?.scheduleRollingAlarms(preferences = currentPreferences.copy(madhab = madhab))
        }
    }

    /**
     * Updates auto-silence timeout preference and reschedules rolling alarms so
     * pending alarm PendingIntents receive the updated timeout extra.
     */
    fun updateAutoSilence(timeout: AutoSilenceTimeout) {
        viewModelScope.launch {
            preferencesRepository.updateAutoSilenceTimeout(timeout)
            alarmScheduler?.scheduleRollingAlarms(
                preferences = currentPreferences.copy(autoSilenceTimeout = timeout)
            )
        }
    }

    /**
     * Updates Congregational Prayer (Mosque) auto-mute duration.
     */
    fun updateAutoMuteDuration(duration: AutoMuteDuration) {
        viewModelScope.launch {
            preferencesRepository.updateAutoMuteDuration(duration)
        }
    }

    /**
     * Updates Congregational Prayer (Mosque) auto-mute target mode (Silent vs Vibrate).
     */
    fun updateAutoMuteTargetMode(mode: AutoMuteTargetMode) {
        viewModelScope.launch {
            preferencesRepository.updateAutoMuteTargetMode(mode)
        }
    }

    /**
     * Updates Friday (Jumu'ah) Khutbah extended auto-mute duration.
     */
    fun updateFridayAutoMuteDuration(duration: FridayAutoMuteDuration) {
        viewModelScope.launch {
            preferencesRepository.updateFridayAutoMuteDuration(duration)
        }
    }

    /**
     * Toggles Surah Al-Kahf completion status for today.
     */
    fun toggleSurahKahfRead() {
        val today = currentDateState.value ?: run {
            val timeZone = TimeZone.currentSystemDefault()
            Instant.fromEpochMilliseconds(clock.now().toEpochMilliseconds()).toLocalDateTime(timeZone).date
        }
        val isRead = !_uiState.value.isSurahKahfRead
        viewModelScope.launch {
            preferencesRepository.setSurahKahfReadForDate(today.toString(), isRead)
        }
    }

    /**
     * Manually activates Friday Khutbah auto-mute immediately (e.g. for early mosque attendees).
     */
    fun activateKhutbahAutoMute(context: Context) {
        viewModelScope.launch {
            val prefs = currentPreferences
            val durationMinutes = prefs.fridayAutoMuteDuration.resolveMinutes(prefs.autoMuteDuration).let {
                if (it > 0) it else 45
            }
            val activated = AutoMuteManager.activateAutoMute(
                context = context,
                durationMinutes = durationMinutes,
                targetMode = prefs.autoMuteTargetMode,
                repository = preferencesRepository,
                isFridayJumuah = true
            )
            if (activated) {
                val endEpochMillis = clock.now().toEpochMilliseconds() + (durationMinutes * 60_000L)
                alarmScheduler?.scheduleAutoUnmute(endEpochMillis)
            }
        }
    }

    /**
     * Manually restores normal system ringer mode and cancels active auto-mute.
     */
    fun restoreNormalRingerMode(context: Context) {
        viewModelScope.launch {
            AutoMuteManager.restoreRingerMode(context, preferencesRepository)
            alarmScheduler?.cancelAutoUnmute()
        }
    }

    /**
     * Toggles expansion state of the dashboard Night & Fasting card.
     */
    fun toggleNightFastingCardExpanded() {
        updateCoreState { it.copy(isNightFastingCardExpanded = !it.isNightFastingCardExpanded) }
    }

    /**
     * Updates gradual volume escalation option and reschedules rolling alarms.
     */
    fun updateVolumeEscalation(option: VolumeEscalationOption) {
        viewModelScope.launch {
            preferencesRepository.updateVolumeEscalation(option)
            alarmScheduler?.scheduleRollingAlarms(
                preferences = currentPreferences.copy(volumeEscalation = option)
            )
        }
    }

    /**
     * Toggles gentle Tahajjud / Qiyam al-Layl pre-alarm.
     */
    fun toggleTahajjudAlarm(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateTahajjudAlarmEnabled(enabled)
            alarmScheduler?.scheduleRollingAlarms(
                preferences = currentPreferences.copy(tahajjudAlarmEnabled = enabled)
            )
        }
    }

    /**
     * Updates Tahajjud pre-alarm offset in minutes.
     */
    fun updateTahajjudMinutesOffset(minutes: Int) {
        viewModelScope.launch {
            preferencesRepository.updateTahajjudAlarmMinutesOffset(minutes)
            alarmScheduler?.scheduleRollingAlarms(
                preferences = currentPreferences.copy(tahajjudAlarmMinutesOffset = minutes)
            )
        }
    }

    /**
     * Updates the current floating navbar tab destination.
     */
    fun selectNavTab(tab: AppNavTab) {
        updateCoreState {
            it.copy(
                currentTab = tab,
                isSettingsOpen = (tab == AppNavTab.SETTINGS),
                isTasbihOpen = false,
                isQiblaOpen = false,
                isRamadanOpen = false,
                isTrackerOpen = false,
                isTahajjudOpen = false,
                isFridayGuideOpen = false,
                isOnboardingForced = false
            )
        }
    }

    /**
     * Toggles visibility of the dedicated Prayer Tracker & 7-Day Habits page.
     */
    fun setTrackerOpen(isOpen: Boolean) {
        updateCoreState { it.copy(isTrackerOpen = isOpen) }
    }

    /**
     * Toggles visibility of the dedicated Night & Tahajjud Sky page.
     */
    fun setTahajjudOpen(isOpen: Boolean) {
        updateCoreState { it.copy(isTahajjudOpen = isOpen) }
    }

    /**
     * Toggles visibility of the dedicated Friday (Jumu'ah) Guide page.
     */
    fun setFridayGuideOpen(isOpen: Boolean) {
        updateCoreState { it.copy(isFridayGuideOpen = isOpen) }
    }

    /**
     * Opens the quick guided setup wizard on demand from Settings.
     */
    fun openOnboarding() {
        updateCoreState { it.copy(isOnboardingForced = true) }
    }

    /**
     * Marks guided onboarding complete and returns to the main dashboard.
     */
    fun completeOnboarding() {
        updateCoreState { it.copy(isOnboardingForced = false) }
        viewModelScope.launch {
            preferencesRepository.updateHasCompletedOnboarding(true)
        }
    }

    /**
     * Increments the Friday Salawat counter atomically in DataStore.
     */
    fun incrementSalawat() {
        updateCoreState { it.copy(salawatCount = it.salawatCount + 1) }
        viewModelScope.launch {
            val persisted = preferencesRepository.incrementSalawatCount()
            updateCoreState { it.copy(salawatCount = persisted) }
        }
    }

    /**
     * Resets the Friday Salawat counter and persists it.
     */
    fun resetSalawat() {
        updateCoreState { it.copy(salawatCount = 0) }
        viewModelScope.launch {
            preferencesRepository.updateSalawatCount(0)
        }
    }

    /**
     * Selects a curated city preset, updating coordinates, location name, and regional method offline.
     */
    fun selectCity(city: CityPreset) {
        viewModelScope.launch {
            preferencesRepository.updateCoordinates(
                latitude = city.latitude,
                longitude = city.longitude,
                locationName = city.displayTitle
            )
            preferencesRepository.updateCalculationMethod(city.recommendedMethod)
            alarmScheduler?.scheduleRollingAlarms(
                preferences = currentPreferences.copy(
                    location = GeoLocation(
                        latitude = city.latitude,
                        longitude = city.longitude,
                        altitude = 0.0,
                        resolvedLocationName = city.displayTitle
                    ),
                    calculationMethod = city.recommendedMethod
                )
            )
        }
    }

    /**
     * Toggles visibility of the dedicated Ramadan & Eid Hub screen.
     */
    fun setRamadanOpen(isOpen: Boolean) {
        updateCoreState { it.copy(isRamadanOpen = isOpen) }
    }

    /**
     * Toggles today's fasting status (Pending -> Fasted -> Missed -> Exempt -> Pending).
     */
    fun toggleTodayFast() {
        val today = currentDateState.value ?: return
        viewModelScope.launch {
            fastTrackerRepository?.cycleFastStatus(today)
        }
    }

    /**
     * Explicitly sets the fast status for [date].
     */
    fun setFastStatus(date: LocalDate, status: FastStatus) {
        viewModelScope.launch {
            fastTrackerRepository?.setFastStatus(date, status)
        }
    }

    /**
     * Cycles the fast status for [date].
     */
    fun cycleFastStatus(date: LocalDate) {
        viewModelScope.launch {
            fastTrackerRepository?.cycleFastStatus(date)
        }
    }

    /**
     * Updates count of completed Qadha make-up fasts.
     */
    fun updateQadhaMadeUp(count: Int) {
        viewModelScope.launch {
            fastTrackerRepository?.updateQadhaMadeUpCount(count)
        }
    }

    /**
     * Updates Suhoor wake-up alarm settings and reschedules rolling alarms.
     */
    fun updateSuhoorAlarm(enabled: Boolean, minutesBefore: Int? = null) {
        viewModelScope.launch {
            fastTrackerRepository?.updateSuhoorAlarmEnabled(enabled)
            if (minutesBefore != null) {
                fastTrackerRepository?.updateSuhoorAlarmMinutesBefore(minutesBefore)
            }
            val updatedSettings = _uiState.value.ramadanSettings.copy(
                suhoorAlarmEnabled = enabled,
                suhoorAlarmMinutesBefore = minutesBefore ?: _uiState.value.ramadanSettings.suhoorAlarmMinutesBefore
            )
            if (!enabled) {
                alarmScheduler?.cancelSuhoorAlarm()
            } else {
                alarmScheduler?.scheduleRollingAlarms(
                    preferences = currentPreferences,
                    ramadanSettings = updatedSettings
                )
            }
        }
    }

    /**
     * Updates Iftar Maghrib notification alert setting and updates pending alarms.
     */
    fun updateIftarAlert(enabled: Boolean) {
        viewModelScope.launch {
            fastTrackerRepository?.updateIftarNotificationEnabled(enabled)
            val updatedSettings = _uiState.value.ramadanSettings.copy(iftarNotificationEnabled = enabled)
            alarmScheduler?.scheduleRollingAlarms(
                preferences = currentPreferences,
                ramadanSettings = updatedSettings
            )
        }
    }

    /**
     * Deprecated for V1: Eid auto-notifications are deferred to a dedicated future Eid screen.
     */
    fun updateEidAlert(enabled: Boolean) {
        viewModelScope.launch {
            fastTrackerRepository?.updateEidNotificationEnabled(enabled)
            @Suppress("DEPRECATION")
            alarmScheduler?.cancelEidCelebration()
        }
    }

    /**
     * Toggles or updates settings screen visibility.
     */
    fun setSettingsOpen(isOpen: Boolean) {
        updateCoreState {
            it.copy(
                isSettingsOpen = isOpen,
                currentTab = if (isOpen) AppNavTab.SETTINGS else AppNavTab.DASHBOARD
            )
        }
    }

    /**
     * Updates visual theme mode (Pure OLED Dark, Warm Light, Follow System).
     */
    fun updateThemeMode(mode: ThemeModeOption) {
        viewModelScope.launch {
            preferencesRepository.updateThemeMode(mode)
        }
    }

    /**
     * Updates Material You dynamic color adaptation toggle.
     */
    fun updateDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateDynamicColorEnabled(enabled)
        }
    }

    /**
     * Toggles or updates Tasbih counter screen visibility.
     */
    fun setTasbihOpen(isOpen: Boolean) {
        updateCoreState { it.copy(isTasbihOpen = isOpen) }
    }

    /**
     * Toggles or updates Qibla compass screen visibility.
     */
    fun setQiblaOpen(isOpen: Boolean) {
        updateCoreState { it.copy(isQiblaOpen = isOpen) }
    }

    /**
     * Toggles the completion status of an obligatory daily prayer for today.
     */
    fun togglePrayerCompletion(prayer: PrayerType) {
        val today = currentDateState.value ?: run {
            val timeZone = TimeZone.currentSystemDefault()
            Instant.fromEpochMilliseconds(clock.now().toEpochMilliseconds()).toLocalDateTime(timeZone).date
        }
        viewModelScope.launch {
            prayerLogRepository?.togglePrayer(today, prayer)
        }
    }

    /**
     * Increments current Tasbih count atomically in DataStore.
     */
    fun incrementTasbih() {
        updateCoreState { it.copy(tasbihCount = it.tasbihCount + 1) }
        viewModelScope.launch {
            val persisted = preferencesRepository.incrementTasbihCount()
            updateCoreState { it.copy(tasbihCount = persisted) }
        }
    }

    /**
     * Resets current Tasbih count back to 0 and persists it.
     */
    fun resetTasbih() {
        updateCoreState { it.copy(tasbihCount = 0) }
        viewModelScope.launch {
            preferencesRepository.updateTasbihCount(0)
        }
    }

    /**
     * Sets operating Tasbih target mode and persists it.
     */
    fun setTasbihMode(mode: TasbihTargetMode) {
        updateCoreState { it.copy(tasbihMode = mode) }
        viewModelScope.launch {
            preferencesRepository.updateTasbihMode(mode)
        }
    }

    /**
     * Updates Post-Adhan Dua on wake screen visibility preference.
     */
    fun togglePostAdhanDua(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateShowPostAdhanDua(enabled)
        }
    }

    /**
     * Updates Isha expiration preference (Midnight vs Fajr).
     */
    fun updateIshaExpiresAtMidnight(expires: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateIshaExpiresAtMidnight(expires)
            alarmScheduler?.scheduleRollingAlarms(preferences = currentPreferences.copy(ishaExpiresAtMidnight = expires))
        }
    }

    /**
     * Updates Pre-Reminder (Tier 1: T - 15m) notification preference.
     */
    fun togglePreReminder(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updatePreReminderEnabled(enabled)
            alarmScheduler?.scheduleRollingAlarms(preferences = currentPreferences.copy(preReminderEnabled = enabled))
        }
    }

    /**
     * Updates Last Call (Tier 3: Window - 20m) notification preference.
     */
    fun toggleLastCall(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateLastCallEnabled(enabled)
            alarmScheduler?.scheduleRollingAlarms(preferences = currentPreferences.copy(lastCallEnabled = enabled))
        }
    }

    /**
     * Updates Pinned Foreground Status Notification preference (LMK anchor).
     */
    fun togglePinnedNotification(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updatePinnedNotificationEnabled(enabled)
        }
    }

    /**
     * Updates High Latitude twilight adjustment rule.
     */
    fun updateHighLatitudeRule(rule: HighLatitudeRuleOption) {
        viewModelScope.launch {
            preferencesRepository.updateHighLatitudeRule(rule)
            alarmScheduler?.scheduleRollingAlarms(preferences = currentPreferences.copy(highLatitudeRule = rule))
        }
    }

    /**
     * Updates regional Hijri moonsighting day offset (`-2..+2`) and reschedules alarms.
     */
    fun updateHijriDayOffsetDays(offsetDays: Int) {
        val clamped = offsetDays.coerceIn(-2, 2)
        viewModelScope.launch {
            preferencesRepository.updateHijriDayOffsetDays(clamped)
            alarmScheduler?.scheduleRollingAlarms(preferences = currentPreferences.copy(hijriDayOffsetDays = clamped))
        }
    }

    /**
     * Updates whether background launch checks against GitHub Releases are enabled.
     */
    fun toggleAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateAutoCheckUpdatesEnabled(enabled)
        }
    }

    /**
     * Schedules an instant or countdown-based test prayer alarm honoring current alert mode and auto-silence.
     */
    fun scheduleTestAlarm(delaySeconds: Int) {
        val nextPrayer = _uiState.value.nextPrayerType
        val alertMode = currentPreferences.getAlertMode(nextPrayer)
        val timeoutSeconds = currentPreferences.autoSilenceTimeout.seconds ?: 0
        alarmScheduler?.scheduleTestAlarm(
            delaySeconds = delaySeconds,
            prayerType = nextPrayer,
            alertMode = alertMode,
            autoSilenceSeconds = timeoutSeconds,
            showPostAdhanDua = currentPreferences.showPostAdhanDua,
            volumeEscalationSeconds = currentPreferences.volumeEscalation.seconds
        )
    }

    /**
     * Forces an immediate recalculation of daily prayer times, Hijri date, and countdowns
     * (e.g., when returning to the app after midnight or a timezone change).
     */
    fun refreshNow() {
        refreshCalculation(clock.now().toEpochMilliseconds())
    }

    /**
     * Updates calculation method convention. Synchronizes reference city coordinates only when
     * the current location is still set to the previous convention's canonical city.
     */
    fun updateCalculationMethod(method: CalculationMethodOption) {
        viewModelScope.launch {
            val previousMethod = currentPreferences.calculationMethod
            val currentLocation = currentPreferences.location
            val isUsingCanonicalDefault = currentLocation.resolvedLocationName.isBlank() ||
                currentLocation.resolvedLocationName == previousMethod.defaultCity ||
                CalculationMethodOption.entries.any {
                    it.defaultCity == currentLocation.resolvedLocationName &&
                        it.defaultLatitude == currentLocation.latitude &&
                        it.defaultLongitude == currentLocation.longitude
                }

            preferencesRepository.updateCalculationMethod(method)

            val updatedLocation = if (isUsingCanonicalDefault) {
                preferencesRepository.updateCoordinates(
                    latitude = method.defaultLatitude,
                    longitude = method.defaultLongitude,
                    locationName = method.defaultCity
                )
                currentLocation.copy(
                    latitude = method.defaultLatitude,
                    longitude = method.defaultLongitude,
                    resolvedLocationName = method.defaultCity
                )
            } else {
                currentLocation
            }

            alarmScheduler?.scheduleRollingAlarms(
                preferences = currentPreferences.copy(
                    calculationMethod = method,
                    location = updatedLocation
                )
            )
        }
    }

    /**
     * Updates manual minute offset for a prayer.
     */
    fun updatePrayerOffset(prayer: PrayerType, offsetMinutes: Int) {
        viewModelScope.launch {
            preferencesRepository.updatePrayerOffset(prayer, offsetMinutes)
            alarmScheduler?.scheduleRollingAlarms()
        }
    }

    /**
     * Updates geographical coordinates and location name.
     */
    fun updateLocation(latitude: Double, longitude: Double, name: String) {
        viewModelScope.launch {
            preferencesRepository.updateCoordinates(latitude, longitude, locationName = name)
            alarmScheduler?.scheduleRollingAlarms()
        }
    }

    /**
     * Checks GitHub Releases once on app launch when enabled and the throttle interval has elapsed.
     */
    fun checkForUpdatesOnLaunch() {
        val engine = updateEngine ?: return
        if (!currentPreferences.autoCheckUpdatesEnabled) return
        if (hasAttemptedLaunchUpdateCheck) return
        hasAttemptedLaunchUpdateCheck = true

        val nowMillis = clock.now().toEpochMilliseconds()
        val lastChecked = currentPreferences.lastUpdateCheckEpochMillis
        if (!engine.shouldAutoCheckOnLaunch(lastChecked, nowMillis)) {
            return
        }
        checkForUpdates(manual = false)
    }

    /**
     * Checks GitHub Releases for a newer version of Hayya.
     * Manual checks surface errors and up-to-date confirmations; automatic checks fail quietly when offline.
     */
    fun checkForUpdates(manual: Boolean = true) {
        val engine = updateEngine ?: return
        val currentUpdate = _uiState.value.updateState
        if (currentUpdate.isChecking || currentUpdate.isDownloading) return

        viewModelScope.launch {
            updateCoreState { state ->
                state.copy(
                    updateState = state.updateState.copy(
                        isChecking = true,
                        errorMessage = null,
                        isUpToDateConfirmed = false
                    )
                )
            }

            val result = engine.checkForUpdate()
            val nowMillis = clock.now().toEpochMilliseconds()
            preferencesRepository.updateLastUpdateCheckEpochMillis(nowMillis)

            when (result) {
                is UpdateCheckResult.UpdateAvailable -> {
                    val release = result.release
                    val alreadyNotified = currentPreferences.lastNotifiedUpdateVersion == release.versionName
                    if (!alreadyNotified) {
                        engine.notifyUpdateAvailable(release)
                        preferencesRepository.updateLastNotifiedUpdateVersion(release.versionName)
                    }
                    updateCoreState { state ->
                        state.copy(
                            updateState = state.updateState.copy(
                                isChecking = false,
                                availableRelease = release,
                                isUpToDateConfirmed = false,
                                lastCheckedEpochMillis = nowMillis,
                                errorMessage = null,
                                showUpdateDialog = manual || !alreadyNotified
                            )
                        )
                    }
                }

                is UpdateCheckResult.UpToDate -> {
                    updateCoreState { state ->
                        state.copy(
                            updateState = state.updateState.copy(
                                isChecking = false,
                                availableRelease = null,
                                downloadedApkPath = null,
                                isUpToDateConfirmed = manual,
                                lastCheckedEpochMillis = nowMillis,
                                errorMessage = null,
                                showUpdateDialog = false
                            )
                        )
                    }
                }

                is UpdateCheckResult.Failure -> {
                    updateCoreState { state ->
                        state.copy(
                            updateState = state.updateState.copy(
                                isChecking = false,
                                isUpToDateConfirmed = false,
                                lastCheckedEpochMillis = nowMillis,
                                errorMessage = if (manual) result.message else null
                            )
                        )
                    }
                }
            }
        }
    }

    fun openUpdateDialog() {
        updateCoreState { state ->
            state.copy(
                updateState = state.updateState.copy(
                    showUpdateDialog = true,
                    errorMessage = null
                )
            )
        }
    }

    fun dismissUpdateDialog() {
        updateCoreState { state ->
            state.copy(
                updateState = state.updateState.copy(
                    showUpdateDialog = false,
                    errorMessage = null
                )
            )
        }
    }

    fun startUpdateDownloadAndInstall() {
        val engine = updateEngine ?: return
        val currentUpdate = _uiState.value.updateState
        val release = currentUpdate.availableRelease ?: return
        if (currentUpdate.isDownloading) return

        if (!engine.canRequestPackageInstalls()) {
            updateCoreState { state ->
                state.copy(
                    updateState = state.updateState.copy(
                        needsInstallPermission = true,
                        errorMessage = null
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            updateCoreState { state ->
                state.copy(
                    updateState = state.updateState.copy(
                        isDownloading = true,
                        downloadProgressPercent = 0,
                        needsInstallPermission = false,
                        errorMessage = null
                    )
                )
            }

            val downloadResult = engine.downloadAndVerifyApk(release) { percent ->
                updateCoreState { state ->
                    state.copy(
                        updateState = state.updateState.copy(downloadProgressPercent = percent)
                    )
                }
            }

            downloadResult.fold(
                onSuccess = { apkFile ->
                    updateCoreState { state ->
                        state.copy(
                            updateState = state.updateState.copy(
                                isDownloading = false,
                                downloadProgressPercent = 100,
                                downloadedApkPath = apkFile.absolutePath,
                                errorMessage = null
                            )
                        )
                    }
                    val installResult = engine.launchApkInstaller(apkFile)
                    if (installResult.isFailure) {
                        updateCoreState { state ->
                            state.copy(
                                updateState = state.updateState.copy(
                                    errorMessage = installResult.exceptionOrNull()?.message
                                        ?: "Unable to open Android package installer."
                                )
                            )
                        }
                    }
                },
                onFailure = { error ->
                    updateCoreState { state ->
                        state.copy(
                            updateState = state.updateState.copy(
                                isDownloading = false,
                                errorMessage = error.message ?: "Failed to download update."
                            )
                        )
                    }
                }
            )
        }
    }

    fun installDownloadedApk() {
        val engine = updateEngine ?: return
        val path = _uiState.value.updateState.downloadedApkPath ?: return
        val apkFile = File(path)
        if (!apkFile.exists()) {
            updateCoreState { state ->
                state.copy(
                    updateState = state.updateState.copy(
                        downloadedApkPath = null,
                        errorMessage = "Cached APK was cleared. Please download again."
                    )
                )
            }
            return
        }
        if (!engine.canRequestPackageInstalls()) {
            updateCoreState { state ->
                state.copy(updateState = state.updateState.copy(needsInstallPermission = true))
            }
            return
        }
        val installResult = engine.launchApkInstaller(apkFile)
        if (installResult.isFailure) {
            updateCoreState { state ->
                state.copy(
                    updateState = state.updateState.copy(
                        errorMessage = installResult.exceptionOrNull()?.message
                            ?: "Unable to open Android package installer."
                    )
                )
            }
        }
    }

    fun refreshInstallPermissionState() {
        val engine = updateEngine ?: return
        if (_uiState.value.updateState.needsInstallPermission && engine.canRequestPackageInstalls()) {
            updateCoreState { state ->
                state.copy(updateState = state.updateState.copy(needsInstallPermission = false))
            }
        }
    }

    private fun refreshCalculation(nowMillis: Long) {
        val timeZone = TimeZone.currentSystemDefault()
        val localDate = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date
        currentDateState.value = localDate

        val coordinates = Coordinates(
            currentPreferences.location.latitude,
            currentPreferences.location.longitude
        )

        val params = calculationEngine.createParameters(
            method = currentPreferences.calculationMethod.toAdhanMethod(),
            madhab = currentPreferences.madhab.toAdhanMadhab(),
            highLatitudeRule = currentPreferences.highLatitudeRule.toAdhanRule()
        )

        val dailyTimes = calculationEngine.computeDailyTimes(
            date = localDate,
            coordinates = coordinates,
            parameters = params,
            manualOffsets = currentPreferences.manualMinuteOffsets
        )
        val yesterdayTimes = calculationEngine.computeDailyTimes(
            date = localDate.minus(1, DateTimeUnit.DAY),
            coordinates = coordinates,
            parameters = params,
            manualOffsets = currentPreferences.manualMinuteOffsets
        )
        cachedDailyTimes = dailyTimes
        cachedYesterdayMaghribMillis = yesterdayTimes.maghrib

        val window = calculationEngine.getPrayerWindow(
            timestampEpochMillis = nowMillis,
            coordinates = coordinates,
            parameters = params,
            timeZone = timeZone,
            manualOffsets = currentPreferences.manualMinuteOffsets,
            ishaExpiresAtMidnight = currentPreferences.ishaExpiresAtMidnight
        )

        val nextPrayer = window.nextPrayer
        val nextPrayerTimeMillis = dailyTimes.upcomingTimeFor(nextPrayer, nowMillis)

        val isFriday = localDate.dayOfWeek == DayOfWeek.FRIDAY
        val isSurahKahfRead = currentPreferences.isSurahKahfReadForDate(localDate.toString())
        val nextPrayerDisplayName = nextPrayer.displayName(isFriday)

        val mosqueInfo = SindhMosqueSchedule.computeDailyInfo(localDate, dailyTimes, isFriday)
        val dailyItems = buildDailyPrayerRows(
            dailyTimes = dailyTimes,
            nowMillis = nowMillis,
            window = window,
            isFriday = isFriday,
            jamaatTimesMillis = mosqueInfo.jamaatTimesMillis
        )
        val hijriOffset = currentPreferences.hijriDayOffsetDays
        val hijriDate = formatHijriDate(localDate, hijriOffset)
        val islamicDate = IslamicCalendarHelper.resolveIslamicDate(localDate, hijriOffset)

        val remainingMillis = (nextPrayerTimeMillis - nowMillis).coerceAtLeast(0L)
        val countdown = formatDuration(remainingMillis)

        val qiblaBearing = QiblaCalculator.calculateBearing(
            currentPreferences.location.latitude,
            currentPreferences.location.longitude
        )
        val qiblaReadout = QiblaCalculator.formatBearingReadout(qiblaBearing)

        val suhoorFormatted = formatTimeOfDay(dailyTimes.fajr, timeZone)
        val iftarFormatted = formatTimeOfDay(dailyTimes.maghrib, timeZone)
        val (fastingCountdownStr, isToIftar) = computeFastingCountdown(dailyTimes, nowMillis)

        val tahajjudSchedule = NightFastingCalculator.resolveTahajjudSchedule(
            dailyTimes = dailyTimes,
            nowEpochMillis = nowMillis,
            timeZone = timeZone,
            yesterdayMaghribMillis = yesterdayTimes.maghrib
        )
        val voluntaryFasting = NightFastingCalculator.resolveVoluntaryFasting(localDate, islamicDate, dailyTimes, nowMillis, timeZone)
        val tahajjudCountdownStr = NightFastingCalculator.formatDuration(tahajjudSchedule.remainingMillis)

        _countdownState.value = PrayerCountdownUiState(
            liveCountdownString = countdown,
            fastingCountdown = fastingCountdownStr,
            isFastingCountdownToIftar = isToIftar,
            tahajjudCountdown = tahajjudCountdownStr,
            isTahajjudActiveNow = tahajjudSchedule.isTahajjudActiveNow
        )

        updateCoreState {
            it.copy(
                locationName = currentPreferences.location.resolvedLocationName.ifEmpty { "Larkana" },
                nextPrayerType = nextPrayer,
                nextPrayerName = nextPrayerDisplayName,
                nextPrayerTimestampMillis = nextPrayerTimeMillis,
                nextPrayerTimeFormatted = formatTimeOfDay(nextPrayerTimeMillis, timeZone),
                liveCountdownString = countdown,
                progressRatio = window.elapsedProgress,
                dailyPrayers = dailyItems,
                hijriDateString = hijriDate,
                userPreferences = currentPreferences,
                qiblaBearing = qiblaBearing,
                qiblaReadout = qiblaReadout,
                islamicDateInfo = islamicDate,
                suhoorCutoffFormatted = suhoorFormatted,
                iftarFormatted = iftarFormatted,
                fastingCountdown = fastingCountdownStr,
                isFastingCountdownToIftar = isToIftar,
                tahajjudCountdown = tahajjudCountdownStr,
                isTahajjudActiveNow = tahajjudSchedule.isTahajjudActiveNow,
                tahajjudStartTimeFormatted = tahajjudSchedule.formattedLastThirdStart,
                islamicMidnightFormatted = tahajjudSchedule.formattedIslamicMidnight,
                tahajjudTargetMillis = tahajjudSchedule.targetEpochMillis,
                fastingDayType = voluntaryFasting.todayFastingType,
                fastingDayTitle = voluntaryFasting.todayReason,
                tomorrowFastingDayTitle = voluntaryFasting.tomorrowReason,
                isSunnahFastingToday = voluntaryFasting.isFastingDayToday,
                isSunnahFastingTomorrow = voluntaryFasting.isFastingDayTomorrow,
                isFriday = isFriday,
                isSurahKahfRead = isSurahKahfRead,
                sehriIhtiyatFormatted = formatTimeOfDay(mosqueInfo.sehriIhtiyatEpochMillis, timeZone),
                ishraqTimeFormatted = formatTimeOfDay(mosqueInfo.ishraqStartEpochMillis, timeZone),
                zawalWindowFormatted = "${formatTimeOfDay(mosqueInfo.zawalStartEpochMillis, timeZone)} – ${formatTimeOfDay(dailyTimes.dhuhr, timeZone)}",
                mosqueSeasonLabel = mosqueInfo.season.displayName,
                tasbihCount = currentPreferences.tasbihCount,
                tasbihMode = currentPreferences.tasbihMode,
                salawatCount = currentPreferences.salawatCount,
                isPreferencesLoaded = true
            )
        }
    }

    private fun updateCountdownAndProgress(nowMillis: Long) {
        val currentState = _uiState.value
        val targetMillis = currentState.nextPrayerTimestampMillis
        val currentLocalDate = Instant.fromEpochMilliseconds(nowMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date

        if (targetMillis <= 0L || nowMillis >= targetMillis || currentDateState.value != currentLocalDate) {
            // Target arrived or calendar date rolled over at midnight, trigger full recalculation
            refreshCalculation(nowMillis)
            return
        }

        val remainingMillis = targetMillis - nowMillis
        val countdown = formatDuration(remainingMillis)

        val times = cachedDailyTimes
        val (fastingCountdownStr, isToIftar) = if (times != null) {
            computeFastingCountdown(times, nowMillis)
        } else {
            Pair(currentState.fastingCountdown, currentState.isFastingCountdownToIftar)
        }

        val (tahajjudCountdownStr, isTahajjudActive) = if (times != null) {
            val schedule = NightFastingCalculator.resolveTahajjudSchedule(
                dailyTimes = times,
                nowEpochMillis = nowMillis,
                yesterdayMaghribMillis = cachedYesterdayMaghribMillis
            )
            Pair(NightFastingCalculator.formatDuration(schedule.remainingMillis), schedule.isTahajjudActiveNow)
        } else {
            Pair(currentState.tahajjudCountdown, currentState.isTahajjudActiveNow)
        }

        _countdownState.value = PrayerCountdownUiState(
            liveCountdownString = countdown,
            fastingCountdown = fastingCountdownStr,
            isFastingCountdownToIftar = isToIftar,
            tahajjudCountdown = tahajjudCountdownStr,
            isTahajjudActiveNow = isTahajjudActive
        )

        if (_uiState.subscriptionCount.value > 0) {
            _uiState.update {
                it.copy(
                    liveCountdownString = countdown,
                    fastingCountdown = fastingCountdownStr,
                    isFastingCountdownToIftar = isToIftar,
                    tahajjudCountdown = tahajjudCountdownStr,
                    isTahajjudActiveNow = isTahajjudActive
                )
            }
        }
    }

    private fun computeFastingCountdown(dailyTimes: DailyPrayerTimes, nowMillis: Long): Pair<String, Boolean> {
        val fajr = dailyTimes.fajr
        val maghrib = dailyTimes.maghrib
        val isDaytimeFasting = nowMillis in fajr until maghrib

        val target = if (isDaytimeFasting) {
            maghrib
        } else if (nowMillis < fajr) {
            fajr
        } else {
            dailyTimes.nextDayFajr
        }

        val remaining = (target - nowMillis).coerceAtLeast(0L)
        return Pair(formatDuration(remaining), isDaytimeFasting)
    }

    private fun buildDailyPrayerRows(
        dailyTimes: DailyPrayerTimes,
        nowMillis: Long,
        window: PrayerWindow,
        isFriday: Boolean = false,
        jamaatTimesMillis: Map<PrayerType, Long> = emptyMap()
    ): List<PrayerTimeRowUiState> {
        val timeZone = TimeZone.currentSystemDefault()

        return PrayerType.entries.map { prayerType ->
            val timeMillis = dailyTimes.timeFor(prayerType)
            val isPast = nowMillis > timeMillis
            val isActive = when (window) {
                is PrayerWindow.Active -> window.currentPrayer == prayerType || window.nextPrayer == prayerType
                is PrayerWindow.SunriseInterval -> prayerType == PrayerType.SUNRISE || prayerType == PrayerType.DHUHR
            }
            val jamaatFormatted = jamaatTimesMillis[prayerType]
                ?.let { formatTimeOfDay(it, timeZone) }
                .orEmpty()

            PrayerTimeRowUiState(
                prayerType = prayerType,
                name = prayerType.displayName(isFriday),
                timeMillis = timeMillis,
                timeFormatted = formatTimeOfDay(timeMillis, timeZone),
                isPast = isPast,
                isActive = isActive,
                audioMode = currentPreferences.getAlertMode(prayerType),
                jamaatTimeFormatted = jamaatFormatted
            )
        }
    }

    companion object {
        fun formatDuration(durationMillis: Long): String {
            val totalSeconds = (durationMillis / 1000L).coerceAtLeast(0L)
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        }

        fun formatTimeOfDay(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
            val instant = Instant.fromEpochMilliseconds(epochMillis)
            val localTime = instant.toLocalDateTime(timeZone).time
            val hour = localTime.hour
            val minute = localTime.minute

            val period = if (hour >= 12) "PM" else "AM"
            val displayHour = when {
                hour == 0 -> 12
                hour > 12 -> hour - 12
                else -> hour
            }
            return String.format(Locale.US, "%02d:%02d %s", displayHour, minute, period)
        }

        fun formatHijriDate(date: LocalDate, hijriOffsetDays: Int = 0): String {
            return try {
                val javaDate = java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
                    .plusDays(hijriOffsetDays.coerceIn(-2, 2).toLong())
                val hijrahDate = HijrahDate.from(javaDate)
                val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'AH'", Locale.US)
                formatter.format(hijrahDate)
            } catch (e: Exception) {
                ""
            }
        }
    }
}
