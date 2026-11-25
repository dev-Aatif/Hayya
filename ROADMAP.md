# Hayya Roadmap

This document tracks known issues and the upcoming feature roadmap for **Hayya**.

> **Current Release (`1.1.0`):** Offline-first prayer times, Doze-resilient exact alarms, Mosque Auto-Silent, Companion tools, 5 Glance AppWidgets, Multilingual Hadith Hub, and GitHub Releases In-App Updater — calibrated for **Pakistan (Larkana Division)**.

---

## Resolved Issues (`v1.1.0`)

1. **Post-Adhan Dua Sheet Closes on Auto-Silence Timeout** *(Fixed)*
   - `PrayerWakeActivity` now defers `finish()` on `ACTION_PLAYBACK_FINISHED` while the **Dua After Adhan** bottom sheet is open and closes cleanly once dismissed.
2. **Pinned Status Notification Friday Label & Service Lifecycle Polish** *(Fixed)*
   - `PinnedPrayerStatusService` now formats Friday Dhuhr as `JUMU'AH`, refreshes via `ACTION_REFRESH` without resetting notification text, and `MainActivity` guards `stop()` with `isRunning`.
3. **Static Calculation Authority Label in Settings** *(Fixed)*
   - `SettingsScreen` (`CalculationSubScreen`) now binds `userPreferences.calculationMethod.displayName` dynamically.
4. **Kotlin & Layout Lint Polish** *(Fixed)*
   - Resolved `ModifierParameter`, `ObsoleteSdkInt`, and `DisableBaselineAlignment` Android Lint warnings.

---

## Known Issues

### Major Issues
- *None identified for `1.1.0`.*

### Minor Issues
- **Informational Android Lint Warnings (0 Errors)**:
  - Remaining warnings in `core/build/reports/lint-results-debug.txt` are limited to static XML widget preview literals (`HardcodedText`, `SmallSp`), API 27/31 manifest/widget XML attributes (`UnusedAttribute`), and dependency update notices (`GradleDependency`).

---

## Planned Features (Upcoming)

### 1. Built-in Adhan Audio Recitations & Custom Tone Picker (`v1.2`)
- Add optional offline Adhan audio presets (e.g., Makkah, Madinah, and a dedicated Fajr Adhan) selectable per prayer or globally, while keeping the device's default alarm tone (`RingtoneManager.TYPE_ALARM`) as a lightweight fallback.
- Support picking a custom local audio file via the Android system file/ringtone picker and routing it through `PrayerAudioService` (`USAGE_ALARM`) with existing gradual volume escalation and fade-out.

### 2. Local Offline Backup / Export & Restore (`v1.2`)
- Keep `android:allowBackup="false"` for strict zero-cloud privacy while giving users a 100% offline **Export / Import Backup (`.json`)** option via Android's Storage Access Framework.
- Back up and restore 7-day/rolling Prayer Habits logs, Ramadan fasting records, Qadha counters, Hadith bookmarks, Tasbih/Salawat counts, and alarm/theme preferences across device upgrades.

### 3. App-Wide Multi-Language Localization — Urdu & Sindhi (`v1.3`)
- Building on the Urdu & Sindhi Hadith Hub foundation in `v1.1.0`, extract remaining UI, notification, and widget strings into Android string resources (`strings.xml`).
- Add full **Urdu (`ur`)** and **Sindhi (`sd`)** translations across all prayer, companion, and settings screens with clean RTL layout verification, especially tailored for users across Larkana Division and Sindh.
