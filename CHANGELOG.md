# Changelog

All notable changes to **Hayya** will be documented in this file.

## [1.1.0] — 2026-11-26

Feature release adding the multilingual offline-first **Hadith Hub**, **GitHub Releases In-App Updater**, and V1 polish fixes.

### Added
- **Hadith Hub (`HadithScreen`, `HadithViewModel`, `HadithDatabase`)**:
  - Dedicated `Hadith` tab in the docked 4-tab navigation bar (`DockedNavBar`) with Noto Naskh Arabic script rendering (`noto_naskh_arabic.ttf`).
  - Curated offline starter pack (`HadithStarterPack`) across 5 spiritual pillars (Prayer & Worship, Creed, Virtues, Manners, Quran & Tafsir) in Arabic, English, Urdu, and Sindhi.
  - Local SQLite + FTS4 full-text search, bookmarks, read-state tracking, and deterministic daily Hadith selection (`DailyHadithSelector`).
  - On-demand thematic sync via HadeethEnc.com (`HadeethEncSyncClient`) with non-legal spiritual topic filtering (`PriorityTopicFilter`), plus full canonical book downloads (`FawazHadithSyncClient` for 40 Nawawi, 40 Qudsi, Sahih al-Bukhari, Sahih Muslim, Tirmidhi, Abu Dawud, Nasa'i, Ibn Majah, and Muwatta Malik).
  - WhatsApp Status (`9:16`) & Square (`1:1`) shareable image card renderer (`HadithStatusCardRenderer`) exported via `FileProvider`.
  - Daily Sunrise Hadith notification scheduled via `PrayerAlarmScheduler` and dispatched by `PrayerAlarmReceiver`.
- **In-App GitHub Releases Updater (`AppUpdateEngine`, `UpdateComponents`)**:
  - Automatic launch check (6-hour throttle) and manual check in Settings against GitHub Releases (`releases/latest`), with dashboard banner, release notes modal, and notification.
  - HTTPS APK download with fail-closed SHA-256 checksum verification (`.apk.sha256`) and Android package installer handoff (`REQUEST_INSTALL_PACKAGES` + `FileProvider`).

### Fixed
- **Post-Adhan Dua Sheet Lifecycle**: `PrayerWakeActivity` now defers `finish()` on `ACTION_PLAYBACK_FINISHED` while the **Dua After Adhan** bottom sheet is open and finishes cleanly upon dismissal.
- **Pinned Status Notification Polish**: `PinnedPrayerStatusService` now formats Friday Dhuhr as `JUMU'AH`, refreshes via `ACTION_REFRESH` without flashing `"Calculating upcoming prayer window..."`, and `MainActivity` guards `stop()` with `isRunning`.
- **Dynamic Calculation Authority Label**: `SettingsScreen` now binds `userPreferences.calculationMethod.displayName` instead of a static label.
- **Lint & Code Quality**: Resolved `ModifierParameter`, `ObsoleteSdkInt`, and `DisableBaselineAlignment` Android Lint warnings across Kotlin screens, services, icons, and widget preview XML layouts.

## [1.0.0] — 2026-11-21

Initial V1 release focused on **Pakistan (Larkana Division)** offline verification.

### Added
- **Offline Astronomical Engine**: Pure offline prayer time calculation via Batoul Apps `adhan` (`1.2.1`) and fallback Jean Meeus solar solver (`SolarAstronomicalCalculator`), plus `SindhMosqueSchedule` precautionary windows (Sehri Ihtiyat, Ishraq, Zawal, and seasonal Jama'at estimates).
- **Larkana Division Offline Directory**: Complete offline catalog of 37 towns, talukas, and municipal committees across all 5 districts of Larkana Division (Larkana, Qambar Shahdadkot, Shikarpur, Jacobabad, Kashmore).
- **Unkillable Background Alarm Pipeline**:
  - `AlarmManager.setAlarmClock` exclusive scheduling across a 7-day rolling queue with deterministic request codes.
  - 3-tier alerts: Pre-Reminder (`T - 15m`), Main Call (`T0` with lockscreen `FullScreenIntent`), and Last Call (`T_end - 20m`).
  - `PrayerAudioService` routed on `USAGE_ALARM` using the device's familiar default alarm tone, with gradual volume escalation and smooth coroutine fade-out.
  - `SystemClockChangedReceiver` auto-rescheduling on boot, clock/timezone shifts, and app updates.
  - `PinnedPrayerStatusService` optional foreground LMK shield and `OemPowerManager` battery optimization helpers.
- **Mosque Auto-Mute**: Automatically switches ringer mode to Silent/Vibrate during Salah or Friday Khutbah, persists exact prior volumes in DataStore, and restores them automatically.
- **Spiritual Companion Suite**: Live Qibla compass (`QiblaOrientationSensor`), tactile OLED Tasbih counter, Prayer Habits 7-day tracker, Night & Tahajjud calculator, Friday (Jumu'ah) checklist & Salawat counter, and Ramadan & Eid Hub (Suhoor/Iftar tracker, 30-day timetable, Sunnah Duas).
- **5 Jetpack Glance Home Screen Widgets**: Compact Next Prayer (`2x2`), Full Timetable (`4x2`), Interactive Daily Prayers (`4x2`), Quick Strip (`4x1`), and Night & Fasting (`3x2`).
- **Release Hardening**: R8 minification, resource shrinking, and release APK signing configuration.
