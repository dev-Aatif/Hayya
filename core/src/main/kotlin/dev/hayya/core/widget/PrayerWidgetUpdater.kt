package dev.hayya.core.widget

import android.content.Context
import android.util.Log
import androidx.glance.appwidget.updateAll
import com.batoulapps.adhan.Coordinates
import dev.hayya.core.data.prayerlog.PrayerLogRepository
import dev.hayya.core.data.preferences.UserPreferencesRepository
import dev.hayya.core.domain.calculation.NightFastingCalculator
import dev.hayya.core.domain.calculation.PrayerCalculationEngine
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.model.PrayerWindow
import dev.hayya.core.domain.model.ThemeModeOption
import dev.hayya.core.domain.ramadan.IslamicCalendarHelper
import dev.hayya.core.system.receivers.PreferenceDataStoreProvider
import dev.hayya.core.ui.viewmodel.PrayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Locale

/**
 * Snapshot representation of current prayer timetable, fasting, and theme data for AppWidgets.
 */
data class WidgetPrayerSnapshot(
    val nextPrayerName: String = "FAJR",
    val nextPrayerTimeFormatted: String = "--:--",
    val countdownFormatted: String = "--:--",
    val activePrayerName: String = "FAJR",
    val locationName: String = "Larkana",
    val dailyTimes: List<Pair<PrayerType, String>> = emptyList(),
    val pastPrayers: Set<PrayerType> = emptySet(),
    val activePrayers: Set<PrayerType> = emptySet(),
    val completedPrayers: Set<PrayerType> = emptySet(),
    val isFriday: Boolean = false,
    val nextPrayerType: PrayerType = PrayerType.FAJR,
    val windowProgress: Float = 0.0f,
    val hijriDateShort: String = "",
    val sunriseFormatted: String = "--:--",
    val suhoorFormatted: String = "--:--",
    val iftarFormatted: String = "--:--",
    val tahajjudStartFormatted: String = "--:--",
    val islamicMidnightFormatted: String = "--:--",
    val isTahajjudActive: Boolean = false,
    val themeMode: ThemeModeOption = ThemeModeOption.DARK,
    val compactDailyTimes: List<Pair<PrayerType, String>> = emptyList()
) {
    val completedCount: Int get() = completedPrayers.size
}

/**
 * Helper class updating all active Glance AppWidget instances with the latest prayer calculations.
 */
object PrayerWidgetUpdater {

    private const val TAG = "PrayerWidgetUpdater"
    const val ACTION_PRAYER_DATA_CHANGED = "dev.hayya.widget.ACTION_PRAYER_DATA_CHANGED"
    const val EXTRA_START_DESTINATION = "dev.hayya.widget.EXTRA_START_DESTINATION"

    const val DEST_TRACKER = "tracker"
    const val DEST_TAHAJJUD = "tahajjud"
    const val DEST_RAMADAN = "ramadan"

    /**
     * Reads current calculation and preferences asynchronously and updates all AppWidgets.
     *
     * IMPORTANT: This function must NOT broadcast [ACTION_PRAYER_DATA_CHANGED]. Receivers handle
     * that action by calling this function, so re-broadcasting here creates an unbounded loop.
     */
    suspend fun updateAllWidgets(context: Context) {
        withContext(Dispatchers.Default) {
            try {
                CompactPrayerWidget().updateAll(context)
                FullTimetableWidget().updateAll(context)
                DailyPrayersWidget().updateAll(context)
                NextPrayerBannerWidget().updateAll(context)
                FastingAndNightWidget().updateAll(context)
                Log.d(TAG, "Successfully refreshed all 5 prayer AppWidgets")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update prayer widgets", e)
            }
        }
    }

    /**
     * Computes the current prayer snapshot directly from the offline calculation engine.
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
                val nextPrayerMillis = dailyTimes.upcomingTimeFor(nextPrayer, nowMillis)
                val remainingMillis = (nextPrayerMillis - nowMillis).coerceAtLeast(0L)

                val remainingMinutes = remainingMillis / 60_000L
                val countdownFormatted = if (remainingMinutes >= 60) {
                    val hours = remainingMinutes / 60
                    val mins = remainingMinutes % 60
                    "in ${hours}h ${mins}m"
                } else {
                    "in ${remainingMinutes}m"
                }

                val obligatoryOrder = listOf(
                    PrayerType.FAJR,
                    PrayerType.DHUHR,
                    PrayerType.ASR,
                    PrayerType.MAGHRIB,
                    PrayerType.ISHA
                )
                val timetable = obligatoryOrder.map { prayer ->
                    prayer to formatCleanTime(dailyTimes.timeFor(prayer), timeZone)
                }
                val compactTimetable = obligatoryOrder.map { prayer ->
                    prayer to formatCompactTime(dailyTimes.timeFor(prayer), timeZone)
                }

                val pastPrayers = PrayerType.entries.filter { nowMillis > dailyTimes.timeFor(it) }.toSet()
                val activePrayers = when (window) {
                    is PrayerWindow.Active -> setOf(window.currentPrayer, window.nextPrayer)
                    is PrayerWindow.SunriseInterval -> setOf(PrayerType.SUNRISE, PrayerType.DHUHR)
                }

                val prayerLogRepo = PrayerLogRepository(dataStore)
                val dailyLog = prayerLogRepo.observeDailyLog(localDate).first()
                val isFriday = localDate.dayOfWeek == DayOfWeek.FRIDAY
                val islamicDate = IslamicCalendarHelper.resolveIslamicDate(localDate)
                val tahajjud = NightFastingCalculator.resolveTahajjudSchedule(dailyTimes, nowMillis, timeZone)

                val shortCity = userPrefs.location.resolvedLocationName
                    .ifEmpty { "Larkana" }
                    .substringBefore(",")
                    .trim()

                WidgetPrayerSnapshot(
                    nextPrayerName = WidgetTheme.shortPrayerLabel(nextPrayer, isFriday),
                    nextPrayerTimeFormatted = formatCleanTime(nextPrayerMillis, timeZone),
                    countdownFormatted = countdownFormatted,
                    activePrayerName = when (window) {
                        is PrayerWindow.Active -> WidgetTheme.shortPrayerLabel(window.currentPrayer, isFriday)
                        is PrayerWindow.SunriseInterval -> "Sunrise"
                    },
                    locationName = shortCity,
                    dailyTimes = timetable,
                    pastPrayers = pastPrayers,
                    activePrayers = activePrayers,
                    completedPrayers = dailyLog.completedPrayers,
                    isFriday = isFriday,
                    nextPrayerType = nextPrayer,
                    windowProgress = window.elapsedProgress,
                    hijriDateShort = "${islamicDate.hijriDay} ${islamicDate.hijriMonthName}",
                    sunriseFormatted = formatCleanTime(dailyTimes.sunrise, timeZone),
                    suhoorFormatted = formatCleanTime(dailyTimes.fajr, timeZone),
                    iftarFormatted = formatCleanTime(dailyTimes.maghrib, timeZone),
                    tahajjudStartFormatted = tahajjud.formattedLastThirdStart,
                    islamicMidnightFormatted = tahajjud.formattedIslamicMidnight,
                    isTahajjudActive = tahajjud.isTahajjudActiveNow,
                    themeMode = userPrefs.themeMode,
                    compactDailyTimes = compactTimetable
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error generating widget snapshot", e)
                WidgetPrayerSnapshot()
            }
        }
    }

    internal fun formatCleanTime(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        val localTime = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone).time
        val hour = localTime.hour
        val minute = localTime.minute
        val period = if (hour >= 12) "PM" else "AM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.US, "%d:%02d %s", displayHour, minute, period)
    }

    internal fun formatCompactTime(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
        val localTime = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone).time
        val hour = localTime.hour
        val minute = localTime.minute
        val period = if (hour >= 12) "p" else "a"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return String.format(Locale.US, "%d:%02d%s", displayHour, minute, period)
    }
}
