package com.autodialer.app.ui.schedule

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodialer.app.data.local.AppLogLogger
import com.autodialer.app.data.model.CallScheduleEntity
import com.autodialer.app.data.model.ScheduleRepeatRule
import com.autodialer.app.data.model.ScheduledContactEntity
import com.autodialer.app.ui.instant.NumberStepper
import java.text.SimpleDateFormat
import java.util.*

/**
 * ================================================================================================
 * WHAT IS IT:
 * Full-featured campaign authoring and editing form Composable for creating or updating scheduled campaigns.
 *
 * WHAT IT DOES:
 * 1. Configures campaign title, start date, and execution time using Material 3 DatePickerDialog and TimePickerDialog.
 * 2. Selects repeat recurrence rule (Once, Hourly, Daily, Weekly, Monthly, Custom Interval in seconds or minutes).
 * 3. Manages queued contact list with manual add, system contact picker, CSV file import, and individual deletion.
 * 4. Configures hardware parameters: SIM slot choice, auto-speakerphone, duration limits, and inter-call delays.
 * 5. Commits campaign and queued contacts atomically into Room database via [ScheduleRepository.saveSchedule].
 * 6. Schedules exact system wakeup alarm via [CallSchedulerManager.scheduleAlarm].
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - Parent Navigation: [com.autodialer.app.ui.main.MainScreen]
 * - Repositories: [com.autodialer.app.data.repository.ScheduleRepository], [com.autodialer.app.data.repository.CsvRepository]
 * - Hardware Engine: [com.autodialer.app.service.CallSchedulerManager]
 * ================================================================================================
 */
@Composable
@Suppress("FunctionName")
fun AddEditScheduleScreen(
    scheduleId: Long = 0,
    viewModel: ScheduleViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val schedulesWithContacts by viewModel.schedules.collectAsState()

    val existingItem = remember(schedulesWithContacts, scheduleId) {
        schedulesWithContacts.find { it.schedule.id == scheduleId }
    }

    var title by remember(scheduleId) { mutableStateOf(existingItem?.schedule?.title ?: "") }

    var scheduledEpochMs by remember(scheduleId) {
        mutableLongStateOf(
            existingItem?.schedule?.nextRunTimeEpochMs ?: (System.currentTimeMillis() + 5 * 60 * 1000L)
        )
    }

    var selectedRepeatRule by remember(scheduleId) {
        mutableStateOf(
            if (existingItem != null) ScheduleRepeatRule.fromName(existingItem.schedule.repeatRule)
            else ScheduleRepeatRule.ONCE
        )
    }

    var repeatIntervalValue by remember(scheduleId) { mutableIntStateOf(existingItem?.schedule?.customIntervalMinutes ?: 20) }
    var repeatIntervalUnit by remember(scheduleId) { mutableStateOf(existingItem?.schedule?.repeatIntervalUnit ?: "SECONDS") }

    var intervalBetweenCallsSeconds by remember(scheduleId) { mutableIntStateOf(existingItem?.schedule?.intervalBetweenCallsSeconds ?: 3) }
    var repetitionsPerNumber by remember(scheduleId) { mutableIntStateOf(existingItem?.schedule?.repetitionsPerNumber ?: 1) }
    var durationLimitSeconds by remember(scheduleId) { mutableIntStateOf(existingItem?.schedule?.durationLimitSeconds ?: 10) }
    var includeDialingTime by remember(scheduleId) { mutableStateOf(existingItem?.schedule?.includeDialingTime ?: true) }
    var speakerphoneEnabled by remember(scheduleId) { mutableStateOf(existingItem?.schedule?.speakerphoneEnabled ?: false) }
    var simSlot by remember(scheduleId) { mutableIntStateOf(existingItem?.schedule?.simSlot ?: 0) }

    val contactsList = remember(scheduleId) { mutableStateListOf<ScheduledContactEntity>() }

    LaunchedEffect(existingItem, scheduleId) {
        if (existingItem != null && contactsList.isEmpty()) {
            contactsList.addAll(existingItem.contacts)
        }
    }

    var newPhoneNumber by remember(scheduleId) { mutableStateOf("") }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { phoneUri ->
                try {
                    val cursor = context.contentResolver.query(
                        phoneUri,
                        arrayOf(
                            ContactsContract.CommonDataKinds.Phone.NUMBER,
                            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                        ),
                        null, null, null
                    )
                    cursor?.use { c ->
                        if (c.moveToFirst()) {
                            val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val number = if (numIdx != -1) c.getString(numIdx) else ""
                            val name = if (nameIdx != -1) c.getString(nameIdx) else ""
                            if (number.isNotBlank()) {
                                contactsList.add(
                                    ScheduledContactEntity(
                                        scheduleId = scheduleId,
                                        phoneNumber = number.trim(),
                                        contactName = name
                                    )
                                )
                                AppLogLogger.log("AddEditSchedule", "Picked contact: $name ($number)")
                            }
                        }
                    }
                } catch (e: Exception) {
                    AppLogLogger.log("AddEditSchedule", "Contact pick error: ${e.message}")
                }
            }
        }
    }

    val csvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.importCsvToContacts(it) { imported ->
                contactsList.addAll(imported)
                AppLogLogger.log("AddEditSchedule", "Imported ${imported.size} contacts from CSV")
                Toast.makeText(context, "Imported ${imported.size} contacts from CSV", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault())

    val autoExtractedNumber = remember(title, newPhoneNumber, contactsList.size) {
        val fromNew = newPhoneNumber.trim()
        val fromTitle = title.trim().filter { it.isDigit() || it == '+' }
        when {
            fromNew.isNotBlank() -> fromNew
            contactsList.isEmpty() && fromTitle.filter { it.isDigit() }.length >= 5 -> fromTitle
            else -> ""
        }
    }

    val canSave = contactsList.isNotEmpty() || autoExtractedNumber.isNotBlank()

    val calculatedIntervalSeconds = remember(repeatIntervalValue, repeatIntervalUnit, selectedRepeatRule) {
        when (selectedRepeatRule) {
            ScheduleRepeatRule.HOURLY -> 3600
            ScheduleRepeatRule.DAILY -> 86400
            ScheduleRepeatRule.WEEKLY -> 604800
            ScheduleRepeatRule.MONTHLY -> 2592000
            ScheduleRepeatRule.REPEAT_INTERVAL -> when (repeatIntervalUnit) {
                "SECONDS" -> repeatIntervalValue.coerceAtLeast(5)
                "MINUTES" -> repeatIntervalValue * 60
                "HOURS" -> repeatIntervalValue * 3600
                "DAYS" -> repeatIntervalValue * 86400
                "WEEKS" -> repeatIntervalValue * 604800
                "MONTHS" -> repeatIntervalValue * 2592000
                else -> repeatIntervalValue * 60
            }
            ScheduleRepeatRule.ONCE -> 0
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(48.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (scheduleId == 0L) "Create Schedule" else "Edit Schedule",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .padding(10.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: Campaign Title
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Campaign Title", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            placeholder = { Text("e.g. Sales Campaign or 9769602020", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )
                    }
                }

                // Card 2: Trigger Date & Time
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Trigger Date & Time", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                            Text(
                                dateFormat.format(Date(scheduledEpochMs)),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 11.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = scheduledEpochMs }
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val updatedCal = Calendar.getInstance().apply {
                                                timeInMillis = scheduledEpochMs
                                                set(Calendar.YEAR, year)
                                                set(Calendar.MONTH, month)
                                                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                            }
                                            scheduledEpochMs = updatedCal.timeInMillis
                                        },
                                        cal.get(Calendar.YEAR),
                                        cal.get(Calendar.MONTH),
                                        cal.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Set Date", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = {
                                    val cal = Calendar.getInstance().apply { timeInMillis = scheduledEpochMs }
                                    TimePickerDialog(
                                        context,
                                        { _, hourOfDay, minute ->
                                            val updatedCal = Calendar.getInstance().apply {
                                                timeInMillis = scheduledEpochMs
                                                set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                set(Calendar.MINUTE, minute)
                                                set(Calendar.SECOND, 0)
                                                set(Calendar.MILLISECOND, 0)
                                            }
                                            scheduledEpochMs = updatedCal.timeInMillis
                                        },
                                        cal.get(Calendar.HOUR_OF_DAY),
                                        cal.get(Calendar.MINUTE),
                                        false
                                    ).show()
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Set Time", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Card 3: Repeat Rule (Once, Hourly, Daily, Weekly, Monthly, Repeat Interval)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Repeat Rule", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ScheduleRepeatRule.entries.forEach { rule ->
                                FilterChip(
                                    selected = selectedRepeatRule == rule,
                                    onClick = { selectedRepeatRule = rule },
                                    label = { Text(rule.label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.height(30.dp)
                                )
                            }
                        }

                        if (selectedRepeatRule == ScheduleRepeatRule.REPEAT_INTERVAL) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Interval Value", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                                NumberStepper(
                                    value = repeatIntervalValue,
                                    onValueChange = { repeatIntervalValue = it },
                                    step = if (repeatIntervalUnit == "SECONDS") 5 else 1,
                                    minValue = 1,
                                    maxValue = 100000,
                                    fieldWidth = 56.dp
                                )
                            }

                            Text("Interval Unit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "SECONDS" to "Seconds",
                                    "MINUTES" to "Minutes",
                                    "HOURS" to "Hours",
                                    "DAYS" to "Daily",
                                    "WEEKS" to "Weekly",
                                    "MONTHS" to "Monthly"
                                ).forEach { (unitKey, unitLabel) ->
                                    FilterChip(
                                        selected = repeatIntervalUnit == unitKey,
                                        onClick = { repeatIntervalUnit = unitKey },
                                        label = { Text(unitLabel, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                        modifier = Modifier.height(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Card 4: Campaign Contacts Queue (+ Contacts & Import CSV Buttons)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Contacts Queue (${contactsList.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 13.sp
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilledTonalButton(
                                    onClick = {
                                        val pickIntent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                        contactPickerLauncher.launch(pickIntent)
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ Contacts", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                FilledTonalButton(
                                    onClick = { csvLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*")) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Import CSV", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newPhoneNumber,
                                onValueChange = { newPhoneNumber = it },
                                placeholder = { Text("Add Phone Number...", fontSize = 12.sp) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = {
                                    if (newPhoneNumber.isNotBlank()) {
                                        contactsList.add(
                                            ScheduledContactEntity(
                                                scheduleId = scheduleId,
                                                phoneNumber = newPhoneNumber.trim()
                                            )
                                        )
                                        newPhoneNumber = ""
                                    }
                                    focusManager.clearFocus()
                                }),
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )

                            IconButton(
                                onClick = {
                                    if (newPhoneNumber.isNotBlank()) {
                                        contactsList.add(
                                            ScheduledContactEntity(
                                                scheduleId = scheduleId,
                                                phoneNumber = newPhoneNumber.trim()
                                            )
                                        )
                                        newPhoneNumber = ""
                                    }
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.size(42.dp)
                            ) {
                                Icon(Icons.Default.AddCircle, contentDescription = "Add Contact", tint = MaterialTheme.colorScheme.primary)
                            }
                        }

                        contactsList.forEachIndexed { index, contact ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${index + 1}. ${contact.phoneNumber} ${if (contact.contactName.isNotBlank()) "(${contact.contactName})" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                IconButton(onClick = { contactsList.removeAt(index) }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Card 5: Execution Rules Parameters
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Campaign Execution Rules", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)

                        // SIM Slot Choice
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("SIM Slot Preference", style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = simSlot == 0,
                                    onClick = { simSlot = 0 },
                                    label = { Text("SIM 1", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                    modifier = Modifier.height(28.dp)
                                )
                                FilterChip(
                                    selected = simSlot == 1,
                                    onClick = { simSlot = 1 },
                                    label = { Text("SIM 2", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                        // Inter-Call Delay
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Inter-Call Delay", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text("Pause between attempts (seconds)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            }

                            NumberStepper(
                                value = intervalBetweenCallsSeconds,
                                onValueChange = { intervalBetweenCallsSeconds = it },
                                step = 1,
                                minValue = 1,
                                maxValue = 3600,
                                fieldWidth = 44.dp
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                        // Retries Per Number
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Retries Per Number", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text("Up to 1,00,000 (1 Lakh)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                            }

                            NumberStepper(
                                value = repetitionsPerNumber,
                                onValueChange = { repetitionsPerNumber = it },
                                step = 1,
                                minValue = 1,
                                maxValue = 100000,
                                fieldWidth = 56.dp
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                        // Call Duration Limit
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Call Duration Limit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                Text("Auto-hangup timer (seconds)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            }

                            NumberStepper(
                                value = durationLimitSeconds,
                                onValueChange = { durationLimitSeconds = it },
                                step = 5,
                                minValue = 1,
                                maxValue = 3600,
                                fieldWidth = 44.dp
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                        // Include Dialing Time
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Include Dialing Time", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Switch(
                                checked = includeDialingTime,
                                onCheckedChange = { includeDialingTime = it },
                                modifier = Modifier.height(26.dp)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                        // Auto Speakerphone
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Auto Speakerphone", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            IconButton(onClick = { speakerphoneEnabled = !speakerphoneEnabled }, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    if (speakerphoneEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = "Speakerphone",
                                    tint = if (speakerphoneEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Save Schedule Action Button
            Button(
                onClick = {
                    if (contactsList.isEmpty() && autoExtractedNumber.isNotBlank()) {
                        contactsList.add(
                            ScheduledContactEntity(
                                scheduleId = scheduleId,
                                phoneNumber = autoExtractedNumber
                            )
                        )
                    } else if (newPhoneNumber.isNotBlank()) {
                        contactsList.add(
                            ScheduledContactEntity(
                                scheduleId = scheduleId,
                                phoneNumber = newPhoneNumber.trim()
                            )
                        )
                        newPhoneNumber = ""
                    }

                    val finalTitle = title.ifBlank { "Schedule ${dateFormat.format(Date(scheduledEpochMs))}" }
                    val entity = CallScheduleEntity(
                        id = scheduleId,
                        title = finalTitle,
                        scheduledDateTimeEpochMs = scheduledEpochMs,
                        nextRunTimeEpochMs = scheduledEpochMs,
                        repeatRule = selectedRepeatRule.name,
                        customIntervalMinutes = repeatIntervalValue,
                        customIntervalSeconds = calculatedIntervalSeconds,
                        repeatIntervalUnit = repeatIntervalUnit,
                        intervalBetweenCallsSeconds = intervalBetweenCallsSeconds,
                        repetitionsPerNumber = repetitionsPerNumber,
                        durationLimitSeconds = durationLimitSeconds,
                        includeDialingTime = includeDialingTime,
                        speakerphoneEnabled = speakerphoneEnabled,
                        simSlot = simSlot,
                        isEnabled = true
                    )
                    viewModel.saveScheduleWithContacts(entity, contactsList.toList())
                    AppLogLogger.log("AddEditSchedule", "Schedule '$finalTitle' saved! Next run in ${calculatedIntervalSeconds}s (${repeatIntervalValue} ${repeatIntervalUnit})")
                    Toast.makeText(context, "Schedule Saved Successfully!", Toast.LENGTH_SHORT).show()
                    onNavigateBack()
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("SAVE SCHEDULE", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
