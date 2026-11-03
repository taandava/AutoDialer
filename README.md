# Auto Dialer — Native Android Automated Call Campaign & Scheduling Engine

**Auto Dialer** is a native, modern Android application engineered for executing automated single and bulk phone call campaigns with precise scheduling, dual SIM preference, auto-speakerphone, call duration limits, dynamic dark/light theme switching, and real-time history logging with executive analytics.

---

## 🌟 Key Features

1. **Instant Auto Dialer:**
   - Dial single or bulk comma/newline-separated phone numbers in sequence with custom inter-call delays ($1 - 3600\text{ seconds}$).
   - Repetitions support up to **1,00,000 (1 Lakh)** call retries per contact.
   - Streamlined, focused calling experience with automatic phone number normalization.
   - Live **START DIALING / STOP DIALING** button toggle with real-time countdown status.

2. **Scheduled Call Campaigns:**
   - Schedule single or bulk phone call tasks at exact dates & times using Android's `AlarmManager.setAlarmClock()` API.
   - Flexible Repeat Rules: `Once`, `Hourly`, `Daily`, `Weekly`, `Monthly`, and `Custom Interval` ($15 - 10,000\text{ minutes}$ or custom seconds).
   - **Past Schedule Retention:** Re-enable and re-run any past or completed schedule anytime in the future with 1 tap without wiping contacts.
   - **Contact Queue:** Import contacts from phonebook or CSV files.

3. **Universal Dialer System Compatibility (No Default Dialer Required):**
   - Places calls via `TelecomManager.placeCall` and `Intent.ACTION_CALL` natively through any built-in or third-party phone dialer (Samsung, Google, Truecaller, Xiaomi, OnePlus, etc.).
   - **True Dual-SIM Selection:** Resolves active `PhoneAccountHandle` and hardware slot indices (`simSlot`, `slot`, `com.android.phone.extra.slot`).
   - Multi-tier call disconnect engine (`InCallService`, `TelecomManager.endCall()`, and `AudioManager` media keyevents) enforces call duration auto-hangup on all devices without private SDK reflection.
   - **Modern Audio Routing:** Direct speakerphone control via InCallService `setAudioRoute(ROUTE_SPEAKER)` and Android 12+ `AudioManager.setCommunicationDevice`.

4. **Background & Lock Screen Execution:**
   - `WakeAndCallActivity` turns display screen ON and dismisses keyguard when scheduled calls fire while device is locked.
   - **Periodic Background Heartbeat:** `HeartbeatReceiver` runs every 15 minutes to verify active Room DB schedules and prevent OEM battery optimizers from sleeping background tasks.

5. **Call History & Ledger with Analytics:**
   - Every instant and scheduled call attempt is logged in Room Database (`call_logs`) with timestamps, SIM slot, duration, status, and campaign title.
   - **Real-Time Outcome Tagging:** Dynamically captures actual disconnect causes (`Connected`, `Completed`, `Busy`, `Cancelled`, `Rejected`, `Missed`, `Failed`).
   - **Executive Dashboard:** Live summary metrics for Total Calls, Connected Calls, and Cumulative Talk Time with filter chips.
   - **1-Tap CSV Export & Clear All** support.

6. **Modern Material 3 Expressive Design & Edge-to-Edge:**
   - Refined Orange brand palette (`#E76F00`, `#FFB77A`, warm surfaces `#FDF2EA`, `#201D1A`) with dynamic dark/light theme switching.
   - Adaptive Edge-to-Edge with status bar and navigation bar insets (`statusBarsPadding()`, `navigationBarsPadding()`).
   - **Perfect Adaptive Launcher Icon Suite:** Gold telephone receiver emblem calibrated to 58% of canvas for 100% squircle and circle safe-zone compliance (zero cropping on Samsung One UI, Pixel, and other Android launchers).

---

## 🏗️ Architecture Overview

- **UI Framework:** Jetpack Compose + Material 3 + Navigation Compose
- **State Management:** Activity-scoped & Screen-scoped ViewModel + Kotlin StateFlow
- **Database:** Room Database (`AppDatabase`, `ScheduleDao`, `CallLogDao`)
- **Concurrency:** Kotlin Coroutines + Structured Concurrency + Flow
- **Background Execution:** `AutoDialerService` (Foreground Service) + `ScheduleReceiver` + `HeartbeatReceiver` + `WakeAndCallActivity`
- **Language:** 100% Idiomatic Kotlin

---

## 🛠️ Build & Installation

### Prerequisites
- JDK 17 / 21
- Android SDK 35 (Minimum SDK 26 / Android 8.0)
- Gradle 8.9 / Android Gradle Plugin 8.7.1

### Building Production Release APK
```powershell
.\gradlew.bat assembleRelease
```
The production signed release APK is output to `autodialer-release-signed.apk` in the root folder.

### Installing onto Connected Device via ADB
```powershell
adb install -r autodialer-release-signed.apk
```

---

## 🧪 Running Automated Tests
```powershell
.\gradlew.bat testDebugUnitTest
```

---

## 📄 License
Licensed under Apache License 2.0.
