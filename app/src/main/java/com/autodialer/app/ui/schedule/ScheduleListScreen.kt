package com.autodialer.app.ui.schedule

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodialer.app.data.model.ScheduleWithContacts
import java.text.SimpleDateFormat
import java.util.*

/**
 * ================================================================================================
 * WHAT IS IT:
 * List screen Composable presenting all configured automated calling campaigns and their statuses.
 *
 * WHAT IT DOES:
 * 1. Displays cards for each campaign showing title, recurrence frequency, next scheduled run time,
 *    contact count, duration limit, and SIM preference.
 * 2. Provides active/inactive toggle switch for each campaign.
 * 3. Provides edit and delete actions with confirmation dialog.
 * 4. Shows an empty state illustration with an 'Add First Schedule' call-to-action button when no campaigns exist.
 *
 * WHAT THINGS IT IS LINKED WITH:
 * - ViewModel: [ScheduleViewModel]
 * - Parent Navigation: [com.autodialer.app.ui.main.MainScreen]
 * ================================================================================================
 */
@Composable
@Suppress("FunctionName")
fun ScheduleListScreen(
    viewModel: ScheduleViewModel = viewModel(),
    onAddSchedule: () -> Unit,
    onEditSchedule: (Long) -> Unit
) {
    val context = LocalContext.current
    val schedulesWithContacts by viewModel.schedules.collectAsState()

    var exportScheduleWithContacts by remember { mutableStateOf<ScheduleWithContacts?>(null) }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            exportScheduleWithContacts?.let { sc ->
                viewModel.exportScheduleToCsv(it, sc.contacts) { success ->
                    val msg = if (success) "Contacts exported to CSV successfully!" else "Failed to export CSV"
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddSchedule,
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Schedule")
            }
        }
    ) { paddingValues ->
        if (schedulesWithContacts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .navigationBarsPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("No Call Schedules Created Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "Tap '+' to create a scheduled single or bulk call campaign.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(10.dp),
                contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(schedulesWithContacts, key = { it.schedule.id }) { item ->
                    CompactScheduleCard(
                        scheduleWithContacts = item,
                        onToggleEnabled = { viewModel.toggleSchedule(item.schedule) },
                        onEdit = { onEditSchedule(item.schedule.id) },
                        onDelete = { viewModel.deleteSchedule(item.schedule) },
                        onExportCsv = {
                            exportScheduleWithContacts = item
                            exportCsvLauncher.launch("${item.schedule.title.replace(" ", "_")}_contacts.csv")
                        }
                    )
                }
            }
        }
    }
}

@Composable
@Suppress("FunctionName")
fun CompactScheduleCard(
    scheduleWithContacts: ScheduleWithContacts,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onExportCsv: () -> Unit
) {
    val schedule = scheduleWithContacts.schedule
    val contacts = scheduleWithContacts.contacts

    val dateFormat = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault())
    val formattedNextRun = dateFormat.format(Date(schedule.nextRunTimeEpochMs))

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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = schedule.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Next: $formattedNextRun",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Switch(
                    checked = schedule.isEnabled,
                    onCheckedChange = { onToggleEnabled() },
                    modifier = Modifier.height(26.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text(schedule.repeatRule, fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.height(26.dp)
                    )

                    SuggestionChip(
                        onClick = { },
                        label = { Text("${contacts.size} Contact(s)", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                        modifier = Modifier.height(26.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onExportCsv, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Schedule", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }

                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Schedule", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
