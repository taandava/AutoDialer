package com.autodialer.app.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.os.OutcomeReceiver
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.CallEndpointException
import android.telecom.DisconnectCause
import android.telecom.InCallService
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import com.autodialer.app.data.local.AppLogLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * ================================================================================================
 * WHAT IS IT:
 * Custom [InCallService] implementation binding with Android's Telecom framework to monitor live call
 * lifecycle events, enforce call duration limits, control speakerphone routing, and execute
 * multi-tier call disconnects.
 *
 * WHAT IT DOES:
 * 1. Receives callbacks when an outbound call is added ([onCallAdded]) or removed ([onCallRemoved]).
 * 2. Emits local broadcasts (`ACTION_CALL_STATE_CHANGED`) notifying [AutoDialerService] of state
 *    transitions (DIALING, RINGING, ACTIVE, DISCONNECTED).
 * 3. Enforces call duration auto-hangup: starts a coroutine timer when call connects and disconnects
 *    the call upon timer expiration.
 * 4. Manages modern audio routing: routes audio to the built-in speakerphone using native InCallService
 *    `setAudioRoute(ROUTE_SPEAKER)` and Android 12+ `AudioManager.setCommunicationDevice`.
 * 5. Provides a multi-tier fallback call disconnection engine ([disconnectCurrentCall]):
 *    - Tier 1: InCallService `Call.disconnect()`
 *    - Tier 2: `TelecomManager.endCall()` (Android 9+)
 *    - Tier 3: `AudioManager` media hook key event (HEADSETHOOK toggle)
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - System Framework: [InCallService], [TelecomManager], [AudioManager]
 * - Companion Engine: [AutoDialerService] (listens to call state broadcasts)
 * - Diagnostics: [AppLogLogger]
 * ================================================================================================
 */
class AppInCallService : InCallService() {

    private var durationTimerJob: Job? = null
    private val availableEndpoints = CopyOnWriteArrayList<CallEndpoint>()

    /**
     * Android 14+ (API 34+) callback invoked when available call audio endpoints change.
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    override fun onAvailableCallEndpointsChanged(endpoints: List<CallEndpoint>) {
        super.onAvailableCallEndpointsChanged(endpoints)
        availableEndpoints.clear()
        availableEndpoints.addAll(endpoints)
        AppLogLogger.log("AppInCallService", "Available CallEndpoints updated: ${endpoints.size} endpoints")
    }

    /**
     * Called by Telecom subsystem when an outbound call is created and registered.
     */
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        activeCall = call
        activeCallState = Call.STATE_DIALING
        lastDisconnectReason = "Dialing"

        AppLogLogger.log("AppInCallService", "onCallAdded: New call detected")

        if (activeSpeakerphone) {
            applySpeakerphone(true)
        }

        call.registerCallback(callCallback)
        broadcastCallState(Call.STATE_DIALING, "Dialing")
    }

    /**
     * Called by Telecom subsystem when an active or dialing call is terminated.
     */
    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        call.unregisterCallback(callCallback)
        durationTimerJob?.cancel()
        durationTimerJob = null
        activeCall = null
        activeCallState = Call.STATE_DISCONNECTED

        val reason = resolveDisconnectCause(call.details?.disconnectCause)
        lastDisconnectReason = reason
        AppLogLogger.log("AppInCallService", "onCallRemoved: Call disconnected. Reason: $reason")
        broadcastCallState(Call.STATE_DISCONNECTED, reason)
    }

    /**
     * Telecom callback monitoring real-time in-call state changes.
     */
    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            activeCallState = state

            val reason = resolveDisconnectCause(call.details?.disconnectCause)
            AppLogLogger.log("AppInCallService", "Call state changed: $state ($reason)")
            broadcastCallState(state, reason)

            if (state == Call.STATE_ACTIVE) {
                if (activeSpeakerphone) {
                    applySpeakerphone(true)
                }

                // If includeDialingTime was false, start duration timer upon connection
                if (!activeIncludeDialingTime && activeDurationLimitSeconds > 0) {
                    startDurationLimitTimer(activeDurationLimitSeconds)
                }
            } else if (state == Call.STATE_DISCONNECTED) {
                durationTimerJob?.cancel()
            }
        }
    }

    /**
     * Configures audio output to built-in speaker or earpiece using modern platform APIs
     * with comprehensive backwards compatibility across all supported Android versions.
     */
    private fun applySpeakerphone(enable: Boolean) {
        try {
            // Priority 1: InCallService native audio routing
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                routeAudioViaCallEndpoint(enable)
            } else {
                applyLegacyAudioRoute(if (enable) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
            }
        } catch (e: Exception) {
            AppLogLogger.log("AppInCallService", "InCallService audio routing error: ${e.message}")
        }

        try {
            // Priority 2: AudioManager communication device routing
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (enable) {
                    val speakerDevice = audioManager.availableCommunicationDevices.find {
                        it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
                    }
                    if (speakerDevice != null) {
                        audioManager.setCommunicationDevice(speakerDevice)
                    }
                } else {
                    audioManager.clearCommunicationDevice()
                }
            } else {
                applyLegacySpeakerphone(audioManager, enable)
            }
        } catch (e: Exception) {
            AppLogLogger.log("AppInCallService", "AudioManager speaker error: ${e.message}")
        }
    }

    /**
     * Routes call audio using Android 14+ [CallEndpoint] and [requestCallEndpointChange].
     */
    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun routeAudioViaCallEndpoint(enable: Boolean) {
        val targetType = if (enable) CallEndpoint.TYPE_SPEAKER else CallEndpoint.TYPE_EARPIECE
        val targetEndpoint = availableEndpoints.find { it.endpointType == targetType }
        if (targetEndpoint != null) {
            requestCallEndpointChange(
                targetEndpoint,
                ContextCompat.getMainExecutor(this),
                object : OutcomeReceiver<Void, CallEndpointException> {
                    override fun onResult(result: Void?) {
                        AppLogLogger.log("AppInCallService", "CallEndpoint changed successfully to type $targetType")
                    }

                    override fun onError(error: CallEndpointException) {
                        AppLogLogger.log("AppInCallService", "CallEndpoint change failed: ${error.message}, falling back to legacy route")
                        applyLegacyAudioRoute(if (enable) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
                    }
                }
            )
        } else {
            // If endpoints have not populated yet, use legacy route
            applyLegacyAudioRoute(if (enable) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
        }
    }

    /**
     * Backwards-compatibility helper executing legacy [setAudioRoute] on Android versions
     * prior to Android 14 (API 34).
     */
    @Suppress("DEPRECATION")
    private fun applyLegacyAudioRoute(route: Int) {
        setAudioRoute(route)
    }

    /**
     * Backwards-compatibility helper setting [AudioManager.isSpeakerphoneOn] on Android
     * versions prior to Android 12 (API 31), where [AudioManager.setCommunicationDevice]
     * was introduced.
     */
    @Suppress("DEPRECATION")
    private fun applyLegacySpeakerphone(audioManager: AudioManager, enable: Boolean) {
        audioManager.isSpeakerphoneOn = enable
    }

    /**
     * Starts a coroutine timer that automatically terminates the call upon duration expiration.
     */
    private fun startDurationLimitTimer(durationSeconds: Int) {
        durationTimerJob?.cancel()
        durationTimerJob = CoroutineScope(Dispatchers.Default).launch {
            delay(durationSeconds.seconds)
            AppLogLogger.log("AppInCallService", "Duration limit reached ($durationSeconds sec). Disconnecting call.")
            lastDisconnectReason = "Completed (Duration Limit)"
            disconnectCurrentCall(this@AppInCallService)
        }
    }

    /**
     * Sends a local broadcast to notify [AutoDialerService] of call state changes.
     */
    private fun broadcastCallState(state: Int, reason: String) {
        val intent = Intent(ACTION_CALL_STATE_CHANGED).apply {
            setPackage(packageName)
            putExtra(EXTRA_CALL_STATE, state)
            putExtra(EXTRA_DISCONNECT_REASON, reason)
        }
        sendBroadcast(intent)
    }

    /**
     * Translates Telecom [DisconnectCause] codes into human-readable strings.
     */
    private fun resolveDisconnectCause(cause: DisconnectCause?): String {
        if (cause == null) return "Unknown"
        return when (cause.code) {
            DisconnectCause.BUSY -> "Busy"
            DisconnectCause.CANCELED -> "Cancelled"
            DisconnectCause.LOCAL -> "Completed (Local Hangup)"
            DisconnectCause.REMOTE -> "Completed (Remote Hangup)"
            DisconnectCause.REJECTED -> "Rejected"
            DisconnectCause.MISSED -> "Missed"
            DisconnectCause.ERROR -> "Failed (Error)"
            DisconnectCause.RESTRICTED -> "Restricted"
            else -> cause.description?.toString() ?: "Completed"
        }
    }

    companion object {
        const val ACTION_CALL_STATE_CHANGED = "com.autodialer.app.ACTION_CALL_STATE_CHANGED"
        const val EXTRA_CALL_STATE = "extra_call_state"
        const val EXTRA_DISCONNECT_REASON = "extra_disconnect_reason"

        @Volatile
        var activeCall: Call? = null

        @Volatile
        var activeCallState: Int = Call.STATE_DISCONNECTED

        @Volatile
        var lastDisconnectReason: String = "Idle"

        @Volatile
        var activeSpeakerphone: Boolean = false

        @Volatile
        var activeDurationLimitSeconds: Int = 10

        @Volatile
        var activeIncludeDialingTime: Boolean = true

        /**
         * Multi-tier call disconnect engine:
         * 1. InCallService Call.disconnect()
         * 2. TelecomManager.endCall() [Android 9+]
         * 3. AudioManager media key event (Headset hook toggle)
         */
        fun disconnectCurrentCall(context: Context? = null) {
            AppLogLogger.log("AppInCallService", "Executing multi-tier call disconnect...")

            // Tier 1: InCallService Call.disconnect()
            try {
                activeCall?.disconnect()
            } catch (e: Exception) {
                AppLogLogger.log("AppInCallService", "Tier 1 disconnect failed: ${e.message}")
            }

            // Tier 2: TelecomManager.endCall()
            if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                try {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ANSWER_PHONE_CALLS
                    ) == PackageManager.PERMISSION_GRANTED

                    if (hasPermission) {
                        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                        if (telecomManager != null) {
                            invokeLegacyTelecomEndCall(telecomManager)
                            AppLogLogger.log("AppInCallService", "Tier 2 TelecomManager.endCall() invoked successfully")
                        }
                    }
                } catch (e: Exception) {
                    AppLogLogger.log("AppInCallService", "Tier 2 TelecomManager.endCall() failed: ${e.message}")
                }
            }

            // Tier 3: Media button event (Headset hook toggle)
            if (context != null) {
                try {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    if (audioManager != null) {
                        val downEvent = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK)
                        val upEvent = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK)
                        audioManager.dispatchMediaKeyEvent(downEvent)
                        audioManager.dispatchMediaKeyEvent(upEvent)
                        AppLogLogger.log("AppInCallService", "Tier 3 AudioManager media keyevent dispatched")
                    }
                } catch (e: Exception) {
                    AppLogLogger.log("AppInCallService", "Tier 3 media keyevent failed: ${e.message}")
                }
            }
        }

        /**
         * Backwards-compatibility fallback invoking [TelecomManager.endCall] on Android 9+ (API 28+)
         * when the primary [InCallService] [Call] object is unavailable or unbound.
         * Deprecated in API 29 by Google in favor of [InCallService.Call.disconnect], which is
         * utilized as Tier 1 above.
         */
        @Suppress("DEPRECATION")
        private fun invokeLegacyTelecomEndCall(telecomManager: TelecomManager): Boolean {
            return telecomManager.endCall()
        }
    }
}
