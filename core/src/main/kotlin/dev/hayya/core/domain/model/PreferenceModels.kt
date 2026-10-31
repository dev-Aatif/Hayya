package dev.hayya.core.domain.model

import com.batoulapps.adhan.CalculationMethod
import com.batoulapps.adhan.HighLatitudeRule
import com.batoulapps.adhan.Madhab

/**
 * Geographic coordinates with optional altitude and resolved location name.
 */
data class GeoLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double = 0.0,
    val resolvedLocationName: String = ""
) {
    init {
        require(latitude in -90.0..90.0) { "Latitude must be in [-90.0, 90.0], was $latitude" }
        require(longitude in -180.0..180.0) { "Longitude must be in [-180.0, 180.0], was $longitude" }
    }
}

/**
 * Recognised calculation conventions across the Islamic world.
 */
enum class CalculationMethodOption(
    val displayName: String,
    val description: String
) {
    MUSLIM_WORLD_LEAGUE(
        "Muslim World League (MWL)",
        "Standard for Europe, Far East, and parts of the Americas. Fajr: 18°, Isha: 17°."
    ),
    NORTH_AMERICA(
        "ISNA (North America)",
        "Islamic Society of North America. Fajr: 15°, Isha: 15°."
    ),
    EGYPTIAN(
        "Egyptian General Authority of Survey",
        "Standard for Egypt, Sudan, Levant, and Africa. Fajr: 19.5°, Isha: 17.5°."
    ),
    UMM_AL_QURA(
        "Umm al-Qura University (Makkah)",
        "Official Saudi Arabia standard. Fajr: 18.5°, Isha: 90 min after Maghrib (120 in Ramadan)."
    ),
    KARACHI(
        "University of Islamic Sciences, Karachi",
        "Standard for Pakistan, India, Bangladesh, and Afghanistan. Fajr: 18°, Isha: 18°."
    ),
    DUBAI(
        "Dubai (UAE)",
        "United Arab Emirates official calculation method."
    ),
    QATAR(
        "Qatar",
        "Qatar official calculation method."
    ),
    KUWAIT(
        "Kuwait",
        "Kuwait official calculation method."
    ),
    SINGAPORE(
        "Singapore (MUIS)",
        "Majlis Ugama Islam Singapura standard."
    ),
    MOON_SIGHTING_COMMITTEE(
        "Moonsighting Committee Worldwide",
        "Moonsighting Committee calculation method."
    ),
    TEHRAN(
        "Institute of Geophysics, University of Tehran",
        "Shia Ithna Ashari standard in Iran. Fajr: 17.7°, Maghrib: 4.5°, Isha: 14°."
    ),
    JAFARI(
        "Shia Ithna Ashari (Leva Institute, Qum)",
        "Fajr: 16°, Maghrib: 4°, Isha: 14°."
    ),
    OTHER(
        "Custom Parameters",
        "User-defined custom astronomical parameters."
    );

    fun toAdhanMethod(): CalculationMethod = when (this) {
        MUSLIM_WORLD_LEAGUE -> CalculationMethod.MUSLIM_WORLD_LEAGUE
        NORTH_AMERICA -> CalculationMethod.NORTH_AMERICA
        EGYPTIAN -> CalculationMethod.EGYPTIAN
        UMM_AL_QURA -> CalculationMethod.UMM_AL_QURA
        KARACHI -> CalculationMethod.KARACHI
        DUBAI -> CalculationMethod.DUBAI
        QATAR -> CalculationMethod.QATAR
        KUWAIT -> CalculationMethod.KUWAIT
        SINGAPORE -> CalculationMethod.SINGAPORE
        MOON_SIGHTING_COMMITTEE -> CalculationMethod.MOON_SIGHTING_COMMITTEE
        TEHRAN, JAFARI, OTHER -> CalculationMethod.OTHER
    }
}

/**
 * Juristic school determining the shadow length ratio for Asr prayer.
 */
enum class MadhabOption(
    val displayName: String,
    val description: String
) {
    STANDARD(
        "Shafi'i, Maliki, Hanbali (Standard)",
        "Asr begins when an object's shadow equals its length plus noon shadow."
    ),
    HANAFI(
        "Hanafi",
        "Asr begins when an object's shadow equals twice its length plus noon shadow."
    );

    fun toAdhanMadhab(): Madhab = when (this) {
        STANDARD -> Madhab.SHAFI
        HANAFI -> Madhab.HANAFI
    }
}

/**
 * Adjustment rule for locations at high latitudes where astronomical twilight may persist.
 */
enum class HighLatitudeRuleOption(
    val displayName: String,
    val description: String
) {
    MIDDLE_OF_THE_NIGHT(
        "Middle of the Night",
        "Fajr and Isha do not exceed half the night between Sunset and Sunrise."
    ),
    SEVENTH_OF_THE_NIGHT(
        "Seventh of the Night",
        "Fajr and Isha are constrained to one-seventh of the total night duration."
    ),
    TWILIGHT_ANGLE(
        "Twilight Angle",
        "Proportional angle-based adjustment based on standard twilight."
    );

    fun toAdhanRule(): HighLatitudeRule = when (this) {
        MIDDLE_OF_THE_NIGHT -> HighLatitudeRule.MIDDLE_OF_THE_NIGHT
        SEVENTH_OF_THE_NIGHT -> HighLatitudeRule.SEVENTH_OF_THE_NIGHT
        TWILIGHT_ANGLE -> HighLatitudeRule.TWILIGHT_ANGLE
    }
}

/**
 * Notification / alert behavior mode for an individual prayer.
 */
enum class AlertMode(val displayName: String) {
    /** Loud Adhan or continuous alarm requiring user dismissal */
    FULL_ALARM("Full Alarm (Adhan / Sound)"),

    /** Silent visual notification or heads-up banner */
    GENTLE_BANNER("Gentle Banner (Silent / Heads-up)"),

    /** No notification or alarm triggered */
    MUTED("Muted (No Notification)")
}

/**
 * Auto-silence duration timeout for loud alerts.
 */
enum class AutoSilenceTimeout(val seconds: Int?, val displayName: String) {
    SECONDS_30(30, "30 Seconds"),
    SECONDS_60(60, "1 Minute"),
    SECONDS_120(120, "2 Minutes"),
    FULL_AUDIO(null, "Full Audio (Plays to completion)");

    val durationMillis: Long?
        get() = seconds?.let { it * 1000L }
}

/**
 * Complete immutable snapshot of user preferences for offline calculation and scheduling.
 */
data class UserPreferences(
    val location: GeoLocation = GeoLocation(21.4225, 39.8262, 0.0, "Makkah, Saudi Arabia"),
    val calculationMethod: CalculationMethodOption = CalculationMethodOption.MUSLIM_WORLD_LEAGUE,
    val madhab: MadhabOption = MadhabOption.STANDARD,
    val highLatitudeRule: HighLatitudeRuleOption = HighLatitudeRuleOption.MIDDLE_OF_THE_NIGHT,
    val ishaExpiresAtMidnight: Boolean = false,
    val manualMinuteOffsets: Map<PrayerType, Int> = PrayerType.entries.associateWith { 0 },
    val alertModes: Map<PrayerType, AlertMode> = PrayerType.entries.associateWith { prayer ->
        if (prayer == PrayerType.SUNRISE) AlertMode.GENTLE_BANNER else AlertMode.FULL_ALARM
    },
    val autoSilenceTimeout: AutoSilenceTimeout = AutoSilenceTimeout.SECONDS_60
) {
    fun getOffset(prayer: PrayerType): Int = manualMinuteOffsets[prayer] ?: 0

    fun getAlertMode(prayer: PrayerType): AlertMode = alertModes[prayer] ?: AlertMode.FULL_ALARM
}
