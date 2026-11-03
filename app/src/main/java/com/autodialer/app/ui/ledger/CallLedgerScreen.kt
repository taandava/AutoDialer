package com.autodialer.app.ui.ledger

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.autodialer.app.data.model.CallLogEntity
import com.autodialer.app.ui.theme.ErrorRed
import com.autodialer.app.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.*

@Composable
@Suppress("FunctionName")
fun CallLedgerScreen(
    viewModel: CallLedgerViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val callLogs by viewModel.callLogs.collectAsState()

    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredLogs = remember(callLogs, selectedFilter) {
        when (selectedFilter) {
            "CONNECTED" -> callLogs.filter { it.status.contains("Connect", ignoreCase = true) || it.status.contains("Completed", ignoreCase = true) }
            "BUSY" -> callLogs.filter { it.status.contains("Busy", ignoreCase = true) }
            "FAILED" -> callLogs.filter { it.status.contains("Fail", ignoreCase = true) || it.status.contains("Reject", ignoreCase = true) || it.status.contains("Miss", ignoreCase = true) }
            else -> callLogs
        }
    }

    val totalDurationSeconds = remember(callLogs) { callLogs.sumOf { it.durationSeconds } }
    val totalConnectedCalls = remember(callLogs) {
        callLogs.count { it.status.contains("Connect", ignoreCase = true) || it.status.contains("Completed", ignoreCase = true) || it.durationSeconds > 0 }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportLedgerToCsv(context, it) { success ->
                val msg = if (success) "Call Ledger exported to CSV!" else "Failed to export CSV"
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(44.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onNavigateBack, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Call History Ledger (${callLogs.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (callLogs.isNotEmpty()) {
                            IconButton(
                                onClick = { exportCsvLauncher.launch("call_ledger_${System.currentTimeMillis()}.csv") },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Export CSV", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }

                            IconButton(
                                onClick = { viewModel.clearAllLogs() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (callLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text("No Call Logs In Ledger Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "Every instant and scheduled call attempt will be logged here automatically with duration limits and disconnect causes.",
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
                    .padding(padding)
                    .padding(10.dp),
                contentPadding = WindowInsets.navigationBars.asPaddingValues(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Analytics Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${callLogs.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 18.sp)
                                Text("Total Calls", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            }

                            VerticalDivider(modifier = Modifier.height(28.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("$totalConnectedCalls", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = SuccessGreen, fontSize = 18.sp)
                                Text("Connected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            }

                            VerticalDivider(modifier = Modifier.height(28.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val mins = totalDurationSeconds / 60
                                val secs = totalDurationSeconds % 60
                                Text("${mins}m ${secs}s", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary, fontSize = 18.sp)
                                Text("Talk Time", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Filter Chips
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ALL" to "All (${callLogs.size})",
                            "CONNECTED" to "Connected ($totalConnectedCalls)",
                            "BUSY" to "Busy (${callLogs.count { it.status.contains("Busy", ignoreCase = true) }})",
                            "FAILED" to "Failed / Rejected (${callLogs.count { it.status.contains("Fail", ignoreCase = true) || it.status.contains("Reject", ignoreCase = true) || it.status.contains("Miss", ignoreCase = true) }})"
                        ).forEach { (key, label) ->
                            FilterChip(
                                selected = selectedFilter == key,
                                onClick = { selectedFilter = key },
                                label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                items(filteredLogs, key = { it.id }) { log ->
                    CompactCallLogCard(log = log)
                }
            }
        }
    }
}

@Composable
@Suppress("FunctionName")
fun CompactCallLogCard(log: CallLogEntity) {
    val dateFormat = SimpleDateFormat("MMM dd, hh:mm:ss a", Locale.getDefault())
    val formattedTime = dateFormat.format(Date(log.timestampEpochMs))

    val statusLower = log.status.lowercase()
    val isSuccess = statusLower.contains("connect") || statusLower.contains("completed")
    val isFailure = statusLower.contains("busy") || statusLower.contains("fail") || statusLower.contains("reject") || statusLower.contains("miss")

    val badgeBgColor = when {
        isSuccess -> SuccessGreen.copy(alpha = 0.15f)
        isFailure -> ErrorRed.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    val badgeTextColor = when {
        isSuccess -> SuccessGreen
        isFailure -> ErrorRed
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

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
                Text(
                    text = log.phoneNumber + if (log.contactName.isNotBlank()) " (${log.contactName})" else "",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                SuggestionChip(
                    onClick = { },
                    label = { Text("SIM ${log.simSlot + 1}", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    modifier = Modifier.height(26.dp)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$formattedTime • ${log.scheduleTitle ?: "Instant"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Surface(
                    color = badgeBgColor,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${log.status} (${log.durationSeconds}s)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = badgeTextColor,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}
