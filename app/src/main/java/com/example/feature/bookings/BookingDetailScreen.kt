package com.example.feature.bookings

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.*
import com.example.core.engine.NotificationAndReminderManager
import com.example.core.engine.SchedulingEngine
import com.example.core.engine.StripePaymentSimulator
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    bookingUid: String,
    viewModel: BookingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val item = remember(uiState.bookings, bookingUid) {
        uiState.bookings.find { it.booking.uid == bookingUid }
    }
    val context = LocalContext.current
    var showCancelDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            UpcomingTopBar(
                title = "Booking Details",
                subtitle = "Reference #${bookingUid.take(8)}",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (item == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = UpcomingTokens.BrandPrimary)
            }
        } else {
            val b = item.booking
            val startUtc = SchedulingEngine.parseIsoUtc(b.startTimeUtc)
            val endUtc = SchedulingEngine.parseIsoUtc(b.endTimeUtc)

            val fullDateFmt = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("America/New_York")
            }
            val timeFmt = SimpleDateFormat("h:mm a", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("America/New_York")
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Status & Header Card
                item {
                    UpcomingCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Status",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            StatusBadge(status = b.status)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = item.eventType?.title ?: "Meeting",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = UpcomingTokens.BrandPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = fullDateFmt.format(startUtc),
                                style = UpcomingTextStyles.monoData,
                                color = Ink
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = UpcomingTokens.BrandPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "${timeFmt.format(startUtc)} – ${timeFmt.format(endUtc)} (EDT)",
                                style = UpcomingTextStyles.monoData,
                                color = Ink
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (b.status == "accepted") {
                            UpcomingPrimaryButton(
                                text = "Join Video Room",
                                onClick = {
                                    val videoUrl = "https://upcoming.daily.co/demo-room"
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
                                    context.startActivity(intent)
                                },
                                leadingIcon = Icons.Default.Videocam
                            )
                        }
                    }
                }

                // 2. Invitee / Attendee Information Card
                item {
                    UpcomingCard {
                        Text(
                            text = "Invitee Information",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        DetailRow(label = "Name", value = item.attendee?.name ?: "—", icon = Icons.Default.Person)
                        DetailRow(label = "Email", value = item.attendee?.email ?: "—", icon = Icons.Default.Email)
                        if (!item.attendee?.phone.isNullOrBlank()) {
                            DetailRow(label = "Phone", value = item.attendee?.phone ?: "", icon = Icons.Default.Phone)
                        }
                        if (!item.attendee?.notes.isNullOrBlank()) {
                            DetailRow(label = "Notes / Prep", value = item.attendee?.notes ?: "", icon = Icons.Default.Notes)
                        }
                    }
                }

                // 3. Payment & Integration Information (Stripe)
                if (item.eventType != null && item.eventType.priceInCents > 0) {
                    item {
                        UpcomingCard {
                            Text(
                                text = "Stripe Payment Summary",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            DetailRow(
                                label = "Amount",
                                value = StripePaymentSimulator.formatPrice(item.eventType.priceInCents),
                                icon = Icons.Default.CreditCard
                            )
                            DetailRow(
                                label = "Payment Status",
                                value = if (b.paid) "Paid & Processed" else "Pending Payment",
                                icon = Icons.Default.CheckCircle
                            )
                            DetailRow(
                                label = "Payment Intent ID",
                                value = b.paymentIntentId ?: "pi_mock_upcoming_test",
                                icon = Icons.Default.Receipt
                            )
                        }
                    }
                }

                // 4. Quick Actions (Calendar Intent & Alarms)
                if (b.status == "accepted") {
                    item {
                        UpcomingCard {
                            Text(
                                text = "Actions & Reminders",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                UpcomingSecondaryButton(
                                    text = "Add to Calendar",
                                    onClick = {
                                        val calIntent = Intent(Intent.ACTION_INSERT).apply {
                                            data = CalendarContract.Events.CONTENT_URI
                                            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startUtc.time)
                                            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endUtc.time)
                                            putExtra(CalendarContract.Events.TITLE, item.eventType?.title ?: "Meeting")
                                            putExtra(CalendarContract.Events.DESCRIPTION, "Upcoming Meeting with ${item.attendee?.name ?: "Guest"}")
                                            putExtra(CalendarContract.Events.EVENT_LOCATION, "Daily.co Video Call")
                                        }
                                        context.startActivity(calIntent)
                                    },
                                    leadingIcon = Icons.Default.EventNote,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = UpcomingTokens.RadiusMedium,
                                border = BorderStroke(1.dp, SemanticError),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SemanticError)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Cancel This Booking")
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(30.dp))
                }
            }
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Meeting") },
            text = { Text("Are you sure you want to cancel this booking? This will free the time slot and notify attendees.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.cancelBooking(bookingUid) {
                            showCancelDialog = false
                            Toast.makeText(context, "Meeting cancelled & slot released.", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = SemanticError)
                ) {
                    Text("Confirm Cancellation")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("Keep Meeting")
                }
            }
        )
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
