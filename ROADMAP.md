# Hayya Roadmap

This document tracks known issues from **V1 (`1.0.0`)** and the planned post-V1 feature roadmap for **Hayya**.

> **V1 Baseline (`1.0.0`):** 100% offline prayer times, Doze-resilient exact alarms, Mosque Auto-Silent, Companion tools, and 5 Glance AppWidgets calibrated for **Pakistan (Larkana Division)**.

---

## Known Issues

### Major Issues
- *None identified for V1 (`1.0.0`).*

### Minor Issues (Targeted for `v1.0.1`)
1. **Post-Adhan Dua Sheet Closes on Auto-Silence Timeout**
   - *Where:* `PrayerWakeActivity.kt` & `PrayerWakeActivityUI.kt`
   - *Behavior:* When a timed alarm (`30s` / `1m` / `2m`) finishes fading out, `PrayerAudioService` broadcasts `ACTION_PLAYBACK_FINISHED` and `PrayerWakeActivity` calls `finish()` immediately, closing the **Dua After Adhan** bottom sheet if the user is still reading it.
   - *Fix:* Defer `finish()` in `PrayerWakeActivity` until the Dua sheet is dismissed.

2. **Pinned Status Notification Friday Label & Service Lifecycle Polish**
   - *Where:* `PinnedPrayerStatusService.kt` & `MainActivity.kt`
   - *Behavior:*
     - Displays `DHUHR` instead of `JUMU'AH` on Fridays in the notification title.
     - `MainActivity` invokes `PinnedPrayerStatusService.stop(this)` on launch whenever the toggle is off, even when `PinnedPrayerStatusService.isRunning` is already `false`.
     - `PinnedPrayerStatusService.refreshIfRunning()` restarts the command and briefly flashes `"Calculating upcoming prayer window..."` before the ticker overwrites it.
   - *Fix:* Format Friday title with `nextPrayer.displayName(isFriday)`, guard `stop()` with `if (isRunning)`, and update the notification directly without resetting text.

3. **Static Calculation Authority Label in Settings**
   - *Where:* `SettingsScreen.kt` (`CalculationSubScreen`)
   - *Behavior:* Renders `CalculationMethodOption.KARACHI.displayName` statically rather than binding `userPreferences.calculationMethod.displayName` (safe in V1 since all Larkana Division presets use Karachi).
   - *Fix:* Bind `userPreferences.calculationMethod.displayName` dynamically.

4. **Non-Blocking Android Lint Warnings (0 Errors, 126 Warnings)**
   - *Where:* `core/build/reports/lint-results-debug.txt`
   - *Details:* Compose `ModifierParameter` ordering on 6 screens, `ObsoleteSdkInt` checks (`Build.VERSION.SDK_INT >= O` with `minSdk = 26`), `HardcodedText` / `SmallSp` / `DisableBaselineAlignment` in XML widget previews, `UnusedAttribute` for API 27/31 XML attributes, and `GradleDependency` version notices.

---

## Planned Features (Post-V1)

### 1. Built-in Adhan Audio Recitations & Custom Tone Picker (`v1.1`)
- Add optional offline Adhan audio presets (e.g., Makkah, Madinah, and a dedicated Fajr Adhan) selectable per prayer or globally, while keeping the device's default alarm tone (`RingtoneManager.TYPE_ALARM`) as a lightweight fallback.
- Support picking a custom local audio file via the Android system file/ringtone picker and routing it through `PrayerAudioService` (`USAGE_ALARM`) with existing gradual volume escalation and fade-out.

### 2. Local Offline Backup / Export & Restore (`v1.1`)
- Keep `android:allowBackup="false"` for strict zero-cloud privacy while giving users a 100% offline **Export / Import Backup (`.json`)** option via Android's Storage Access Framework.
- Back up and restore 7-day/rolling Prayer Habits logs, Ramadan fasting records, Qadha counters, Tasbih/Salawat counts, and alarm/theme preferences across device upgrades.

### 3. Multi-Language Localization — Urdu & Sindhi (`v1.2`)
- Extract remaining UI, notification, and widget strings into Android string resources (`strings.xml`).
- Add full **Urdu (`ur`)** and **Sindhi (`sd`)** translations with clean RTL layout verification, especially tailored for users across Larkana Division and Sindh.
