# Hayya Core — Headless Calculation & Scheduling Engine

Hayya Core is a headless Kotlin calculation and scheduling engine for offline Android prayer reminder applications targeting **Android SDK 35** (`minSdk 26`), built using pure Kotlin and modern Jetpack libraries.

It has zero UI dependencies and is designed specifically for battery-efficient background alarm scheduling via `AlarmManager` and Android notifications.

---

## Architecture Overview

```
dev.hayya.core/
├── domain/
│   ├── model/
│   │   ├── PrayerModels.kt             # PrayerType, AlertTier, ScheduledAlert, PrayerWindow
│   │   └── PreferenceModels.kt         # CalculationMethodOption, MadhabOption, UserPreferences
│   └── calculation/
│       ├── AstronomicalCalculator.kt   # Pluggable astronomical provider interface
│       ├── AdhanAstronomicalCalculator.kt # Wrapper over Batoul Apps adhan-kotlin (KMP)
│       ├── SolarAstronomicalCalculator.kt # Pure Kotlin offline astronomical solver (Jean Meeus)
│       └── PrayerCalculationEngine.kt  # Window resolver, 3-tier triggers, 7-day rolling queue
└── data/
    └── preferences/
        └── UserPreferencesRepository.kt # Jetpack DataStore implementation
```

---

## Components

### 1. Domain Models (`domain/model/PrayerModels.kt`)

* **`PrayerType`**: `FAJR`, `SUNRISE`, `DHUHR`, `ASR`, `MAGHRIB`, `ISHA`.
  - Distinguishes obligatory daily prayers (`isObligatory`) from solar transitions (`SUNRISE`).
* **`AlertTier`**:
  - `PRE_REMINDER`: Exactly 15 minutes before prayer time ($T_0 - 15\text{m}$).
  - `MAIN_CALL`: Exactly at prayer start ($T_0$).
  - `LAST_CALL`: Exactly 20 minutes before window expiration ($T_{\text{end}} - 20\text{m}$).
* **`ScheduledAlert`**:
  - Encapsulates `prayerType`, `tier`, `triggerEpochMillis`, and `notificationId`.
  - **Deterministic Notification ID**:
    $$\text{notificationId} = (\text{dayOfYear} \times 100) + (\text{prayerType.ordinal} \times 10) + \text{tier.ordinal}$$
    For $\text{dayOfYear} \in [1, 366]$, $\text{prayerType.ordinal} \in [0, 5]$, and $\text{tier.ordinal} \in [0, 2]$, IDs span from `100` to `36652`. This guarantees:
    1. Zero ID collisions across days, prayers, and tiers.
    2. Strictly positive 32-bit `Int` values compatible with Android `NotificationManager`.
    3. Bijective, reversible extraction of day-of-year, prayer, and tier from the integer ID.
* **`PrayerWindow`**:
  - Sealed class representing active prayer, next impending prayer, and elapsed window progress:
    $$\text{progress} = \text{clamp}\left(\frac{t - t_{\text{start}}}{t_{\text{end}} - t_{\text{start}}}, 0.0\text{f}, 1.0\text{f}\right)$$
  - Variants: `PrayerWindow.Active` (for Salah windows) and `PrayerWindow.SunriseInterval` (for Duha / non-obligatory interval).

---

### 2. Calculation Engine (`domain/calculation/PrayerCalculationEngine.kt`)

Wraps Batoul Apps `adhan-kotlin` (`com.batoulapps.adhan:adhan2:0.0.7`) and astronomical solar equations:

* **Pure Functions**: Computes daily prayer times for any coordinate pair and Gregorian local date.
* **Manual Offsets**: Safely clamps per-prayer minute adjustments within $[-30, +30]$ minutes.
* **Islamic Midnight (Nisf al-Layl)**:
  $$\text{Islamic Midnight} = \text{Maghrib} + \frac{\text{NextDayFajr} - \text{Maghrib}}{2}$$
* **Prayer Expiration Rules**:
  - `FAJR` expires at `SUNRISE`
  - `DHUHR` expires at `ASR`
  - `ASR` expires at `MAGHRIB`
  - `MAGHRIB` expires at `ISHA`
  - `ISHA` expires at `FAJR` (next day) or `Islamic Midnight` (if preference enabled)
* **Active Window Resolution**:
  - Resolves any millisecond timestamp into its current prayer and impending next prayer with exact progress $[0.0\text{f}, 1.0\text{f}]$.
* **Trigger Generation**:
  - Computes the 3 discrete alert tiers per prayer.
* **Rolling Queue Scheduling**:
  - Generates a 7-day chronological alert queue for Android `AlarmManager`.
  - Automatically filters out triggers that have already passed in the current moment.

---

### 3. Preferences Repository (`data/preferences/UserPreferencesRepository.kt`)

Built on Jetpack DataStore Preferences (`androidx.datastore:datastore-preferences:1.1.1`):

* **Coordinates & Location**: `latitude`, `longitude`, `altitude`, `resolvedLocationName`.
* **Calculation Methods**: MWL, ISNA, Egypt, Umm al-Qura (Makkah), Karachi, Dubai, Qatar, Kuwait, Singapore, Moonsighting Committee, Tehran, Jafari, and Custom.
* **Madhab**: Standard (Shafi'i, Maliki, Hanbali) vs. Hanafi.
* **High Latitude Rules**: Middle of the Night, Seventh of the Night, Twilight Angle.
* **Manual Offsets**: Per-prayer offsets ($-30$ to $+30$ minutes) with strict runtime validation.
* **Alert Modes**: `FULL_ALARM`, `GENTLE_BANNER`, `MUTED` per prayer.
* **Auto-Silence**: 30s, 60s, 120s, or Full Audio.

---

### 4. Edge Cases Solved

1. **Midnight Crossing between Isha and Fajr**:
   - At 02:30 AM (after 00:00 midnight and before today's Fajr), naive calendar lookups fail because today's Fajr has not arrived yet. The engine queries yesterday's Isha, correctly designating `ISHA` as active, `FAJR` as next, and accurately calculating elapsed progress.
2. **Daylight Saving Time (DST) Transitions**:
   - Astronomical times are anchored to UTC epoch milliseconds. Rolling alert queues remain strictly monotonic and continuous across spring-forward (+1h) and fall-back (-1h) clock shifts, preventing skipped or duplicated alarms.
3. **High-Latitude Summer Solstice**:
   - In locations like Oslo (60°N) or Reykjavik (64°N) during midsummer where twilight persists all night, the sun never dips below $-18^\circ$ or $-17^\circ$. High-latitude rules automatically bound Fajr and Isha so that prayer times remain strictly ordered:
   $$\text{Fajr} < \text{Sunrise} < \text{Dhuhr} < \text{Asr} < \text{Maghrib} < \text{Isha} < \text{NextDayFajr}$$

---

## Local Verification (Three-Tier Testing Strategy)

Execute the following commands locally to verify the build and tests:

### Level 1 — While Developing (Micro-iterations)

```bash
# Run deterministic calculation engine tests
./gradlew :core:testDebugUnitTest --tests "dev.hayya.core.domain.calculation.PrayerCalculationEngineTest"

# Run edge cases test suite (midnight crossing, DST, high latitudes)
./gradlew :core:testDebugUnitTest --tests "dev.hayya.core.domain.calculation.EdgeCasesTest"

# Run Jetpack DataStore preferences test suite
./gradlew :core:testDebugUnitTest --tests "dev.hayya.core.data.preferences.UserPreferencesRepositoryTest"

# Run domain models & notification ID tests
./gradlew :core:testDebugUnitTest --tests "dev.hayya.core.domain.model.PrayerModelsTest"
```

### Level 2 — Before Committing

```bash
# Run all core unit tests
./gradlew :core:testDebugUnitTest
```

### Level 3 — Before Release

```bash
# Run full unit tests, lint checks, and assemble library AAR
./gradlew test lint assembleRelease
```
