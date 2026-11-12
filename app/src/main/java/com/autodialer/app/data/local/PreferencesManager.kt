package com.autodialer.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * ================================================================================================
 * WHAT IS IT:
 * Strongly-typed wrapper around Android's [SharedPreferences] managing persistent user preferences,
 * dialer configurations, default hardware states, and UI theme options.
 *
 * WHAT IT DOES:
 * 1. Reads and writes default SIM card preference (0 for SIM 1, 1 for SIM 2).
 * 2. Persists default speakerphone state (enabled/disabled).
 * 3. Persists default countdown intervals (seconds between redials).
 * 4. Persists default duration limit timers (auto-hangup cutoff).
 * 5. Controls whether dialing time is counted toward duration limit or only connected talk time.
 * 6. Stores application theme mode (dark vs. light).
 * 7. Tracks initial onboarding / first launch completion flag.
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Direct Callers:
 *   * [com.autodialer.app.ui.instant.InstantDialerViewModel]: Initializes default parameters for instant dialer.
 *   * [com.autodialer.app.ui.settings.SettingsViewModel]: Reads and updates configuration switches.
 *   * [com.autodialer.app.service.AutoDialerService]: Fallback parameter defaults.
 *   * [com.autodialer.app.AutoDialerApplication]: Global singleton instantiation.
 * ================================================================================================
 */
class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("autodialer_prefs", Context.MODE_PRIVATE)

    /**
     * Preferred SIM slot index (0 = SIM 1, 1 = SIM 2).
     */
    var defaultSimSlot: Int
        get() = prefs.getInt(KEY_DEFAULT_SIM, 0)
        set(value) = prefs.edit { putInt(KEY_DEFAULT_SIM, value) }

    /**
     * Whether speakerphone should automatically activate when a call connects.
     */
    var defaultSpeakerphoneEnabled: Boolean
        get() = prefs.getBoolean(KEY_DEFAULT_SPEAKER, false)
        set(value) = prefs.edit { putBoolean(KEY_DEFAULT_SPEAKER, value) }

    /**
     * Default delay in seconds between subsequent call attempts.
     */
    var defaultIntervalSeconds: Int
        get() = prefs.getInt(KEY_DEFAULT_INTERVAL, 3)
        set(value) = prefs.edit { putInt(KEY_DEFAULT_INTERVAL, value) }

    /**
     * Default maximum duration in seconds before automatically disconnecting an active call.
     */
    var defaultDurationLimitSeconds: Int
        get() = prefs.getInt(KEY_DEFAULT_DURATION, 10)
        set(value) = prefs.edit { putInt(KEY_DEFAULT_DURATION, value) }

    /**
     * When true, the duration limit timer starts immediately when dialing begins.
     * When false, the timer starts only when the remote party answers.
     */
    var defaultIncludeDialingTime: Boolean
        get() = prefs.getBoolean(KEY_DEFAULT_INCLUDE_DIAL_TIME, true)
        set(value) = prefs.edit { putBoolean(KEY_DEFAULT_INCLUDE_DIAL_TIME, value) }

    /**
     * Application dark theme toggle state.
     */
    var isDarkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, true)
        set(value) = prefs.edit { putBoolean(KEY_DARK_THEME, value) }

    /**
     * Flag indicating whether the application is running for the very first time.
     */
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
