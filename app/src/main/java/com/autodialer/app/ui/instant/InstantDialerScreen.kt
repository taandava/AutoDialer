package com.autodialer.app.ui.instant

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
@Suppress("FunctionName")
fun NumberStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    step: Int = 1,
    minValue: Int = 1,
    maxValue: Int = 100000,
    fieldWidth: Dp = 56.dp,
) {
    val focusManager = LocalFocusManager.current
    var textState by remember(value) { mutableStateOf(value.toString()) }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(1.dp)
        ) {
            IconButton(
                onClick = {
                    onValueChange((value - step).coerceIn(minValue, maxValue))
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(14.dp))
            }

            BasicTextField(
                value = textState,
                onValueChange = { str ->
                    val filtered = str.filter { it.isDigit() }
                    textState = filtered
                    val num = filtered.toIntOrNull()
                    if (num != null) {
                        onValueChange(num.coerceIn(minValue, maxValue))
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier
                    .width(fieldWidth)
                    .padding(horizontal = 2.dp, vertical = 2.dp)
            )

            IconButton(
                onClick = {
                    onValueChange((value + step).coerceIn(minValue, maxValue))
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
@Suppress("FunctionName")
fun InstantDialerScreen(
    viewModel: InstantDialerViewModel = viewModel(),
    onPickContact: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val selectedSimSlot by viewModel.selectedSimSlot.collectAsState()
    val intervalSeconds by viewModel.intervalSeconds.collectAsState()
    val repetitions by viewModel.repetitions.collectAsState()
    val durationLimitSeconds by viewModel.durationLimitSeconds.collectAsState()
    val includeDialingTime by viewModel.includeDialingTime.collectAsState()
    val isSpeakerphoneEnabled by viewModel.isSpeakerphoneEnabled.collectAsState()
    val dialerState by viewModel.dialerState.collectAsState()

    val parsedNumbers = remember(phoneNumber) { viewModel.getParsedNumbers() }
    val isDialing = dialerState != "Idle" && dialerState != "Stopped"
    val scrollState = rememberScrollState()


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Card 1: Target Phone Number & SIM Selection
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
                            text = if (parsedNumbers.size > 1) "Target Numbers (${parsedNumbers.size})" else "Target Phone Number",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = selectedSimSlot == 0,
                                onClick = { viewModel.setSimSlot(0) },
                                label = { Text("SIM 1", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.height(28.dp)
                            )
                            FilterChip(
                                selected = selectedSimSlot == 1,
                                onClick = { viewModel.setSimSlot(1) },
                                label = { Text("SIM 2", fontSize = 10.sp, fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = phoneNumber,
                            onValueChange = { viewModel.setPhoneNumber(it) },
                            placeholder = { Text("Single or comma-separated numbers...", fontSize = 12.sp) },
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            modifier = Modifier.weight(1f),
                            singleLine = parsedNumbers.size <= 1
                        )

                        FilledTonalIconButton(
                            onClick = { onPickContact() },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                Icons.Default.ContactPhone,
                                contentDescription = "Pick Contact",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            // Card 2: Dialing Parameters
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
                    Text(
                        text = "Dialing Parameters",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )

                    // Inter-Call Delay Row
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
                            value = intervalSeconds,
                            onValueChange = { viewModel.setIntervalSeconds(it) },
                            step = 1,
                            minValue = 1,
                            maxValue = 3600,
                            fieldWidth = 44.dp
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    // Call Retries Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Call Retries", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("Type up to 1,00,000 (1 Lakh)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontSize = 10.sp)
                        }

                        NumberStepper(
                            value = repetitions,
                            onValueChange = { viewModel.setRepetitions(it) },
                            step = 1,
                            minValue = 1,
                            maxValue = 100000,
                            fieldWidth = 56.dp
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    // Call Duration Limit Row
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
                            onValueChange = { viewModel.setDurationLimitSeconds(it) },
                            step = 5,
                            minValue = 1,
                            maxValue = 3600,
                            fieldWidth = 44.dp
                        )
                    }
                }
            }

            // Card 3: Audio & Timing Settings
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Include Dialing Time", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text(
                                if (includeDialingTime) "Timer starts on Start tap" else "Timer starts when call connects",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                        Switch(
                            checked = includeDialingTime,
                            onCheckedChange = { viewModel.toggleIncludeDialingTime() },
                            modifier = Modifier.height(26.dp)
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auto Speakerphone", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                            Text("Enable speakerphone on connect", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                        }
                        IconButton(onClick = { viewModel.toggleSpeakerphone() }, modifier = Modifier.size(32.dp)) {
                            Icon(
                                if (isSpeakerphoneEnabled) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeOff,
                                contentDescription = "Speakerphone",
                                tint = if (isSpeakerphoneEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Fixed Action Button (Dynamic Toggle between Green START DIALING and Red STOP DIALING)
        if (isDialing) {
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.stopDialing(context)
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.CallEnd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("STOP DIALING ($dialerState)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            val totalParsed = parsedNumbers.size
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.startDialing(context)
                },
                enabled = totalParsed > 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                val buttonText = if (totalParsed > 1) "START DIALING ($totalParsed NUMBERS)" else "START DIALING"
                Text(buttonText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
