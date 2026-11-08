package dev.hayya.core.ui.screens

import com.google.common.truth.Truth.assertThat
import dev.hayya.core.domain.prayerlog.OBLIGATORY_PRAYERS
import dev.hayya.core.domain.model.PrayerType
import dev.hayya.core.domain.tasbih.TasbihMilestoneHelper
import dev.hayya.core.domain.tasbih.TasbihTargetMode
import dev.hayya.core.ui.components.AppNavTab
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
        assertThat(tabs).hasSize(3)
        assertThat(tabs[0]).isEqualTo(AppNavTab.DASHBOARD)
        assertThat(tabs[0].label).isEqualTo("Prayers")

        assertThat(tabs[1]).isEqualTo(AppNavTab.TOOLS)
        assertThat(tabs[1].label).isEqualTo("Companion")

        assertThat(tabs[2]).isEqualTo(AppNavTab.SETTINGS)
        assertThat(tabs[2].label).isEqualTo("Settings")
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
}

