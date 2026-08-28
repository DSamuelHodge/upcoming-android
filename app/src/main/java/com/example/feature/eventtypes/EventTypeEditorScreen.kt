package com.example.feature.eventtypes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.core.designsystem.*
import com.example.core.model.EventType
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventTypeEditorScreen(
    eventTypeId: Long,
    viewModel: EventTypesViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val existing = remember(uiState.eventTypes, eventTypeId) {
        uiState.eventTypes.find { it.id == eventTypeId }
    }

    var title by remember(existing) { mutableStateOf(existing?.title ?: "") }
    var slug by remember(existing) { mutableStateOf(existing?.slug ?: "") }
    var description by remember(existing) { mutableStateOf(existing?.description ?: "") }
    var durationMinutes by remember(existing) { mutableIntStateOf(existing?.lengthMinutes ?: 30) }
    var bufferBefore by remember(existing) { mutableIntStateOf(existing?.bufferBefore ?: 5) }
    var bufferAfter by remember(existing) { mutableIntStateOf(existing?.bufferAfter ?: 10) }
    var minNoticeMinutes by remember(existing) { mutableIntStateOf(existing?.minBookingNotice ?: 60) }
    var schedulingType by remember(existing) { mutableStateOf(existing?.schedulingType ?: "individual") }
    var locationType by remember(existing) { mutableStateOf("daily") }
    var isPaid by remember(existing) { mutableStateOf((existing?.priceInCents ?: 0) > 0) }
    var priceDollars by remember(existing) { mutableStateOf(if ((existing?.priceInCents ?: 0) > 0) "${(existing?.priceInCents ?: 0) / 100}" else "50") }
    var selectedColorHex by remember(existing) { mutableStateOf(existing?.colorHex ?: "#0B5CFF") }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            UpcomingTopBar(
                title = if (eventTypeId == 0L) "New Event Type" else "Edit Event Type",
                subtitle = "Configure scheduling rules & integrations",
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
                        text = "Cancel",
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f)
                    )
                    UpcomingPrimaryButton(
                        text = "Save Event Type",
                        onClick = {
                            if (title.isBlank()) {
                                errorMessage = "Please enter an event title"
                                return@UpcomingPrimaryButton
                            }
                            val safeSlug = slug.ifBlank {
                                title.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "")
                            }

                            val locationsJson = when (locationType) {
                                "daily" -> """[{"type":"integrations:daily","label":"Daily Video Call","url":"https://upcoming.daily.co/$safeSlug"}]"""
                                "meet" -> """[{"type":"googleMeet","label":"Google Meet"}]"""
                                "phone" -> """[{"type":"userPhone","label":"Phone Call"}]"""
                                else -> """[{"type":"inPerson","label":"In-Person Meeting"}]"""
                            }

                            val priceCents = if (isPaid) {
                                (priceDollars.toIntOrNull() ?: 0) * 100
                            } else 0

                            val updated = EventType(
                                id = eventTypeId,
                                ownerUserId = 1,
                                slug = safeSlug,
                                title = title,
                                description = description,
                                lengthMinutes = durationMinutes,
                                slotIntervalMinutes = durationMinutes,
                                bufferBefore = bufferBefore,
                                bufferAfter = bufferAfter,
                                schedulingType = schedulingType,
                                locationsJson = locationsJson,
                                minBookingNotice = minNoticeMinutes,
                                priceInCents = priceCents,
                                currency = "usd",
                                colorHex = selectedColorHex,
                                isActive = true
                            )

                            viewModel.saveEventType(updated) {
                                onNavigateBack()
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
            if (errorMessage != null) {
                item {
                    Surface(
                        shape = UpcomingTokens.RadiusMedium,
                        color = AccentRoseLight,
                        border = BorderStroke(1.dp, AccentRose)
                    ) {
                        Text(
                            text = errorMessage ?: "",
                            color = AccentRose,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // 1. Basic Info Card
            item {
                UpcomingCard {
                    Text(
                        text = "Event Details",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (eventTypeId == 0L) {
                                slug = it.lowercase().replace(" ", "-").replace(Regex("[^a-z0-9-]"), "")
                            }
                        },
                        label = { Text("Event Name *") },
                        placeholder = { Text("e.g. 30 Min Strategy Session") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = UpcomingTokens.RadiusMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = slug,
                        onValueChange = { slug = it },
                        label = { Text("Link URL Slug") },
                        prefix = { Text("upcoming.io/alex/") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = UpcomingTokens.RadiusMedium
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description & Instructions") },
                        placeholder = { Text("Share what invitees should prepare for this session...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = UpcomingTokens.RadiusMedium
                    )
                }
            }

            // 2. Duration & Buffers Card
            item {
                UpcomingCard {
                    Text(
                        text = "Duration & Timing Rules",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Meeting Duration", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val durations = listOf(15, 30, 45, 60, 90)
                        items(durations) { dur ->
                            val isSelected = durationMinutes == dur
                            FilterChip(
                                selected = isSelected,
                                onClick = { durationMinutes = dur },
                                label = { Text("$dur min") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = UpcomingTokens.BrandBlue,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Buffer Time (Before & After)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = "$bufferBefore min",
                            onValueChange = { bufferBefore = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 },
                            label = { Text("Buffer Before") },
                            modifier = Modifier.weight(1f),
                            shape = UpcomingTokens.RadiusMedium
                        )
                        OutlinedTextField(
                            value = "$bufferAfter min",
                            onValueChange = { bufferAfter = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 },
                            label = { Text("Buffer After") },
                            modifier = Modifier.weight(1f),
                            shape = UpcomingTokens.RadiusMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = "${minNoticeMinutes / 60} hours",
                        onValueChange = {
                            val h = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 1
                            minNoticeMinutes = h * 60
                        },
                        label = { Text("Minimum Notice (Notice before booking)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = UpcomingTokens.RadiusMedium
                    )
                }
            }

            // 3. Multi-Host Scheduling Type Card
            item {
                UpcomingCard {
                    Text(
                        text = "Scheduling Type (Multi-Host Routing)",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val types = listOf(
                        Triple("individual", "1-on-1 (Individual)", "Book time directly with primary host"),
                        Triple("round_robin", "Round Robin (Team)", "Auto-distribute appointments evenly across free team hosts"),
                        Triple("collective", "Collective (Team)", "Invitees can only book when ALL team hosts are simultaneously free")
                    )

                    types.forEach { (typeKey, typeTitle, typeDesc) ->
                        val selected = schedulingType == typeKey
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(UpcomingTokens.RadiusMedium)
                                .clickable { schedulingType = typeKey },
                            shape = UpcomingTokens.RadiusMedium,
                            color = if (selected) UpcomingTokens.BrandBlueLight else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                1.5.dp,
                                if (selected) UpcomingTokens.BrandBlue else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = { schedulingType = typeKey },
                                    colors = RadioButtonDefaults.colors(selectedColor = UpcomingTokens.BrandBlue)
                                )
                                Column {
                                    Text(
                                        text = typeTitle,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = if (selected) UpcomingTokens.BrandBlue else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = typeDesc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Location Integration
            item {
                UpcomingCard {
                    Text(
                        text = "Location / Video Call",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val locations = listOf(
                        "daily" to ("Daily.co Video (Recommended)" to Icons.Default.Videocam),
                        "meet" to ("Google Meet" to Icons.Default.VideoCall),
                        "phone" to ("Phone Call" to Icons.Default.Phone),
                        "inPerson" to ("In-Person Meeting" to Icons.Default.Place)
                    )

                    locations.forEach { (locKey, info) ->
                        val (locTitle, icon) = info
                        val selected = locationType == locKey
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(UpcomingTokens.RadiusMedium)
                                .clickable { locationType = locKey },
                            shape = UpcomingTokens.RadiusMedium,
                            color = if (selected) UpcomingTokens.BrandBlueLight else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, if (selected) UpcomingTokens.BrandBlue else MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(imageVector = icon, contentDescription = null, tint = UpcomingTokens.BrandBlue)
                                Text(
                                    text = locTitle,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (selected) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = UpcomingTokens.BrandBlue)
                                }
                            }
                        }
                    }
                }
            }

            // 5. Stripe Payments Collection Card
            item {
                UpcomingCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(imageVector = Icons.Outlined.CreditCard, contentDescription = null, tint = AccentPurple)
                            Column {
                                Text(
                                    text = "Stripe Payment Collection",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Require deposit or fee to book",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isPaid,
                            onCheckedChange = { isPaid = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = AccentPurple
                            )
                        )
                    }

                    if (isPaid) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = priceDollars,
                            onValueChange = { priceDollars = it },
                            label = { Text("Booking Fee (USD)") },
                            prefix = { Text("$ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            shape = UpcomingTokens.RadiusMedium
                        )
                    }
                }
            }

            // 6. Accent Color
            item {
                UpcomingCard {
                    Text(
                        text = "Card Theme Color",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val colors = listOf(
                        "#0B5CFF", "#10B981", "#7C3AED", "#F59E0B", "#EF4444", "#0D9488"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        colors.forEach { hex ->
                            val col = Color(android.graphics.Color.parseColor(hex))
                            val isSelected = selectedColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .clickable { selectedColorHex = hex }
                                    .then(
                                        if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
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
}
