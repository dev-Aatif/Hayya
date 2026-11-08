package dev.hayya.core.domain.model

import androidx.annotation.FloatRange

/**
 * Islamic prayer types and solar milestones.
 * In Islamic tradition, five are obligatory daily prayers (Salah), while Sunrise (Shuruq)
 * marks the conclusion of the Fajr window and the start of the non-obligatory Duha interval.
 */
enum class PrayerType {
    FAJR,
    SUNRISE,
    DHUHR,
    ASR,
    MAGHRIB,
    ISHA;

    /**
     * Whether this prayer is one of the five obligatory daily prayers.
     */
    val isObligatory: Boolean
        get() = this != SUNRISE

    /**
     * Returns the subsequent prayer or solar event in the daily cycle.
     */
    val nextInSequence: PrayerType
        get() = when (this) {
            FAJR -> SUNRISE
            SUNRISE -> DHUHR
            DHUHR -> ASR
            ASR -> MAGHRIB
            MAGHRIB -> ISHA
            ISHA -> FAJR
        }

    /**
     * Resolves the canonical display name for the prayer, honoring Friday (Jumu'ah)
     * convention where Friday prayer replaces standard Dhuhr.
     */
    fun displayName(isFriday: Boolean = false): String {
        return if (this == DHUHR && isFriday) "JUMU'AH" else this.name
    }
}

/**
 * Alert tiers for each prayer window:
 * - [PRE_REMINDER]: Exactly 15 minutes before prayer time (T - 15m).
 * - [MAIN_CALL]: Exactly at prayer start (T0).
 * - [LAST_CALL]: Exactly 20 minutes before the prayer window expires (Next prayer T - 20m).
 */
enum class AlertTier(
    val description: String,
    val offsetMinutesFromAnchor: Long
) {
    /** Tier 1: Exactly 15 minutes before prayer time (T - 15m) */
    PRE_REMINDER("15 minutes before prayer starts", -15L),

    /** Tier 2: Exactly at prayer start (T0) */
    MAIN_CALL("At prayer start time (T0)", 0L),

    /** Tier 3: Exactly 20 minutes before prayer window expires (T_end - 20m) */
    LAST_CALL("20 minutes before prayer window expires", -20L);

    val offsetMillis: Long
        get() = offsetMinutesFromAnchor * 60_000L
}

/**
 * Represents a discrete scheduled alert for Android AlarmManager / NotificationManager.
 *
 * @property prayerType The prayer or solar event associated with the alert.
 * @property tier The alert tier (Pre-reminder, Main call, Last call).
 * @property triggerEpochMillis The exact millisecond UTC timestamp when the alarm must fire.
 * @property notificationId Deterministic unique 32-bit ID based on day-of-year, prayer, and tier.
 */
data class ScheduledAlert(
    val prayerType: PrayerType,
    val tier: AlertTier,
    val triggerEpochMillis: Long,
    val notificationId: Int
) : Comparable<ScheduledAlert> {

    constructor(
        dayOfYear: Int,
        prayerType: PrayerType,
        tier: AlertTier,
        triggerEpochMillis: Long
    ) : this(
        prayerType = prayerType,
        tier = tier,
        triggerEpochMillis = triggerEpochMillis,
        notificationId = createNotificationId(dayOfYear, prayerType, tier)
    )

    override fun compareTo(other: ScheduledAlert): Int =
        triggerEpochMillis.compareTo(other.triggerEpochMillis)

    companion object {
        /**
         * Computes a deterministic, collision-free notification ID based on
         * day-of-year (1..366), prayer type ordinal (0..5), and tier ordinal (0..2).
         *
         * Formula: (dayOfYear * 100) + (prayerType.ordinal * 10) + tier.ordinal
         *
         * ID range: 100 (Jan 1, FAJR, PRE_REMINDER) to 36652 (Dec 31, ISHA, LAST_CALL).
         * Guaranteed to fit within standard positive 32-bit signed Integer.
         */
        fun createNotificationId(dayOfYear: Int, prayerType: PrayerType, tier: AlertTier): Int {
            require(dayOfYear in 1..366) { "dayOfYear must be in 1..366, was $dayOfYear" }
            return (dayOfYear * 100) + (prayerType.ordinal * 10) + tier.ordinal
        }

        fun extractDayOfYear(notificationId: Int): Int = notificationId / 100

        fun extractPrayerType(notificationId: Int): PrayerType {
            val ordinal = (notificationId % 100) / 10
            return PrayerType.entries[ordinal]
        }

        fun extractAlertTier(notificationId: Int): AlertTier {
            val ordinal = notificationId % 10
            return AlertTier.entries[ordinal]
        }
    }
}

/**
 * Sealed class representing the current active prayer window, next impending prayer,
 * and elapsed progress from 0.0f (just started) to 1.0f (window concluding).
 */
sealed class PrayerWindow {
    abstract val currentPrayer: PrayerType?
    abstract val nextPrayer: PrayerType
    abstract val startEpochMillis: Long
    abstract val endEpochMillis: Long
    @get:FloatRange(from = 0.0, to = 1.0)
    abstract val elapsedProgress: Float

    /**
     * Active obligatory prayer window (Fajr, Dhuhr, Asr, Maghrib, or Isha).
     */
    data class Active(
        override val currentPrayer: PrayerType,
        override val nextPrayer: PrayerType,
        override val startEpochMillis: Long,
        override val endEpochMillis: Long,
        @FloatRange(from = 0.0, to = 1.0)
        override val elapsedProgress: Float
    ) : PrayerWindow() {
        init {
            require(elapsedProgress in 0.0f..1.0f) {
                "elapsedProgress must be between 0.0f and 1.0f, was $elapsedProgress"
            }
        }
    }

    /**
     * Non-obligatory interval between Sunrise and Dhuhr (Duha / Ishraq).
     */
    data class SunriseInterval(
        override val startEpochMillis: Long,
        override val endEpochMillis: Long,
        @FloatRange(from = 0.0, to = 1.0)
        override val elapsedProgress: Float,
        override val currentPrayer: PrayerType? = PrayerType.SUNRISE,
        override val nextPrayer: PrayerType = PrayerType.DHUHR
    ) : PrayerWindow() {
        init {
            require(elapsedProgress in 0.0f..1.0f) {
                "elapsedProgress must be between 0.0f and 1.0f, was $elapsedProgress"
            }
        }
    }

    companion object {
        /**
         * Calculates normalized progress [0.0f, 1.0f] for a timestamp between start and end.
         */
        fun calculateProgress(currentMillis: Long, startMillis: Long, endMillis: Long): Float {
            if (endMillis <= startMillis) return 0.0f
            val elapsed = currentMillis - startMillis
            val total = endMillis - startMillis
            val raw = elapsed.toDouble() / total.toDouble()
            return raw.toFloat().coerceIn(0.0f, 1.0f)
        }
    }
}
