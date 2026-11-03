package com.autodialer.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("autodialer_prefs", Context.MODE_PRIVATE)

    var defaultSimSlot: Int
        get() = prefs.getInt(KEY_DEFAULT_SIM, 0)
        set(value) = prefs.edit { putInt(KEY_DEFAULT_SIM, value) }

    var defaultSpeakerphoneEnabled: Boolean
        get() = prefs.getBoolean(KEY_DEFAULT_SPEAKER, false)
        set(value) = prefs.edit { putBoolean(KEY_DEFAULT_SPEAKER, value) }

    var defaultIntervalSeconds: Int
        get() = prefs.getInt(KEY_DEFAULT_INTERVAL, 3)
        set(value) = prefs.edit { putInt(KEY_DEFAULT_INTERVAL, value) }

    var defaultDurationLimitSeconds: Int
        get() = prefs.getInt(KEY_DEFAULT_DURATION, 10)
        set(value) = prefs.edit { putInt(KEY_DEFAULT_DURATION, value) }

    var defaultIncludeDialingTime: Boolean
        get() = prefs.getBoolean(KEY_DEFAULT_INCLUDE_DIAL_TIME, true)
        set(value) = prefs.edit { putBoolean(KEY_DEFAULT_INCLUDE_DIAL_TIME, value) }

    var isDarkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, true)
        set(value) = prefs.edit { putBoolean(KEY_DARK_THEME, value) }

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit { putBoolean(KEY_FIRST_LAUNCH, value) }

    companion object {
        private const val KEY_DEFAULT_SIM = "pref_default_sim"
        private const val KEY_DEFAULT_SPEAKER = "pref_default_speaker"
        private const val KEY_DEFAULT_INTERVAL = "pref_default_interval"
        private const val KEY_DEFAULT_DURATION = "pref_default_duration"
        private const val KEY_DEFAULT_INCLUDE_DIAL_TIME = "pref_default_include_dial_time"
        private const val KEY_DARK_THEME = "pref_dark_theme"
        private const val KEY_FIRST_LAUNCH = "pref_first_launch"
    }
}
