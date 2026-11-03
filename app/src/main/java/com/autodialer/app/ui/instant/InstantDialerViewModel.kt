package com.autodialer.app.ui.instant

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import com.autodialer.app.AutoDialerApplication
import com.autodialer.app.service.AutoDialerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class InstantDialerViewModel : ViewModel() {

    private val prefs = AutoDialerApplication.instance.preferencesManager

    private val _phoneNumber = MutableStateFlow("9820443175")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _selectedSimSlot = MutableStateFlow(prefs.defaultSimSlot)
    val selectedSimSlot: StateFlow<Int> = _selectedSimSlot.asStateFlow()

    private val _intervalSeconds = MutableStateFlow(prefs.defaultIntervalSeconds)
    val intervalSeconds: StateFlow<Int> = _intervalSeconds.asStateFlow()

    private val _repetitions = MutableStateFlow(1)
    val repetitions: StateFlow<Int> = _repetitions.asStateFlow()

    private val _durationLimitSeconds = MutableStateFlow(prefs.defaultDurationLimitSeconds)
    val durationLimitSeconds: StateFlow<Int> = _durationLimitSeconds.asStateFlow()

    private val _includeDialingTime = MutableStateFlow(prefs.defaultIncludeDialingTime)
    val includeDialingTime: StateFlow<Boolean> = _includeDialingTime.asStateFlow()

    private val _isSpeakerphoneEnabled = MutableStateFlow(prefs.defaultSpeakerphoneEnabled)
    val isSpeakerphoneEnabled: StateFlow<Boolean> = _isSpeakerphoneEnabled.asStateFlow()

    val dialerState: StateFlow<String> = AutoDialerService.dialStateFlow

    fun setPhoneNumber(number: String) {
        _phoneNumber.value = number
    }

    fun appendPhoneNumber(number: String) {
        val current = _phoneNumber.value.trim()
        if (current.isBlank()) {
            _phoneNumber.value = number.trim()
        } else {
            _phoneNumber.value = "$current, ${number.trim()}"
        }
    }

    fun setSimSlot(slot: Int) {
        _selectedSimSlot.value = slot
    }

    fun setIntervalSeconds(seconds: Int) {
        _intervalSeconds.value = seconds.coerceIn(1, 3600)
    }

    fun setRepetitions(count: Int) {
        _repetitions.value = count.coerceIn(1, 100000)
    }

    fun setDurationLimitSeconds(seconds: Int) {
        _durationLimitSeconds.value = seconds.coerceIn(1, 3600)
    }

    fun toggleIncludeDialingTime() {
        _includeDialingTime.value = !_includeDialingTime.value
    }

    fun toggleSpeakerphone() {
        _isSpeakerphoneEnabled.value = !_isSpeakerphoneEnabled.value
    }

    fun getParsedNumbers(): List<String> {
        val raw = _phoneNumber.value
        return raw.split(',', '\n', ';')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { it.replace(Regex("[^0-9+]"), "") }
            .filter { it.length >= 3 }
    }

    fun startDialing(context: Context) {
        val numbers = getParsedNumbers()
        if (numbers.isEmpty()) return

        val intent = Intent(context, AutoDialerService::class.java).apply {
            putStringArrayListExtra(AutoDialerService.EXTRA_PHONE_NUMBERS, ArrayList(numbers))
            putExtra(AutoDialerService.EXTRA_INTERVAL_SECONDS, _intervalSeconds.value)
            putExtra(AutoDialerService.EXTRA_REPETITIONS, _repetitions.value)
            putExtra(AutoDialerService.EXTRA_DURATION_LIMIT, _durationLimitSeconds.value)
            putExtra(AutoDialerService.EXTRA_INCLUDE_DIAL_TIME, _includeDialingTime.value)
            putExtra(AutoDialerService.EXTRA_SPEAKERPHONE, _isSpeakerphoneEnabled.value)
            putExtra(AutoDialerService.EXTRA_SIM_SLOT, _selectedSimSlot.value)
        }

        context.startForegroundService(intent)
    }

    fun stopDialing(context: Context) {
        val intent = Intent(context, AutoDialerService::class.java).apply {
            action = AutoDialerService.ACTION_STOP_DIALING
        }
        context.startService(intent)
    }
}
