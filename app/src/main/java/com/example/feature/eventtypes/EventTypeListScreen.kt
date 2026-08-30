package com.example.feature.eventtypes

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.core.designsystem.*
import com.example.core.engine.StripePaymentSimulator
import com.example.core.model.EventType
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventTypeListScreen(
    viewModel: EventTypesViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToCreate: () -> Unit,
    onNavigateToEdit: (Long) -> Unit,
    onOpenBookingFlow: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.syncError) {
        uiState.syncError?.let { snackbarHostState.showSnackbar("Sync failed: $it") }
    }
    var deleteCandidateId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            UpcomingTopBar(
                title = "Event Types",
                subtitle = "${uiState.eventTypes.size} scheduling links",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack,
                actions = {
                    IconButton(
                        onClick = onNavigateToCreate,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Event Type",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToCreate,
                containerColor = SurfaceCreamStrong,
                contentColor = Ink,
                shape = UpcomingTokens.RadiusLarge
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null)
                    Text(
                        "New Event Type",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Search field
            item {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text("Search event types or slugs...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = UpcomingTokens.RadiusMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = UpcomingTokens.BrandPrimary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    ),
                    singleLine = true
                )
            }

            // Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val filters = listOf(
                        "ALL" to "All Types",
                        "INDIVIDUAL" to "1-on-1",
                        "COLLECTIVE" to "Team & Collective",
                        "PAID" to "Paid (Stripe)"
                    )
                    items(filters) { (key, label) ->
                        val selected = uiState.filterType == key
                        FilterChip(
                            selected = selected,
                            onClick = { viewModel.setFilterType(key) },
                            label = { Text(label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = UpcomingTokens.SelectedBg,
                                selectedLabelColor = UpcomingTokens.BrandPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                selectedBorderColor = UpcomingTokens.BrandPrimary,
                                borderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }

            // Server sync in flight — visible spinner instead of a misleading
            // empty state while the Cloudflare round-trip lands.
            if (uiState.isLoading || uiState.isRefreshing) {
                item {
                    UpcomingLoadingRow(
                        label = if (uiState.isRefreshing) "Syncing event types…" else "Loading event types…"
                    )
                }
            }

            if (uiState.eventTypes.isEmpty() && !uiState.isLoading && !uiState.isRefreshing) {
                item {
                    UpcomingCard(
                        modifier = Modifier.fillMaxWidth(),
                        padding = PaddingValues(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.EventBusy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No Event Types Found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Create a custom event type or adjust your active search filters.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            UpcomingPrimaryButton(
                                text = "Create Event Type",
                                onClick = onNavigateToCreate,
                                leadingIcon = Icons.Default.Add,
                                modifier = Modifier.widthIn(max = 200.dp)
                            )
                        }
                    }
                }
            } else {
                items(uiState.eventTypes, key = { it.id }) { eventType ->
                    EventTypeItemCard(
                        eventType = eventType,
                        username = uiState.user?.username ?: "alex",
                        onToggleActive = { viewModel.toggleEventTypeActive(eventType) },
                        onEdit = { onNavigateToEdit(eventType.id) },
                        onDuplicate = { viewModel.duplicateEventType(eventType) },
                        onDelete = { deleteCandidateId = eventType.id },
                        onTestBooking = { onOpenBookingFlow(eventType.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // Delete Confirmation Dialog
    if (deleteCandidateId != null) {
        AlertDialog(
            onDismissRequest = { deleteCandidateId = null },
            title = { Text("Delete Event Type") },
            text = { Text("Are you sure you want to delete this event type? Any share links for this event will be deactivated.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteCandidateId?.let { viewModel.deleteEventType(it) }
                        deleteCandidateId = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = SemanticError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidateId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun EventTypeItemCard(
    eventType: EventType,
    username: String,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onTestBooking: () -> Unit
) {
    val shareUrl = "https://upcoming.io/$username/${eventType.slug}"
    var showMenu by remember { mutableStateOf(false) }

    val brandColor = try {
        Color(android.graphics.Color.parseColor(eventType.colorHex))
    } catch (e: Exception) {
        UpcomingTokens.BrandPrimary
    }

    UpcomingCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (eventType.isActive) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(brandColor)
                )
                Column {
                    Text(
                        text = eventType.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "upcoming.io/$username/${eventType.slug}",
                        style = MaterialTheme.typography.bodySmall,
                        color = UpcomingTokens.BrandPrimary
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Details") },
                        onClick = { showMenu = false; onEdit() },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        onClick = { showMenu = false; onDuplicate() },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = SemanticError) },
                        onClick = { showMenu = false; onDelete() },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = SemanticError) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (eventType.description.isNotBlank()) {
            Text(
                text = eventType.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Tags & Details row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = UpcomingTokens.RadiusFull,
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Timer, contentDescription = null, modifier = Modifier.size(12.dp))
                    Text(text = "${eventType.lengthMinutes} min", style = MaterialTheme.typography.labelSmall)
                }
            }

            StatusBadge(status = eventType.schedulingType)

            if (eventType.priceInCents > 0) {
                Surface(
                    shape = UpcomingTokens.RadiusFull,
                    color = SurfaceCard
                ) {
                    Text(
                        text = StripePaymentSimulator.formatPrice(eventType.priceInCents),
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentTeal,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Switch(
                    checked = eventType.isActive,
                    onCheckedChange = { onToggleActive() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = UpcomingTokens.BrandPrimary
                    ),
                    modifier = Modifier.height(24.dp)
                )
                Text(
                    text = if (eventType.isActive) "Active" else "Off",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (eventType.isActive) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShareLinkButton(url = shareUrl)

                Button(
                    onClick = onTestBooking,
                    shape = UpcomingTokens.RadiusMedium,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = UpcomingTokens.BrandPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Text("Invitee View", style = MaterialTheme.typography.labelSmall)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(12.dp))
                }
            }
        }
    }
}
