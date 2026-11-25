package dev.hayya.core.ui.screens

import com.google.common.truth.Truth.assertThat
import dev.hayya.core.domain.model.CalculationMethodOption
import dev.hayya.core.domain.model.CityCatalog
import dev.hayya.core.domain.prayerlog.OBLIGATORY_PRAYERS
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.tasbih.TasbihMilestoneHelper
import dev.hayya.core.domain.tasbih.TasbihTargetMode
import dev.hayya.core.ui.components.AppNavTab
import dev.hayya.core.ui.components.friendlyPrayerName
import dev.hayya.core.ui.components.prayerTimeOfDayHint
import org.junit.Test

/**
 * Unit tests verifying UX contracts, navigation hierarchies, and design constants
 * established during the ultra-minimalist UX design overhaul.
 *
 * NOTE: As per zero-execution policy, this test is statically authored and verified.
 */
class MinimalistUxScreensTest {

    @Test
    fun appNavTab_hasExpectedLabelsAndOrder() {
        val tabs = AppNavTab.entries
        assertThat(tabs).hasSize(4)
        assertThat(tabs[0]).isEqualTo(AppNavTab.DASHBOARD)
        assertThat(tabs[0].label).isEqualTo("Prayers")

        assertThat(tabs[1]).isEqualTo(AppNavTab.HADITH)
        assertThat(tabs[1].label).isEqualTo("Hadith")

        assertThat(tabs[2]).isEqualTo(AppNavTab.TOOLS)
        assertThat(tabs[2].label).isEqualTo("Companion")

        assertThat(tabs[3]).isEqualTo(AppNavTab.SETTINGS)
        assertThat(tabs[3].label).isEqualTo("Settings")
    }

    @Test
    fun ramadanHubSection_hasExpectedLabelsAndOrder() {
        val sections = RamadanHubSection.entries
        assertThat(sections).hasSize(5)
        assertThat(sections.map { it.label }).containsExactly(
            "Tracker",
            "Duas",
            "Timetable",
            "Eid & Takbirat",
            "Alerts"
        ).inOrder()
    }

    @Test
    fun settingsSubSection_coversAllKeyCategories() {
        val subSections = SettingsSubSection.entries
        assertThat(subSections).hasSize(5)
        assertThat(subSections.map { it.title }).containsExactly(
            "APPEARANCE & THEME",
            "CALCULATION & JURISTICS",
            "ALERTS & OFFSETS",
            "SYSTEM RELIABILITY",
            "ALARM SIMULATION"
        ).inOrder()
    }

    @Test
    fun obligatoryPrayers_sequenceAndInitials_areConsistent() {
        assertThat(OBLIGATORY_PRAYERS).containsExactly(
            PrayerType.FAJR,
            PrayerType.DHUHR,
            PrayerType.ASR,
            PrayerType.MAGHRIB,
            PrayerType.ISHA
        ).inOrder()

        val standardInitials = OBLIGATORY_PRAYERS.map { prayer ->
            when (prayer) {
                PrayerType.FAJR -> "F"
                PrayerType.DHUHR -> "D"
                PrayerType.ASR -> "A"
                PrayerType.MAGHRIB -> "M"
                PrayerType.ISHA -> "I"
                else -> prayer.name.take(1)
            }
        }
        assertThat(standardInitials).containsExactly("F", "D", "A", "M", "I").inOrder()
    }

    @Test
    fun tasbihTargetsAndMilestones_areCalculatedCorrectly() {
        assertThat(TasbihMilestoneHelper.isMilestone(33, TasbihTargetMode.TARGET_33)).isTrue()
        assertThat(TasbihMilestoneHelper.isMilestone(32, TasbihTargetMode.TARGET_33)).isFalse()

        assertThat(TasbihMilestoneHelper.isMilestone(33, TasbihTargetMode.FATIMA_100)).isTrue()
        assertThat(TasbihMilestoneHelper.isMilestone(66, TasbihTargetMode.FATIMA_100)).isTrue()
        assertThat(TasbihMilestoneHelper.isMilestone(99, TasbihTargetMode.FATIMA_100)).isTrue()
        assertThat(TasbihMilestoneHelper.isMilestone(100, TasbihTargetMode.FATIMA_100)).isTrue()
        assertThat(TasbihMilestoneHelper.isMilestone(32, TasbihTargetMode.FATIMA_100)).isFalse()

        assertThat(TasbihMilestoneHelper.isMilestone(33, TasbihTargetMode.TARGET_100)).isTrue()
        assertThat(TasbihMilestoneHelper.isMilestone(66, TasbihTargetMode.TARGET_100)).isTrue()
        assertThat(TasbihMilestoneHelper.isMilestone(99, TasbihTargetMode.TARGET_100)).isTrue()
        assertThat(TasbihMilestoneHelper.isMilestone(100, TasbihTargetMode.TARGET_100)).isTrue()
    }

    @Test
    fun cityCatalog_containsValidCoordinatesAndSearchWorks() {
        assertThat(CityCatalog.allCities).isNotEmpty()
        assertThat(CityCatalog.popularCities).isNotEmpty()

        CityCatalog.allCities.forEach { city ->
            assertThat(city.latitude).isIn(com.google.common.collect.Range.closed(-90.0, 90.0))
            assertThat(city.longitude).isIn(com.google.common.collect.Range.closed(-180.0, 180.0))
            assertThat(city.name).isNotEmpty()
            assertThat(city.country).isNotEmpty()
        }

        val londonResults = CityCatalog.search("london")
        assertThat(londonResults.any { it.name == "London" }).isTrue()

        val emptyResults = CityCatalog.search("   ")
        assertThat(emptyResults).hasSize(CityCatalog.allCities.size)
    }

    @Test
    fun pakistanCities_coverAllRegionsWithValidPakistanBoundsAndKarachiMethod() {
        val pkCities = CityCatalog.pakistanCities
        assertThat(pkCities.size).isAtLeast(90)

        // No duplicate city names in Pakistan
        assertThat(pkCities.map { it.name }).containsNoDuplicates()

        // Covers all major provinces & territories
        val regions = pkCities.map { it.region }.toSet()
        assertThat(regions).containsAtLeast(
            "Islamabad Capital Territory",
            "Punjab",
            "Sindh",
            "Khyber Pakhtunkhwa",
            "Balochistan",
            "Azad Kashmir",
            "Gilgit-Baltistan"
        )

        // All coordinates fall cleanly within Pakistan's geographic bounds and use Karachi method
        pkCities.forEach { city ->
            assertThat(city.latitude).isIn(com.google.common.collect.Range.closed(23.5, 37.5))
            assertThat(city.longitude).isIn(com.google.common.collect.Range.closed(60.5, 78.0))
            assertThat(city.recommendedMethod).isEqualTo(CalculationMethodOption.KARACHI)
        }

        assertThat(CityCatalog.search("Faisalabad", onlyPakistan = true).first().name).isEqualTo("Faisalabad")
        assertThat(CityCatalog.search("London", onlyPakistan = true)).isEmpty()
    }

    @Test
    fun larkanaDivision_coversAllFiveDistrictsAndMunicipalAuthorities() {
        val larkanaCities = CityCatalog.larkanaDivisionCities
        assertThat(larkanaCities).hasSize(37)
        assertThat(larkanaCities.map { it.name }).containsNoDuplicates()

        val districts = larkanaCities.map { it.district }.toSet()
        assertThat(districts).containsExactlyElementsIn(CityCatalog.larkanaDistricts)

        larkanaCities.forEach { city ->
            assertThat(city.division).isEqualTo("Larkana Division")
            assertThat(city.region).isEqualTo("Sindh")
            assertThat(city.authorityType).isNotEmpty()
            assertThat(city.latitude).isIn(com.google.common.collect.Range.closed(27.0, 28.6))
            assertThat(city.longitude).isIn(com.google.common.collect.Range.closed(67.5, 69.8))
            assertThat(city.recommendedMethod).isEqualTo(CalculationMethodOption.KARACHI)
        }

        assertThat(CityCatalog.search("Nasirabad", district = "Qambar Shahdadkot").first().name)
            .isEqualTo("Nasirabad")
        assertThat(CityCatalog.search("Kandhkot", division = "Larkana Division").first().district)
            .isEqualTo("Kashmore")
    }

    @Test
    fun friendlyPrayerNames_andHints_areReadable() {
        assertThat(friendlyPrayerName(PrayerType.FAJR)).isEqualTo("Fajr")
        assertThat(friendlyPrayerName(PrayerType.DHUHR, isFriday = false)).isEqualTo("Dhuhr")
        assertThat(friendlyPrayerName(PrayerType.DHUHR, isFriday = true)).isEqualTo("Jumu'ah")
        assertThat(prayerTimeOfDayHint(PrayerType.MAGHRIB)).isEqualTo("Sunset")
        assertThat(prayerTimeOfDayHint(PrayerType.ISHA)).isEqualTo("Night")
    }
}

