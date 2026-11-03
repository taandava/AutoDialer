# Security & Privacy Documentation

## 1. Security Principles & Hardening

1. **Explicit Broadcast Protection (`Intent.setPackage`):**
   - Internal broadcasts (such as `ACTION_CALL_STATE_CHANGED`) are explicitly targeted to `packageName` using `setPackage(packageName)`. This prevents broadcast interception and avoids Android 14+ `SecurityException` for non-exported receivers.

2. **Component Component Exposure Control:**
   - Activities and Services not requiring external invocation are explicitly marked `android:exported="false"`.
   - `AppInCallService` is protected with `android:permission="android.permission.BIND_INCALL_SERVICE"`.

3. **Sensitive Data Protection:**
   - Phone numbers and contact names are processed locally on-device and stored in Room Database.
   - Zero telemetry, tracking, or remote data exfiltration.

4. **Runtime Permission Hygiene:**
   - Requests minimal permissions necessary: `CALL_PHONE`, `READ_PHONE_STATE`, `ANSWER_PHONE_CALLS`, `READ_CONTACTS`, `POST_NOTIFICATIONS`.

5. **Key Management:**
   - Production APK is signed with `upload-keystore.jks`.
