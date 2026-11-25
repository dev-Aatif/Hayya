package dev.hayya.core

import android.app.Application
import dev.hayya.core.data.hadith.HadithDatabase
import dev.hayya.core.data.hadith.HadithPreferencesRepository
import dev.hayya.core.data.hadith.HadithRepository
import dev.hayya.core.data.prayerlog.PrayerLogRepository
import dev.hayya.core.data.preferences.UserPreferencesRepository
import dev.hayya.core.data.ramadan.FastTrackerRepository
import dev.hayya.core.domain.calculation.PrayerCalculationEngine
import dev.hayya.core.system.alarm.PrayerAlarmScheduler
import dev.hayya.core.system.receivers.PreferenceDataStoreProvider
import dev.hayya.core.system.update.AppUpdateEngine

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

    lateinit var updateEngine: AppUpdateEngine
        private set

    lateinit var hadithPreferencesRepository: HadithPreferencesRepository
        private set

    lateinit var hadithRepository: HadithRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        val dataStore = PreferenceDataStoreProvider.getDataStore(this)
        calculationEngine = PrayerCalculationEngine()
        preferencesRepository = UserPreferencesRepository(dataStore)
        prayerLogRepository = PrayerLogRepository(dataStore)
        fastTrackerRepository = FastTrackerRepository(dataStore)
        hadithPreferencesRepository = HadithPreferencesRepository(dataStore)
        alarmScheduler = PrayerAlarmScheduler(
            context = this,
            calculationEngine = calculationEngine,
            preferencesRepository = preferencesRepository,
            fastTrackerRepository = fastTrackerRepository,
            hadithPreferencesRepository = hadithPreferencesRepository
        )
        updateEngine = AppUpdateEngine(context = this)
        hadithRepository = HadithRepository(
            database = HadithDatabase(this),
            preferencesRepository = hadithPreferencesRepository
        )
    }

    companion object {
        lateinit var instance: HayyaApplication
            private set
    }
}
