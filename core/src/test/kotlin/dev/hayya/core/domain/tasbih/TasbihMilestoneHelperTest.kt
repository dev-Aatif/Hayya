package dev.hayya.core.domain.tasbih

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class TasbihMilestoneHelperTest {

    @Test
    fun isMilestone_fatimaMode_detectsExactSunnahMilestones() {
        // Milestones: 33, 66, 99, 100
        assertThat(TasbihMilestoneHelper.isMilestone(33, TasbihTargetMode.FATIMA_100)).isTrue()
        assertThat(TasbihMilestoneHelper.isMilestone(66, TasbihTargetMode.FATIMA_100)).isTrue()
        assertThat(TasbihMilestoneHelper.isMilestone(99, TasbihTargetMode.FATIMA_100)).isTrue()
        assertThat(TasbihMilestoneHelper.isMilestone(100, TasbihTargetMode.FATIMA_100)).isTrue()

        // Non-milestones
        assertThat(TasbihMilestoneHelper.isMilestone(0, TasbihTargetMode.FATIMA_100)).isFalse()
        assertThat(TasbihMilestoneHelper.isMilestone(1, TasbihTargetMode.FATIMA_100)).isFalse()
        assertThat(TasbihMilestoneHelper.isMilestone(32, TasbihTargetMode.FATIMA_100)).isFalse()
        assertThat(TasbihMilestoneHelper.isMilestone(34, TasbihTargetMode.FATIMA_100)).isFalse()
        assertThat(TasbihMilestoneHelper.isMilestone(65, TasbihTargetMode.FATIMA_100)).isFalse()
        assertThat(TasbihMilestoneHelper.isMilestone(67, TasbihTargetMode.FATIMA_100)).isFalse()
        assertThat(TasbihMilestoneHelper.isMilestone(98, TasbihTargetMode.FATIMA_100)).isFalse()
    }

    @Test
    fun isMilestone_secondLap_continuesMilestones() {
        // Lap 2 in Fatima Mode: 133 (33 in lap 2), 166 (66 in lap 2), 199, 200
        assertThat(TasbihMilestoneHelper.isMilestone(133, TasbihTargetMode.FATIMA_100)).isTrue()
        assertThat(TasbihMilestoneHelper.isMilestone(166, TasbihTargetMode.FATIMA_100)).isTrue()
        assertThat(TasbihMilestoneHelper.isMilestone(199, TasbihTargetMode.FATIMA_100)).isTrue()
        assertThat(TasbihMilestoneHelper.isMilestone(200, TasbihTargetMode.FATIMA_100)).isTrue()

        assertThat(TasbihMilestoneHelper.isMilestone(132, TasbihTargetMode.FATIMA_100)).isFalse()
        assertThat(TasbihMilestoneHelper.isMilestone(134, TasbihTargetMode.FATIMA_100)).isFalse()
    }

    @Test
    fun isMilestone_target33Mode_pulsesEvery33() {
        assertThat(TasbihMilestoneHelper.isMilestone(33, TasbihTargetMode.TARGET_33)).isTrue()
        assertThat(TasbihMilestoneHelper.isMilestone(66, TasbihTargetMode.TARGET_33)).isTrue()
        assertThat(TasbihMilestoneHelper.isMilestone(99, TasbihTargetMode.TARGET_33)).isTrue()
        assertThat(TasbihMilestoneHelper.isMilestone(34, TasbihTargetMode.TARGET_33)).isFalse()
    }

    @Test
    fun getPhaseForCount_mapsToAuthenticDhikrPhases() {
        // 1 to 33: SubhanAllah
        val phase1 = TasbihMilestoneHelper.getPhaseForCount(1, TasbihTargetMode.FATIMA_100)
        assertThat(phase1.transliteration).isEqualTo("Subḥān Allāh")
        assertThat(phase1.arabic).contains("سُبْحَانَ")

        val phase33 = TasbihMilestoneHelper.getPhaseForCount(33, TasbihTargetMode.FATIMA_100)
        assertThat(phase33.transliteration).isEqualTo("Subḥān Allāh")

        // 34 to 66: Alhamdulillah
        val phase34 = TasbihMilestoneHelper.getPhaseForCount(34, TasbihTargetMode.FATIMA_100)
        assertThat(phase34.transliteration).isEqualTo("Al-ḥamdu lillāh")
        assertThat(phase34.arabic).contains("ٱلْحَمْدُ")

        val phase66 = TasbihMilestoneHelper.getPhaseForCount(66, TasbihTargetMode.FATIMA_100)
        assertThat(phase66.transliteration).isEqualTo("Al-ḥamdu lillāh")

        // 67 to 99: Allahu Akbar
        val phase67 = TasbihMilestoneHelper.getPhaseForCount(67, TasbihTargetMode.FATIMA_100)
        assertThat(phase67.transliteration).isEqualTo("Allāhu Akbar")
        assertThat(phase67.arabic).contains("ٱللَّٰهُ أَكْبَرُ")

        val phase99 = TasbihMilestoneHelper.getPhaseForCount(99, TasbihTargetMode.FATIMA_100)
        assertThat(phase99.transliteration).isEqualTo("Allāhu Akbar")

        // 100: Seal of 100 (La ilaha illallah...)
        val phase100 = TasbihMilestoneHelper.getPhaseForCount(100, TasbihTargetMode.FATIMA_100)
        assertThat(phase100.transliteration).contains("Lā ilāha illallāhu")
        assertThat(phase100.arabic).contains("لَا إِلَٰهَ إِلَّا ٱللَّٰهُ")
    }

    @Test
    fun calculateLap_computesCorrectCycleNumber() {
        assertThat(TasbihMilestoneHelper.calculateLap(0, TasbihTargetMode.FATIMA_100)).isEqualTo(1)
        assertThat(TasbihMilestoneHelper.calculateLap(1, TasbihTargetMode.FATIMA_100)).isEqualTo(1)
        assertThat(TasbihMilestoneHelper.calculateLap(100, TasbihTargetMode.FATIMA_100)).isEqualTo(1)
        assertThat(TasbihMilestoneHelper.calculateLap(101, TasbihTargetMode.FATIMA_100)).isEqualTo(2)
        assertThat(TasbihMilestoneHelper.calculateLap(200, TasbihTargetMode.FATIMA_100)).isEqualTo(2)
        assertThat(TasbihMilestoneHelper.calculateLap(201, TasbihTargetMode.FATIMA_100)).isEqualTo(3)
    }

    @Test
    fun calculateProgress_returnsNormalizedFraction() {
        assertThat(TasbihMilestoneHelper.calculateProgress(0, TasbihTargetMode.FATIMA_100)).isEqualTo(0f)
        assertThat(TasbihMilestoneHelper.calculateProgress(50, TasbihTargetMode.FATIMA_100)).isEqualTo(0.5f)
        assertThat(TasbihMilestoneHelper.calculateProgress(100, TasbihTargetMode.FATIMA_100)).isEqualTo(1.0f)
    }

    @Test
    fun getNextMilestone_returnsAppropriateTarget() {
        assertThat(TasbihMilestoneHelper.getNextMilestone(0, TasbihTargetMode.FATIMA_100)).isEqualTo(33)
        assertThat(TasbihMilestoneHelper.getNextMilestone(20, TasbihTargetMode.FATIMA_100)).isEqualTo(33)
        assertThat(TasbihMilestoneHelper.getNextMilestone(33, TasbihTargetMode.FATIMA_100)).isEqualTo(66)
        assertThat(TasbihMilestoneHelper.getNextMilestone(50, TasbihTargetMode.FATIMA_100)).isEqualTo(66)
        assertThat(TasbihMilestoneHelper.getNextMilestone(66, TasbihTargetMode.FATIMA_100)).isEqualTo(99)
        assertThat(TasbihMilestoneHelper.getNextMilestone(80, TasbihTargetMode.FATIMA_100)).isEqualTo(99)
        assertThat(TasbihMilestoneHelper.getNextMilestone(99, TasbihTargetMode.FATIMA_100)).isEqualTo(100)
    }
}
