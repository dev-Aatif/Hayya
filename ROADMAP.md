# Hayya Roadmap

This document tracks known issues and the upcoming feature roadmap for **Hayya**.

> **Current Release (`1.1.1`):** Offline-first prayer times, Doze-resilient exact alarms, Mosque Auto-Silent, Companion tools, 5 Glance AppWidgets, Multilingual Hadith Hub, and fail-closed GitHub Releases In-App Updater — calibrated for **Pakistan (Larkana Division)** with regional Hijri moonsighting offset (`-2d` to `+2d`).

---

## Resolved Issues (`v1.1.1`)

1. **Alarm Notification & Foreground Service Isolation** *(Fixed)*
   - `PrayerAudioService` now uses dedicated notification IDs/action `PendingIntent` request codes (`FOREGROUND_NOTIFICATION_ID`), preserves `CHANNEL_MAIN_CALL` / Suhoor wake extras, honors `AudioFocus`, suppresses loud Adhan during active voice/VoIP calls, and scopes `ACTION_PLAYBACK_FINISHED` by notification ID without forcing unnecessary keyguard unlock prompts.
2. **Fail-Closed In-App Updater & Opt-Out Control** *(Fixed)*
   - `AppUpdateEngine` enforces fail-closed SHA-256 verification and strict HTTPS GitHub host allowlisting across redirects and browser fallbacks, alongside a user-facing **Auto-Check Updates on Launch** toggle in Settings.
3. **Background Broadcast ANR, FGS Fallback & Auto-Mute Recovery** *(Fixed)*
   - `SystemClockChangedReceiver` validates exported actions, stays within an 8s `goAsync()`/`WakeLock` budget, falls back to direct status notifications if Android 12–16 disallows background `specialUse` FGS start, and `PrayerAlarmScheduler` clears prior/shifted alarm windows across backward clock jumps. `AutoMuteManager` preserves saved ringer state and surfaces an actionable notification if DND access is revoked.
4. **Widget Fan-Out, Countdown Recomposition & SQLite FTS4 Scaling** *(Fixed)*
   - Removed duplicate `ACTION_PRAYER_DATA_CHANGED` filters from individual widget receivers (eliminating `25x` refresh fan-out), isolated 1-second `countdownState` ticks from `MainActivity` root, added `hadith_fts_map` (`docid` deletes) + safe FTS4/`LIKE` query escaping, sequential collection indexing in `FawazHadithSyncClient`, and resumable HadeethEnc `syncedPages` checkpoints.
5. **True-North Qibla Calibration, Regional Hijri Offset & RTL/Accessibility** *(Fixed)*
   - Added `GeomagneticField` declination compensation + `ON_PAUSE` sensor release in Qibla, `-2..+2` day Hijri moonsighting offset for Pakistan/Larkana Ruet-e-Hilal, shared `ArabicScriptText` (`Noto Naskh Arabic` + RTL) across all Arabic screens/cards, safe canvas clipping/RTL direction in `HadithStatusCardRenderer`, and `48dp`/responsive `320dp` layout improvements.
6. **Post-Adhan Dua Sheet & Lint Polish** *(Fixed)*
   - Resolved `PrayerWakeActivity` Dua sheet dismissal deferral, `PinnedPrayerStatusService` Friday label, `SettingsScreen` dynamic method label, and `ObsoleteSdkInt` (`mipmap-anydpi`).

---

## Known Issues

### Major Issues
- *None identified for `1.1.1`.*

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
