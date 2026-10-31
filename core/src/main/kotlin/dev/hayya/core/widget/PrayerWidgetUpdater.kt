package dev.hayya.core.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.batoulapps.adhan.Coordinates
import dev.hayya.core.data.preferences.UserPreferencesRepository
import dev.hayya.core.domain.calculation.DailyPrayerTimes
import dev.hayya.core.domain.calculation.PrayerCalculationEngine
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.model.PrayerWindow
import dev.hayya.core.domain.model.UserPreferences
import dev.hayya.core.system.receivers.PreferenceDataStoreProvider
import dev.hayya.core.ui.viewmodel.PrayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Snapshot representation of current prayer timetable data for AppWidgets.
 */
data class WidgetPrayerSnapshot(
    val nextPrayerName: String = "FAJR",
    val nextPrayerTimeFormatted: String = "--:--",
    val countdownFormatted: String = "--:--",
    val activePrayerName: String = "FAJR",
    val locationName: String = "Makkah",
    val dailyTimes: List<Pair<PrayerType, String>> = emptyList(),
    val pastPrayers: Set<PrayerType> = emptySet(),
    val activePrayers: Set<PrayerType> = emptySet()
)

/**
 * Helper class updating all active Glance AppWidget instances with the latest prayer calculations.
 */
object PrayerWidgetUpdater {

    private const val TAG = "PrayerWidgetUpdater"
    const val ACTION_PRAYER_DATA_CHANGED = "dev.hayya.widget.ACTION_PRAYER_DATA_CHANGED"

    /**
     * Reads current calculation and preferences asynchronously and updates all AppWidgets.
     */
    suspend fun updateAllWidgets(context: Context) {
        withContext(Dispatchers.Default) {
            try {
                // Update Glance widgets
                CompactPrayerWidget().updateAll(context)
                FullTimetableWidget().updateAll(context)

                // Dispatch broadcast to any registered custom receivers
                val broadcastIntent = Intent(ACTION_PRAYER_DATA_CHANGED).apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(broadcastIntent)
                Log.d(TAG, "Successfully refreshed all prayer AppWidgets")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update prayer widgets", e)
            }
        }
    }

    /**
     * Computes the current prayer snapshot directly from offline calculation engine.
     */
    suspend fun loadCurrentSnapshot(context: Context): WidgetPrayerSnapshot {
        return withContext(Dispatchers.Default) {
            try {
                val dataStore = PreferenceDataStoreProvider.getDataStore(context)
                val repository = UserPreferencesRepository(dataStore)
                val userPrefs = repository.userPreferencesFlow.first()
                val engine = PrayerCalculationEngine()

                val nowMillis = Clock.System.now().toEpochMilliseconds()
                val timeZone = TimeZone.currentSystemDefault()
                val localDate = Instant.fromEpochMilliseconds(nowMillis).toLocalDateTime(timeZone).date

                val coordinates = Coordinates(userPrefs.location.latitude, userPrefs.location.longitude)
                val params = engine.createParameters(
                    method = userPrefs.calculationMethod.toAdhanMethod(),
                    madhab = userPrefs.madhab.toAdhanMadhab(),
                    highLatitudeRule = userPrefs.highLatitudeRule.toAdhanRule()
                )

                val dailyTimes = engine.computeDailyTimes(
                    date = localDate,
                    coordinates = coordinates,
                    parameters = params,
                    manualOffsets = userPrefs.manualMinuteOffsets
                )

                val window = engine.getPrayerWindow(
                    timestampEpochMillis = nowMillis,
                    coordinates = coordinates,
                    parameters = params,
                    timeZone = timeZone,
                    manualOffsets = userPrefs.manualMinuteOffsets,
                    ishaExpiresAtMidnight = userPrefs.ishaExpiresAtMidnight
                )

                val nextPrayer = window.nextPrayer
                val nextPrayerMillis = dailyTimes.timeFor(nextPrayer)
                val remainingMillis = (nextPrayerMillis - nowMillis).coerceAtLeast(0L)

                val remainingMinutes = remainingMillis / 60_000L
                val countdownFormatted = if (remainingMinutes >= 60) {
                    val hours = remainingMinutes / 60
                    val mins = remainingMinutes % 60
                    "in ${hours}h ${mins}m"
                } else {
                    "in ${remainingMinutes}m"
                }

                // 5 daily prayers list (Fajr, Dhuhr, Asr, Maghrib, Isha)
                val timetable = listOf(
                    PrayerType.FAJR,
                    PrayerType.DHUHR,
                    PrayerType.ASR,
                    PrayerType.MAGHRIB,
                    PrayerType.ISHA
                ).map { prayer ->
                    prayer to PrayerViewModel.formatTimeOfDay(dailyTimes.timeFor(prayer), timeZone)
                }

                val pastPrayers = PrayerType.entries.filter { nowMillis > dailyTimes.timeFor(it) }.toSet()
                val activePrayers = when (window) {
                    is PrayerWindow.Active -> setOf(window.currentPrayer, window.nextPrayer)
                    is PrayerWindow.SunriseInterval -> setOf(PrayerType.SUNRISE, PrayerType.DHUHR)
                }

                WidgetPrayerSnapshot(
                    nextPrayerName = nextPrayer.name,
                    nextPrayerTimeFormatted = PrayerViewModel.formatTimeOfDay(nextPrayerMillis, timeZone),
                    countdownFormatted = countdownFormatted,
                    activePrayerName = when (window) {
                        is PrayerWindow.Active -> window.currentPrayer.name
                        is PrayerWindow.SunriseInterval -> "SUNRISE"
                    },
                    locationName = userPrefs.location.resolvedLocationName.ifEmpty { "Makkah" },
                    dailyTimes = timetable,
                    pastPrayers = pastPrayers,
                    activePrayers = activePrayers
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error generating widget snapshot", e)
                WidgetPrayerSnapshot()
            }
        }
    }
}
