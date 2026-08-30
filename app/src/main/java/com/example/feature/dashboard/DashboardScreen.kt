package com.example.feature.dashboard

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.*
import com.example.core.engine.SchedulingEngine
import com.example.core.engine.StripePaymentSimulator
import com.example.core.model.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToEventTypes: () -> Unit,
    onNavigateToAvailability: () -> Unit,
    onNavigateToBookings: () -> Unit,
    onNavigateToNotifications: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onOpenBookingFlow: (Long) -> Unit,
    onOpenBookingDetail: (String) -> Unit,
    onNavigateToCreateEventType: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            UpcomingTopBar(
                title = "Upcoming",
                subtitle = "Booking & Scheduling Platform",
                userInitials = uiState.user?.displayName?.split(" ")?.mapNotNull { it.firstOrNull()?.toString() }?.take(2)?.joinToString("") ?: "JD",
                onAvatarClick = onNavigateToSettings,
                actions = {
                    IconButton(
                        onClick = onNavigateToNotifications,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                ) {
                                    Text("1")
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications & Reminders",
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            // Geometric Balance FAB: light blue rounded-2xl
            FloatingActionButton(
                onClick = {
                    if (onNavigateToCreateEventType != null) {
                        onNavigateToCreateEventType()
                    } else {
                        onNavigateToEventTypes()
                    }
                },
                containerColor = SurfaceCreamStrong,
                contentColor = Ink,
                shape = UpcomingTokens.RadiusLarge,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .size(56.dp)
                    .padding(bottom = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create Event Type",
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Host Profile Banner (Clean Geometric Card)
            item {
                HostHeaderCard(
                    user = uiState.user,
                    onShareProfile = {
                        val shareUrl = "https://getupcoming.app/${uiState.user?.username ?: ""}"
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Book time with me on Upcoming: $shareUrl")
                        }
                        context.startActivity(Intent.createChooser(intent, "Share Booking Page"))
                    }
                )
            }

            // 2. Geometric Featured Callout (Upcoming Widget Banner)
            item {
                GeometricGlanceWidgetBanner(
                    upcomingCount = uiState.upcomingBookings.size,
                    nextBooking = uiState.nextBooking,
                    nextAttendee = uiState.nextBookingAttendee,
                    nextEventType = uiState.nextBookingEventType,
                    onJoinMeeting = { url ->
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback
                        }
                    },
                    onViewDetails = { uid -> onOpenBookingDetail(uid) }
                )
            }

            // 3. Performance Metrics
            item {
                GeometricSectionHeader(title = "Performance Metrics")
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "Upcoming",
                        value = "${uiState.upcomingBookings.size}",
                        subtitle = "Active Meetings",
                        icon = Icons.Outlined.EventAvailable,
                        iconTint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Hours",
                        value = String.format(Locale.US, "%.1fh", uiState.hoursBookedThisMonth),
                        subtitle = "This Month",
                        icon = Icons.Outlined.Schedule,
                        iconTint = SemanticSuccess,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Revenue",
                        value = StripePaymentSimulator.formatPrice(uiState.totalRevenueCents),
                        subtitle = "Processed",
                        icon = Icons.Outlined.CreditCard,
                        iconTint = AccentTeal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. Quick Actions
            item {
                GeometricSectionHeader(title = "Quick Navigation")
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionTile(
                        icon = Icons.Outlined.AddCircleOutline,
                        title = "New Event",
                        subtitle = "Create type",
                        color = MaterialTheme.colorScheme.primary,
                        onClick = onNavigateToEventTypes,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Outlined.DateRange,
                        title = "Availability",
                        subtitle = "Hours & rules",
                        color = AccentTeal,
                        onClick = onNavigateToAvailability,
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionTile(
                        icon = Icons.Outlined.CalendarMonth,
                        title = "All Bookings",
                        subtitle = "Manage events",
                        color = SemanticSuccess,
                        onClick = onNavigateToBookings,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 5. Event Types Section (Geometric Balance Cards)
            item {
                GeometricSectionHeader(
                    title = "Event Types",
                    tag = "Online-First"
                )
            }

            items(uiState.eventTypes) { eventType ->
                GeometricEventTypeCard(
                    eventType = eventType,
                    username = uiState.user?.username ?: "",
                    onBookNow = { onOpenBookingFlow(eventType.id) }
                )
            }
        }
    }
}

@Composable
fun HostHeaderCard(
    user: User?,
    onShareProfile: () -> Unit
) {
    UpcomingCard(
        backgroundColor = MaterialTheme.colorScheme.surface,
        borderColor = MaterialTheme.colorScheme.outline,
        padding = PaddingValues(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = SurfaceCard,
                    border = BorderStroke(1.dp, Hairline),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = user?.displayName?.split(" ")?.mapNotNull { it.firstOrNull()?.toString() }?.take(2)?.joinToString("") ?: "JD",
                            color = Ink,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Column {
                    Text(
                        text = user?.displayName ?: "Alex Rivera",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "getupcoming.app/${user?.username ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Surface(
                            shape = UpcomingTokens.RadiusFull,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = user?.timezone?.substringAfterLast("/")?.replace("_", " ") ?: "EST",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = onShareProfile,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Profile",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun GeometricGlanceWidgetBanner(
    upcomingCount: Int,
    nextBooking: Booking?,
    nextAttendee: Attendee?,
    nextEventType: EventType?,
    onJoinMeeting: (String) -> Unit,
    onViewDetails: (String) -> Unit
) {
    Surface(
        shape = UpcomingTokens.RadiusLarge,
        color = SurfaceCard,
        border = BorderStroke(1.dp, Hairline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(UpcomingTokens.RadiusMedium)
                        .background(Ink),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Upcoming Widget",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "UPCOMING WIDGET",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp
                        ),
                        color = Ink
                    )
                    Text(
                        text = if (upcomingCount > 0) "You have $upcomingCount meetings scheduled" else "No meetings scheduled today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Ink
                    )
                }
            }

            if (nextBooking != null && nextEventType != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val startDate = SchedulingEngine.parseIsoUtc(nextBooking.startTimeUtc)
                val timeFmt = SimpleDateFormat("EEE, MMM d • h:mm a", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("America/New_York")
                }

                Surface(
                    shape = UpcomingTokens.RadiusMedium,
                    color = Color.White,
                    border = BorderStroke(1.dp, Hairline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = nextEventType.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Ink
                                )
                                Text(
                                    text = "with ${nextAttendee?.name ?: nextAttendee?.email ?: "Guest"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedText
                                )
                            }
                            StatusBadge(status = nextBooking.status)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = timeFmt.format(startDate),
                                style = UpcomingTextStyles.monoLabel,
                                color = Ink
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            UpcomingPrimaryButton(
                                text = "Join Video Call",
                                onClick = { onJoinMeeting("https://upcoming.daily.co/demo-room") },
                                leadingIcon = Icons.Default.Videocam,
                                modifier = Modifier.weight(1f)
                            )
                            UpcomingSecondaryButton(
                                text = "Details",
                                onClick = { onViewDetails(nextBooking.uid) },
                                modifier = Modifier.weight(0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier
) {
    UpcomingCard(
        modifier = modifier,
        padding = PaddingValues(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun QuickActionTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    UpcomingCard(
        modifier = modifier,
        onClick = onClick,
        padding = PaddingValues(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(UpcomingTokens.RadiusMedium)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun GeometricEventTypeCard(
    eventType: EventType,
    username: String,
    onBookNow: () -> Unit
) {
    val shareUrl = "getupcoming.app/$username/${eventType.slug}"

    UpcomingCard(
        modifier = Modifier.fillMaxWidth(),
        padding = PaddingValues(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = eventType.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = eventType.description.ifBlank { "${eventType.lengthMinutes}m intro or sync call" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Status Indicator Dot (Geometric Balance style)
            Box(
                modifier = Modifier
                    .padding(top = 4.dp, start = 8.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(
                        if (eventType.priceInCents > 0) MaterialTheme.colorScheme.primary else SemanticSuccess
                    )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Geometric Chips Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Duration Chip
                Surface(
                    shape = UpcomingTokens.RadiusSmall,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "${eventType.lengthMinutes}m",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // URL Chip
                Surface(
                    shape = UpcomingTokens.RadiusSmall,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Link,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = shareUrl,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                if (eventType.priceInCents > 0) {
                    Surface(
                        shape = UpcomingTokens.RadiusSmall,
                        color = SurfaceCreamStrong
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CreditCard,
                                contentDescription = "Stripe Active",
                                tint = AccentTeal,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Book flow action
            Button(
                onClick = onBookNow,
                shape = UpcomingTokens.RadiusMedium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = SurfaceCreamStrong,
                    contentColor = Ink
                ),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Book", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

