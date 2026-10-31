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
    val description: String,
    val defaultCity: String,
    val defaultLatitude: Double,
    val defaultLongitude: Double
) {
    MUSLIM_WORLD_LEAGUE(
        "Muslim World League (MWL)",
        "Standard method for Europe, the Far East, and parts of the Americas. Evaluates Fajr at 18.0° and Isha at 17.0° twilight angles.",
        "Makkah / International",
        21.4225,
        39.8262
    ),
    NORTH_AMERICA(
        "ISNA (North America)",
        "Islamic Society of North America convention widely followed in the United States and Canada. Evaluates both Fajr and Isha at 15.0° twilight angles.",
        "New York, USA",
        40.7128,
        -74.0060
    ),
    EGYPTIAN(
        "Egyptian General Authority of Survey",
        "Standard method across Egypt, Sudan, the Levant, and North/East Africa. Evaluates Fajr at 19.5° and Isha at 17.5° twilight angles.",
        "Cairo, Egypt",
        30.0444,
        31.2357
    ),
    UMM_AL_QURA(
        "Umm al-Qura University (Makkah)",
        "Official standard of the Kingdom of Saudi Arabia and the Arabian Peninsula. Evaluates Fajr at 18.5° and Isha fixed at 90 minutes after Maghrib (120 minutes during Ramadan).",
        "Makkah, Saudi Arabia",
        21.4225,
        39.8262
    ),
    KARACHI(
        "University of Islamic Sciences, Karachi",
        "Standard method across Pakistan, India, Bangladesh, and Afghanistan. Evaluates both Fajr and Isha at 18.0° twilight angles.",
        "Karachi, Pakistan",
        24.8607,
        67.0011
    ),
    DUBAI(
        "Dubai (UAE)",
        "Official calculation standard for the United Arab Emirates. Evaluates Fajr at 18.2° and Isha at 18.2° twilight angles.",
        "Dubai, UAE",
        25.2048,
        55.2708
    ),
    QATAR(
        "Qatar",
        "Official Ministry of Awqaf standard for the State of Qatar. Evaluates Fajr at 18.0° and Isha fixed at 90 minutes after Maghrib.",
        "Doha, Qatar",
        25.2854,
        51.5310
    ),
    KUWAIT(
        "Kuwait",
        "Official Ministry of Awqaf standard for Kuwait. Evaluates Fajr at 18.0° and Isha at 17.5° twilight angles.",
        "Kuwait City, Kuwait",
        29.3759,
        47.9774
    ),
    SINGAPORE(
        "Singapore (MUIS)",
        "Official Majlis Ugama Islam Singapura standard. Evaluates Fajr at 20.0° and Isha at 18.0° twilight angles.",
        "Singapore",
        1.3521,
        103.8198
    ),
    MOON_SIGHTING_COMMITTEE(
        "Moonsighting Committee Worldwide",
        "Global moonsighting standard using 18.0° for Fajr and Isha with specialized seasonal twilight adjustments for higher latitudes.",
        "Moonsighting Global",
        41.8781,
        -87.6298
    ),
    TEHRAN(
        "Institute of Geophysics, University of Tehran",
        "Official Shia Ithna Ashari standard in Iran. Evaluates Fajr at 17.7°, Maghrib at 4.5°, and Isha at 14.0° twilight angles.",
        "Tehran, Iran",
        35.6892,
        51.3890
    ),
    JAFARI(
        "Shia Ithna Ashari (Leva Institute, Qum)",
        "Shia Ithna Ashari standard according to the Leva Research Institute. Evaluates Fajr at 16.0°, Maghrib at 4.0°, and Isha at 14.0°.",
        "Qom, Iran",
        34.6401,
        50.8764
    ),
    OTHER(
        "Custom / Regional Parameters",
        "User-configured astronomical angles and manual interval adjustments.",
        "Custom Location",
        21.4225,
        39.8262
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
    val autoSilenceTimeout: AutoSilenceTimeout = AutoSilenceTimeout.SECONDS_60,
    val preReminderEnabled: Boolean = true,
    val lastCallEnabled: Boolean = true
) {
    fun getOffset(prayer: PrayerType): Int = manualMinuteOffsets[prayer] ?: 0

    fun getAlertMode(prayer: PrayerType): AlertMode = alertModes[prayer] ?: AlertMode.FULL_ALARM
}
