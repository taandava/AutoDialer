package com.autodialer.app.service

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.telecom.TelecomManager
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.autodialer.app.data.local.AppLogLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * ================================================================================================
 * WHAT IS IT:
 * Transparent, transient [Activity] specifically designed to turn the device display ON and bypass/
 * dismiss the keyguard lockscreen when a scheduled call campaign fires while the device is asleep.
 *
 * WHAT IT DOES:
 * 1. Configures `setShowWhenLocked(true)` and `setTurnScreenOn(true)` on Android 8.1+ (API 27+).
 * 2. Requests `KeyguardManager.requestDismissKeyguard()` to unlock the screen.
 * 3. Acquires a temporary `PARTIAL_WAKE_LOCK` to ensure the CPU does not sleep during dialing.
 * 4. Resolves the requested SIM card preference via TelecomManager / Intent extras.
 * 5. Places the outbound call via `TelecomManager.placeCall` or `Intent.ACTION_CALL`.
 * 6. Finishes immediately after placing the call.
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Upstream Caller: [ScheduleReceiver] when device keyguard is locked.
 * - System Frameworks:
 *   * [TelecomManager]: Outbound call placement.
 *   * [PowerManager]: Screen wake locks.
 *   * [android.app.KeyguardManager]: Lockscreen dismissal.
 * ================================================================================================
 */
class WakeAndCallActivity : Activity() {

    private val activityScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        AppLogLogger.log("WakeAndCallActivity", "onCreate: Waking screen and displaying over PIN lockscreen...")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? android.app.KeyguardManager
        keyguardManager?.requestDismissKeyguard(this, null)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "AutoDialer:WakeAndCallBrightWakeLock"
            )
            wakeLock.acquire(10 * 1000L)
            AppLogLogger.log("WakeAndCallActivity", "Acquired wake lock for screen wakeup")
        } catch (e: Exception) {
            AppLogLogger.log("WakeAndCallActivity", "WakeLock error: ${e.message}")
        }

        val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: ""
        val simSlot = intent.getIntExtra(EXTRA_SIM_SLOT, 0)
        placeCallAndFinish(phoneNumber, simSlot)
    }

    private fun placeCallAndFinish(phoneNumber: String, simSlot: Int) {
        if (phoneNumber.isBlank()) {
            AppLogLogger.log("WakeAndCallActivity", "Cannot place call: Phone number is blank. Finishing.")
            finish()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            AppLogLogger.log("WakeAndCallActivity", "Permission CALL_PHONE missing. Finishing.")
            finish()
            return
        }

        activityScope.launch {
            try {
                delay(800.milliseconds) // Allow screen illumination and window stabilization

                val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                val uri = "tel:${Uri.encode(phoneNumber)}".toUri()

                val callExtras = AutoDialerService.buildCallExtras(simSlot, this@WakeAndCallActivity)

                var placedSuccessfully = false

                if (telecomManager != null) {
                    try {
                        AppLogLogger.log("WakeAndCallActivity", "Attempting placeCall on TelecomManager (SIM Slot $simSlot)...")
                        telecomManager.placeCall(uri, callExtras)
                        placedSuccessfully = true
                    } catch (e: Exception) {
                        AppLogLogger.log("WakeAndCallActivity", "placeCall failed, attempting ACTION_CALL fallback: ${e.message}")
                    }
                }

                if (!placedSuccessfully) {
                    val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtras(callExtras)
                    }
                    startActivity(callIntent)
                    AppLogLogger.log("WakeAndCallActivity", "Outbound call placed via ACTION_CALL fallback")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogLogger.log("WakeAndCallActivity", "Call placement exception: ${e.message}")
            } finally {
                delay(1.seconds)
                finish()
            }
        }
    }

    companion object {
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_SIM_SLOT = "extra_sim_slot"
    }
}
