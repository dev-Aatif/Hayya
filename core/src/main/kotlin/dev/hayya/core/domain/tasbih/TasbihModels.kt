package dev.hayya.core.domain.tasbih

/**
 * Operating target modes for the ultra-minimalist OLED Tasbih counter.
 */
enum class TasbihTargetMode(
    val displayName: String,
    val description: String,
    val defaultTarget: Int?
) {
    /**
     * Canonical Sunnah Tasbih of Fatimah (Sahih Muslim 597):
     * 33 SubhanAllah -> 33 Alhamdulillah -> 33 Allahu Akbar -> 1 La ilaha illallah (Seal to 100).
     * Milestone pulses at 33, 66, 99, and 100.
     */
    FATIMA_100(
        "33 · 33 · 34 (100)",
        "Sunnah after-prayer tasbih with milestone pulses at 33, 66, 99, and 100",
        100
    ),

    /**
     * Single cycle of 33 counts. Milestone pulse at 33.
     */
    TARGET_33(
        "33 Counts",
        "Focused single cycle of 33 repetitions",
        33
    ),

    /**
     * 100 counts with milestone pulses at 33, 66, 99, and 100.
     */
    TARGET_100(
        "100 Counts",
        "Full century dhikr count with pulses at 33, 66, 99, and 100",
        100
    ),

    /**
     * Continuous unlimited count. Pulses at every 33 and 100 milestone.
     */
    FREE(
        "Free Count",
        "Unrestricted open count with rhythmic milestone pulses",
        null
    )
}

/**
 * Dhikr phase metadata containing authentic Arabic text, transliteration, translation, and range.
 */
data class DhikrPhase(
    val arabic: String,
    val transliteration: String,
    val translation: String,
    val targetRangeLabel: String
)

/**
 * Helper utility orchestrating milestone interval detection, phase identification,
 * and progress calculations for [TasbihTargetMode].
 */
object TasbihMilestoneHelper {

    val PHASE_SUBHANALLAH = DhikrPhase(
        arabic = "سُبْحَانَ ٱللَّٰهِ",
        transliteration = "Subḥān Allāh",
        translation = "Glory be to Allah",
        targetRangeLabel = "1 – 33"
    )

    val PHASE_ALHAMDULILLAH = DhikrPhase(
        arabic = "ٱلْحَمْدُ لِلَّٰهِ",
        transliteration = "Al-ḥamdu lillāh",
        translation = "Praise be to Allah",
        targetRangeLabel = "34 – 66"
    )

    val PHASE_ALLAHU_AKBAR = DhikrPhase(
        arabic = "ٱللَّٰهُ أَكْبَرُ",
        transliteration = "Allāhu Akbar",
        translation = "Allah is the Greatest",
        targetRangeLabel = "67 – 99"
    )

    val PHASE_SEAL_OF_100 = DhikrPhase(
        arabic = "لَا إِلَٰهَ إِلَّا ٱللَّٰهُ وَحْدَهُ لَا شَرِيكَ لَهُ",
        transliteration = "Lā ilāha illallāhu waḥdahu lā sharīka lah",
        translation = "None is worthy of worship but Allah alone, without partner",
        targetRangeLabel = "Seal of 100"
    )

    val PHASE_FREE_DHIKR = DhikrPhase(
        arabic = "ذِكْرُ ٱللَّٰهِ",
        transliteration = "Dhikrullāh",
        translation = "Remembrance of Allah",
        targetRangeLabel = "Continuous"
    )

    /**
     * Determines whether the given [count] represents a tactile milestone interval.
     * Milestones trigger a distinct heavy double vibration pulse.
     *
     * Specifically returns true at:
     * - 33, 66, 99, 100 in [TasbihTargetMode.FATIMA_100] and [TasbihTargetMode.TARGET_100]
     * - Multiples of 33 in [TasbihTargetMode.TARGET_33]
     * - Every 33, 66, 99, 100 cycle in [TasbihTargetMode.FREE]
     */
    fun isMilestone(count: Int, mode: TasbihTargetMode = TasbihTargetMode.FATIMA_100): Boolean {
        if (count <= 0) return false

        return when (mode) {
            TasbihTargetMode.FATIMA_100, TasbihTargetMode.TARGET_100 -> {
                val cyclePos = ((count - 1) % 100) + 1
                cyclePos == 33 || cyclePos == 66 || cyclePos == 99 || cyclePos == 100
            }
            TasbihTargetMode.TARGET_33 -> {
                count % 33 == 0
            }
            TasbihTargetMode.FREE -> {
                val cyclePos = ((count - 1) % 100) + 1
                cyclePos == 33 || cyclePos == 66 || cyclePos == 99 || cyclePos == 100 || (count % 33 == 0)
            }
        }
    }

    /**
     * Resolves the current authentic [DhikrPhase] based on count and mode.
     */
    fun getPhaseForCount(count: Int, mode: TasbihTargetMode = TasbihTargetMode.FATIMA_100): DhikrPhase {
        return when (mode) {
            TasbihTargetMode.FATIMA_100 -> {
                val cyclePos = if (count == 0) 1 else ((count - 1) % 100) + 1
                when {
                    cyclePos in 1..33 -> PHASE_SUBHANALLAH
                    cyclePos in 34..66 -> PHASE_ALHAMDULILLAH
                    cyclePos in 67..99 -> PHASE_ALLAHU_AKBAR
                    else -> PHASE_SEAL_OF_100
                }
            }
            TasbihTargetMode.TARGET_33 -> PHASE_SUBHANALLAH
            TasbihTargetMode.TARGET_100 -> {
                val cyclePos = if (count == 0) 1 else ((count - 1) % 100) + 1
                when {
                    cyclePos in 1..33 -> PHASE_SUBHANALLAH
                    cyclePos in 34..66 -> PHASE_ALHAMDULILLAH
                    cyclePos in 67..99 -> PHASE_ALLAHU_AKBAR
                    else -> PHASE_SEAL_OF_100
                }
            }
            TasbihTargetMode.FREE -> {
                val cyclePos = if (count == 0) 1 else ((count - 1) % 100) + 1
                when {
                    cyclePos in 1..33 -> PHASE_SUBHANALLAH
                    cyclePos in 34..66 -> PHASE_ALHAMDULILLAH
                    cyclePos in 67..99 -> PHASE_ALLAHU_AKBAR
                    cyclePos == 100 -> PHASE_SEAL_OF_100
                    else -> PHASE_FREE_DHIKR
                }
            }
        }
    }

    /**
     * Computes the current cycle lap number (1-based).
     */
    fun calculateLap(count: Int, mode: TasbihTargetMode): Int {
        if (count <= 0) return 1
        val target = mode.defaultTarget ?: return 1
        return ((count - 1) / target) + 1
    }

    /**
     * Computes the progress fraction (0.0f..1.0f) towards the next milestone or target.
     */
    fun calculateProgress(count: Int, mode: TasbihTargetMode): Float {
        return when (mode) {
            TasbihTargetMode.FATIMA_100, TasbihTargetMode.TARGET_100 -> {
                val cyclePos = if (count == 0) 0 else ((count - 1) % 100) + 1
                (cyclePos / 100f).coerceIn(0f, 1f)
            }
            TasbihTargetMode.TARGET_33 -> {
                val cyclePos = if (count == 0) 0 else ((count - 1) % 33) + 1
                (cyclePos / 33f).coerceIn(0f, 1f)
            }
            TasbihTargetMode.FREE -> {
                val cyclePos = if (count == 0) 0 else ((count - 1) % 33) + 1
                (cyclePos / 33f).coerceIn(0f, 1f)
            }
        }
    }

    /**
     * Returns the next target milestone number.
     */
    fun getNextMilestone(count: Int, mode: TasbihTargetMode): Int {
        return when (mode) {
            TasbihTargetMode.FATIMA_100, TasbihTargetMode.TARGET_100 -> {
                val cyclePos = ((count) % 100)
                when {
                    cyclePos < 33 -> 33
                    cyclePos < 66 -> 66
                    cyclePos < 99 -> 99
                    else -> 100
                }
            }
            TasbihTargetMode.TARGET_33 -> 33
            TasbihTargetMode.FREE -> {
                val rem = count % 33
                count + (33 - rem)
            }
        }
    }
}
