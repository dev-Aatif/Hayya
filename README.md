# Hayya

**An ultra-minimalist, 100% offline Islamic prayer reminder and spiritual companion app for Android.**

Built with pure Kotlin, Jetpack Compose (Material 3), and Jetpack Glance AppWidgets, Hayya prioritizes deterministic astronomical accuracy, Doze-resilient exact alarms, and zero telemetry.

> **Release V1 Scope — Pakistan (Larkana Division):** V1 is calibrated and verified for **Pakistan**, featuring complete offline directory coverage for **Larkana Division** (all 5 districts and 37 towns/talukas), University of Islamic Sciences, Karachi (`18.0° / 18.0°`) twilight angles, and Hanafi Asr defaults.

---

## Table of Contents

- [Core Features](#core-features)
- [Download & Installation](#download--installation)
- [Background & Alarm Reliability](#background--alarm-reliability)
- [Permissions](#permissions)
- [Tech Stack & Architecture](#tech-stack--architecture)
- [Build & Verification](#build--verification)
- [License](#license)

---

## Core Features

### Prayer Time & Astronomical Engine
- **100% Offline & Private:** Zero internet permission, zero analytics, zero trackers. All prayer times, Hijri calendar dates (`java.time.chrono.HijrahDate`), Qibla bearings, and city coordinates execute locally on-device.
- **Dual Astronomical Solver:** Powered by Batoul Apps `adhan` (`1.2.1`) with an offline Jean Meeus solar solver (`SolarAstronomicalCalculator`).
- **Regional Mosque Conventions (`SindhMosqueSchedule`):** Models Sehri Ihtiyat (10m safety window), Ishraq, Zawal, pre-Maghrib Makrooh buffers, and seasonal Summer/Winter Adhan-to-Jama'at intervals.
- **Larkana Division Offline Directory:** Covers 37 municipal corporations, taluka headquarters, and town committees across Larkana, Qambar Shahdadkot, Shikarpur, Jacobabad, and Kashmore.

### Alarms & Mosque Mode
- **3-Tier Alert Pipeline:** Optional 15-minute Pre-Reminder (`T0 - 15m`), Main Call (`T0`) with lockscreen `FullScreenIntent` wake, and 20-minute Last Call warning (`T_end - 20m`).
- **Familiar Alarm Audio:** Uses your device's default alarm tone (`RingtoneManager.TYPE_ALARM`) on `USAGE_ALARM`, with optional gradual volume escalation (`10s–30s`) and smooth 2-second fade-out.
- **Mosque Auto-Silent (`AutoMuteManager`):** Automatically switches the ringer to Silent (via Do Not Disturb access) or Vibrate during Salah (`15–30m`) and Jumu'ah Khutbah (`45–60m`), saving and restoring exact prior ringer mode and stream volumes.

### Spiritual Companion & Widgets
- **Companion Suite:** Live Qibla compass (`QiblaOrientationSensor`), tactile OLED Tasbih counter, 7-day Prayer Habits tracker, Night & Tahajjud calculator (Islamic Midnight & Last Third), Friday (Jumu'ah) checklist & Salawat counter, and Ramadan & Eid Hub (30-day timetable, fast tracker, Qadha counter, Sunnah Duas, Suhoor wake-up alarm).
- **5 Jetpack Glance Home Screen Widgets:**
  - `Hayya Next Prayer` (`2x2`) — Upcoming prayer, countdown, and window progress bar
  - `Hayya Full Timetable` (`4x2`) — Dual-pane next prayer hero and full 5-prayer schedule
  - `Hayya Daily Prayers` (`4x2`) — Interactive 1-tap prayer check-off tracker
  - `Hayya Quick Strip` (`4x1`) — Compact bar with interactive prayer completion pips
  - `Hayya Night & Fasting` (`3x2`) — Suhoor, Iftar, Last Third (Tahajjud), and Midnight times
- **Visual Craft:** Pure OLED Dark (`#07080A` / `#000000` lockscreen), Warm Sand Light (`#F8F4EC`), System Auto, optional Material You dynamic color (Android 12+), and `tnum` tabular figures for jitter-free countdowns.

---

## Download & Installation

1. Download **`Hayya-v1.0.0.apk`** (and optionally **`Hayya-v1.0.0.apk.sha256`**) from the [GitHub Releases](../../releases) page.
2. *(Optional)* Verify the APK integrity before installing:
   ```bash
   sha256sum -c Hayya-v1.0.0.apk.sha256
   ```
3. Install on **Android 8.0+ (API 26+)**:
   - Open the APK on your device and allow installation from your browser/file manager, **or** install via ADB:
     ```bash
     adb install -r Hayya-v1.0.0.apk
     ```
4. Complete the 3-step Welcome Setup to select your town in **Larkana Division**, choose your **Asr** juristic school (**Hanafi** or **Standard**), and grant **Notifications**, **Unrestricted Battery**, and **Exact Alarm** permissions.

---

## Background & Alarm Reliability

1. **Doze Bypass (`AlarmManager.setAlarmClock`):** Schedules a 7-day rolling queue exclusively via `AlarmManager.setAlarmClock` with deterministic request codes: `requestCode = (dayOfYear * 100) + (prayerOrdinal * 10) + tierOrdinal`.
2. **WakeLock Safety Contract:** `PrayerAlarmReceiver` acquires a bounded `PARTIAL_WAKE_LOCK` inside `goAsync()`, dispatches notifications or starts `PrayerAudioService`, and releases the WakeLock in `finally`.
3. **Foreground Protection:** `PrayerAudioService` (`mediaPlayback`) manages alarm playback; an optional `PinnedPrayerStatusService` (`specialUse`) anchors the process against aggressive OEM Low Memory Killers.
4. **Reboot & Clock Resilience (`SystemClockChangedReceiver`):** Intercepts `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, and `MY_PACKAGE_REPLACED` to rebuild the 7-day rolling queue, refresh Glance widgets, and recover interrupted auto-mute state.

---

## Permissions

| Permission | Purpose |
| :--- | :--- |
| `USE_EXACT_ALARM`, `SCHEDULE_EXACT_ALARM` (`maxSdkVersion="32"`) | Exact prayer, Suhoor, Tahajjud, and auto-unmute scheduling |
| `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED` | CPU retention during dispatch and post-reboot rescheduling |
| `USE_FULL_SCREEN_INTENT` | Lockscreen wake screen (`PrayerWakeActivity`) |
| `POST_NOTIFICATIONS` | Prayer alerts, pre-reminders, and ongoing status (Android 13+) |
| `VIBRATE` | Haptic feedback and vibrate-mode auto-mute |
| `ACCESS_NOTIFICATION_POLICY` | Optional Mosque Auto-Silent Do Not Disturb control |
| `FOREGROUND_SERVICE` (`MEDIA_PLAYBACK`, `SPECIAL_USE`) | Active alarm playback and optional pinned status notification |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Battery optimization check and OEM exemption shortcut |

Hardware features `android.hardware.sensor.compass` and `android.hardware.sensor.accelerometer` are declared with `required="false"` for the offline Qibla compass.

---

## Tech Stack & Architecture

- **Language & Build:** Kotlin `2.0.21`, Android Gradle Plugin `8.7.2`, JDK 17, `minSdk 26` / `targetSdk 36` / `compileSdk 36`
- **UI & Widgets:** Jetpack Compose (BOM `2024.10.01`, Material 3), Jetpack Glance `1.1.1`
- **Persistence & Time:** Jetpack DataStore Preferences `1.1.1`, `kotlinx-datetime` `0.6.0`, `kotlinx-coroutines` `1.8.1`
- **Astronomical Engine:** `com.batoulapps.adhan:adhan:1.2.1`

```text
core/src/main/kotlin/dev/hayya/core/
├── HayyaApplication.kt                   # App-scoped DataStore, engine & scheduler singletons
├── data/
│   ├── preferences/                      # UserPreferencesRepository (DataStore)
│   ├── prayerlog/                        # PrayerLogRepository (7-day prayer habit history)
│   └── ramadan/                          # FastTrackerRepository (fasting log, Qadha & Suhoor)
├── domain/
│   ├── calculation/                      # Astronomical calculators, PrayerCalculationEngine,
│   │                                     # NightFastingCalculator, SindhMosqueSchedule
│   ├── friday/                           # FridayReminders (Surah Al-Kahf & Salawat)
│   ├── model/                            # PrayerModels, PreferenceModels, CityCatalog, PostAdhanDua
│   ├── prayerlog/                        # DailyPrayerLog & RollingPrayerHistory
│   ├── qibla/                            # QiblaCalculator (great-circle bearing & smoothing)
│   ├── ramadan/                          # RamadanModels, RamadanDuasStore, EidGuide, Hijri helper
│   └── tasbih/                           # TasbihModels & milestone helper
├── system/
│   ├── alarm/                            # PrayerAlarmScheduler (7-day setAlarmClock queue)
│   ├── audio/                            # PrayerAudioService & AutoMuteManager
│   ├── notifications/                    # PinnedPrayerStatusService
│   ├── oem/                              # OemPowerManager (battery & autostart settings)
│   ├── receivers/                        # PrayerAlarmReceiver & SystemClockChangedReceiver
│   ├── sensor/                           # QiblaOrientationSensor
│   └── ui/                               # PrayerWakeActivity (lockscreen wake host)
├── ui/
│   ├── MainActivity.kt                   # Root navigation, permissions & widget/FGS sync
│   ├── components/                       # FloatingNavBar & VisualComponents
│   ├── screens/                          # Dashboard, Companion, Settings, Onboarding & tools
│   ├── theme/                            # OLED Dark, Warm Sand Light, Dynamic Color & typography
│   └── viewmodel/                        # PrayerViewModel (StateFlow & 1s ticker)
└── widget/                               # 5 Glance AppWidgets, receivers, updater & theme
```

---

## Build & Verification

### 1. Local Configuration
- **Android SDK:** Copy `local.properties.example` to `local.properties` and set `sdk.dir` to your local Android SDK path.
- **Release Signing:** Copy `keystore.properties.example` to `keystore.properties` and set `storeFile`, `storePassword`, `keyAlias`, and `keyPassword` — or export `HAYYA_KEYSTORE_FILE`, `HAYYA_KEYSTORE_PASSWORD`, `HAYYA_KEY_ALIAS`, and `HAYYA_KEY_PASSWORD`.

### 2. Gradle Verification & Release Build

```bash
# Run all unit tests (domain, data, system, UI, widgets)
./gradlew :core:testDebugUnitTest

# Run Android Lint
./gradlew :core:lintDebug

# Assemble minified & signed release APK -> core/build/outputs/apk/release/core-release.apk
./gradlew :core:assembleRelease
```

### 3. On-Device Alarm Stress Test (ADB)

```bash
./scripts/verify_alarm_engine.sh
```

### 4. GitHub Actions CI & Automated Releases
- Pushes and pull requests to `main` run `:core:testDebugUnitTest` and `:core:lintDebug`.
- Pushing a `v*` tag (e.g., `v1.0.0`) runs verification, decodes `HAYYA_KEYSTORE_BASE64`, signs `:core:assembleRelease`, and publishes `Hayya-<tag>.apk` and `Hayya-<tag>.apk.sha256` to GitHub Releases.
- Required repository secrets: `HAYYA_KEYSTORE_BASE64`, `HAYYA_KEYSTORE_PASSWORD`, `HAYYA_KEY_ALIAS`, `HAYYA_KEY_PASSWORD`.

---

## Changelog & License

- See [CHANGELOG.md](CHANGELOG.md) for release notes.
- Copyright © 2026 **dev-Aatif**. All Rights Reserved. Licensed under a **proprietary license** — see [LICENSE](LICENSE) for full terms. Public source visibility on GitHub is provided for transparency and security inspection only and does not grant rights to copy, modify, redistribute, or publish derivative builds.
