package dev.hayya.core.data.prayerlog

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.prayerlog.DailyPrayerLog
import dev.hayya.core.domain.prayerlog.OBLIGATORY_PRAYERS
import dev.hayya.core.domain.prayerlog.RollingPrayerHistory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import java.io.IOException

/**
 * Jetpack DataStore-backed repository managing daily prayer completion logs and
 * automatic date-partitioned archiving across midnight boundaries.
 */
class PrayerLogRepository(
    private val dataStore: DataStore<Preferences>
) {

    /**
     * Continuous reactive Flow emitting the [DailyPrayerLog] for the given [date].
     */
    fun observeDailyLog(date: LocalDate): Flow<DailyPrayerLog> {
        val key = logKeyFor(date)
        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val serialized = preferences[key] ?: ""
                parseLog(date, serialized)
            }
    }

    /**
     * Observes a rolling window of [daysCount] days leading up to and including [today].
     * Emits chronologically from oldest day to today.
     */
    fun observeRollingHistory(today: LocalDate, daysCount: Int = 7): Flow<RollingPrayerHistory> {
        val dates = (0 until daysCount).map { offset ->
            today.minus(offset, DateTimeUnit.DAY)
        }.reversed()

        val keys = dates.associateWith { logKeyFor(it) }

        return dataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val logs = dates.map { date ->
                    val key = keys[date] ?: logKeyFor(date)
                    val serialized = preferences[key] ?: ""
                    parseLog(date, serialized)
                }
                RollingPrayerHistory(dailyLogs = logs)
            }
    }

    /**
     * Toggles the completion status of [prayer] on [date] atomically.
     */
    suspend fun togglePrayer(date: LocalDate, prayer: PrayerType): DailyPrayerLog {
        require(prayer in OBLIGATORY_PRAYERS) {
            "Only obligatory prayers (Fajr, Dhuhr, Asr, Maghrib, Isha) can be logged, was $prayer"
        }
        val key = logKeyFor(date)
        var updatedLog = DailyPrayerLog(date)

        dataStore.edit { preferences ->
            val serialized = preferences[key] ?: ""
            val currentLog = parseLog(date, serialized)
            updatedLog = currentLog.withToggled(prayer)
            preferences[key] = serializeLog(updatedLog)
        }

        return updatedLog
    }

    /**
     * Explicitly sets the completion status of [prayer] on [date].
     */
    suspend fun setPrayerCompleted(date: LocalDate, prayer: PrayerType, isCompleted: Boolean) {
        require(prayer in OBLIGATORY_PRAYERS) {
            "Only obligatory prayers (Fajr, Dhuhr, Asr, Maghrib, Isha) can be logged, was $prayer"
        }
        val key = logKeyFor(date)

        dataStore.edit { preferences ->
            val serialized = preferences[key] ?: ""
            val currentLog = parseLog(date, serialized)
            val newSet = if (isCompleted) {
                currentLog.completedPrayers + prayer
            } else {
                currentLog.completedPrayers - prayer
            }
            preferences[key] = serializeLog(currentLog.copy(completedPrayers = newSet))
        }
    }

    private fun logKeyFor(date: LocalDate): Preferences.Key<String> {
        return stringPreferencesKey("pref_prayer_log_${date}")
    }

    private fun parseLog(date: LocalDate, serialized: String): DailyPrayerLog {
        if (serialized.isBlank()) return DailyPrayerLog(date)
        val prayers = serialized.split(",")
            .mapNotNull { name ->
                runCatching { PrayerType.valueOf(name.trim()) }.getOrNull()
            }
            .filter { it in OBLIGATORY_PRAYERS }
            .toSet()
        return DailyPrayerLog(date = date, completedPrayers = prayers)
    }

    private fun serializeLog(log: DailyPrayerLog): String {
        return log.completedPrayers.joinToString(",") { it.name }
    }
}
