package dev.hayya.core

import android.app.Application
import dev.hayya.core.data.prayerlog.PrayerLogRepository
import dev.hayya.core.data.preferences.UserPreferencesRepository
import dev.hayya.core.data.ramadan.FastTrackerRepository
import dev.hayya.core.domain.calculation.PrayerCalculationEngine
import dev.hayya.core.system.alarm.PrayerAlarmScheduler
import dev.hayya.core.system.receivers.PreferenceDataStoreProvider

/**
 * Android Application entry point managing application-scoped singletons.
 */
class HayyaApplication : Application() {

    lateinit var calculationEngine: PrayerCalculationEngine
        private set

    lateinit var preferencesRepository: UserPreferencesRepository
        private set

    lateinit var alarmScheduler: PrayerAlarmScheduler
        private set

    lateinit var prayerLogRepository: PrayerLogRepository
        private set

    lateinit var fastTrackerRepository: FastTrackerRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        val dataStore = PreferenceDataStoreProvider.getDataStore(this)
        calculationEngine = PrayerCalculationEngine()
        preferencesRepository = UserPreferencesRepository(dataStore)
        prayerLogRepository = PrayerLogRepository(dataStore)
        fastTrackerRepository = FastTrackerRepository(dataStore)
        alarmScheduler = PrayerAlarmScheduler(this, calculationEngine, preferencesRepository)
    }

    companion object {
        lateinit var instance: HayyaApplication
            private set
    }
}
