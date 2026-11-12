package com.autodialer.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.autodialer.app.ui.instant.InstantDialerViewModel
import com.autodialer.app.ui.main.MainScreen
import com.autodialer.app.ui.settings.SettingsViewModel
import com.autodialer.app.ui.theme.AutoDialerTheme

/**
 * ================================================================================================
 * WHAT IS IT:
 * Single-activity entry point and root UI host for the Auto Dialer application, inheriting from
 * [ComponentActivity] and hosting Jetpack Compose declarative UI hierarchy.
 *
 * WHAT IT DOES:
 * 1. Enables modern full edge-to-edge transparent system bars via `enableEdgeToEdge()`.
 * 2. Checks and requests essential runtime Android permissions (CALL_PHONE, READ_PHONE_STATE,
 *    ANSWER_PHONE_CALLS, READ_CONTACTS, and Android 13+ POST_NOTIFICATIONS).
 * 3. Scopes root [SettingsViewModel] and [InstantDialerViewModel] across the activity lifecycle.
 * 4. Observes dark/light theme state flow and binds [AutoDialerTheme] to recompose dynamically
 *    when toggled in the Settings screen.
 * 5. Hosts [MainScreen] which encapsulates the bottom navigation and child navigation graphs.
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Upstream Trigger: Android Launcher / OS Task Manager (`android.intent.action.MAIN`).
 * - Downstream UI:
 *   * [AutoDialerTheme]: Dynamic Material 3 design palette host.
 *   * [MainScreen]: Bottom navigation and NavHost container.
 * - ViewModels:
 *   * [InstantDialerViewModel]: Instant calling parameter state.
 *   * [SettingsViewModel]: Global audio, SIM, and theme preferences.
 * - System Frameworks:
 *   * [ActivityResultContracts.RequestMultiplePermissions]: Asynchronous permission gatekeeper.
 * ================================================================================================
 */
class MainActivity : ComponentActivity() {

    /**
     * Activity-scoped ViewModel controlling instant dialing inputs, state, and service launches.
     */
    private val instantViewModel: InstantDialerViewModel by viewModels()

    /**
     * Activity-scoped ViewModel providing global theme, default SIM, and audio routing settings.
     */
    private val settingsViewModel: SettingsViewModel by viewModels()

    /**
     * Activity Result Launcher for dispatching multiple Android runtime permission requests.
     */
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    /**
     * Activity lifecycle entry point. Configures edge-to-edge window insets, requests runtime
     * permissions, and binds the Compose content hierarchy.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        requestCorePermissions()

        setContent {
            val isDarkTheme by settingsViewModel.isDarkTheme.collectAsState()

            AutoDialerTheme(darkTheme = isDarkTheme) {
                MainScreen(
                    instantViewModel = instantViewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }
    }

    /**
     * Inspects permission grants for critical telecom, telephony, contact, and notification APIs.
     * Launches the system permission dialog for any missing entitlements.
     */
    private fun requestCorePermissions() {
        val permissions = mutableListOf(
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.READ_CONTACTS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            requestPermissionLauncher.launch(missing.toTypedArray())
        }
    }
}
