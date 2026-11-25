package dev.hayya.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.hayya.core.domain.model.AlertMode
import dev.hayya.core.domain.model.AutoMuteDuration
import dev.hayya.core.domain.model.AutoMuteTargetMode
import dev.hayya.core.domain.model.AutoSilenceTimeout
import dev.hayya.core.domain.model.CalculationMethodOption
import dev.hayya.core.domain.model.FridayAutoMuteDuration
import dev.hayya.core.domain.model.GeoLocation
import dev.hayya.core.domain.model.HighLatitudeRuleOption
import dev.hayya.core.domain.model.MadhabOption
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.model.SavedRingerState
import dev.hayya.core.domain.model.ThemeModeOption
import dev.hayya.core.domain.model.UserPreferences
import dev.hayya.core.domain.model.VolumeEscalationOption
import dev.hayya.core.domain.tasbih.TasbihTargetMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * Storage keys for Android Jetpack DataStore preferences.
 */
object PreferenceKeys {
    val LATITUDE = doublePreferencesKey("pref_latitude")
    val LONGITUDE = doublePreferencesKey("pref_longitude")
    val ALTITUDE = doublePreferencesKey("pref_altitude")
    val LOCATION_NAME = stringPreferencesKey("pref_location_name")

    val CALCULATION_METHOD = stringPreferencesKey("pref_calculation_method")
    val MADHAB = stringPreferencesKey("pref_madhab")
    val HIGH_LATITUDE_RULE = stringPreferencesKey("pref_high_latitude_rule")
    val ISHA_EXPIRES_AT_MIDNIGHT = booleanPreferencesKey("pref_isha_expires_at_midnight")

    val AUTO_SILENCE_TIMEOUT = stringPreferencesKey("pref_auto_silence_timeout")
    val AUTO_MUTE_DURATION = stringPreferencesKey("pref_auto_mute_duration")
    val AUTO_MUTE_TARGET_MODE = stringPreferencesKey("pref_auto_mute_target_mode")
    val FRIDAY_AUTO_MUTE_DURATION = stringPreferencesKey("pref_friday_auto_mute_duration")
    val SURAH_KAHF_READ_DATE = stringPreferencesKey("pref_surah_kahf_read_date")
    val SAVED_RINGER_MODE = intPreferencesKey("pref_saved_ringer_mode")
    val SAVED_RING_VOLUME = intPreferencesKey("pref_saved_ring_volume")
    val SAVED_NOTIFICATION_VOLUME = intPreferencesKey("pref_saved_notification_volume")
    val IS_AUTO_MUTED = booleanPreferencesKey("pref_is_auto_muted")
    val AUTO_MUTE_END_EPOCH_MILLIS = longPreferencesKey("pref_auto_mute_end_epoch_millis")

    val PRE_REMINDER_ENABLED = booleanPreferencesKey("pref_pre_reminder_enabled")
    val LAST_CALL_ENABLED = booleanPreferencesKey("pref_last_call_enabled")
    val PINNED_NOTIFICATION_ENABLED = booleanPreferencesKey("pref_pinned_notification_enabled")
    val SHOW_POST_ADHAN_DUA = booleanPreferencesKey("pref_show_post_adhan_dua")
    val THEME_MODE = stringPreferencesKey("pref_theme_mode")
    val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("pref_dynamic_color_enabled")
    val VOLUME_ESCALATION = stringPreferencesKey("pref_volume_escalation")
    val TAHAJJUD_ALARM_ENABLED = booleanPreferencesKey("pref_tahajjud_alarm_enabled")
    val TAHAJJUD_ALARM_MINUTES_OFFSET = intPreferencesKey("pref_tahajjud_alarm_minutes_offset")
    val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("pref_has_completed_onboarding")
    val TASBIH_COUNT = intPreferencesKey("pref_tasbih_count")
    val TASBIH_MODE = stringPreferencesKey("pref_tasbih_mode")
    val SALAWAT_COUNT = intPreferencesKey("pref_salawat_count")
    val LAST_UPDATE_CHECK_EPOCH_MILLIS = longPreferencesKey("pref_last_update_check_epoch_millis")
    val LAST_NOTIFIED_UPDATE_VERSION = stringPreferencesKey("pref_last_notified_update_version")

    fun offsetKeyFor(prayer: PrayerType): Preferences.Key<Int> =
        intPreferencesKey("pref_offset_${prayer.name.lowercase()}")

    fun alertModeKeyFor(prayer: PrayerType): Preferences.Key<String> =
        stringPreferencesKey("pref_alert_mode_${prayer.name.lowercase()}")
}

/**
 * Jetpack DataStore implementation managing persistence of prayer calculation settings,
 * geographic coordinates, per-prayer manual minute offsets, and notification alert configurations.
 */
class UserPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {

    /**
     * Continuous reactive Flow emitting current [UserPreferences].
     * Recovers from [IOException] by falling back to default preferences.
     */
    val userPreferencesFlow: Flow<UserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            mapUserPreferences(preferences)
        }

    /**
     * Updates coordinates and resolved human-readable location name.
     */
    suspend fun updateCoordinates(
        latitude: Double,
        longitude: Double,
        altitude: Double = 0.0,
        locationName: String? = null
    ) {
        require(latitude in -90.0..90.0) { "Latitude must be between -90 and 90, was $latitude" }
        require(longitude in -180.0..180.0) { "Longitude must be between -180 and 180, was $longitude" }

        dataStore.edit { prefs ->
            prefs[PreferenceKeys.LATITUDE] = latitude
            prefs[PreferenceKeys.LONGITUDE] = longitude
            prefs[PreferenceKeys.ALTITUDE] = altitude
            if (locationName != null) {
                prefs[PreferenceKeys.LOCATION_NAME] = locationName
            }
        }
    }

    /**
     * Updates calculation method convention.
     */
    suspend fun updateCalculationMethod(method: CalculationMethodOption) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.CALCULATION_METHOD] = method.name
        }
    }

    /**
     * Updates juristic Madhab for Asr calculation.
     */
    suspend fun updateMadhab(madhab: MadhabOption) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.MADHAB] = madhab.name
        }
    }

    /**
     * Updates high latitude twilight adjustment rule.
     */
    suspend fun updateHighLatitudeRule(rule: HighLatitudeRuleOption) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.HIGH_LATITUDE_RULE] = rule.name
        }
    }

    /**
     * Updates Isha expiration rule (Islamic Midnight vs Next Day Fajr).
     */
    suspend fun updateIshaExpiresAtMidnight(expiresAtMidnight: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.ISHA_EXPIRES_AT_MIDNIGHT] = expiresAtMidnight
        }
    }

    /**
     * Updates Pre-Reminder (Tier 1: T - 15m) toggle.
     */
    suspend fun updatePreReminderEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.PRE_REMINDER_ENABLED] = enabled
        }
    }

    /**
     * Updates Last Call (Tier 3: Window - 20m) toggle.
     */
    suspend fun updateLastCallEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.LAST_CALL_ENABLED] = enabled
        }
    }

    /**
     * Updates Pinned Foreground Status Notification preference.
     */
    suspend fun updatePinnedNotificationEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.PINNED_NOTIFICATION_ENABLED] = enabled
        }
    }

    /**
     * Updates Post-Adhan Dua on wake screen visibility preference.
     */
    suspend fun updateShowPostAdhanDua(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.SHOW_POST_ADHAN_DUA] = enabled
        }
    }

    /**
     * Updates visual theme mode (Pure OLED Dark, Warm Light, Follow System).
     */
    suspend fun updateThemeMode(mode: ThemeModeOption) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.THEME_MODE] = mode.name
        }
    }

    /**
     * Updates Material You dynamic color adaptation toggle.
     */
    suspend fun updateDynamicColorEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.DYNAMIC_COLOR_ENABLED] = enabled
        }
    }

    /**
     * Updates manual minute offset for a specific prayer (-30 to +30 minutes).
     */
    suspend fun updatePrayerOffset(prayer: PrayerType, offsetMinutes: Int) {
        require(offsetMinutes in -30..30) {
            "Offset minutes must be between -30 and +30, was $offsetMinutes"
        }
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.offsetKeyFor(prayer)] = offsetMinutes
        }
    }

    /**
     * Batch updates manual minute offsets for multiple prayers.
     */
    suspend fun updateAllOffsets(offsets: Map<PrayerType, Int>) {
        offsets.forEach { (prayer, offsetMinutes) ->
            require(offsetMinutes in -30..30) {
                "Offset for $prayer must be between -30 and +30, was $offsetMinutes"
            }
        }
        dataStore.edit { prefs ->
            offsets.forEach { (prayer, offsetMinutes) ->
                prefs[PreferenceKeys.offsetKeyFor(prayer)] = offsetMinutes
            }
        }
    }

    /**
     * Updates alert mode behavior for an individual prayer.
     */
    suspend fun updateAlertMode(prayer: PrayerType, mode: AlertMode) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.alertModeKeyFor(prayer)] = mode.name
        }
    }

    /**
     * Batch updates alert modes for multiple prayers.
     */
    suspend fun updateAllAlertModes(modes: Map<PrayerType, AlertMode>) {
        dataStore.edit { prefs ->
            modes.forEach { (prayer, mode) ->
                prefs[PreferenceKeys.alertModeKeyFor(prayer)] = mode.name
            }
        }
    }

    /**
     * Updates auto-silence timeout preference for full alarms.
     */
    suspend fun updateAutoSilenceTimeout(timeout: AutoSilenceTimeout) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.AUTO_SILENCE_TIMEOUT] = timeout.name
        }
    }

    /**
     * Updates Congregational Prayer (Mosque) auto-mute duration.
     */
    suspend fun updateAutoMuteDuration(duration: AutoMuteDuration) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.AUTO_MUTE_DURATION] = duration.name
        }
    }

    /**
     * Updates Congregational Prayer (Mosque) auto-mute target mode.
     */
    suspend fun updateAutoMuteTargetMode(mode: AutoMuteTargetMode) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.AUTO_MUTE_TARGET_MODE] = mode.name
        }
    }

    /**
     * Updates Friday (Jumu'ah) Khutbah extended auto-mute duration.
     */
    suspend fun updateFridayAutoMuteDuration(duration: FridayAutoMuteDuration) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.FRIDAY_AUTO_MUTE_DURATION] = duration.name
        }
    }

    /**
     * Sets or clears Surah Al-Kahf completion state for the given date (ISO format YYYY-MM-DD).
     */
    suspend fun setSurahKahfReadForDate(dateIso: String, isRead: Boolean) {
        dataStore.edit { prefs ->
            if (isRead) {
                prefs[PreferenceKeys.SURAH_KAHF_READ_DATE] = dateIso
            } else {
                prefs.remove(PreferenceKeys.SURAH_KAHF_READ_DATE)
            }
        }
    }

    /**
     * Saves system ringer state and volume levels prior to muting.
     */
    suspend fun saveRingerState(
        ringerMode: Int,
        ringVolume: Int,
        notificationVolume: Int,
        endEpochMillis: Long
    ) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.SAVED_RINGER_MODE] = ringerMode
            prefs[PreferenceKeys.SAVED_RING_VOLUME] = ringVolume
            prefs[PreferenceKeys.SAVED_NOTIFICATION_VOLUME] = notificationVolume
            prefs[PreferenceKeys.IS_AUTO_MUTED] = true
            prefs[PreferenceKeys.AUTO_MUTE_END_EPOCH_MILLIS] = endEpochMillis
        }
    }

    /**
     * Clears active auto-mute state upon restoration.
     */
    suspend fun clearAutoMuteState() {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.IS_AUTO_MUTED] = false
            prefs[PreferenceKeys.AUTO_MUTE_END_EPOCH_MILLIS] = 0L
        }
    }

    /**
     * Reactive stream observing captured ringer state for auto-mute restoration.
     */
    val savedRingerStateFlow: Flow<SavedRingerState> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            SavedRingerState(
                ringerMode = preferences[PreferenceKeys.SAVED_RINGER_MODE] ?: 2, // AudioManager.RINGER_MODE_NORMAL
                ringVolume = preferences[PreferenceKeys.SAVED_RING_VOLUME] ?: 7,
                notificationVolume = preferences[PreferenceKeys.SAVED_NOTIFICATION_VOLUME] ?: 7,
                isAutoMuted = preferences[PreferenceKeys.IS_AUTO_MUTED] ?: false,
                endEpochMillis = preferences[PreferenceKeys.AUTO_MUTE_END_EPOCH_MILLIS] ?: 0L
            )
        }

    /**
     * Updates gradual volume escalation option for full prayer alarms.
     */
    suspend fun updateVolumeEscalation(option: VolumeEscalationOption) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.VOLUME_ESCALATION] = option.name
        }
    }

    /**
     * Updates gentle Tahajjud / Qiyam al-Layl pre-alarm toggle.
     */
    suspend fun updateTahajjudAlarmEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.TAHAJJUD_ALARM_ENABLED] = enabled
        }
    }

    /**
     * Updates Tahajjud pre-alarm minute offset.
     */
    suspend fun updateTahajjudAlarmMinutesOffset(offsetMinutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.TAHAJJUD_ALARM_MINUTES_OFFSET] = offsetMinutes
        }
    }

    /**
     * Updates whether the user has completed the quick welcome setup.
     */
    suspend fun updateHasCompletedOnboarding(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    /**
     * Persists the Tasbih counter value.
     */
    suspend fun updateTasbihCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.TASBIH_COUNT] = count.coerceAtLeast(0)
        }
    }

    /**
     * Persists the selected Tasbih target mode.
     */
    suspend fun updateTasbihMode(mode: TasbihTargetMode) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.TASBIH_MODE] = mode.name
        }
    }

    /**
     * Persists the Friday Salawat counter value.
     */
    suspend fun updateSalawatCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SALAWAT_COUNT] = count.coerceAtLeast(0)
        }
    }

    /**
     * Persists the timestamp of the last app update check.
     */
    suspend fun updateLastUpdateCheckEpochMillis(epochMillis: Long) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.LAST_UPDATE_CHECK_EPOCH_MILLIS] = epochMillis.coerceAtLeast(0L)
        }
    }

    /**
     * Persists the latest release version for which an update notification was posted.
     */
    suspend fun updateLastNotifiedUpdateVersion(version: String) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.LAST_NOTIFIED_UPDATE_VERSION] = version
        }
    }

    /**
     * Resets all preferences to initial defaults.
     */
    suspend fun resetToDefaults() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    private fun mapUserPreferences(preferences: Preferences): UserPreferences {
        val lat = preferences[PreferenceKeys.LATITUDE] ?: 27.5590
        val lng = preferences[PreferenceKeys.LONGITUDE] ?: 68.2120
        val alt = preferences[PreferenceKeys.ALTITUDE] ?: 0.0
        val locationName = preferences[PreferenceKeys.LOCATION_NAME] ?: "Larkana, Pakistan"

        val calculationMethod = preferences[PreferenceKeys.CALCULATION_METHOD]?.let { name ->
            runCatching { CalculationMethodOption.valueOf(name) }.getOrNull()
        }?.takeUnless {
            it == CalculationMethodOption.TEHRAN ||
                it == CalculationMethodOption.JAFARI ||
                it == CalculationMethodOption.OTHER
        } ?: CalculationMethodOption.KARACHI

        val madhab = preferences[PreferenceKeys.MADHAB]?.let { name ->
            runCatching { MadhabOption.valueOf(name) }.getOrNull()
        } ?: MadhabOption.HANAFI

        val highLatitudeRule = preferences[PreferenceKeys.HIGH_LATITUDE_RULE]?.let { name ->
            runCatching { HighLatitudeRuleOption.valueOf(name) }.getOrNull()
        } ?: HighLatitudeRuleOption.MIDDLE_OF_THE_NIGHT

        val ishaExpiresAtMidnight = preferences[PreferenceKeys.ISHA_EXPIRES_AT_MIDNIGHT] ?: false

        val offsets = PrayerType.entries.associateWith { prayer ->
            val stored = preferences[PreferenceKeys.offsetKeyFor(prayer)] ?: 0
            stored.coerceIn(-30, 30)
        }

        val alertModes = PrayerType.entries.associateWith { prayer ->
            val stored = preferences[PreferenceKeys.alertModeKeyFor(prayer)]
            if (stored != null) {
                runCatching { AlertMode.valueOf(stored) }.getOrElse {
                    if (prayer == PrayerType.SUNRISE) AlertMode.GENTLE_BANNER else AlertMode.FULL_ALARM
                }
            } else {
                if (prayer == PrayerType.SUNRISE) AlertMode.GENTLE_BANNER else AlertMode.FULL_ALARM
            }
        }

        val autoSilenceTimeout = preferences[PreferenceKeys.AUTO_SILENCE_TIMEOUT]?.let { name ->
            runCatching { AutoSilenceTimeout.valueOf(name) }.getOrNull()
        } ?: AutoSilenceTimeout.SECONDS_60

        val autoMuteDuration = preferences[PreferenceKeys.AUTO_MUTE_DURATION]?.let { name ->
            runCatching { AutoMuteDuration.valueOf(name) }.getOrNull()
        } ?: AutoMuteDuration.OFF

        val autoMuteTargetMode = preferences[PreferenceKeys.AUTO_MUTE_TARGET_MODE]?.let { name ->
            runCatching { AutoMuteTargetMode.valueOf(name) }.getOrNull()
        } ?: AutoMuteTargetMode.SILENT

        val fridayAutoMuteDuration = preferences[PreferenceKeys.FRIDAY_AUTO_MUTE_DURATION]?.let { name ->
            runCatching { FridayAutoMuteDuration.valueOf(name) }.getOrNull()
        } ?: FridayAutoMuteDuration.MATCH_STANDARD

        val surahKahfReadDate = preferences[PreferenceKeys.SURAH_KAHF_READ_DATE]

        val preReminderEnabled = preferences[PreferenceKeys.PRE_REMINDER_ENABLED] ?: true
        val lastCallEnabled = preferences[PreferenceKeys.LAST_CALL_ENABLED] ?: true
        val pinnedNotificationEnabled = preferences[PreferenceKeys.PINNED_NOTIFICATION_ENABLED] ?: false
        val showPostAdhanDua = preferences[PreferenceKeys.SHOW_POST_ADHAN_DUA] ?: true

        val themeMode = preferences[PreferenceKeys.THEME_MODE]?.let { name ->
            runCatching { ThemeModeOption.valueOf(name) }.getOrNull()
        } ?: ThemeModeOption.DARK

        val dynamicColorEnabled = preferences[PreferenceKeys.DYNAMIC_COLOR_ENABLED] ?: false

        val volumeEscalation = preferences[PreferenceKeys.VOLUME_ESCALATION]?.let { name ->
            runCatching { VolumeEscalationOption.valueOf(name) }.getOrNull()
        } ?: VolumeEscalationOption.SECONDS_20

        val tahajjudAlarmEnabled = preferences[PreferenceKeys.TAHAJJUD_ALARM_ENABLED] ?: false
        val tahajjudAlarmMinutesOffset = preferences[PreferenceKeys.TAHAJJUD_ALARM_MINUTES_OFFSET] ?: 0
        val hasCompletedOnboarding = preferences[PreferenceKeys.HAS_COMPLETED_ONBOARDING] ?: false
        val tasbihCount = (preferences[PreferenceKeys.TASBIH_COUNT] ?: 0).coerceAtLeast(0)
        val tasbihMode = preferences[PreferenceKeys.TASBIH_MODE]?.let { name ->
            runCatching { TasbihTargetMode.valueOf(name) }.getOrNull()
        } ?: TasbihTargetMode.FATIMA_100
        val salawatCount = (preferences[PreferenceKeys.SALAWAT_COUNT] ?: 0).coerceAtLeast(0)
        val lastUpdateCheckEpochMillis = preferences[PreferenceKeys.LAST_UPDATE_CHECK_EPOCH_MILLIS] ?: 0L
        val lastNotifiedUpdateVersion = preferences[PreferenceKeys.LAST_NOTIFIED_UPDATE_VERSION]

        return UserPreferences(
            location = GeoLocation(lat, lng, alt, locationName),
            calculationMethod = calculationMethod,
            madhab = madhab,
            highLatitudeRule = highLatitudeRule,
            ishaExpiresAtMidnight = ishaExpiresAtMidnight,
            manualMinuteOffsets = offsets,
            alertModes = alertModes,
            autoSilenceTimeout = autoSilenceTimeout,
            autoMuteDuration = autoMuteDuration,
            autoMuteTargetMode = autoMuteTargetMode,
            fridayAutoMuteDuration = fridayAutoMuteDuration,
            surahKahfReadDate = surahKahfReadDate,
            preReminderEnabled = preReminderEnabled,
            lastCallEnabled = lastCallEnabled,
            pinnedNotificationEnabled = pinnedNotificationEnabled,
            showPostAdhanDua = showPostAdhanDua,
            themeMode = themeMode,
            dynamicColorEnabled = dynamicColorEnabled,
            volumeEscalation = volumeEscalation,
            tahajjudAlarmEnabled = tahajjudAlarmEnabled,
            tahajjudAlarmMinutesOffset = tahajjudAlarmMinutesOffset,
            hasCompletedOnboarding = hasCompletedOnboarding,
            tasbihCount = tasbihCount,
            tasbihMode = tasbihMode,
            salawatCount = salawatCount,
            lastUpdateCheckEpochMillis = lastUpdateCheckEpochMillis,
            lastNotifiedUpdateVersion = lastNotifiedUpdateVersion
        )
    }
}
