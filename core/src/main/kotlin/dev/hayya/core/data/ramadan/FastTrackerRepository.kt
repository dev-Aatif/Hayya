package dev.hayya.core.data.ramadan

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.hayya.core.domain.ramadan.DailyFastRecord
import dev.hayya.core.domain.ramadan.FastStatus
import dev.hayya.core.domain.ramadan.IslamicCalendarHelper
import dev.hayya.core.domain.ramadan.RamadanMonthProgress
import dev.hayya.core.domain.ramadan.RamadanSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import java.io.IOException

/**
 * Jetpack DataStore repository managing fasting tracking records, Qadha counts,
 * and Ramadan/Eid alert preferences.
 */
class FastTrackerRepository(
    private val dataStore: DataStore<Preferences>
) {

    object PreferenceKeys {
        val QADHA_MADE_UP_COUNT = intPreferencesKey("pref_qadha_made_up_count")
        val SUHOOR_ALARM_ENABLED = booleanPreferencesKey("pref_suhoor_alarm_enabled")
        val SUHOOR_ALARM_MINUTES_BEFORE = intPreferencesKey("pref_suhoor_alarm_minutes_before")
        val IFTAR_NOTIFICATION_ENABLED = booleanPreferencesKey("pref_iftar_notification_enabled")
        val EID_NOTIFICATION_ENABLED = booleanPreferencesKey("pref_eid_notification_enabled")

        fun fastStatusKey(date: LocalDate): Preferences.Key<String> =
            stringPreferencesKey("pref_fast_status_${date.year}_${date.monthNumber}_${date.dayOfMonth}")

        fun fastNoteKey(date: LocalDate): Preferences.Key<String> =
            stringPreferencesKey("pref_fast_note_${date.year}_${date.monthNumber}_${date.dayOfMonth}")
    }

    /**
     * Observes the fast status record for a specific Gregorian [date].
     */
    fun observeDailyFast(date: LocalDate): Flow<DailyFastRecord> {
        val key = PreferenceKeys.fastStatusKey(date)
        val noteKey = PreferenceKeys.fastNoteKey(date)

        return dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { preferences ->
                val statusName = preferences[key]
                val status = statusName?.let {
                    runCatching { FastStatus.valueOf(it) }.getOrNull()
                } ?: FastStatus.NOT_RECORDED

                val note = preferences[noteKey] ?: ""
                val islamicDate = IslamicCalendarHelper.resolveIslamicDate(date)

                DailyFastRecord(
                    date = date,
                    hijriDay = islamicDate.hijriDay,
                    status = status,
                    note = note
                )
            }
    }

    /**
     * Observes 30-day Ramadan month progress for the specified [hijriYear].
     */
    fun observeRamadanProgress(hijriYear: Int): Flow<RamadanMonthProgress> {
        val startDate = IslamicCalendarHelper.getRamadanStartDate(hijriYear)
        val dates = (0 until 30).map { startDate.plus(it, DateTimeUnit.DAY) }
        val dateKeys = dates.associateWith { PreferenceKeys.fastStatusKey(it) }

        return dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { preferences ->
                val dailyMap = mutableMapOf<Int, FastStatus>()
                dates.forEachIndexed { index, date ->
                    val dayNum = index + 1
                    val key = dateKeys[date] ?: PreferenceKeys.fastStatusKey(date)
                    val statusName = preferences[key]
                    val status = statusName?.let {
                        runCatching { FastStatus.valueOf(it) }.getOrNull()
                    } ?: FastStatus.NOT_RECORDED
                    dailyMap[dayNum] = status
                }

                val qadhaMadeUp = preferences[PreferenceKeys.QADHA_MADE_UP_COUNT] ?: 0

                RamadanMonthProgress(
                    hijriYear = hijriYear,
                    dailyRecords = dailyMap,
                    qadhaMadeUpCount = qadhaMadeUp,
                    totalDays = 30
                )
            }
    }

    /**
     * Observes current Ramadan alert and Suhoor reminder settings.
     */
    fun observeRamadanSettings(): Flow<RamadanSettings> {
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) emit(emptyPreferences()) else throw exception
            }
            .map { preferences ->
                RamadanSettings(
                    suhoorAlarmEnabled = preferences[PreferenceKeys.SUHOOR_ALARM_ENABLED] ?: false,
                    suhoorAlarmMinutesBefore = preferences[PreferenceKeys.SUHOOR_ALARM_MINUTES_BEFORE] ?: 40,
                    iftarNotificationEnabled = preferences[PreferenceKeys.IFTAR_NOTIFICATION_ENABLED] ?: true,
                    eidNotificationEnabled = preferences[PreferenceKeys.EID_NOTIFICATION_ENABLED] ?: true
                )
            }
    }

    /**
     * Sets the fasting status for a given [date].
     */
    suspend fun setFastStatus(date: LocalDate, status: FastStatus, note: String = "") {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.fastStatusKey(date)] = status.name
            if (note.isNotEmpty()) {
                preferences[PreferenceKeys.fastNoteKey(date)] = note
            }
        }
    }

    /**
     * Cycles through the fasting status for a given [date] (Pending -> Fasted -> Missed -> Exempt -> Pending).
     */
    suspend fun cycleFastStatus(date: LocalDate): FastStatus {
        var nextStatus = FastStatus.FASTED
        dataStore.edit { preferences ->
            val key = PreferenceKeys.fastStatusKey(date)
            val current = preferences[key]?.let {
                runCatching { FastStatus.valueOf(it) }.getOrNull()
            } ?: FastStatus.NOT_RECORDED

            nextStatus = when (current) {
                FastStatus.NOT_RECORDED -> FastStatus.FASTED
                FastStatus.FASTED -> FastStatus.MISSED
                FastStatus.MISSED -> FastStatus.EXEMPT
                FastStatus.EXEMPT -> FastStatus.NOT_RECORDED
            }

            preferences[key] = nextStatus.name
        }
        return nextStatus
    }

    /**
     * Updates the number of completed Qadha (make-up) fasts.
     */
    suspend fun updateQadhaMadeUpCount(count: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.QADHA_MADE_UP_COUNT] = count.coerceAtLeast(0)
        }
    }

    /**
     * Updates Suhoor pre-reminder alarm preference.
     */
    suspend fun updateSuhoorAlarmEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SUHOOR_ALARM_ENABLED] = enabled
        }
    }

    /**
     * Updates minutes before Fajr for the Suhoor wake-up alarm (e.g. 30m, 40m, 50m, 60m).
     */
    suspend fun updateSuhoorAlarmMinutesBefore(minutes: Int) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.SUHOOR_ALARM_MINUTES_BEFORE] = minutes
        }
    }

    /**
     * Updates Iftar notification preference at Maghrib.
     */
    suspend fun updateIftarNotificationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.IFTAR_NOTIFICATION_ENABLED] = enabled
        }
    }

    /**
     * Updates Eid morning greeting notification preference.
     */
    suspend fun updateEidNotificationEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferenceKeys.EID_NOTIFICATION_ENABLED] = enabled
        }
    }
}
