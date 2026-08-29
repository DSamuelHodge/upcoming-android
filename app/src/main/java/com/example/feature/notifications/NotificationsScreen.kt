package com.example.feature.notifications

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.*
import com.example.core.engine.NotificationAndReminderManager
import com.example.core.model.Booking
import com.example.core.repository.UpcomingRepository
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    repository: UpcomingRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val upcomingBookings by repository.upcomingBookings.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    // Persisted device-local prefs (Settings → Notifications). Survives
    // restarts and gates the repository's post-booking alarm + FCM paths.
    val userPreferences = remember { com.example.core.prefs.UserPreferences(context) }
    val prefs by userPreferences.notificationPrefs.collectAsState(
        initial = com.example.core.prefs.NotificationPrefs()
    )
    val pushAlertsEnabled = prefs.pushAlertsEnabled
    val soundVibrateEnabled = prefs.soundVibrateEnabled
    val tenMinReminderEnabled = prefs.tenMinReminderEnabled

    Scaffold(
        topBar = {
            UpcomingTopBar(
                title = "Notifications & Reminders",
                subtitle = "Device-level alerts; toggles persist and gate booking alarms",
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
            // 1. Overview card
            item {
                UpcomingCard(
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                    borderColor = UpcomingTokens.BrandPrimary.copy(alpha = 0.3f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.NotificationsActive,
                            contentDescription = null,
                            tint = UpcomingTokens.BrandPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "Notification Engine Active",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "FCM High-Priority Channels & Exact Alarm Manager (SCHEDULE_EXACT_ALARM)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // 2. Interactive Test Actions
            item {
                UpcomingCard {
                    Text(
                        text = "Trigger Simulated Alerts",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    UpcomingPrimaryButton(
                        text = "Simulate FCM New Booking Alert",
                        onClick = {
                            NotificationAndReminderManager.triggerFcmNotification(
                                context = context,
                                title = "New Meeting Booked! 📅",
                                body = "Sarah Jenkins booked '30 Min Strategy Session' for Tomorrow at 2:00 PM.",
                                bookingUid = "demo-uid-123"
                            )
                            Toast.makeText(context, "FCM Notification dispatched!", Toast.LENGTH_SHORT).show()
                        },
                        leadingIcon = Icons.Default.CloudSync
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    UpcomingSecondaryButton(
                        text = "Simulate 10-Min Pre-Meeting Exact Alarm",
                        onClick = {
                            NotificationAndReminderManager.triggerFcmNotification(
                                context = context,
                                title = "Meeting in 10 minutes: Daily Standup ⏰",
                                body = "Tap to open your Daily.co video room directly.",
                                bookingUid = "demo-uid-456"
                            )
                            Toast.makeText(context, "Exact alarm notification fired!", Toast.LENGTH_SHORT).show()
                        },
                        leadingIcon = Icons.Default.Alarm
                    )
                }
            }

            // 3. Notification Preferences
            item {
                UpcomingCard {
                    Text(
                        text = "Preferences",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Push Notifications", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Text("Receive instant alerts when an invitee books time", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = pushAlertsEnabled,
                            onCheckedChange = { scope.launch { userPreferences.setPushAlertsEnabled(it) } },
                            colors = SwitchDefaults.colors(checkedTrackColor = UpcomingTokens.BrandPrimary)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("10-Minute Pre-Meeting Alarms", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Text("Schedule local exact device alarms before meetings start", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = tenMinReminderEnabled,
                            onCheckedChange = { scope.launch { userPreferences.setTenMinReminderEnabled(it) } },
                            colors = SwitchDefaults.colors(checkedTrackColor = UpcomingTokens.BrandPrimary)
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outline)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Sound & Vibration", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Text("Play sound and vibrate on upcoming meeting triggers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = soundVibrateEnabled,
                            onCheckedChange = { scope.launch { userPreferences.setSoundVibrateEnabled(it) } },
                            colors = SwitchDefaults.colors(checkedTrackColor = UpcomingTokens.BrandPrimary)
                        )
                    }
                }
            }

            // 4. Scheduled Meeting Alarms Queue
            item {
                Text(
                    text = "Scheduled Alarms Queue (${upcomingBookings.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            items(upcomingBookings) { booking ->
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
                            Icon(Icons.Default.AlarmOn, contentDescription = null, tint = SemanticSuccess, modifier = Modifier.size(20.dp))
                            Column {
                                Text("Alarm Set: #${booking.uid.take(8)}", style = MaterialTheme.typography.titleSmall)
                                Text("Trigger: 10 mins prior to start", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        StatusBadge(status = "armed")
                    }
                }
            }
        }
    }
}
