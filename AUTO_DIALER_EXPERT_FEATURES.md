# Auto Dialer Expert — Comprehensive Feature Analysis & Specification Document

> **App Overview:**  
> **Package Name:** `ru.lithiums.autodialer2`  
> **App Title:** Auto Dialer Expert  
> **Architecture:** Modern Android Native Application with Room SQLite Database, InCallService Telephony Integration, Floating Window Overlay Service, and Multi-SIM Dual-Mode Execution.

---

## Table of Contents
1. [Core Architecture & Operational Modes](#1-core-architecture--operational-modes)
2. [Mode 1: Single Number Auto Redial (`AUTO REDIAL`)](#2-mode-1-single-number-auto-redial-auto-redial)
3. [Mode 2: Multi-Number Sequential Auto Redial (`AUTO REDIAL LIST`)](#3-mode-2-multi-number-sequential-auto-redial-auto-redial-list)
4. [In-Call Screen & Real-Time Call Management (`CallActivity`)](#4-in-call-screen--real-time-call-management-callactivity)
5. [Floating Overlay Widget (`OverlayService`)](#5-floating-overlay-widget-overlayservice)
6. [System Preferences & Customizations (`SettingsActivity`)](#6-system-preferences--customizations-settingsactivity)
7. [Database Schema & Data Model](#7-database-schema--data-model)
8. [Telephony, Permissions & System Integration](#8-telephony-permissions--system-integration)
9. [Monetization & Ad Integration](#9-monetization--ad-integration)
10. [Recommendations & Enhancements for Your New Custom App](#10-recommendations--enhancements-for-your-new-custom-app)

---

## 1. Core Architecture & Operational Modes

The app is built around two primary execution modes accessed via a top horizontal tab bar (`ViewPager` with `TabLayout`):

1. **AUTO REDIAL (Single Target Mode):** Continually dials a single specified phone number with defined retries, delay intervals, call duration limits, and DTMF extension sequences.
2. **AUTO REDIAL LIST (Campaign Queue Mode):** Sequentially dials a list/queue of contacts stored in a Room database (`calltask_table_4`), maintaining state, repetition counters, specific SIM rules, and color-coded tags.

---

## 2. Mode 1: Single Number Auto Redial (`AUTO REDIAL`)

### 2.1 SIM Card Selection
- **Multi-SIM Support:** Dropdown spinner (`spinnerSimSelection`) to select **SIM 1** or **SIM 2** dynamically.
- Auto-detects dual-SIM capability on the device (`is_dual_sim_phone_true_or_not`).

### 2.2 Phone Number Entry & Contact Picker
- **Input Field:** Direct phone number edit box with text validation.
- **Contact Picker Integration:** Contact button (`imagebuttonAddFromContact`) opening system contact selector (`android.intent.action.PICK` for `vnd.android.cursor.dir/phone_v2`).
- **Country Identification:** Automatically parses international phone prefixes and displays country name (`textViewCountryOfNumber`).

### 2.3 DTMF & Extension Dialing
- **Extension Number Checkbox:** Enable/disable dialing DTMF extension digits after call connection.
- **Pre-Dial Delay:** Configurable pause/delay before dialing extension digits (in seconds).
- **Inter-Digit Delay:** Configurable interval/delay between individual extension digits.
- **Extension Method Selector (`spinnerDialerMethod`):**
  - *Dialing with parameters:* Standard automated redialing with interval and duration limits.
  - *Dialing extension:* Automatic DTMF tone injection upon call pickup.

### 2.4 Call Control Parameters
- **Interval Between Calls:** Precise countdown timer (in seconds) between call attempts (adjusted via increment/decrement arrows or direct number entry). Default: 3 seconds.
- **Number of Calls / Repetitions:** Sets maximum call attempts (up to 1,000,000,001 attempts or infinite loop).
- **Limit Call Duration Switch:** Master toggle (`swDuration1`) to automatically hang up calls after a set duration.
- **Call Duration Timer:** Configurable call timer (e.g. 8 seconds, 15 seconds, 30 seconds).
- **Dialing Time Inclusion Checkbox (`chkDurationTimeWithBeeps`):**
  - *Checked:* Duration countdown begins immediately when pressing "START DIALING" (includes ringback/dialing time).
  - *Unchecked:* Duration countdown starts only after the remote party answers/picks up the call.
- **Start / Stop Action Button:** Large bottom action button (`startCall`) to begin or abort dialing.

---

## 3. Mode 2: Multi-Number Sequential Auto Redial (`AUTO REDIAL LIST`)

### 3.1 Call Campaign Queue
- Displays a structured list of contacts from Room database (`calltask_table_4`).
- **Queue Controls:** Play/Start All, Pause Execution, Skip Current, Reset Status.

### 3.2 Add & Edit Contact Items (`ManuallyAddToCallListActivity` & `EditItemInCallList`)
Each entry in the list contains detailed custom parameters:
1. **Target Phone Number:** Primary destination number.
2. **SIM Assignment:** Specific SIM card (SIM 1 / SIM 2) assigned to this particular number.
3. **Repetitions Count:** Number of retry attempts for this specific contact.
4. **Custom Call Duration:** Individual call duration limit override for this contact.
5. **Extension Digits & Delay:** Custom DTMF extension sequence and initial delay.
6. **Active Status Toggle (`status`):** Enable/disable individual contact without deleting it (1 = Active, 0 = Paused/Disabled).
7. **Custom Comments:** Free-form text notes for each contact.
8. **Sticky Badge / Tag (`stick_text` & `stick_color`):** Color-coded labels (e.g., "VIP", "Follow Up", "Urgent").

### 3.3 Data Import & Export Features
- **CSV Export (`exportToCSV`):** Export call queue to a CSV file for backup or reporting.
- **CSV / File Import:** Batch import numbers, comments, and parameters from CSV files.
- **Reordering:** Reorder queue entries manually (`order_field`).

---

## 4. In-Call Screen & Real-Time Call Management (`CallActivity`)

When an automated call is active, `CallActivity` provides a full custom In-Call UI:

### 4.1 Visual Interface
- **Contact Details:** Contact Name, Phone Number, Profile Photo, Country Name & Flag.
- **Call Status Header:** Shows real-time state: *Dialing*, *Ringing*, *Connected*, *Disconnected*, *Waiting for next interval*.
- **Live Chronometer:** Live elapsed call timer and duration limit countdown.

### 4.2 Interactive Controls
- **Speakerphone Toggle:** Auto-activates or manually toggles phone speaker (`EXTRA_SPEAKERPHONE`).
- **Microphone Mute:** Mute/unmute microphone (`EXTRA_MICROPHONE`).
- **In-Call DTMF Keypad:** Animated slide-up/slide-down dialpad (`dialpad`, `slide_up`, `slide_down`) for manually entering DTMF codes during call.
- **Hang Up / Abort:** Large prominent red end-call button.
- **Pause Campaign Button (`CallActivityCallByList`):** Temporarily pause list execution mid-campaign.

### 4.3 Hardware Interop
- **Proximity Sensor Integration (`SensorManager`):** Automatically turns screen off when phone is moved near ear to prevent accidental touches.
- **WakeLock Management (`PowerManager`):** Keeps CPU awake during automated redial intervals.

---

## 5. Floating Overlay Widget (`OverlayService`)

- **System Alert Window (`SYSTEM_ALERT_WINDOW`):** Displays a floating bubble/overlay on top of other apps or standard Android phone UI.
- **Draggable Window (`onTouch`):** Touch and drag overlay anywhere on screen.
- **Real-Time Timer Display:** Shows current call countdown timer even when the main app is minimized.
- **Embedded Ad Container (`layoutAdvertisement_OW`):** Shows banner ads within floating overlay window.

---

## 6. System Preferences & Customizations (`SettingsActivity`)

Accessible via the top overflow menu (3 dots):

### 6.1 Call & Audio Defaults
- Default SIM preference.
- Auto-enable Speakerphone on call start (`is_speakerphone_enabled_`).
- Delay before DTMF extension dialing (`DELAY_BEFORE_DIAL_EXTENSION_NUMBER__`).
- Delay between DTMF digits (`DELAY_BETWEEN_DIGITS_IN_EXTENSION_NUMBER`).

### 6.2 System & Dialer Integration
- Request to become Android Default Dialer (`CHANGE_DEFAULT_DIALER`).
- Draw Over Other Apps Overlay permission request.

### 6.3 Haptic & Audio Feedback
- Vibration on call connection/disconnection.
- Audio beep notifications before hanging up.

### 6.4 Developer & Debug Tools (`DebugActivity`)
- In-app debug logger toggle (`switchIsEnableDebug`).
- Version & Build details (`textviewVersion`).
- Ad & billing diagnostics.

---

## 7. Database Schema & Data Model

The app uses **Room Persistence Library** with two main database files:

### 7.1 Call Tasks Database (`calltask_table_4`)
| Column Name | Data Type | Description |
| :--- | :--- | :--- |
| `_id` | INTEGER PK AUTOINCREMENT | Unique ID |
| `numberB` | TEXT NOT NULL | Target Phone Number |
| `order_field` | INTEGER NOT NULL | Sort order in list queue |
| `repetitions` | TEXT NOT NULL | Repeat call count |
| `duration` | TEXT NOT NULL | Max call duration limit (sec) |
| `sim_id` | TEXT NOT NULL | Preferred SIM slot (1 or 2) |
| `status` | TEXT NOT NULL | Active status (1 = Active, 0 = Paused) |
| `ext_number` | TEXT NOT NULL | DTMF Extension digits |
| `ext_nameB` | TEXT NOT NULL | Extension label |
| `comments` | TEXT NOT NULL | Custom contact comment |
| `stick_text` | TEXT NOT NULL | Sticky badge text |
| `stick_color` | TEXT NOT NULL | Sticky badge hex color |
| `ext_num_del_before_` | TEXT NOT NULL | Delay before extension dialing |
| `is_stop_timer_` | TEXT NOT NULL | Stop timer on answer rule |

### 7.2 Billing & Purchases Database (`autodialer2_purchase_db`)
- `full_premium_version_3`: Full ad-free premium entitlement.
- `version_without_ads_3`: Ad-free version entitlement.
- `premium_version_with_ads_3`: Discounted premium tier.
- `purchase_table`: Cached Play Store receipt data.

---

## 8. Telephony, Permissions & System Integration

### Required Manifest Permissions
- `android.permission.CALL_PHONE` — Direct call dialing.
- `android.permission.ANSWER_PHONE_CALLS` — Auto-hangup and call state detection.
- `android.permission.READ_PHONE_STATE` — Telephony state monitoring.
- `android.permission.READ_CONTACTS` — Access phonebook.
- `android.permission.SYSTEM_ALERT_WINDOW` — Floating overlay window.
- `android.permission.MODIFY_AUDIO_SETTINGS` — Speakerphone & mute control.
- `android.permission.FOREGROUND_SERVICE` — Background dialing execution.
- `android.permission.WAKE_LOCK` — Screen & CPU awake management.

---

## 9. Monetization & Ad Integration

- **Google AdMob:** Banner Ads, Interstitial Ads, Rewarded Video Ads.
- **In-App Purchases:** Google Play Billing Library v3.0+.
- **Remote Config:** Dynamically adjusts ad frequency via Firebase Remote Config (`admob_autodialer2_interstitial_1_value`).

---

## 10. Recommendations & Enhancements for Your New Custom App

When developing your new upgraded Auto Dialer app, here are key improvements and custom features to add:

### 🚀 Modern Tech Stack Upgrades
1. **Jetpack Compose UI:** Replace legacy XML layouts and ViewPager with modern declarative Jetpack Compose UI with Material 3 design.
2. **Modern Telecom / InCallService API:** Use modern Android Telecom framework APIs for seamless Android 12 to 16 compatibility.
3. **Kotlin Coroutines & Flow:** Replaces legacy thread handlers and CountDownTimers with reactive Kotlin Flow timers.

### ✨ Recommended New Custom Features to Add
1. **WhatsApp & WhatsApp Business Auto Direct Messaging:** Option to send an automated WhatsApp message / follow-up SMS if a call is unanswered or rejected.
2. **Call Outcome Tagging:** After each call, show a 1-tap outcome popup: *Answered*, *Busy*, *No Answer*, *Wrong Number*, *Follow Up Scheduled*.
3. **Analytics & Call Reporting Dashboard:** Charts showing total calls made, total duration, call success rate, and daily activity logs.
4. **Smart Call Scheduling:** Schedule auto-dialing campaigns to start automatically at specific dates and times.
5. **Excel (.xlsx) & Google Sheets Live Sync:** Direct sync with Google Sheets or Excel files in addition to CSV import/export.
6. **AI Voice Note / Call Note Transcription:** Record call notes or voice summaries automatically saved to contact entries.
