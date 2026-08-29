package com.example.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.*
import com.example.core.network.LocationDto
import com.example.ui.theme.*

// Curated IANA list — the common zones a host actually picks from.
private val COMMON_TIMEZONES = listOf(
    "America/New_York", "America/Chicago", "America/Denver", "America/Los_Angeles",
    "America/Phoenix", "America/Sao_Paulo", "Europe/London", "Europe/Paris",
    "Europe/Berlin", "Europe/Madrid", "Europe/Amsterdam", "Africa/Cairo",
    "Asia/Dubai", "Asia/Kolkata", "Asia/Singapore", "Asia/Tokyo", "Asia/Shanghai",
    "Australia/Sydney", "Pacific/Auckland", "UTC"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateToNotifications: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.savedTick) {
        if (state.savedTick > 0) snackbarHostState.showSnackbar("Settings saved")
    }
    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    var showProfileEditor by remember { mutableStateOf(false) }
    var showTimezonePicker by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            UpcomingTopBar(
                title = "Settings",
                subtitle = "Account, time & booking defaults",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Account -------------------------------------------------------
            item {
                val user = state.user
                UpcomingCard {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AvatarCircle(initials = initialsOf(user?.displayName, user?.email))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = user?.displayName?.ifBlank { "Set your name" } ?: "…",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = user?.email ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showProfileEditor = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit profile", tint = UpcomingTokens.BrandPrimary)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(12.dp))
                    SettingDataRow(
                        icon = Icons.Outlined.Link,
                        label = "Booking link",
                        value = "/u/${user?.username ?: ""}",
                        valueMono = true
                    )
                }
            }

            // 2. Time & Locale -------------------------------------------------
            item {
                SectionHeader("TIME & LOCALE")
                UpcomingCard {
                    SettingRow(
                        icon = Icons.Outlined.Public,
                        label = "Default timezone",
                        description = "Drives availability and every booking display",
                        value = state.scheduleTimezone ?: "UTC",
                        valueMono = true,
                        onClick = { showTimezonePicker = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null, tint = MutedText, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Time format", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
                            Text("Applies across the dashboard and bookings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TimeFormatToggle(
                            selected = state.timeFormat,
                            onSelect = viewModel::setTimeFormat
                        )
                    }
                }
            }

            // 3. Default location ----------------------------------------------
            item {
                SectionHeader("BOOKING DEFAULTS")
                UpcomingCard {
                    SettingRow(
                        icon = Icons.Outlined.LocationOn,
                        label = "Default location",
                        description = "Prefilled when you create new event types",
                        value = state.defaultLocation?.let { locationLabel(it) } ?: "None set",
                        onClick = { showLocationPicker = true }
                    )
                }
            }

            // 4. Notifications --------------------------------------------------
            item {
                SectionHeader("APP")
                UpcomingCard {
                    SettingRow(
                        icon = Icons.Outlined.NotificationsActive,
                        label = "Notifications & Reminders",
                        description = "Push alerts and exact pre-meeting alarms",
                        value = "On device",
                        onClick = onNavigateToNotifications,
                        showChevron = true
                    )
                }
            }

            // 5. Integrations ---------------------------------------------------
            item {
                SectionHeader("INTEGRATIONS")
                UpcomingCard {
                    SettingRow(
                        icon = Icons.Outlined.Videocam,
                        label = "Daily.co Video",
                        description = "Video rooms minted per booking, server-side",
                        value = "Active",
                        valueTint = SemanticSuccess,
                        onClick = null
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline)
                    SettingRow(
                        icon = Icons.Outlined.CalendarMonth,
                        label = "Calendar Connections",
                        description = "Google / Outlook two-way sync",
                        value = "Coming soon",
                        valueTint = MutedText,
                        onClick = null
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline)
                    SettingRow(
                        icon = Icons.Outlined.CreditCard,
                        label = "Payments",
                        description = "Collect payment when invitees book",
                        value = "Test mode",
                        valueTint = AccentTeal,
                        onClick = null
                    )
                }
            }
        }
    }

    val currentUser = state.user
    if (showProfileEditor && currentUser != null) {
        ProfileEditDialog(
            user = currentUser,
            saving = state.saving,
            onDismiss = { showProfileEditor = false },
            onSave = { name, email, username, avatar ->
                viewModel.updateProfile(name, email, username, avatar)
                showProfileEditor = false
            }
        )
    }
    if (showTimezonePicker) {
        TimezonePickerDialog(
            current = state.scheduleTimezone ?: "UTC",
            onDismiss = { showTimezonePicker = false },
            onSelect = { tz ->
                viewModel.updateTimezone(tz)
                showTimezonePicker = false
            }
        )
    }
    if (showLocationPicker) {
        DefaultLocationDialog(
            current = state.defaultLocation,
            onDismiss = { showLocationPicker = false },
            onSave = { loc ->
                viewModel.setDefaultLocation(loc)
                showLocationPicker = false
            }
        )
    }
}

// --------------------------------------------------------------------- pieces

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp, fontFamily = DmMonoFontFamily),
        color = MutedText
    )
}

@Composable
private fun AvatarCircle(initials: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(UpcomingTokens.BrandPrimary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.titleMedium.copy(color = OnPrimary, fontWeight = FontWeight.SemiBold)
        )
    }
}

private fun initialsOf(name: String?, email: String?): String {
    val source = name?.takeIf { it.isNotBlank() } ?: email?.takeIf { it.isNotBlank() } ?: "?"
    return source.trim().split(Regex("\\s+")).take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("")
}

@Composable
private fun TimeFormatToggle(selected: String, onSelect: (String) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf("12h", "24h").forEach { format ->
            val isSelected = selected == format
            Surface(
                shape = UpcomingTokens.RadiusMedium,
                color = if (isSelected) UpcomingTokens.BrandPrimary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.clickable { onSelect(format) }
            ) {
                Text(
                    text = format,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    label: String,
    description: String,
    value: String,
    valueMono: Boolean = false,
    valueTint: androidx.compose.ui.graphics.Color? = null,
    onClick: (() -> Unit)?,
    showChevron: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = UpcomingTokens.BrandPrimary, modifier = Modifier.size(22.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            text = value,
            style = if (valueMono) MaterialTheme.typography.labelMedium.copy(fontFamily = DmMonoFontFamily) else MaterialTheme.typography.labelMedium,
            color = valueTint ?: MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (showChevron) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MutedText, modifier = Modifier.size(16.dp))
        }
    }
}

// Compact key/value row without a description (used inside the account card).
@Composable
private fun SettingDataRow(icon: ImageVector, label: String, value: String, valueMono: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MutedText, modifier = Modifier.size(18.dp))
        Text(label, style = MaterialTheme.typography.bodySmall, color = MutedText, modifier = Modifier.weight(1f))
        Text(
            text = value,
            style = if (valueMono) MaterialTheme.typography.labelMedium.copy(fontFamily = DmMonoFontFamily) else MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ProfileEditDialog(
    user: com.example.core.model.User,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, email: String, username: String, avatarUrl: String) -> Unit
) {
    var name by remember { mutableStateOf(user.displayName) }
    var email by remember { mutableStateOf(user.email) }
    var username by remember { mutableStateOf(user.username) }
    var avatar by remember { mutableStateOf(user.avatarUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Display name") }, singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true)
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username (booking slug)") }, singleLine = true)
                OutlinedTextField(value = avatar, onValueChange = { avatar = it }, label = { Text("Avatar URL") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && name.isNotBlank() && email.isNotBlank() && username.isNotBlank(),
                onClick = { onSave(name.trim(), email.trim(), username.trim(), avatar.trim()) }
            ) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimezonePickerDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default timezone") },
        text = {
            LazyColumn(modifier = Modifier.height(320.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                items(COMMON_TIMEZONES.size) { index ->
                    val tz = COMMON_TIMEZONES[index]
                    val isSelected = tz == current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(UpcomingTokens.RadiusMedium)
                            .background(if (isSelected) UpcomingTokens.CreamStrongBg else androidx.compose.ui.graphics.Color.Transparent)
                            .clickable { onSelect(tz) }
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tz,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = DmMonoFontFamily),
                            color = if (isSelected) UpcomingTokens.BrandPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun DefaultLocationDialog(
    current: LocationDto?,
    onDismiss: () -> Unit,
    onSave: (LocationDto?) -> Unit
) {
    var selectedType by remember {
        mutableStateOf(current?.type ?: "integrations:daily")
    }
    var label by remember { mutableStateOf(current?.label ?: "") }
    var url by remember { mutableStateOf(current?.url ?: "") }
    var address by remember { mutableStateOf(current?.address ?: "") }
    var phone by remember { mutableStateOf(current?.phone ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default location") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "integrations:daily" to "Video",
                        "inPerson" to "In person",
                        "userPhone" to "Phone"
                    ).forEach { (type, display) ->
                        val isSelected = selectedType == type
                        Surface(
                            shape = UpcomingTokens.RadiusMedium,
                            color = if (isSelected) UpcomingTokens.BrandPrimary else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { selectedType = type }
                        ) {
                            Text(
                                text = display,
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isSelected) OnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Label") }, singleLine = true)
                when (selectedType) {
                    "integrations:daily" -> OutlinedTextField(
                        value = url, onValueChange = { url = it },
                        label = { Text("Permanent room URL (optional — blank mints per-booking rooms)") }, singleLine = true
                    )
                    "inPerson" -> OutlinedTextField(
                        value = address, onValueChange = { address = it },
                        label = { Text("Address") }, singleLine = true
                    )
                    "userPhone" -> OutlinedTextField(
                        value = phone, onValueChange = { phone = it },
                        label = { Text("Phone number") }, singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val location = when (selectedType) {
                    "integrations:daily" -> LocationDto(
                        type = selectedType,
                        label = label.ifBlank { "Video (Daily.co)" },
                        url = url.trim().ifBlank { null }
                    )
                    "inPerson" -> LocationDto(
                        type = selectedType,
                        label = label.ifBlank { "In person" },
                        address = address.trim().ifBlank { null }
                    )
                    else -> LocationDto(
                        type = selectedType,
                        label = label.ifBlank { "Phone call" },
                        phone = phone.trim().ifBlank { null }
                    )
                }
                onSave(location)
            }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = { onSave(null) }) { Text("Remove default") }
        }
    )
}

private fun locationLabel(location: LocationDto): String =
    location.label ?: when (location.type) {
        "integrations:daily" -> "Video (Daily.co)"
        "inPerson" -> location.address ?: "In person"
        "userPhone" -> location.phone ?: "Phone call"
        else -> location.type
    }
