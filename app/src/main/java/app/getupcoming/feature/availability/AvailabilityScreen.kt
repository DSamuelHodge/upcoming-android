package app.getupcoming.feature.availability

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.getupcoming.core.designsystem.*
import app.getupcoming.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvailabilityScreen(
    viewModel: AvailabilityViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showTimezonePicker by remember { mutableStateOf(false) }
    var showAddOverrideDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            UpcomingTopBar(
                title = "Availability & Hours",
                subtitle = "Set your standard working hours & date overrides",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UpcomingSecondaryButton(
                        text = "Back",
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f)
                    )
                    UpcomingPrimaryButton(
                        text = "Save Schedule",
                        isLoading = uiState.isSaving,
                        onClick = {
                            viewModel.saveWeeklyRules {
                                Toast.makeText(context, "Availability saved successfully!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Timezone Card
            item {
                UpcomingCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Working Timezone",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = uiState.schedule?.timezone ?: "America/New_York",
                                style = MaterialTheme.typography.bodyMedium,
                                color = UpcomingTokens.BrandPrimary
                            )
                        }
                        TextButton(onClick = { showTimezonePicker = true }) {
                            Text("Change", color = UpcomingTokens.BrandPrimary)
                        }
                    }
                }
            }

            // 2. Weekly Schedule Header & Quick Presets
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Weekly Working Hours",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // 3. Daily Rows
            items(uiState.days) { day ->
                UpcomingCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = if (day.isEnabled) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Switch(
                                checked = day.isEnabled,
                                onCheckedChange = { isChecked -> viewModel.toggleDay(day.dayOfWeek, isChecked) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = UpcomingTokens.BrandPrimary
                                )
                            )
                            Text(
                                text = day.dayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (day.isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (day.isEnabled) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                TimeChip(
                                    time = day.startTime,
                                    onTimeSelected = { newStart -> viewModel.updateDayTimes(day.dayOfWeek, newStart, day.endTime) }
                                )
                                Text("–", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                TimeChip(
                                    time = day.endTime,
                                    onTimeSelected = { newEnd -> viewModel.updateDayTimes(day.dayOfWeek, day.startTime, newEnd) }
                                )
                            }
                        } else {
                            Text(
                                text = "Unavailable",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 4. Date-Specific Overrides Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Date-Specific Overrides",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Add holidays, vacation days, or custom hours",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { showAddOverrideDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Date")
                    }
                }
            }

            if (uiState.dateOverrides.isEmpty()) {
                item {
                    UpcomingCard(
                        modifier = Modifier.fillMaxWidth(),
                        padding = PaddingValues(20.dp)
                    ) {
                        Text(
                            text = "No date overrides added. You are operating on standard weekly hours.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(uiState.dateOverrides) { override ->
                    UpcomingCard(
                        modifier = Modifier.fillMaxWidth(),
                        padding = PaddingValues(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Event,
                                    contentDescription = null,
                                    tint = UpcomingTokens.BrandPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column {
                                    Text(
                                        text = override.dateOverride ?: "",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${override.startTime} – ${override.endTime}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(onClick = { viewModel.removeOverride(override.id) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove override",
                                    tint = SemanticError,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // Timezone Selector Dialog
    if (showTimezonePicker) {
        val timezones = listOf(
            "America/New_York" to "Eastern Time (New York)",
            "America/Chicago" to "Central Time (Chicago)",
            "America/Denver" to "Mountain Time (Denver)",
            "America/Los_Angeles" to "Pacific Time (Los Angeles)",
            "Europe/London" to "Greenwich Mean Time (London)",
            "Europe/Paris" to "Central European (Paris)",
            "Asia/Tokyo" to "Japan Standard Time (Tokyo)",
            "UTC" to "Coordinated Universal Time (UTC)"
        )

        AlertDialog(
            onDismissRequest = { showTimezonePicker = false },
            title = { Text("Select Working Timezone") },
            text = {
                LazyColumn {
                    items(timezones) { (tzKey, tzLabel) ->
                        val selected = uiState.schedule?.timezone == tzKey
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateTimezone(tzKey)
                                    showTimezonePicker = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = tzLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (selected) UpcomingTokens.BrandPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = tzKey,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (selected) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = UpcomingTokens.BrandPrimary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTimezonePicker = false }) {
                    Text("Close")
                }
            }
        )
    }

    // Add Date Override Dialog
    if (showAddOverrideDialog) {
        var dateStr by remember { mutableStateOf("2026-09-01") }
        var startTime by remember { mutableStateOf("10:00") }
        var endTime by remember { mutableStateOf("14:00") }

        AlertDialog(
            onDismissRequest = { showAddOverrideDialog = false },
            title = { Text("Add Date Override") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = dateStr,
                        onValueChange = { dateStr = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startTime,
                            onValueChange = { startTime = it },
                            label = { Text("Start Time") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = endTime,
                            onValueChange = { endTime = it },
                            label = { Text("End Time") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (dateStr.isNotBlank()) {
                            viewModel.addDateOverride(dateStr, startTime, endTime)
                            showAddOverrideDialog = false
                        }
                    }
                ) {
                    Text("Add Override")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddOverrideDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun TimeChip(
    time: String,
    onTimeSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val timesList = listOf(
        "08:00", "08:30", "09:00", "09:30", "10:00", "10:30", "11:00", "11:30",
        "12:00", "12:30", "13:00", "13:30", "14:00", "14:30", "15:00", "15:30",
        "16:00", "16:30", "17:00", "17:30", "18:00", "18:30", "19:00", "20:00"
    )

    Box {
        Surface(
            shape = UpcomingTokens.RadiusSmall,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            timesList.forEach { t ->
                DropdownMenuItem(
                    text = { Text(t) },
                    onClick = {
                        onTimeSelected(t)
                        expanded = false
                    }
                )
            }
        }
    }
}
