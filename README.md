# Hayya — Ultra-Minimalist Offline Prayer App & Unkillable Background Pipeline

Hayya is an ultra-minimalist offline Android prayer reminder application targeting **Android SDK 36** (`minSdk 26`), built using pure Kotlin, Jetpack Compose (Material 3), and modern Android Jetpack libraries.

It combines an unkillable low-level background alarm execution pipeline resilient through Android Doze mode and lockscreens with a pure OLED dark theme user interface designed for zero visual distraction and instant utility.

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
├── data/
│   └── preferences/
│       └── UserPreferencesRepository.kt # Jetpack DataStore implementation
├── system/
│   ├── alarm/
│   │   └── PrayerAlarmScheduler.kt     # AlarmManager.setAlarmClock dispatcher
│   ├── receivers/
│   │   ├── PrayerAlarmReceiver.kt      # WakeLock-protected alarm receiver (Tiers 1, 2, 3)
│   │   └── SystemClockChangedReceiver.kt # BOOT_COMPLETED & time change auto-rescheduler
│   ├── audio/
│   │   └── PrayerAudioService.kt       # ALARM audio stream foreground service with fade-out
│   ├── ui/
│   │   └── PrayerWakeActivity.kt       # Lockscreen FullScreenIntent activity hosting PrayerWakeScreen
│   └── device/
│       └── OemBatteryHelper.kt         # Ignore battery optimizations & OEM autostart helper
└── ui/
    ├── theme/
    │   └── Theme.kt                    # Pure OLED dark theme, color tokens & tabular figures (tnum)
    ├── viewmodel/
    │   └── PrayerViewModel.kt          # Unified StateFlow, 1s ticker, Hijri date, action handlers
    └── screens/
        ├── MainDashboardScreen.kt      # Minimal single-screen utility, live countdown, timetable
        ├── PrayerWakeActivityUI.kt     # Lockscreen wake screen with pulsing glow & dismiss slider
        ├── SettingsScreen.kt           # Grouped cards: auto-silence, madhab, method, battery shield
        └── OnboardingScreen.kt         # 3-card minimalist onboarding & permissions setup
```

---

## UI & Design System

### 1. Pure OLED Dark Palette
* **Surface Background**: `#08090A`
* **Lockscreen Wake Surface**: `#000000`
* **Elevated Card Surface**: `#121417` with 1px border (`#1F2328`)
* **Warm Sand Accent**: `#E5C378` strictly applied to countdown numbers, active prayer pill highlight, and interactive drag thumbs.
* **Typography**: Geometric sans-serif with `FontFeature.tabularFigures()` (`tnum`) applied across all timers, countdowns, and clocks to eliminate horizontal digit jitter during live ticking.

### 2. Reactive Presentation Layer (`PrayerViewModel`)
* Exposes a unified `StateFlow<PrayerDashboardUiState>` consumed by Compose screens.
* **1-Second Coroutine Ticker**: Continuously recalculates remaining time to the active/upcoming prayer window and updates elapsed progress (0.0f to 1.0f).
* **Deterministic Offline Hijri Date**: Computes Islamic dates natively using `java.time.chrono.HijrahDate` without requiring external network APIs.
* **Action Handlers**: Direct mutations for cycling prayer audio alert modes (`FULL_ALARM` $\to$ `GENTLE` $\to$ `MUTED`), switching Juristic Madhab (Standard vs Hanafi Asr), adjusting calculation methods, and selecting auto-silence durations.

### 3. Screen Components
* **`MainDashboardScreen`**:
  - Single-screen utility layout with top bar city indicator and settings gear.
  - Tracked uppercase next prayer label (e.g. `ASR`) with large tabular countdown (`01:42:18`), start time subtitle, and smooth arc progress indicator.
  - Daily 6-row timetable (Fajr, Sunrise, Dhuhr, Asr, Maghrib, Isha) with past prayers rendered at 40% alpha, active prayer encased in an elevated card with warm sand border, and quick-tap inline audio mode cycle toggle.
  - Bottom muted Hijri date card.
* **`PrayerWakeScreen` (`PrayerWakeActivityUI`)**:
  - Lockscreen full-screen intent interface rendered against OLED `#000000`.
  - Massive prayer name (`56sp` bold) with an ambient radial pulsing glow during active audio playback.
  - Subtext detailing window expiration (`Window ends at Sunrise 06:28 AM`) and auto-silence countdown timer.
  - Interactive horizontal drag-to-dismiss slider built with `pointerInput`, providing haptic feedback at the 85% completion threshold before silencing audio and finishing the activity.
* **`SettingsScreen`**:
  - Grouped clean cards with auto-silence pills `[30s | 1m | 2m | Full]`, 3-tier notification toggles (Pre-reminder 15m, Last Call 20m), Juristic Madhab selector, and battery shield status check.
* **`OnboardingScreen`**:
  - 3-card first-launch wizard: Location confirmation, Asr juristic school selection, and battery optimization / exact alarm permissions grant card with a "Complete Setup" action button.

---

## Low-Level Background Execution & System Architecture

### 1. Doze Mode Bypass via `AlarmManager.setAlarmClock`
* Never uses `setExactAndAllowWhileIdle()`—uses **`AlarmManager.setAlarmClock(AlarmClockInfo, PendingIntent)` exclusively**.
* In Android's power management hierarchy, `setAlarmClock`:
  1. Grants the highest priority execution window, completely ignoring Doze mode maintenance windows.
  2. Guarantees that the CPU wakes up at the exact target millisecond.
  3. Displays the native alarm clock icon in the status bar and lockscreen, providing user visibility.
* PendingIntent request codes are strictly deterministic:
  $$\text{requestCode} = (\text{DayOfYear} \times 100) + (\text{PrayerOrdinal} \times 10) + \text{TierOrdinal}$$

### 2. WakeLock Safety Contract
* `PrayerAlarmReceiver` immediately acquires a `PowerManager.PARTIAL_WAKE_LOCK` with a **10-second safety timeout** upon receiving an alarm trigger.
* Work is executed inside a `try ... finally` block, ensuring the WakeLock is unconditionally released after handing off execution to `PrayerAudioService`.
* `PrayerAudioService` holds its own partial WakeLock for the duration of the audio playback.

### 3. Multi-Tier Alert Dispatching
* **Tier 1 (Pre-Reminder, $T_0 - 15\text{m}$):** Posts a high-priority heads-up notification on `CHANNEL_PRE_REMINDER` with gentle notification sound.
* **Tier 2 (Main Call, $T_0$):** Launches `PrayerAudioService` in the foreground and presents a high-priority notification with `FullScreenIntent` targeting `PrayerWakeActivity` to wake the screen.
* **Tier 3 (Last Call, $T_{\text{end}} - 20\text{m}$):** Posts a high-priority heads-up notification on `CHANNEL_LAST_CALL` indicating the window expiration.

### 4. Audio Routing & Automated Coroutine Fade-Out
* Managed by `PrayerAudioService` via native `MediaPlayer`.
* Configures `AudioAttributes`:
  ```kotlin
  AudioAttributes.Builder()
      .setUsage(AudioAttributes.USAGE_ALARM)
      .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
      .build()
  ```
  Routing through `USAGE_ALARM` bypasses hardware silent mode switches and standard Do Not Disturb (DND) profiles.
* Coroutine Fade-Out: If `timeoutSeconds` is specified (e.g. 60s), plays at full volume until 2 seconds before expiration, then smoothly reduces volume over a 20-step loop (100ms intervals) before calling `stopSelf()`.

### 5. Resilient Auto-Rescheduling (`SystemClockChangedReceiver`)
* Catches system events:
  - `Intent.ACTION_BOOT_COMPLETED` & `Intent.ACTION_LOCKED_BOOT_COMPLETED`
  - `Intent.ACTION_TIME_SET` (Clock adjusted manually or by NITZ)
  - `Intent.ACTION_TIMEZONE_CHANGED` (Timezone change or Daylight Saving shift)
  - `Intent.ACTION_MY_PACKAGE_REPLACED` (App update)
* Runs asynchronously using `BroadcastReceiver.goAsync()` and a 15-second WakeLock, clears existing alarms, and schedules a fresh 7-day rolling queue. Also aliased as `BootAndClockReceiver`.

### 6. OEM Aggressive Battery Optimizer Whitelisting (`OemBatteryHelper`)
* Checks and requests `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
* Inspects `Build.MANUFACTURER` and exposes intent dispatchers for manufacturer autostart and background protection screens:
  - **Samsung**: OneUI Device Care / Smart Manager Battery screens.
  - **Xiaomi / Redmi / POCO**: MIUI / HyperOS Security Center Autostart and PowerKeeper hidden apps config.
  - **Huawei / Honor**: EMUI / MagicOS Startup Normal App List and Protect Activity.
  - **Oppo / Realme / OnePlus**: ColorOS / OxygenOS Startup App List and OplusBattery.
  - **Vivo / iQOO**: FuntouchOS / OriginOS Background StartUp Manager.
  - **Transsion (Tecno, Infinix, itel)**: HiOS / XOS AutoStart app list.

---

## Permissions Declared in `AndroidManifest.xml`

```xml
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
```

---

## Local Verification (Three-Tier Testing Strategy)

Execute the following commands locally to verify the build and test suites:

### Level 1 — While Developing (Micro-iterations)

```bash
# Test UI & ViewModel state flow, countdown ticker, and audio cycling
./gradlew :core:testDebugUnitTest --tests "dev.hayya.core.ui.viewmodel.PrayerViewModelTest"

# Test AlarmManager.setAlarmClock scheduling & request codes
./gradlew :core:testDebugUnitTest --tests "dev.hayya.core.system.alarm.PrayerAlarmSchedulerTest"

# Test PrayerAlarmReceiver WakeLock retention and alert tiers
./gradlew :core:testDebugUnitTest --tests "dev.hayya.core.system.receivers.PrayerAlarmReceiverTest"

# Test SystemClockChangedReceiver boot & time set handling
./gradlew :core:testDebugUnitTest --tests "dev.hayya.core.system.receivers.SystemClockChangedReceiverTest"

# Test PrayerAudioService AudioAttributes and fade-out volume loop
./gradlew :core:testDebugUnitTest --tests "dev.hayya.core.system.audio.PrayerAudioServiceTest"

# Test OEM battery optimization & autostart dispatchers
./gradlew :core:testDebugUnitTest --tests "dev.hayya.core.system.device.OemBatteryHelperTest"

# Test calculation engine & edge cases (midnight crossing, DST, high latitudes)
./gradlew :core:testDebugUnitTest --tests "dev.hayya.core.domain.calculation.PrayerCalculationEngineTest"
./gradlew :core:testDebugUnitTest --tests "dev.hayya.core.domain.calculation.EdgeCasesTest"
```

### Level 2 — Before Committing

```bash
# Run all unit tests across domain, data, and system packages
./gradlew :core:testDebugUnitTest
```

### Level 3 — Before Release

```bash
# Validate full tests, lint, and assemble release library
./gradlew test lint assembleRelease
```
