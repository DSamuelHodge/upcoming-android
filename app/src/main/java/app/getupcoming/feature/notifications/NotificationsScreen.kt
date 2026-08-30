package app.getupcoming.feature.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import app.getupcoming.core.designsystem.*
import app.getupcoming.core.engine.NotificationAndReminderManager
import app.getupcoming.core.prefs.REMINDER_PRESETS
import app.getupcoming.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val reminderTimeFmt = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationsViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val prefs = state.prefs

    // Notifications are silently dropped without the runtime grant on 33+.
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            UpcomingTopBar(
                title = "Notifications & Reminders",
                subtitle = "How Upcoming alerts you about bookings and meetings",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
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
            // 1. General
            item {
                UpcomingCard {
                    Text(
                        text = "General",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ToggleRow(
                        title = "Booking Confirmations",
                        subtitle = "Notify me when someone books or cancels time",
                        checked = prefs.pushAlertsEnabled,
                        onCheckedChange = { viewModel.setPushAlerts(it) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)
                    ToggleRow(
                        title = "Sound & Vibration",
                        subtitle = "Play a sound and vibrate with alerts",
                        checked = prefs.soundVibrateEnabled,
                        onCheckedChange = { viewModel.setSoundVibration(it) }
                    )
                }
            }

            // 2. Meeting Reminders
            item {
                UpcomingCard {
                    Text(
                        text = "Meeting Reminders",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ToggleRow(
                        title = "Reminders",
                        subtitle = "Alert me before my meetings start",
                        checked = prefs.remindersEnabled,
                        onCheckedChange = { viewModel.setReminderSettings(it, prefs.reminderOffsets) }
                    )

                    if (prefs.remindersEnabled) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)
                        Text(
                            text = "Remind me before each meeting",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        for (offset in prefs.reminderOffsets) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${NotificationAndReminderManager.formatOffset(offset)} before",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = {
                                        viewModel.setReminderSettings(
                                            true,
                                            prefs.reminderOffsets - offset
                                        )
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove ${NotificationAndReminderManager.formatOffset(offset)} reminder",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (prefs.reminderOffsets.size < 5) {
                            UpcomingSecondaryButton(
                                text = "Add reminder",
                                onClick = { showAddDialog = true },
                                leadingIcon = Icons.Default.Add
                            )
                        }
                    }
                }
            }

            // 3. Upcoming Reminders (real preview: upcoming bookings × offsets)
            item {
                Text(
                    text = "Upcoming Reminders (${state.upcomingReminders.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            if (state.upcomingReminders.isEmpty()) {
                item {
                    UpcomingCard(
                        modifier = Modifier.fillMaxWidth(),
                        padding = PaddingValues(16.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            MutedTextBody(
                                text = if (prefs.remindersEnabled) "Nothing coming up — reminders appear once you have upcoming bookings."
                                else "Reminders are off."
                            )
                        }
                    }
                }
            }

            items(state.upcomingReminders, key = { "${it.booking.uid}:${it.minutesBefore}" }) { reminder ->
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
                                Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = UpcomingTokens.BrandPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Column {
                                Text(reminder.bookingTitle, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${reminderTimeFmt.format(Date(reminder.triggerTimeMs))} · ${NotificationAndReminderManager.formatOffset(reminder.minutesBefore)} before",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        StatusBadge(status = "armed")
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddReminderDialog(
            existingOffsets = prefs.reminderOffsets,
            onDismiss = { showAddDialog = false },
            onConfirm = { offset ->
                viewModel.setReminderSettings(true, prefs.reminderOffsets + offset)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun MutedTextBody(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = UpcomingTokens.BrandPrimary)
        )
    }
}

@Composable
private fun AddReminderDialog(
    existingOffsets: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var customMinutes by remember { mutableStateOf("") }
    val availablePresets = REMINDER_PRESETS.filter { it !in existingOffsets }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Reminder") },
        text = {
            Column {
                Text(
                    "How long before your meeting should we alert you?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (availablePresets.isNotEmpty()) {
                    for (preset in availablePresets) {
                        TextButton(onClick = { onConfirm(preset) }) {
                            Text("${NotificationAndReminderManager.formatOffset(preset)} before")
                        }
                    }
                } else {
                    Text(
                        "All presets are in use — enter a custom time below.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customMinutes,
                    onValueChange = { customMinutes = it.filter { ch -> ch.isDigit() }.take(5) },
                    label = { Text("Custom (minutes before)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            val minutes = customMinutes.toIntOrNull()
            val valid = minutes != null && minutes in 1..10080 && minutes !in existingOffsets
            TextButton(
                onClick = { minutes?.let(onConfirm) },
                enabled = valid
            ) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
