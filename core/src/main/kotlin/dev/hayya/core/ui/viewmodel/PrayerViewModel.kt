package dev.hayya.core.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.batoulapps.adhan.Coordinates
import dev.hayya.core.data.preferences.UserPreferencesRepository
import dev.hayya.core.domain.calculation.DailyPrayerTimes
import dev.hayya.core.domain.calculation.PrayerCalculationEngine
import dev.hayya.core.domain.model.AlertMode
import dev.hayya.core.domain.model.AutoSilenceTimeout
import dev.hayya.core.domain.model.CalculationMethodOption
import dev.hayya.core.domain.model.HighLatitudeRuleOption
import dev.hayya.core.domain.model.MadhabOption
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.model.PrayerWindow
import dev.hayya.core.domain.model.UserPreferences
import dev.hayya.core.system.alarm.PrayerAlarmScheduler
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
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
    val audioMode: AlertMode
)

/**
 * Complete, unified immutable UI state for the ultra-minimalist prayer dashboard.
 */
data class PrayerDashboardUiState(
    val locationName: String = "Makkah, Saudi Arabia",
    val nextPrayerType: PrayerType = PrayerType.DHUHR,
    val nextPrayerName: String = "DHUHR",
    val nextPrayerTimestampMillis: Long = 0L,
    val nextPrayerTimeFormatted: String = "12:00 PM",
    val liveCountdownString: String = "00:00:00",
    val progressRatio: Float = 0.0f,
    val dailyPrayers: List<PrayerTimeRowUiState> = emptyList(),
    val hijriDateString: String = "",
    val userPreferences: UserPreferences = UserPreferences(),
    val isSettingsOpen: Boolean = false
)

/**
 * ViewModel orchestrating live prayer time calculations, 1-second countdown tickers,
 * and user preference updates with automated alarm rescheduling.
 */
class PrayerViewModel(
    private val calculationEngine: PrayerCalculationEngine,
    private val preferencesRepository: UserPreferencesRepository,
    private val alarmScheduler: PrayerAlarmScheduler? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PrayerDashboardUiState())
    val uiState: StateFlow<PrayerDashboardUiState> = _uiState.asStateFlow()

    private var currentPreferences = UserPreferences()

    init {
        // Observe preferences and recalculate on changes
        viewModelScope.launch {
            preferencesRepository.userPreferencesFlow.collectLatest { prefs ->
                currentPreferences = prefs
                refreshCalculation(Clock.System.now().toEpochMilliseconds())
            }
        }

        // 1-second live countdown ticker
        viewModelScope.launch {
            while (isActive) {
                val nowMillis = Clock.System.now().toEpochMilliseconds()
                updateCountdownAndProgress(nowMillis)
                delay(1000L)
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
     * Updates auto-silence timeout preference.
     */
    fun updateAutoSilence(timeout: AutoSilenceTimeout) {
        viewModelScope.launch {
            preferencesRepository.updateAutoSilenceTimeout(timeout)
        }
    }

    /**
     * Toggles or updates settings screen visibility.
     */
    fun setSettingsOpen(isOpen: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = isOpen) }
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
     * Updates High Latitude twilight adjustment rule.
     */
    fun updateHighLatitudeRule(rule: HighLatitudeRuleOption) {
        viewModelScope.launch {
            preferencesRepository.updateHighLatitudeRule(rule)
            alarmScheduler?.scheduleRollingAlarms(preferences = currentPreferences.copy(highLatitudeRule = rule))
        }
    }

    /**
     * Schedules an instant or countdown-based test prayer alarm.
     */
    fun scheduleTestAlarm(delaySeconds: Int) {
        alarmScheduler?.scheduleTestAlarm(delaySeconds, _uiState.value.nextPrayerType)
    }

    /**
     * Updates calculation method convention and syncs reference city coordinates.
     */
    fun updateCalculationMethod(method: CalculationMethodOption) {
        viewModelScope.launch {
            preferencesRepository.updateCalculationMethod(method)
            // Synchronize reference location to the method's canonical city
            preferencesRepository.updateCoordinates(
                latitude = method.defaultLatitude,
                longitude = method.defaultLongitude,
                locationName = method.defaultCity
            )
            alarmScheduler?.scheduleRollingAlarms(
                preferences = currentPreferences.copy(
                    calculationMethod = method,
                    location = currentPreferences.location.copy(
                        latitude = method.defaultLatitude,
                        longitude = method.defaultLongitude,
                        resolvedLocationName = method.defaultCity
                    )
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

    private fun refreshCalculation(nowMillis: Long) {
        val timeZone = TimeZone.currentSystemDefault()
        val localDate = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date

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

        val window = calculationEngine.getPrayerWindow(
            timestampEpochMillis = nowMillis,
            coordinates = coordinates,
            parameters = params,
            timeZone = timeZone,
            manualOffsets = currentPreferences.manualMinuteOffsets,
            ishaExpiresAtMidnight = currentPreferences.ishaExpiresAtMidnight
        )

        val nextPrayer = window.nextPrayer
        val nextPrayerTimeMillis = dailyTimes.timeFor(nextPrayer)

        val dailyItems = buildDailyPrayerRows(dailyTimes, nowMillis, window)
        val hijriDate = formatHijriDate(localDate)

        val remainingMillis = (nextPrayerTimeMillis - nowMillis).coerceAtLeast(0L)
        val countdown = formatDuration(remainingMillis)

        _uiState.update {
            it.copy(
                locationName = currentPreferences.location.resolvedLocationName.ifEmpty { "Makkah" },
                nextPrayerType = nextPrayer,
                nextPrayerName = nextPrayer.name,
                nextPrayerTimestampMillis = nextPrayerTimeMillis,
                nextPrayerTimeFormatted = formatTimeOfDay(nextPrayerTimeMillis, timeZone),
                liveCountdownString = countdown,
                progressRatio = window.elapsedProgress,
                dailyPrayers = dailyItems,
                hijriDateString = hijriDate,
                userPreferences = currentPreferences
            )
        }
    }

    private fun updateCountdownAndProgress(nowMillis: Long) {
        val currentState = _uiState.value
        val targetMillis = currentState.nextPrayerTimestampMillis

        if (targetMillis <= 0L || nowMillis >= targetMillis) {
            // Target arrived, trigger full recalculation
            refreshCalculation(nowMillis)
            return
        }

        val remainingMillis = targetMillis - nowMillis
        val countdown = formatDuration(remainingMillis)

        _uiState.update {
            it.copy(liveCountdownString = countdown)
        }
    }

    private fun buildDailyPrayerRows(
        dailyTimes: DailyPrayerTimes,
        nowMillis: Long,
        window: PrayerWindow
    ): List<PrayerTimeRowUiState> {
        val timeZone = TimeZone.currentSystemDefault()

        return PrayerType.entries.map { prayerType ->
            val timeMillis = dailyTimes.timeFor(prayerType)
            val isPast = nowMillis > timeMillis
            val isActive = when (window) {
                is PrayerWindow.Active -> window.currentPrayer == prayerType || window.nextPrayer == prayerType
                is PrayerWindow.SunriseInterval -> prayerType == PrayerType.SUNRISE || prayerType == PrayerType.DHUHR
            }

            PrayerTimeRowUiState(
                prayerType = prayerType,
                name = prayerType.name,
                timeMillis = timeMillis,
                timeFormatted = formatTimeOfDay(timeMillis, timeZone),
                isPast = isPast,
                isActive = isActive,
                audioMode = currentPreferences.getAlertMode(prayerType)
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

        fun formatHijriDate(date: LocalDate): String {
            return try {
                val hijrahDate = HijrahDate.from(
                    java.time.LocalDate.of(date.year, date.monthNumber, date.dayOfMonth)
                )
                val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy 'AH'", Locale.US)
                formatter.format(hijrahDate)
            } catch (e: Exception) {
                ""
            }
        }
    }
}
