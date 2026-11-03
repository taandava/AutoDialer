package com.autodialer.app.ui.main

import android.provider.ContactsContract
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.autodialer.app.ui.instant.InstantDialerScreen
import com.autodialer.app.ui.instant.InstantDialerViewModel
import com.autodialer.app.ui.ledger.CallLedgerScreen
import com.autodialer.app.ui.permissions.PermissionManagerScreen
import com.autodialer.app.ui.schedule.AddEditScheduleScreen
import com.autodialer.app.ui.schedule.ScheduleListScreen
import com.autodialer.app.ui.settings.SettingsScreen
import com.autodialer.app.ui.settings.SettingsViewModel

sealed class Screen(val route: String, val title: String, val icon: @Composable () -> Unit) {
    object Instant : Screen("instant", "Auto Dialer", { Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp)) })
    object Schedules : Screen("schedules", "Schedules", { Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(18.dp)) })
    object Settings : Screen("settings", "Settings", { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) })
    object CallLedger : Screen("call_ledger", "Ledger", { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(18.dp)) })
    object AddEditSchedule : Screen("add_edit_schedule?id={id}", "Schedule", { Icon(Icons.Default.Edit, contentDescription = null) })
    object PermissionManager : Screen("permissions", "Permissions", { Icon(Icons.Default.Security, contentDescription = null) })
}

@Composable
@Suppress("FunctionName")
fun MainScreen(
    instantViewModel: InstantDialerViewModel,
    settingsViewModel: SettingsViewModel
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Instant.route

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let { contactUri ->
            try {
                val cursor = context.contentResolver.query(contactUri, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val idIndex = c.getColumnIndex(ContactsContract.Contacts._ID)
                        val hasPhoneIndex = c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                        if (idIndex != -1 && hasPhoneIndex != -1) {
                            val contactId = c.getString(idIndex)
                            val hasPhone = c.getString(hasPhoneIndex)

                            if (hasPhone == "1") {
                                val phoneCursor = context.contentResolver.query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                                    arrayOf(contactId),
                                    null
                                )
                                phoneCursor?.use { pc ->
                                    if (pc.moveToFirst()) {
                                        val numberIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                        if (numberIndex != -1) {
                                            val number = pc.getString(numberIndex)
                                            instantViewModel.setPhoneNumber(number)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val bottomNavItems = listOf(
        Screen.Instant,
        Screen.Schedules,
        Screen.Settings
    )

    val showBottomBar = currentRoute in listOf(Screen.Instant.route, Screen.Schedules.route, Screen.Settings.route)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            if (showBottomBar) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(44.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (currentRoute) {
                                Screen.Instant.route -> "Auto Dialer"
                                Screen.Schedules.route -> "Call Schedules"
                                Screen.Settings.route -> "Settings"
                                else -> "Auto Dialer"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Call History Ledger Action Button
                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    navController.navigate(Screen.CallLedger.route)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = "Call Ledger",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            if (currentRoute != Screen.Settings.route) {
                                IconButton(
                                    onClick = {
                                        keyboardController?.hide()
                                        focusManager.clearFocus(force = true)
                                        navController.navigate(Screen.PermissionManager.route)
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Shield,
                                        contentDescription = "Permissions",
                                        tint = Color(0xFFE53935),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (showBottomBar) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .height(52.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        bottomNavItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            val contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

                            Surface(
                                onClick = {
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    Log.d("MainScreen", "Tab clicked target=${screen.route}, current=$currentRoute")
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id)
                                            launchSingleTop = true
                                        }
                                    }
                                },
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    CompositionLocalProvider(LocalContentColor provides contentColor) {
                                        screen.icon()
                                    }
                                    if (isSelected) {
                                        Text(
                                            text = screen.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = contentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Instant.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.Instant.route) {
                InstantDialerScreen(
                    viewModel = instantViewModel,
                    onPickContact = { contactPickerLauncher.launch(null) }
                )
            }

            composable(Screen.Schedules.route) {
                ScheduleListScreen(
                    onAddSchedule = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        Log.d("MainScreen", "Navigating to add schedule")
                        navController.navigate("add_edit_schedule?id=0")
                    },
                    onEditSchedule = { scheduleId ->
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        Log.d("MainScreen", "Navigating to edit schedule id=$scheduleId")
                        navController.navigate("add_edit_schedule?id=$scheduleId")
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateToPermissions = {
                        keyboardController?.hide()
                        focusManager.clearFocus(force = true)
                        Log.d("MainScreen", "Navigating to permissions from Settings")
                        navController.navigate(Screen.PermissionManager.route)
                    }
                )
            }

            composable(Screen.CallLedger.route) {
                CallLedgerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(
                route = "add_edit_schedule?id={id}",
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = 0L })
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getLong("id") ?: 0L
                AddEditScheduleScreen(
                    scheduleId = id,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.PermissionManager.route) {
                PermissionManagerScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
