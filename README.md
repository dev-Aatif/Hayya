# Hayya Core — Headless Calculation & Unkillable Background Alarm Pipeline

Hayya Core is a headless Kotlin calculation engine and low-level background alarm execution pipeline for offline Android prayer reminder applications targeting **Android SDK 36** (`minSdk 26`), built using pure Kotlin and modern Jetpack libraries.

It has zero Compose/UI dependencies and is specifically architected for resilient, unkillable alarm execution through Android Doze mode, deep CPU sleep, and lockscreens.

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
└── system/
    ├── alarm/
    │   └── PrayerAlarmScheduler.kt     # AlarmManager.setAlarmClock dispatcher
    ├── receivers/
    │   ├── PrayerAlarmReceiver.kt      # WakeLock-protected alarm receiver (Tiers 1, 2, 3)
    │   └── SystemClockChangedReceiver.kt # BOOT_COMPLETED & time change auto-rescheduler
    ├── audio/
    │   └── PrayerAudioService.kt       # ALARM audio stream foreground service with fade-out
    ├── ui/
    │   └── PrayerWakeActivity.kt       # Stub activity for lockscreen FullScreenIntent
    └── device/
        └── OemBatteryHelper.kt         # Ignore battery optimizations & OEM autostart helper
```

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
