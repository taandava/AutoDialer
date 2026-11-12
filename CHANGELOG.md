# Changelog

All notable changes to the **Auto Dialer** project are documented in this file.

## [1.3.0] - 2026-11-12

### Added
- **Embedded Self-Contained Codebase in Release APK:** Configured automated packaging (`package_codebase_to_apk.py` & `package_codebase.bat`) to bundle the complete, buildable Android Studio project source tree directly into the APK under `codebase/` and `assets/codebase/`. Extracting the APK with any zip utility provides instant access to all source files, Gradle scripts, resources, and tests for portable app updates.
- **Architectural KDoc Documentation Across All Codeblocks:** Comprehensive, professional KDoc headers and block comments added to every class, entity, DAO, repository, service, receiver, ViewModel, and Composable detailing identity (What is it), behavior (What it does), and system/subsystem links (What it is linked with).
- **Extraction Guide:** Added `HOW_TO_BUILD_AND_UPDATE.md` inside the embedded codebase folder.

### Fixed
- **Dynamic System Bar Contrast Synchronization:** Resolved conflict where switching to Light Theme rendered status bar icons and navigation buttons invisible. Added `SideEffect` in `AutoDialerTheme` dynamically updating `WindowInsetsControllerCompat.isAppearanceLightStatusBars = !darkTheme` and `isAppearanceLightNavigationBars = !darkTheme`, ensuring dark icons on light surfaces and light icons on dark surfaces.
- **Edge-to-Edge Surface & TopBar Inset Architecture:** Refactored top bar and bottom bar surfaces across `MainScreen`, `CallLedgerScreen`, `AddEditScheduleScreen`, and `PermissionManagerScreen`. Surfaces now extend cleanly behind status and navigation bars while padding internal row content, preventing camera cutout crowding and ensuring navigation buttons sit directly over high-contrast theme surfaces.
- **JDK 21 Environment Integration:** Updated `gradle.properties` and Windows environment configuration to point to JDK 21 (`jdk-21.0.13.9-hotspot`), ensuring seamless command-line build execution without path conflicts.
- **Static Telephony Helper Refactoring:** Refactored `AutoDialerService.buildCallExtras()` to a public companion object method with context pass-through, eliminating code duplication across `AutoDialerService` and `WakeAndCallActivity`.

## [1.2.0] - 2026-11-03

### Removed
- **Extension Dialing (DTMF) Elimination:** Completely removed DTMF extension dialing, tone injection, pre-delay steppers, and related UI components across `InstantDialerScreen`, `InstantDialerViewModel`, `AutoDialerService`, `AppInCallService`, and tests for a streamlined, focused calling experience.

### Fixed
- **Settings Theme Toggle:** Fixed theme toggle reactivity in Settings screen where toggling Dark Mode did not change the app theme. Connected `MainActivity`'s root-level `SettingsViewModel` directly to `MainScreen` and `SettingsScreen`, ensuring instant recomposition of `AutoDialerTheme` upon toggle.
- **Adaptive App Icon Cropping (Samsung One UI & Modern Launchers):** Fixed icon edge cropping where the circular gold medallion was clipped by squircle masks on Samsung devices. Scaled adaptive foreground assets to 58% of canvas (well within the 66dp Android safe zone) and calibrated legacy mipmaps to 83.3% diameter with safe keyline margins across all densities (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`).
- **Trailing UI Divider Cleanup:** Removed redundant horizontal dividers in instant dialer cards following DTMF removal.

## [1.1.0] - 2026-11-03

### Fixed
- **Schedule Repeat Rules Compilation & Execution:** Added full enum definitions (`HOURLY`, `DAILY`, `WEEKLY`, `MONTHLY`, `REPEAT_INTERVAL`) and implemented exact calendar arithmetic in `CallSchedulerManager.calculateNextRunTime()`.
- **Database Contact Deletion Bug:** Fixed critical data loss bug in `ScheduleViewModel.toggleSchedule()` where toggling a schedule enabled/disabled called `saveSchedule(..., emptyList())` and wiped out all associated contacts. Implemented `updateScheduleOnly()` in `ScheduleRepository` and `ScheduleDao`.
- **Multi-SIM Selection Pipeline:** Fixed issue where selected SIM slot was ignored due to empty `extras` bundle. Implemented `buildCallExtras(simSlot)` resolving `TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE`, plus OEM fallback keys (`simSlot`, `slot`, `com.android.phone.extra.slot`, `SLOT_INDEX`) across `AutoDialerService` and `WakeAndCallActivity`.
- **Modern Audio Routing & Speakerphone:** Migrated speakerphone control from deprecated `AudioManager.isSpeakerphoneOn` to InCallService native `setAudioRoute(CallAudioState.ROUTE_SPEAKER)` and Android 12+ `AudioManager.setCommunicationDevice(AudioDeviceInfo.TYPE_BUILTIN_SPEAKER)`.
- **Multi-Tier Disconnect Engine:** Implemented 4-tier fallback disconnect (`InCallService Call.disconnect()`, `TelecomManager.endCall()`, and `AudioManager` media headset-hook key event) without private SDK reflection.
- **Call Outcome Tagging:** Replaced static "Dialed" ledger entries with dynamic tracking of real disconnect causes (`DisconnectCause.BUSY`, `CANCELED`, `MISSED`, `REJECTED`, `REMOTE`, `LOCAL`, `ERROR`) and exact elapsed talk durations.
- **Icons Deprecation:** Replaced deprecated `Icons.Default.OpenInNew` with `Icons.AutoMirrored.Filled.OpenInNew`.

### Added
- **Material 3 Expressive Orange Theme:** Complete Brand Orange palette (`#E76F00`, `#FFB77A`, warm light surface `#FDF2EA`, dark container `#201D1A`) with dynamic color support complying with `design.md`.
- **Bulk Number Input & Parsing in Instant Mode:** Support for comma, newline, and semicolon separated numbers with automatic sanitization and batch dial loop.
- **Executive Call Ledger Analytics:** Live summary dashboard in Call History Ledger displaying Total Calls, Connected Calls, and Cumulative Talk Time with outcome filter chips.
- **Comprehensive Unit Test Suite:** Expanded unit tests covering repeat rule calculations, contact entities, CSV format parsing, bulk number extraction, and future fast-forwarding logic.

## [1.0.0] - 2026-10-24

### Added
- **Instant Auto Dialer:** Single/bulk auto-dialer with customizable inter-call delay, retries (up to 1,00,000), duration limits, and speakerphone.
- **Scheduled Campaigns:** Exact scheduling via `AlarmManager.setAlarmClock()`, repeat rules (`Once`, `Hourly`, `Daily`, `Weekly`, `Custom Interval`), and contact queueing from Phonebook / CSV.
- **Call History & Ledger:** Room Database logging of all call attempts with SIM slot, duration, status, and 1-tap CSV export.
- **Universal Dialer Compatibility:** Multi-tier call disconnect engine (`InCallService`, `TelecomManager.endCall()`, reflection, media keyevents) supporting any phone dialer without default dialer requirement.
- **Lock Screen Execution:** `WakeAndCallActivity` turns display screen ON and dismisses keyguard when scheduled calls fire while device is locked.
- **Background Heartbeat Keep-Alive:** `HeartbeatReceiver` periodic 15-minute background pulse verifying active Room DB schedules and preventing OEM battery sleep.
- **Modern Jetpack Compose M3 UI:** Edge-to-edge support with `statusBarsPadding()` and `navigationBarsPadding()`.
- **Automated Unit Tests:** 100% passing unit tests covering entities, repeat rules, and repositories.
