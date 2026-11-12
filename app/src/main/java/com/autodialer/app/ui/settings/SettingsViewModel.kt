package com.autodialer.app.ui.settings

import androidx.lifecycle.ViewModel
import com.autodialer.app.AutoDialerApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ================================================================================================
 * WHAT IS IT:
 * Architecture ViewModel managing user preferences, audio defaults, SIM selection, and theme state.
 *
 * WHAT IT DOES:
 * 1. Exposes observable [StateFlow] properties for default SIM card slot, speakerphone preference,
 *    and dark theme mode.
 * 2. Directly mutates [PreferencesManager] in SharedPreferences on user interaction.
 * 3. Shared between [com.autodialer.app.MainActivity] and [SettingsScreen] ensuring instantaneous
 *    recomposition of the global application theme upon toggle.
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - UI Screen: [SettingsScreen]
 * - Activity Host: [com.autodialer.app.MainActivity]
 * - Storage: [com.autodialer.app.data.local.PreferencesManager]
 * ================================================================================================
 */
class SettingsViewModel : ViewModel() {

    private val prefs = AutoDialerApplication.instance.preferencesManager

    private val _defaultSimSlot = MutableStateFlow(prefs.defaultSimSlot)
    val defaultSimSlot: StateFlow<Int> = _defaultSimSlot.asStateFlow()

    private val _defaultSpeakerphone = MutableStateFlow(prefs.defaultSpeakerphoneEnabled)
    val defaultSpeakerphone: StateFlow<Boolean> = _defaultSpeakerphone.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(prefs.isDarkTheme)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun setDefaultSimSlot(slot: Int) {
        _defaultSimSlot.value = slot
        prefs.defaultSimSlot = slot
    }

    fun toggleDefaultSpeakerphone() {
        val newStatus = !_defaultSpeakerphone.value
        _defaultSpeakerphone.value = newStatus
        prefs.defaultSpeakerphoneEnabled = newStatus
    }

    fun toggleDarkTheme() {
        val newStatus = !_isDarkTheme.value
        _isDarkTheme.value = newStatus
        prefs.isDarkTheme = newStatus
    }
}
