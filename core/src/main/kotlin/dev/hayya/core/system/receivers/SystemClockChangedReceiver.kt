package dev.hayya.core.system.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import dev.hayya.core.data.hadith.HadithPreferencesRepository
import dev.hayya.core.data.preferences.UserPreferencesRepository
import dev.hayya.core.data.ramadan.FastTrackerRepository
import dev.hayya.core.domain.calculation.PrayerCalculationEngine
import dev.hayya.core.system.alarm.PrayerAlarmScheduler
import dev.hayya.core.system.notifications.PinnedPrayerStatusService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.io.File

/**
 * Singleton holder providing access to the application DataStore for background receivers.
 */
object PreferenceDataStoreProvider {
    @Volatile
    private var instance: DataStore<Preferences>? = null

    fun getDataStore(context: Context): DataStore<Preferences> {
        return instance ?: synchronized(this) {
            instance ?: PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                produceFile = {
                    File(context.applicationContext.filesDir, "datastore/hayya_user_prefs.preferences_pb")
                }
            ).also { instance = it }
        }
    }
}

/**
 * System BroadcastReceiver triggered on system state changes:
 * - [Intent.ACTION_BOOT_COMPLETED]
 * - [Intent.ACTION_TIME_SET] (Manual clock adjustment by user or network)
 * - [Intent.ACTION_TIMEZONE_CHANGED] (Travel or Daylight Saving shift)
 * - [Intent.ACTION_MY_PACKAGE_REPLACED] (App update / re-installation)
 *
 * Automatically purges existing scheduled alarms and recalculates a fresh 7-day rolling queue.
 */
open class SystemClockChangedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action !in ALLOWED_ACTIONS) {
            Log.w(TAG, "Ignoring unexpected broadcast action on exported receiver: $action")
            return
        }
        Log.i(TAG, "Received system broadcast action: $action")

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            WAKELOCK_TAG
        ).apply {
            setReferenceCounted(false)
            acquire(WAKELOCK_TIMEOUT_MS)
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.Default + SupervisorJob()).launch {
            try {
                withTimeout(WAKELOCK_TIMEOUT_MS) {
                    val dataStore = PreferenceDataStoreProvider.getDataStore(context)
                    val repository = UserPreferencesRepository(dataStore)
                    val fastTrackerRepository = FastTrackerRepository(dataStore)
                    val hadithPreferencesRepository = HadithPreferencesRepository(dataStore)
                    val engine = PrayerCalculationEngine()
                    val scheduler = PrayerAlarmScheduler(
                        context = context,
                        calculationEngine = engine,
                        preferencesRepository = repository,
                        fastTrackerRepository = fastTrackerRepository,
                        hadithPreferencesRepository = hadithPreferencesRepository
                    )

                    Log.d(TAG, "Cancelling outdated alarms and scheduling fresh rolling batch")
                    scheduler.cancelAllScheduledAlarms()
                    scheduler.scheduleRollingAlarms()
                    dev.hayya.core.widget.PrayerWidgetUpdater.updateAllWidgets(context)
                    val userPrefs = repository.userPreferencesFlow.first()
                    if (userPrefs.pinnedNotificationEnabled) {
                        PinnedPrayerStatusService.startOrPostFallback(context, userPrefs)
                    }

                    // Verify and reschedule or restore auto-mute state if interrupted across reboots
                    val savedRingerState = repository.savedRingerStateFlow.first()
                    if (savedRingerState.isAutoMuted) {
                        val now = System.currentTimeMillis()
                        if (now >= savedRingerState.endEpochMillis) {
                            dev.hayya.core.system.audio.AutoMuteManager.restoreRingerMode(context, repository)
                        } else {
                            scheduler.scheduleAutoUnmute(savedRingerState.endEpochMillis)
                        }
                    }

                    Log.i(TAG, "Successfully refreshed rolling alarms following $action")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to reschedule alarms after $action", e)
            } finally {
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
                pendingResult?.finish()
            }
        }
    }

    companion object {
        private const val TAG = "SystemClockChangedRecv"
        private const val WAKELOCK_TAG = "hayya:SystemClockChangedWakeLock"
        const val WAKELOCK_TIMEOUT_MS = 8_000L

        val ALLOWED_ACTIONS = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED
        )
    }
}

/**
 * Typealias and explicit subclass providing compatibility for the BootAndClockReceiver name.
 */
class BootAndClockReceiver : SystemClockChangedReceiver()
