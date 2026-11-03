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
            finish()
            return
        }

        activityScope.launch {
            delay(300.milliseconds)
            try {
                val uri = "tel:${Uri.encode(phoneNumber)}".toUri()
                val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                val hasCallPermission = ContextCompat.checkSelfPermission(this@WakeAndCallActivity, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

                val extras = Bundle()
                if (telecomManager != null && ContextCompat.checkSelfPermission(this@WakeAndCallActivity, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                    try {
                        val accounts = telecomManager.callCapablePhoneAccounts
                        if (accounts != null && accounts.size > simSlot) {
                            extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accounts[simSlot])
                        }
                    } catch (e: Exception) {
                        AppLogLogger.log("WakeAndCallActivity", "Error setting PhoneAccountHandle: ${e.message}")
                    }
                }
                extras.putInt("simSlot", simSlot)
                extras.putInt("slot", simSlot)
                extras.putInt("com.android.phone.extra.slot", simSlot)
                extras.putInt("android.telecom.extra.SLOT_INDEX", simSlot)

                if (telecomManager != null && hasCallPermission) {
                    AppLogLogger.log("WakeAndCallActivity", "Direct TelecomManager.placeCall executed over lockscreen for $phoneNumber (SIM ${simSlot + 1})")
                    telecomManager.placeCall(uri, extras)
                } else {
                    val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                        putExtras(extras)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    }
                    startActivity(callIntent)
                    AppLogLogger.log("WakeAndCallActivity", "ACTION_CALL launched over lockscreen for $phoneNumber (SIM ${simSlot + 1})")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLogLogger.log("WakeAndCallActivity", "Call placement error over lockscreen: ${e.message}")
            }

            delay(2.seconds)
            finish()
        }
    }

    companion object {
        const val EXTRA_PHONE_NUMBER = "extra_phone_number"
        const val EXTRA_SIM_SLOT = "extra_sim_slot"
    }
}
