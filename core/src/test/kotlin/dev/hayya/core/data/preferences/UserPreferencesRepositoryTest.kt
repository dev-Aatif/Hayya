package dev.hayya.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import dev.hayya.core.domain.model.AlertMode
import dev.hayya.core.domain.model.AutoSilenceTimeout
import dev.hayya.core.domain.model.CalculationMethodOption
import dev.hayya.core.domain.model.HighLatitudeRuleOption
import dev.hayya.core.domain.model.MadhabOption
import dev.hayya.core.domain.model.PrayerType
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class UserPreferencesRepositoryTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var tempFile: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: UserPreferencesRepository

    @Before
    fun setUp() {
        tempFile = File.createTempFile("test_preferences", ".preferences_pb")
        dataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tempFile }
        )
        repository = UserPreferencesRepository(dataStore)
    }

    @After
    fun tearDown() {
        tempFile.delete()
    }

    @Test
    fun defaultPreferences_matchesExpectedDefaults() = testScope.runTest {
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
    }

    @Test
    fun updateCoordinates_updatesValuesAndEmitsNewState() = testScope.runTest {
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
    fun updateCoordinates_validatesBounds() = testScope.runTest {
        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.updateCoordinates(latitude = 95.0, longitude = 0.0) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.updateCoordinates(latitude = -91.0, longitude = 0.0) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.updateCoordinates(latitude = 0.0, longitude = 185.0) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.updateCoordinates(latitude = 0.0, longitude = -181.0) }
        }
    }

    @Test
    fun updateCalculationMethodAndMadhab_persistsCorrectly() = testScope.runTest {
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
    fun updatePrayerOffset_storesOffsetsAndEnforcesBounds() = testScope.runTest {
        repository.updatePrayerOffset(PrayerType.FAJR, 15)
        repository.updatePrayerOffset(PrayerType.ASR, -10)

        val updated = repository.userPreferencesFlow.first()
        assertThat(updated.getOffset(PrayerType.FAJR)).isEqualTo(15)
        assertThat(updated.getOffset(PrayerType.ASR)).isEqualTo(-10)
        assertThat(updated.getOffset(PrayerType.DHUHR)).isEqualTo(0)

        // Exceeding bounds throws
        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.updatePrayerOffset(PrayerType.FAJR, 31) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runTest { repository.updatePrayerOffset(PrayerType.FAJR, -31) }
        }
    }

    @Test
    fun updateAlertModeAndAutoSilence_persistsSettings() = testScope.runTest {
        repository.updateAlertMode(PrayerType.FAJR, AlertMode.MUTED)
        repository.updateAlertMode(PrayerType.MAGHRIB, AlertMode.GENTLE_BANNER)
        repository.updateAutoSilenceTimeout(AutoSilenceTimeout.FULL_AUDIO)

        val updated = repository.userPreferencesFlow.first()
        assertThat(updated.getAlertMode(PrayerType.FAJR)).isEqualTo(AlertMode.MUTED)
        assertThat(updated.getAlertMode(PrayerType.MAGHRIB)).isEqualTo(AlertMode.GENTLE_BANNER)
        assertThat(updated.autoSilenceTimeout).isEqualTo(AutoSilenceTimeout.FULL_AUDIO)
    }

    @Test
    fun resetToDefaults_clearsCustomSettings() = testScope.runTest {
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
}
