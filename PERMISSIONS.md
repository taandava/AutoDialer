# Permissions Documentation

## Declared Manifest Permissions & Usage Rationale

| Permission Name | Category | Purpose / Usage |
|---|---|---|
| `android.permission.CALL_PHONE` | Telephony | Required to place automated outgoing phone calls |
| `android.permission.READ_PHONE_STATE` | Telephony | Required to detect call status (ringing, connected, disconnected) |
| `android.permission.ANSWER_PHONE_CALLS` | Telephony | Required for `TelecomManager.endCall()` auto-hangup duration limits |
| `android.permission.MANAGE_OWN_CALLS` | Telephony | Required for Android 14+ Foreground Service `phoneCall` type compliance |
| `android.permission.READ_CONTACTS` | Contacts | Required to pick contacts from phonebook for campaign queues |
| `android.permission.SCHEDULE_EXACT_ALARM` | Scheduling | Required for `AlarmManager.setAlarmClock()` exact trigger timing |
| `android.permission.USE_EXACT_ALARM` | Scheduling | Standard exact alarm permission for scheduled tasks |
| `android.permission.FOREGROUND_SERVICE` | Background | Required for running active dialing loop as foreground service |
| `android.permission.FOREGROUND_SERVICE_PHONE_CALL` | Background | Android 14+ foreground service type for call operations |
| `android.permission.FOREGROUND_SERVICE_SPECIAL_USE` | Background | Fallback foreground service type for background execution |
| `android.permission.POST_NOTIFICATIONS` | Notifications | Required to display active dialing notification on Android 13+ |
| `android.permission.RECEIVE_BOOT_COMPLETED` | System | Re-arms scheduled campaigns automatically when device reboots |
| `android.permission.WAKE_LOCK` | System | Wakes CPU from Doze mode when scheduled alarms trigger |
| `android.permission.SYSTEM_ALERT_WINDOW` | System | Allows overlay display for dialing status when app is in background |
