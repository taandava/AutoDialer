package com.autodialer.app.service

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.DisconnectCause
import android.telecom.InCallService
import android.telecom.TelecomManager
import android.telephony.TelephonyManager
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.autodialer.app.data.local.AppLogLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AppInCallService : InCallService() {

    private var durationTimerJob: Job? = null

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

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            super.onStateChanged(call, state)
            activeCallState = state

            val reason = if (state == Call.STATE_DISCONNECTED) {
                resolveDisconnectCause(call.details?.disconnectCause)
            } else {
                when (state) {
                    Call.STATE_DIALING -> "Dialing"
                    Call.STATE_RINGING -> "Ringing"
                    Call.STATE_ACTIVE -> "Connected"
                    Call.STATE_HOLDING -> "On Hold"
                    else -> "State $state"
                }
            }

            lastDisconnectReason = reason
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

    private fun applySpeakerphone(enable: Boolean) {
        try {
            // Priority 1: InCallService native audio route
            setAudioRoute(if (enable) CallAudioState.ROUTE_SPEAKER else CallAudioState.ROUTE_EARPIECE)
        } catch (e: Exception) {
            AppLogLogger.log("AppInCallService", "setAudioRoute error: ${e.message}")
        }

        try {
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
                audioManager.isSpeakerphoneOn = enable
            }
        } catch (e: Exception) {
            AppLogLogger.log("AppInCallService", "AudioManager speaker error: ${e.message}")
        }
    }

    private fun startDurationLimitTimer(durationSeconds: Int) {
        durationTimerJob?.cancel()
        durationTimerJob = CoroutineScope(Dispatchers.Default).launch {
            delay(durationSeconds.seconds)
            AppLogLogger.log("AppInCallService", "Duration limit reached ($durationSeconds sec). Disconnecting call.")
            lastDisconnectReason = "Completed (Duration Limit)"
            disconnectCurrentCall(applicationContext)
        }
    }

    private fun broadcastCallState(state: Int, reason: String) {
        activeCallState = state
        val intent = Intent(ACTION_CALL_STATE_CHANGED).apply {
            putExtra(EXTRA_CALL_STATE, state)
            putExtra(EXTRA_DISCONNECT_REASON, reason)
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }

    private fun resolveDisconnectCause(cause: DisconnectCause?): String {
        if (cause == null) return "Disconnected"
        return when (cause.code) {
            DisconnectCause.BUSY -> "Busy"
            DisconnectCause.CANCELED -> "Cancelled"
            DisconnectCause.MISSED -> "Missed"
            DisconnectCause.REJECTED -> "Rejected"
            DisconnectCause.REMOTE -> "Remote Hung Up"
            DisconnectCause.LOCAL -> "Completed (Duration Limit)"
            DisconnectCause.ERROR -> "Call Failed"
            DisconnectCause.RESTRICTED -> "Call Restricted"
            else -> cause.description?.toString()?.ifBlank { null } ?: "Disconnected"
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
         * 3. ITelephony reflection
         * 4. AudioManager media key event (Headset hook toggle)
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
                        telecomManager?.endCall()
                    }
                } catch (e: Exception) {
                    AppLogLogger.log("AppInCallService", "Tier 2 disconnect failed: ${e.message}")
                }
            }

            // Tier 3: Official InCallService disconnect verification
            if (activeCall != null && activeCallState != Call.STATE_DISCONNECTED) {
                try {
                    activeCall?.reject(false, null)
                } catch (e: Exception) {
                    // Ignore fallback reject failure
                }
            }

            // Tier 4: AudioManager Media Key Fallback
            if (context != null) {
                try {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_HEADSETHOOK))
                    audioManager?.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK))
                } catch (e: Exception) {
                    // Ignore fallback failure
                }
            }
        }
    }
}
