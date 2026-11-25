package dev.hayya.core.data.hadith

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.hayya.core.domain.hadith.HadithLanguage
import dev.hayya.core.domain.hadith.HadithUserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * DataStore-backed preferences dedicated to the Hadith reading and morning notification experience.
 */
class HadithPreferencesRepository(
    private val dataStore: DataStore<Preferences>
) {

    val preferencesFlow: Flow<HadithUserPreferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            HadithUserPreferences(
                preferredLanguage = HadithLanguage.fromCode(prefs[KEY_LANGUAGE]),
                showArabic = prefs[KEY_SHOW_ARABIC] ?: true,
                fontScale = (prefs[KEY_FONT_SCALE] ?: 1.0f).coerceIn(0.85f, 1.35f),
                morningNotificationEnabled = prefs[KEY_MORNING_ENABLED] ?: true,
                morningNotificationOnSunrise = prefs[KEY_MORNING_ON_SUNRISE] ?: true
            )
        }

    suspend fun updateLanguage(language: HadithLanguage) {
        dataStore.edit { prefs ->
            prefs[KEY_LANGUAGE] = language.code
        }
    }

    suspend fun updateShowArabic(showArabic: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_SHOW_ARABIC] = showArabic
        }
    }

    suspend fun updateFontScale(scale: Float) {
        dataStore.edit { prefs ->
            prefs[KEY_FONT_SCALE] = scale.coerceIn(0.85f, 1.35f)
        }
    }

    suspend fun updateMorningNotificationEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_MORNING_ENABLED] = enabled
        }
    }

    private companion object {
        val KEY_LANGUAGE = stringPreferencesKey("hadith_preferred_language")
        val KEY_SHOW_ARABIC = booleanPreferencesKey("hadith_show_arabic")
        val KEY_FONT_SCALE = floatPreferencesKey("hadith_font_scale")
        val KEY_MORNING_ENABLED = booleanPreferencesKey("hadith_morning_notification_enabled")
        val KEY_MORNING_ON_SUNRISE = booleanPreferencesKey("hadith_morning_notification_on_sunrise")
    }
}
