package dev.hayya.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.hayya.core.domain.model.AlertMode
import dev.hayya.core.domain.model.AutoSilenceTimeout
import dev.hayya.core.domain.model.CalculationMethodOption
import dev.hayya.core.domain.model.GeoLocation
import dev.hayya.core.domain.model.HighLatitudeRuleOption
import dev.hayya.core.domain.model.MadhabOption
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.model.UserPreferences
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
     * Resets all preferences to initial defaults.
     */
    suspend fun resetToDefaults() {
        dataStore.edit { prefs ->
            prefs.clear()
        }
    }

    private fun mapUserPreferences(preferences: Preferences): UserPreferences {
        val lat = preferences[PreferenceKeys.LATITUDE] ?: 21.4225
        val lng = preferences[PreferenceKeys.LONGITUDE] ?: 39.8262
        val alt = preferences[PreferenceKeys.ALTITUDE] ?: 0.0
        val locationName = preferences[PreferenceKeys.LOCATION_NAME] ?: "Makkah, Saudi Arabia"

        val calculationMethod = preferences[PreferenceKeys.CALCULATION_METHOD]?.let { name ->
            runCatching { CalculationMethodOption.valueOf(name) }.getOrNull()
        } ?: CalculationMethodOption.MUSLIM_WORLD_LEAGUE

        val madhab = preferences[PreferenceKeys.MADHAB]?.let { name ->
            runCatching { MadhabOption.valueOf(name) }.getOrNull()
        } ?: MadhabOption.STANDARD

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

        return UserPreferences(
            location = GeoLocation(lat, lng, alt, locationName),
            calculationMethod = calculationMethod,
            madhab = madhab,
            highLatitudeRule = highLatitudeRule,
            ishaExpiresAtMidnight = ishaExpiresAtMidnight,
            manualMinuteOffsets = offsets,
            alertModes = alertModes,
            autoSilenceTimeout = autoSilenceTimeout
        )
    }
}
