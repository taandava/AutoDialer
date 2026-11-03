package com.autodialer.app.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.telecom.Call
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.autodialer.app.AutoDialerApplication
import com.autodialer.app.MainActivity
import com.autodialer.app.data.local.AppLogLogger
import com.autodialer.app.data.model.CallLogEntity
import com.autodialer.app.data.model.CallScheduleEntity
import com.autodialer.app.data.model.ScheduledContactEntity
import com.autodialer.app.data.repository.ScheduleRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AutoDialerService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var isRunning = false

    // Instant Mode Parameters
    private var instantPhoneNumbers: List<String> = emptyList()
    private var currentNumberIndex = 0
    private var currentRepetition = 0

    private var scheduleEntity: CallScheduleEntity? = null
    private var contactsList: List<ScheduledContactEntity> = emptyList()

    private var callStateReceiverRegistered = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        registerCallStateReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        safeStartForeground()

        val action = intent?.action
        AppLogLogger.log("AutoDialerService", "onStartCommand action=$action")

        if (action == ACTION_STOP_DIALING) {
            stopAutoDialer()
            return START_NOT_STICKY
        }

        val scheduleId = intent?.getLongExtra(EXTRA_SCHEDULE_ID, -1L) ?: -1L
        if (scheduleId != -1L) {
            startScheduledCampaign(scheduleId)
        } else {
            val numbers = intent?.getStringArrayListExtra(EXTRA_PHONE_NUMBERS) ?: arrayListOf()
            val interval = intent?.getIntExtra(EXTRA_INTERVAL_SECONDS, 3) ?: 3
            val repetitions = intent?.getIntExtra(EXTRA_REPETITIONS, 1) ?: 1
            val duration = intent?.getIntExtra(EXTRA_DURATION_LIMIT, 10) ?: 10
            val includeDialTime = intent?.getBooleanExtra(EXTRA_INCLUDE_DIAL_TIME, true) ?: true
            val speakerphone = intent?.getBooleanExtra(EXTRA_SPEAKERPHONE, false) ?: false
            val simSlot = intent?.getIntExtra(EXTRA_SIM_SLOT, 0) ?: 0
            val dtmfExtension = intent?.getStringExtra(EXTRA_DTMF_EXTENSION) ?: ""
            val dtmfDelay = intent?.getIntExtra(EXTRA_DTMF_DELAY, 2) ?: 2

            startInstantDialer(
                phoneNumbers = numbers,
                intervalSeconds = interval,
                repetitions = repetitions,
                durationSeconds = duration,
                includeDialTime = includeDialTime,
                speakerphone = speakerphone,
                simSlot = simSlot,
                dtmfExtension = dtmfExtension,
                dtmfDelay = dtmfDelay
            )
        }

        return START_STICKY
    }

    private fun safeStartForeground() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    startForeground(
                        NOTIFICATION_ID,
                        createNotification("Auto Dialer Active..."),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                    )
                } catch (_: Exception) {
                    startForeground(
                        NOTIFICATION_ID,
                        createNotification("Auto Dialer Active..."),
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    createNotification("Auto Dialer Active..."),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                )
            } else {
                startForeground(NOTIFICATION_ID, createNotification("Auto Dialer Active..."))
            }
        } catch (e: Exception) {
            AppLogLogger.log("AutoDialerService", "Foreground service start exception: ${e.message}")
            try {
                startForeground(NOTIFICATION_ID, createNotification("Auto Dialer Active..."))
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun startInstantDialer(
        phoneNumbers: List<String>,
        intervalSeconds: Int,
        repetitions: Int,
        durationSeconds: Int,
        includeDialTime: Boolean,
        speakerphone: Boolean,
        simSlot: Int,
        dtmfExtension: String = "",
        dtmfDelay: Int = 2
    ) {
        if (phoneNumbers.isEmpty()) {
            AppLogLogger.log("AutoDialerService", "Instant dialer started with empty numbers list. Stopping.")
            stopSelf()
            return
        }

        instantPhoneNumbers = phoneNumbers
        currentNumberIndex = 0
        currentRepetition = 0
        isRunning = true

        AppInCallService.activeDtmfExtension = dtmfExtension
        AppInCallService.activeDtmfDelaySeconds = dtmfDelay

        scheduleEntity = CallScheduleEntity(
            id = 0,
            title = "Instant Auto Dialer",
            scheduledDateTimeEpochMs = System.currentTimeMillis(),
            nextRunTimeEpochMs = System.currentTimeMillis(),
            repeatRule = "ONCE",
            intervalBetweenCallsSeconds = intervalSeconds,
            repetitionsPerNumber = repetitions,
            durationLimitSeconds = durationSeconds,
            includeDialingTime = includeDialTime,
            speakerphoneEnabled = speakerphone,
            simSlot = simSlot
        )

        AppLogLogger.log("AutoDialerService", "Starting Instant Campaign numbers=${phoneNumbers.size}, repetitions=$repetitions, duration=${durationSeconds}s, SIM=${simSlot + 1}")

        serviceScope.launch {
            runDialingLoop()
        }
    }

    private fun startScheduledCampaign(scheduleId: Long) {
        val db = AutoDialerApplication.instance.database
        val repository = ScheduleRepository(db.scheduleDao())

        serviceScope.launch(Dispatchers.IO) {
            val scheduleWithContacts = repository.getScheduleById(scheduleId)
            if (scheduleWithContacts == null || scheduleWithContacts.contacts.isEmpty()) {
                AppLogLogger.log("AutoDialerService", "Scheduled Campaign ID=$scheduleId has no contacts. Stopping.")
                stopSelf()
                return@launch
            }

            scheduleEntity = scheduleWithContacts.schedule
            contactsList = scheduleWithContacts.contacts.sortedBy { it.orderIndex }
            instantPhoneNumbers = contactsList.map { it.phoneNumber }
            currentNumberIndex = 0
            currentRepetition = 0
            isRunning = true

            AppLogLogger.log("AutoDialerService", "Starting Scheduled Campaign ID=$scheduleId '${scheduleEntity?.title}', contacts=${contactsList.size}")

            runDialingLoop()
        }
    }

    private suspend fun runDialingLoop() {
        val config = scheduleEntity ?: return

        AppInCallService.activeSpeakerphone = config.speakerphoneEnabled
        AppInCallService.activeDurationLimitSeconds = config.durationLimitSeconds
        AppInCallService.activeIncludeDialingTime = config.includeDialingTime

        while (isRunning && currentNumberIndex < instantPhoneNumbers.size) {
            val phoneNumber = instantPhoneNumbers[currentNumberIndex]

            updateNotification("Dialing $phoneNumber (Attempt ${currentRepetition + 1}/${config.repetitionsPerNumber})")
            _dialStateFlow.value = "Dialing $phoneNumber"

            val callStartTimeMs = System.currentTimeMillis()
            var callConnectedTimeMs = 0L

            // Create initial Call Log in Ledger
            val contactName = contactsList.find { it.phoneNumber == phoneNumber }?.contactName ?: ""
            val logEntity = CallLogEntity(
                phoneNumber = phoneNumber,
                contactName = contactName,
                timestampEpochMs = callStartTimeMs,
                simSlot = config.simSlot,
                status = "Dialing",
                durationSeconds = 0,
                scheduleTitle = config.title
            )

            val logId = withContext(Dispatchers.IO) {
                try {
                    AutoDialerApplication.instance.database.callLogDao().insertCallLog(logEntity)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    AppLogLogger.log("AutoDialerService", "Error inserting initial CallLog: ${e.message}")
                    0L
                }
            }

            // Place Call with correct SIM slot parameters
            placeCall(phoneNumber, config.simSlot)

            if (!config.includeDialingTime) {
                // Option: Duration begins once connected
                var waitRingingCount = 0
                while (isRunning && AppInCallService.activeCallState != Call.STATE_ACTIVE && waitRingingCount < 30) {
                    delay(1.seconds)
                    waitRingingCount++
                    if (AppInCallService.activeCall == null && waitRingingCount > 3) {
                        break
                    }
                }

                if (AppInCallService.activeCallState == Call.STATE_ACTIVE) {
                    callConnectedTimeMs = System.currentTimeMillis()
                }

                val durationMs = config.durationLimitSeconds.coerceAtLeast(3) * 1000L
                val connectTimeMs = if (callConnectedTimeMs > 0L) callConnectedTimeMs else System.currentTimeMillis()
                AppLogLogger.log("AutoDialerService", "Call connected! Duration timer running for ${config.durationLimitSeconds}s")

                while (isRunning && (System.currentTimeMillis() - connectTimeMs) < durationMs) {
                    delay(500.milliseconds)
                    if (AppInCallService.activeCall == null && (System.currentTimeMillis() - connectTimeMs) > 1500L) {
                        break
                    }
                }
            } else {
                // Option: Duration begins immediately upon dialing
                val durationMs = config.durationLimitSeconds.coerceAtLeast(3) * 1000L
                val startTimeMs = System.currentTimeMillis()
                AppLogLogger.log("AutoDialerService", "Duration timer running for ${config.durationLimitSeconds}s (including dial time)")

                while (isRunning && (System.currentTimeMillis() - startTimeMs) < durationMs) {
                    if (AppInCallService.activeCallState == Call.STATE_ACTIVE && callConnectedTimeMs == 0L) {
                        callConnectedTimeMs = System.currentTimeMillis()
                    }
                    delay(500.milliseconds)
                    if (AppInCallService.activeCall == null && (System.currentTimeMillis() - startTimeMs) > 2000L) {
                        break
                    }
                }
            }

            AppLogLogger.log("AutoDialerService", "Duration limit reached or remote disconnected. Disconnecting $phoneNumber")
            AppInCallService.disconnectCurrentCall(this)
            delay(1.seconds)

            // Determine actual elapsed duration and final status
            val elapsedSec = if (callConnectedTimeMs > 0L) {
                ((System.currentTimeMillis() - callConnectedTimeMs) / 1000).toInt()
            } else {
                ((System.currentTimeMillis() - callStartTimeMs) / 1000).toInt()
            }

            val finalStatus = AppInCallService.lastDisconnectReason.ifBlank { "Completed" }

            if (logId > 0L) {
                withContext(Dispatchers.IO) {
                    try {
                        AutoDialerApplication.instance.database.callLogDao().updateCallStatus(logId, finalStatus, elapsedSec)
                        AppLogLogger.log("AutoDialerService", "Updated CallLog ID=$logId: $finalStatus (${elapsedSec}s)")
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        AppLogLogger.log("AutoDialerService", "Error updating CallLog: ${e.message}")
                    }
                }
            }

            currentRepetition++
            if (currentRepetition >= config.repetitionsPerNumber) {
                currentRepetition = 0
                currentNumberIndex++
            }

            if (currentNumberIndex < instantPhoneNumbers.size && isRunning) {
                val interval = config.intervalBetweenCallsSeconds
                for (s in interval downTo 1) {
                    if (!isRunning) break
                    updateNotification("Next call in $s sec...")
                    _dialStateFlow.value = "Waiting $s sec..."
                    delay(1.seconds)
                }
            }
        }

        stopAutoDialer()
    }

    private fun buildCallExtras(simSlot: Int): Bundle {
        val extras = Bundle()
        try {
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            if (telecomManager != null && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                val accounts = telecomManager.callCapablePhoneAccounts
                if (accounts != null && accounts.size > simSlot) {
                    extras.putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, accounts[simSlot])
                }
            }
        } catch (e: Exception) {
            AppLogLogger.log("AutoDialerService", "Error resolving PhoneAccountHandle: ${e.message}")
        }
        extras.putInt("simSlot", simSlot)
        extras.putInt("slot", simSlot)
        extras.putInt("com.android.phone.extra.slot", simSlot)
        extras.putInt("android.telecom.extra.SLOT_INDEX", simSlot)
        return extras
    }

    private fun placeCall(phoneNumber: String, simSlot: Int) {
        val extras = buildCallExtras(simSlot)
        try {
            val uri = "tel:${Uri.encode(phoneNumber)}".toUri()
            val telecomManager = getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
            val hasCallPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED

            if (telecomManager != null && hasCallPermission) {
                AppLogLogger.log("AutoDialerService", "Placing direct call via TelecomManager.placeCall for $phoneNumber (SIM ${simSlot + 1})")
                telecomManager.placeCall(uri, extras)
            } else {
                AppLogLogger.log("AutoDialerService", "Fallback: Launching WakeAndCallActivity for $phoneNumber (SIM ${simSlot + 1})")
                val wakeIntent = Intent(this, WakeAndCallActivity::class.java).apply {
                    putExtra(WakeAndCallActivity.EXTRA_PHONE_NUMBER, phoneNumber)
                    putExtra(WakeAndCallActivity.EXTRA_SIM_SLOT, simSlot)
                    putExtras(extras)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                val fullScreenPendingIntent = PendingIntent.getActivity(
                    this,
                    System.currentTimeMillis().toInt(),
                    wakeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Auto Dialer - Calling $phoneNumber")
                    .setContentText("Dialing $phoneNumber (SIM ${simSlot + 1})...")
                    .setSmallIcon(android.R.drawable.ic_menu_call)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setCategory(NotificationCompat.CATEGORY_CALL)
                    .setFullScreenIntent(fullScreenPendingIntent, true)
                    .setOngoing(true)
                    .build()

                notificationManager.notify(NOTIFICATION_ID, notification)
                startActivity(wakeIntent)
            }
        } catch (e: Exception) {
            AppLogLogger.log("AutoDialerService", "Error placing call: ${e.message}")
            try {
                val uri = "tel:${Uri.encode(phoneNumber)}".toUri()
                val callIntent = Intent(Intent.ACTION_CALL, uri).apply {
                    putExtras(extras)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(callIntent)
            } catch (ex: Exception) {
                ex.printStackTrace()
                Toast.makeText(this, "Call permission required to place calls", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopAutoDialer() {
        AppLogLogger.log("AutoDialerService", "Stopping AutoDialerService campaign.")
        isRunning = false
        AppInCallService.disconnectCurrentCall(this)
        _dialStateFlow.value = "Stopped"
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun registerCallStateReceiver() {
        if (!callStateReceiverRegistered) {
            val filter = IntentFilter(AppInCallService.ACTION_CALL_STATE_CHANGED)
            ContextCompat.registerReceiver(
                this,
                callStateReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            callStateReceiverRegistered = true
        }
    }

    private val callStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val state = intent?.getIntExtra(AppInCallService.EXTRA_CALL_STATE, Call.STATE_DISCONNECTED)
            val reason = intent?.getStringExtra(AppInCallService.EXTRA_DISCONNECT_REASON) ?: ""
            AppLogLogger.log("AutoDialerService", "Call state broadcast received: state=$state reason=$reason")
            when (state) {
                Call.STATE_RINGING -> _dialStateFlow.value = "Ringing..."
                Call.STATE_ACTIVE -> _dialStateFlow.value = "Connected"
                Call.STATE_DISCONNECTED -> _dialStateFlow.value = reason.ifBlank { "Disconnected" }
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Auto Dialer Campaign",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AutoDialerService::class.java).apply {
            action = ACTION_STOP_DIALING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Auto Dialer Running")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "STOP", stopPendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, createNotification(contentText))
    }

    override fun onDestroy() {
        if (callStateReceiverRegistered) {
            try {
                unregisterReceiver(callStateReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        serviceJob.cancel()
        _dialStateFlow.value = "Idle"
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "auto_dialer_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_STOP_DIALING = "com.autodialer.app.ACTION_STOP_DIALING"

        const val EXTRA_PHONE_NUMBERS = "extra_phone_numbers"
        const val EXTRA_INTERVAL_SECONDS = "extra_interval_seconds"
        const val EXTRA_REPETITIONS = "extra_repetitions"
        const val EXTRA_DURATION_LIMIT = "extra_duration_limit"
        const val EXTRA_INCLUDE_DIAL_TIME = "extra_include_dial_time"
        const val EXTRA_SPEAKERPHONE = "extra_speakerphone"
        const val EXTRA_SIM_SLOT = "extra_sim_slot"
        const val EXTRA_DTMF_EXTENSION = "extra_dtmf_extension"
        const val EXTRA_DTMF_DELAY = "extra_dtmf_delay"

        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"

        private val _dialStateFlow = MutableStateFlow("Idle")
        val dialStateFlow: StateFlow<String> = _dialStateFlow
    }
}
