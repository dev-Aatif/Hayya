# Changelog

All notable changes to **Hayya** will be documented in this file.

## [1.0.0] — 2026-11-21

Initial V1 release focused on **Pakistan (Larkana Division)** offline verification.

### Added
- **Offline Astronomical Engine**: Pure offline prayer time calculation via Batoul Apps `adhan` (`1.2.1`) and fallback Jean Meeus solar solver (`SolarAstronomicalCalculator`), plus `SindhMosqueSchedule` precautionary windows (Sehri Ihtiyat, Ishraq, Zawal, and seasonal Jama'at estimates).
- **Larkana Division Offline Directory**: Complete offline catalog of 37 towns, talukas, and municipal committees across all 5 districts of Larkana Division (Larkana, Qambar Shahdadkot, Shikarpur, Jacobabad, Kashmore).
- **Unkillable Background Alarm Pipeline**:
  - `AlarmManager.setAlarmClock` exclusive scheduling across a 7-day rolling queue with deterministic request codes.
  - 3-tier alerts: Pre-Reminder (`T - 15m`), Main Call (`T0` with lockscreen `FullScreenIntent`), and Last Call (`T_end - 20m`).
  - `PrayerAudioService` routed on `USAGE_ALARM` using the device's familiar default alarm tone, with gradual volume escalation and smooth coroutine fade-out.
  - `SystemClockChangedReceiver` auto-rescheduling on boot, locked boot, clock/timezone shifts, and app updates.
  - `PinnedPrayerStatusService` optional foreground LMK shield and `OemPowerManager` battery optimization helpers.
- **Mosque Auto-Mute**: Automatically switches ringer mode to Silent/Vibrate during Salah or Friday Khutbah, persists exact prior volumes in DataStore, and restores them automatically.
- **Spiritual Companion Suite**: Live Qibla compass (`QiblaOrientationSensor`), tactile OLED Tasbih counter, Prayer Habits 7-day tracker, Night & Tahajjud calculator, Friday (Jumu'ah) checklist & Salawat counter, and Ramadan & Eid Hub (Suhoor/Iftar tracker, 30-day timetable, Sunnah Duas).
- **5 Jetpack Glance Home Screen Widgets**: Compact Next Prayer (`2x2`), Full Timetable (`4x2`), Interactive Daily Prayers (`4x2`), Quick Strip (`4x1`), and Night & Fasting (`3x2`).
- **Release Hardening**: R8 minification, resource shrinking, and release APK signing configuration.
