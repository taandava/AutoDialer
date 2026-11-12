# Architecture & System Design Documentation

## 1. High-Level System Architecture

The **Auto Dialer** application follows the recommended **Unidirectional Data Flow (UDF)** architecture and clean separation of layers:

```
[ UI Layer (Jetpack Compose M3) ]
          │ (UI Events)
          ▼
[ ViewModel Layer (StateFlow & Coroutines) ]
          │ (Use Cases / Repository Calls)
          ▼
[ Data Layer (ScheduleRepository & CsvRepository) ]
   ├── Local Storage: Room Database (AppDatabase, ScheduleDao, CallLogDao)
   └── Key-Value Prefs: PreferencesManager (SharedPreferences)
          │
          ▼
[ Execution Services (Background & Telecom) ]
   ├── AutoDialerService (Foreground Service executing dialing loop)
   ├── AppInCallService (InCallService for call duration disconnects)
   ├── CallSchedulerManager (AlarmManager.setAlarmClock API)
   ├── ScheduleReceiver & HeartbeatReceiver (Background Alarms & Keep-Alive)
   └── WakeAndCallActivity (Lockscreen screen wake & keyguard bypass)
```

---

## 2. Key Modules & Class Responsibilities

| Class / Component | Responsibilities |
|---|---|
| `MainActivity` | Single activity host, sets up Edge-to-Edge and Navigation Compose graph |
| `MainScreen` | Scaffold container holding TopBar, Bottom Navigation, and NavHost screens |
| `InstantDialerScreen` & `InstantDialerViewModel` | UI & State for single/bulk instant auto-dialing campaigns |
| `ScheduleListScreen` & `AddEditScheduleScreen` & `ScheduleViewModel` | Campaign creation, editing, contact queueing, repeat rules, CSV import/export |
| `CallLedgerScreen` & `CallLedgerViewModel` | Real-time call attempt history log & 1-tap CSV export |
| `SettingsScreen` & `SettingsViewModel` | Default SIM preferences, auto-speakerphone, dark theme toggle |
| `PermissionManagerScreen` | Manages runtime permissions (`CALL_PHONE`, `READ_PHONE_STATE`, `ANSWER_PHONE_CALLS`, `READ_CONTACTS`, `POST_NOTIFICATIONS`) |
| `AutoDialerService` | Foreground service managing the execution loop, delay timers, and notifications |
| `AppInCallService` | Multi-tier call disconnect engine (`InCallService`, `TelecomManager.endCall()`, and `AudioManager` media keyevents) |
| `CallSchedulerManager` | Schedules exact alarms via `AlarmManager.setAlarmClock()` |
| `ScheduleReceiver` & `HeartbeatReceiver` | Receives exact alarms and periodic 15-minute keep-alive pulses |
| `WakeAndCallActivity` | Wakes display screen and bypasses keyguard when device is locked |

---

## 3. Embedded Codebase Packaging Architecture

To enable autonomous app maintenance, code extraction, and update portability, the release APK contains the full, buildable source code packaged directly inside the archive:

```
autodialer-release-signed.apk (ZIP Archive)
   ├── classes.dex / classes2.dex
   ├── res/
   ├── AndroidManifest.xml
   ├── META-INF/ (v2/v3 signature blocks)
   │
   ├── codebase/                     <-- Root Codebase Container
   │     ├── build.gradle.kts
   │     ├── settings.gradle.kts
   │     ├── gradle.properties
   │     ├── gradlew / gradlew.bat
   │     ├── gradle/wrapper/
   │     ├── app/
   │     │    ├── build.gradle.kts
   │     │    ├── proguard-rules.pro
   │     │    ├── src/main/java/com/autodialer/app/...
   │     │    ├── src/main/res/...
   │     │    └── src/test/...
   │     ├── README.md / ARCHITECTURE.md / CHANGELOG.md
   │     └── HOW_TO_BUILD_AND_UPDATE.md
   │
   └── assets/codebase/              <-- Standard Android Asset Container (Mirrored)
```

Extraction is fully transparent: extracting `autodialer-release-signed.apk` yields a ready-to-build Android Studio project inside `codebase/` that can be modified and re-compiled with `./gradlew assembleRelease`.
