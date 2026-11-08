package dev.hayya.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dev.hayya.core.domain.model.AlertMode
import dev.hayya.core.domain.model.AutoMuteDuration
import dev.hayya.core.domain.model.AutoMuteTargetMode
import dev.hayya.core.domain.model.AutoSilenceTimeout
import dev.hayya.core.domain.model.CalculationMethodOption
import dev.hayya.core.domain.model.FridayAutoMuteDuration
import dev.hayya.core.domain.model.HighLatitudeRuleOption
import dev.hayya.core.domain.model.MadhabOption
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.model.ThemeModeOption
import dev.hayya.core.domain.model.VolumeEscalationOption
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryTest {

    private val dataStoreScope = CoroutineScope(UnconfinedTestDispatcher() + Job())

    private lateinit var tempFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: UserPreferencesRepository

    @Before
    fun setUp() {
        tempFile = File.createTempFile("test_preferences", ".preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = dataStoreScope,
            produceFile = { tempFile }
        )
        repository = UserPreferencesRepository(dataStore)
    }

    @After
    fun tearDown() {
        dataStoreScope.cancel()
        tempFile.delete()
    }

    @Test
    fun defaultPreferences_matchesExpectedDefaults() = runTest {
        val prefs = repository.userPreferencesFlow.first()

        // Default Makkah coordinates
        assertThat(prefs.location.latitude).isEqualTo(21.4225)
        assertThat(prefs.location.longitude).isEqualTo(39.8262)
        assertThat(prefs.location.altitude).isEqualTo(0.0)
        assertThat(prefs.location.resolvedLocationName).isEqualTo("Makkah, Saudi Arabia")

        // Default calculation settings
        assertThat(prefs.calculationMethod).isEqualTo(CalculationMethodOption.MUSLIM_WORLD_LEAGUE)
        assertThat(prefs.madhab).isEqualTo(MadhabOption.STANDARD)
        assertThat(prefs.highLatitudeRule).isEqualTo(HighLatitudeRuleOption.MIDDLE_OF_THE_NIGHT)
        assertThat(prefs.ishaExpiresAtMidnight).isFalse()

        // Default offsets (all 0)
        for (prayer in PrayerType.entries) {
            assertThat(prefs.getOffset(prayer)).isEqualTo(0)
        }

        // Default alert modes (Sunrise gentle, others full alarm)
        assertThat(prefs.getAlertMode(PrayerType.SUNRISE)).isEqualTo(AlertMode.GENTLE_BANNER)
        assertThat(prefs.getAlertMode(PrayerType.FAJR)).isEqualTo(AlertMode.FULL_ALARM)
        assertThat(prefs.getAlertMode(PrayerType.DHUHR)).isEqualTo(AlertMode.FULL_ALARM)

        // Default auto-silence
        assertThat(prefs.autoSilenceTimeout).isEqualTo(AutoSilenceTimeout.SECONDS_60)

        // Default post-adhan dua enabled
        assertThat(prefs.showPostAdhanDua).isTrue()
    }

    @Test
    fun updateCoordinates_updatesValuesAndEmitsNewState() = runTest {
        repository.updateCoordinates(
            latitude = 24.8607,
            longitude = 67.0011,
            altitude = 10.0,
            locationName = "Karachi, Pakistan"
        )

        val updated = repository.userPreferencesFlow.first()
        assertThat(updated.location.latitude).isEqualTo(24.8607)
        assertThat(updated.location.longitude).isEqualTo(67.0011)
        assertThat(updated.location.altitude).isEqualTo(10.0)
        assertThat(updated.location.resolvedLocationName).isEqualTo("Karachi, Pakistan")
    }

    @Test
    fun updateCoordinates_validatesBounds() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.updateCoordinates(latitude = 95.0, longitude = 0.0) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.updateCoordinates(latitude = -91.0, longitude = 0.0) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.updateCoordinates(latitude = 0.0, longitude = 185.0) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.updateCoordinates(latitude = 0.0, longitude = -181.0) }
        }
    }

    @Test
    fun updateCalculationMethodAndMadhab_persistsCorrectly() = runTest {
        repository.updateCalculationMethod(CalculationMethodOption.KARACHI)
        repository.updateMadhab(MadhabOption.HANAFI)
        repository.updateHighLatitudeRule(HighLatitudeRuleOption.SEVENTH_OF_THE_NIGHT)
        repository.updateIshaExpiresAtMidnight(true)

        val updated = repository.userPreferencesFlow.first()
        assertThat(updated.calculationMethod).isEqualTo(CalculationMethodOption.KARACHI)
        assertThat(updated.madhab).isEqualTo(MadhabOption.HANAFI)
        assertThat(updated.highLatitudeRule).isEqualTo(HighLatitudeRuleOption.SEVENTH_OF_THE_NIGHT)
        assertThat(updated.ishaExpiresAtMidnight).isTrue()
    }

    @Test
    fun updatePrayerOffset_storesOffsetsAndEnforcesBounds() = runTest {
        repository.updatePrayerOffset(PrayerType.FAJR, 15)
        repository.updatePrayerOffset(PrayerType.ASR, -10)

        val updated = repository.userPreferencesFlow.first()
        assertThat(updated.getOffset(PrayerType.FAJR)).isEqualTo(15)
        assertThat(updated.getOffset(PrayerType.ASR)).isEqualTo(-10)
        assertThat(updated.getOffset(PrayerType.DHUHR)).isEqualTo(0)

        // Exceeding bounds throws
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.updatePrayerOffset(PrayerType.FAJR, 31) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { repository.updatePrayerOffset(PrayerType.FAJR, -31) }
        }
    }

    @Test
    fun updateAlertModeAndAutoSilence_persistsSettings() = runTest {
        repository.updateAlertMode(PrayerType.FAJR, AlertMode.MUTED)
        repository.updateAlertMode(PrayerType.MAGHRIB, AlertMode.GENTLE_BANNER)
        repository.updateAutoSilenceTimeout(AutoSilenceTimeout.FULL_AUDIO)

        val updated = repository.userPreferencesFlow.first()
        assertThat(updated.getAlertMode(PrayerType.FAJR)).isEqualTo(AlertMode.MUTED)
        assertThat(updated.getAlertMode(PrayerType.MAGHRIB)).isEqualTo(AlertMode.GENTLE_BANNER)
        assertThat(updated.autoSilenceTimeout).isEqualTo(AutoSilenceTimeout.FULL_AUDIO)
    }

    @Test
    fun resetToDefaults_clearsCustomSettings() = runTest {
        repository.updateCoordinates(40.7128, -74.0060, locationName = "New York")
        repository.updateCalculationMethod(CalculationMethodOption.NORTH_AMERICA)
        repository.updatePrayerOffset(PrayerType.DHUHR, 10)

        var prefs = repository.userPreferencesFlow.first()
        assertThat(prefs.location.latitude).isEqualTo(40.7128)
        assertThat(prefs.calculationMethod).isEqualTo(CalculationMethodOption.NORTH_AMERICA)

        repository.resetToDefaults()

        prefs = repository.userPreferencesFlow.first()
        assertThat(prefs.location.latitude).isEqualTo(21.4225)
        assertThat(prefs.calculationMethod).isEqualTo(CalculationMethodOption.MUSLIM_WORLD_LEAGUE)
        assertThat(prefs.getOffset(PrayerType.DHUHR)).isEqualTo(0)
    }

    @Test
    fun updatePinnedNotificationEnabled_persistsAndEmitsUpdatedPreference() = runTest {
        val initial = repository.userPreferencesFlow.first()
        assertThat(initial.pinnedNotificationEnabled).isFalse()

        repository.updatePinnedNotificationEnabled(true)

        val updated = repository.userPreferencesFlow.first()
        assertThat(updated.pinnedNotificationEnabled).isTrue()

        repository.updatePinnedNotificationEnabled(false)

        val reverted = repository.userPreferencesFlow.first()
        assertThat(reverted.pinnedNotificationEnabled).isFalse()
    }

    @Test
    fun updateShowPostAdhanDua_persistsAndEmitsUpdatedPreference() = runTest {
        val initial = repository.userPreferencesFlow.first()
        assertThat(initial.showPostAdhanDua).isTrue()

        repository.updateShowPostAdhanDua(false)

        val updated = repository.userPreferencesFlow.first()
        assertThat(updated.showPostAdhanDua).isFalse()

        repository.updateShowPostAdhanDua(true)

        val restored = repository.userPreferencesFlow.first()
        assertThat(restored.showPostAdhanDua).isTrue()
    }

    @Test
    fun updateThemeMode_persistsAndEmitsUpdatedPreference() = runTest {
        val initial = repository.userPreferencesFlow.first()
        assertThat(initial.themeMode).isEqualTo(ThemeModeOption.DARK)

        repository.updateThemeMode(ThemeModeOption.LIGHT)
        val lightState = repository.userPreferencesFlow.first()
        assertThat(lightState.themeMode).isEqualTo(ThemeModeOption.LIGHT)

        repository.updateThemeMode(ThemeModeOption.SYSTEM)
        val systemState = repository.userPreferencesFlow.first()
        assertThat(systemState.themeMode).isEqualTo(ThemeModeOption.SYSTEM)
    }

    @Test
    fun updateDynamicColorEnabled_persistsAndEmitsUpdatedPreference() = runTest {
        val initial = repository.userPreferencesFlow.first()
        assertThat(initial.dynamicColorEnabled).isFalse()

        repository.updateDynamicColorEnabled(true)
        val updated = repository.userPreferencesFlow.first()
        assertThat(updated.dynamicColorEnabled).isTrue()

        repository.updateDynamicColorEnabled(false)
        val reverted = repository.userPreferencesFlow.first()
        assertThat(reverted.dynamicColorEnabled).isFalse()
    }

    @Test
    fun autoMuteDuration_persistsAndEmitsUpdatedPreference() = runTest {
        val initial = repository.userPreferencesFlow.first()
        assertThat(initial.autoMuteDuration).isEqualTo(AutoMuteDuration.OFF)

        repository.updateAutoMuteDuration(AutoMuteDuration.MINUTES_20)
        val updated = repository.userPreferencesFlow.first()
        assertThat(updated.autoMuteDuration).isEqualTo(AutoMuteDuration.MINUTES_20)

        repository.updateAutoMuteDuration(AutoMuteDuration.OFF)
        val reverted = repository.userPreferencesFlow.first()
        assertThat(reverted.autoMuteDuration).isEqualTo(AutoMuteDuration.OFF)
    }

    @Test
    fun autoMuteTargetMode_persistsAndEmitsUpdatedPreference() = runTest {
        val initial = repository.userPreferencesFlow.first()
        assertThat(initial.autoMuteTargetMode).isEqualTo(AutoMuteTargetMode.SILENT)

        repository.updateAutoMuteTargetMode(AutoMuteTargetMode.VIBRATE)
        val updated = repository.userPreferencesFlow.first()
        assertThat(updated.autoMuteTargetMode).isEqualTo(AutoMuteTargetMode.VIBRATE)
    }

    @Test
    fun fridayAutoMuteDuration_persistsAndEmitsUpdatedPreference() = runTest {
        val initial = repository.userPreferencesFlow.first()
        assertThat(initial.fridayAutoMuteDuration).isEqualTo(FridayAutoMuteDuration.MATCH_STANDARD)

        repository.updateFridayAutoMuteDuration(FridayAutoMuteDuration.MINUTES_45)
        val updated45 = repository.userPreferencesFlow.first()
        assertThat(updated45.fridayAutoMuteDuration).isEqualTo(FridayAutoMuteDuration.MINUTES_45)

        repository.updateFridayAutoMuteDuration(FridayAutoMuteDuration.MINUTES_60)
        val updated60 = repository.userPreferencesFlow.first()
        assertThat(updated60.fridayAutoMuteDuration).isEqualTo(FridayAutoMuteDuration.MINUTES_60)

        repository.updateFridayAutoMuteDuration(FridayAutoMuteDuration.OFF)
        val updatedOff = repository.userPreferencesFlow.first()
        assertThat(updatedOff.fridayAutoMuteDuration).isEqualTo(FridayAutoMuteDuration.OFF)
    }

    @Test
    fun surahKahfReadDate_persistsAndEmitsCorrectStatus() = runTest {
        val initial = repository.userPreferencesFlow.first()
        assertThat(initial.surahKahfReadDate).isNull()
        assertThat(initial.isSurahKahfReadForDate("2026-11-06")).isFalse()

        // Mark as read for Friday Nov 6, 2026
        repository.setSurahKahfReadForDate("2026-11-06", isRead = true)
        val updated = repository.userPreferencesFlow.first()
        assertThat(updated.surahKahfReadDate).isEqualTo("2026-11-06")
        assertThat(updated.isSurahKahfReadForDate("2026-11-06")).isTrue()
        assertThat(updated.isSurahKahfReadForDate("2026-11-13")).isFalse()

        // Unmark
        repository.setSurahKahfReadForDate("2026-11-06", isRead = false)
        val cleared = repository.userPreferencesFlow.first()
        assertThat(cleared.surahKahfReadDate).isNull()
        assertThat(cleared.isSurahKahfReadForDate("2026-11-06")).isFalse()
    }

    @Test
    fun saveRingerStateAndClearAutoMuteState_persistsAndClearsCorrectly() = runTest {
        val initial = repository.savedRingerStateFlow.first()
        assertThat(initial.isAutoMuted).isFalse()
        assertThat(initial.ringerMode).isEqualTo(2)

        val endEpoch = System.currentTimeMillis() + 1200000L
        repository.saveRingerState(
            ringerMode = 2,
            ringVolume = 7,
            notificationVolume = 5,
            endEpochMillis = endEpoch
        )

        val saved = repository.savedRingerStateFlow.first()
        assertThat(saved.isAutoMuted).isTrue()
        assertThat(saved.ringerMode).isEqualTo(2)
        assertThat(saved.ringVolume).isEqualTo(7)
        assertThat(saved.notificationVolume).isEqualTo(5)
        assertThat(saved.endEpochMillis).isEqualTo(endEpoch)

        repository.clearAutoMuteState()
        val cleared = repository.savedRingerStateFlow.first()
        assertThat(cleared.isAutoMuted).isFalse()
        assertThat(cleared.endEpochMillis).isEqualTo(0L)
    }

    @Test
    fun volumeEscalation_persistsAndEmitsUpdatedPreference() = runTest {
        val initial = repository.userPreferencesFlow.first()
        assertThat(initial.volumeEscalation).isEqualTo(VolumeEscalationOption.SECONDS_20)

        repository.updateVolumeEscalation(VolumeEscalationOption.SECONDS_30)
        val updated = repository.userPreferencesFlow.first()
        assertThat(updated.volumeEscalation).isEqualTo(VolumeEscalationOption.SECONDS_30)

        repository.updateVolumeEscalation(VolumeEscalationOption.OFF)
        val turnedOff = repository.userPreferencesFlow.first()
        assertThat(turnedOff.volumeEscalation).isEqualTo(VolumeEscalationOption.OFF)
    }

    @Test
    fun tahajjudAlarm_persistsEnabledAndOffsetCorrectly() = runTest {
        val initial = repository.userPreferencesFlow.first()
        assertThat(initial.tahajjudAlarmEnabled).isFalse()
        assertThat(initial.tahajjudAlarmMinutesOffset).isEqualTo(0)

        repository.updateTahajjudAlarmEnabled(true)
        repository.updateTahajjudAlarmMinutesOffset(15)

        val updated = repository.userPreferencesFlow.first()
        assertThat(updated.tahajjudAlarmEnabled).isTrue()
        assertThat(updated.tahajjudAlarmMinutesOffset).isEqualTo(15)
    }
}
