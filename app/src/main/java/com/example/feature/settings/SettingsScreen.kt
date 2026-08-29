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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.*
import com.example.core.network.LocationDto
import com.example.core.network.LocationsMapDto
import com.example.ui.theme.*

// Comprehensive IANA list — the zones a host actually picks from, ordered
// west→east: Americas, Europe, Africa/Middle East, Asia, Pacific, UTC.
private val COMMON_TIMEZONES = listOf(
    "Pacific/Honolulu", "America/Anchorage", "America/Los_Angeles", "America/Vancouver",
    "America/Phoenix", "America/Denver", "America/Edmonton", "America/Mexico_City",
    "America/Chicago", "America/Winnipeg", "America/New_York", "America/Toronto",
    "America/Havana", "America/Bogota", "America/Lima", "America/Caracas",
    "America/Santiago", "America/Sao_Paulo", "America/Argentina/Buenos_Aires", "America/Montevideo",
    "UTC", "Atlantic/Reykjavik", "Europe/London", "Europe/Dublin",
    "Europe/Lisbon", "Europe/Madrid", "Europe/Paris", "Europe/Brussels",
    "Europe/Amsterdam", "Europe/Berlin", "Europe/Zurich", "Europe/Vienna",
    "Europe/Copenhagen", "Europe/Oslo", "Europe/Stockholm", "Europe/Warsaw",
    "Europe/Prague", "Europe/Budapest", "Europe/Rome", "Europe/Athens",
    "Europe/Helsinki", "Europe/Stockholm", "Europe/Bucharest", "Europe/Kyiv",
    "Europe/Istanbul", "Europe/Moscow", "Africa/Casablanca", "Africa/Lagos",
    "Africa/Cairo", "Africa/Johannesburg", "Africa/Nairobi", "Asia/Jerusalem",
    "Asia/Riyadh", "Asia/Baghdad", "Asia/Tehran", "Asia/Dubai",
    "Asia/Karachi", "Asia/Kolkata", "Asia/Kathmandu", "Asia/Dhaka",
    "Asia/Bangkok", "Asia/Jakarta", "Asia/Singapore", "Asia/Hong_Kong",
    "Asia/Shanghai", "Asia/Taipei", "Asia/Manila", "Asia/Seoul",
    "Asia/Tokyo", "Australia/Perth", "Australia/Adelaide", "Australia/Sydney",
    "Australia/Melbourne", "Australia/Brisbane", "Pacific/Auckland", "Pacific/Fiji"
).distinct()

private const val TYPE_DAILY = "integrations:daily"
private const val TYPE_IN_PERSON = "inPerson"
private const val TYPE_PHONE = "userPhone"

// Bring-your-own integration credentials (stored AES-256-GCM-encrypted
// server-side; the app only ever sees masked hints back).
private data class CredentialSpec(
    val type: String,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val secret: Boolean
)

private val CREDENTIAL_SPECS = listOf(
    CredentialSpec("daily_api_key", "Daily.co API Key", "Mint private video rooms with your own Daily account", Icons.Outlined.Videocam, secret = true),
    CredentialSpec("ical_url", "iCal Feed URL", "Read external calendars over a private iCal (.ics) link", Icons.Outlined.CalendarMonth, secret = false),
    CredentialSpec("caldav_url", "CalDAV Server URL", "Two-way sync with a CalDAV calendar server", Icons.Outlined.EventRepeat, secret = false),
    CredentialSpec("stripe_secret_key", "Stripe Secret Key", "Collect booking payments with your own Stripe account", Icons.Outlined.CreditCard, secret = true)
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
    var editingLocationType by remember { mutableStateOf<String?>(null) }
    var editingCredential by remember { mutableStateOf<CredentialSpec?>(null) }

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

            // 3. Booking defaults ----------------------------------------------
            item {
                SectionHeader("BOOKING DEFAULTS")
                UpcomingCard {
                    Text(
                        text = "Configure each location, then pick the default used for new event types.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    LocationDefaultRow(
                        icon = Icons.Outlined.Videocam,
                        label = "Video",
                        entry = state.locations.daily,
                        isDefault = state.defaultLocationType == TYPE_DAILY,
                        onClick = { editingLocationType = TYPE_DAILY },
                        onMakeDefault = { viewModel.setDefaultLocationType(TYPE_DAILY) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline)
                    LocationDefaultRow(
                        icon = Icons.Outlined.LocationOn,
                        label = "In person",
                        entry = state.locations.inPerson,
                        isDefault = state.defaultLocationType == TYPE_IN_PERSON,
                        onClick = { editingLocationType = TYPE_IN_PERSON },
                        onMakeDefault = { viewModel.setDefaultLocationType(TYPE_IN_PERSON) }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline)
                    LocationDefaultRow(
                        icon = Icons.Outlined.Call,
                        label = "Phone",
                        entry = state.locations.userPhone,
                        isDefault = state.defaultLocationType == TYPE_PHONE,
                        onClick = { editingLocationType = TYPE_PHONE },
                        onMakeDefault = { viewModel.setDefaultLocationType(TYPE_PHONE) }
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

            // 5. Integrations & credentials -------------------------------------
            item {
                SectionHeader("INTEGRATIONS")
                UpcomingCard {
                    Text(
                        text = "Store your own API keys and private URLs — encrypted server-side, shown masked here only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    CREDENTIAL_SPECS.forEachIndexed { index, spec ->
                        if (index > 0) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outline)
                        }
                        val hint = state.credentialHints[spec.type]
                        SettingRow(
                            icon = spec.icon,
                            label = spec.label,
                            description = spec.description,
                            value = hint ?: "Not set",
                            valueTint = if (hint != null) SemanticSuccess else MutedText,
                            onClick = { editingCredential = spec }
                        )
                    }
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
    editingLocationType?.let { type ->
        LocationEditDialog(
            type = type,
            current = state.locations.entryFor(type),
            isDefault = state.defaultLocationType == type,
            saving = state.saving,
            onDismiss = { editingLocationType = null },
            onSave = { location, makeDefault ->
                viewModel.saveLocationDefault(type, location, makeDefault)
                editingLocationType = null
            },
            onMakeDefault = {
                viewModel.setDefaultLocationType(type)
                editingLocationType = null
            }
        )
    }
    editingCredential?.let { spec ->
        CredentialDialog(
            spec = spec,
            currentHint = state.credentialHints[spec.type],
            saving = state.saving,
            onDismiss = { editingCredential = null },
            onSave = { value ->
                viewModel.putCredential(spec.type, value)
                editingCredential = null
            },
            onRemove = {
                viewModel.deleteCredential(spec.type)
                editingCredential = null
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

/** One booking-defaults row: label + current value summary, tap to edit the
 *  entry, trailing radio to make it the default. */
@Composable
private fun LocationDefaultRow(
    icon: ImageVector,
    label: String,
    entry: LocationDto?,
    isDefault: Boolean,
    onClick: () -> Unit,
    onMakeDefault: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = UpcomingTokens.BrandPrimary, modifier = Modifier.size(22.dp))
        Row(
            modifier = Modifier
                .weight(1f)
                .clip(UpcomingTokens.RadiusMedium)
                .clickable(onClick = onClick)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label + if (entry?.label?.isNotBlank() == true) " — ${entry.label}" else "",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
                Text(
                    text = entry?.let { locationSummary(it) } ?: "Not configured — tap to set",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (entry != null) MaterialTheme.colorScheme.onSurfaceVariant else MutedText
                )
            }
        }
        RadioButton(selected = isDefault, onClick = onMakeDefault, colors = RadioButtonDefaults.colors(selectedColor = UpcomingTokens.BrandPrimary))
    }
}

private fun locationSummary(location: LocationDto): String = when (location.type) {
    TYPE_DAILY -> location.url?.ifBlank { null }?.let { "Room: $it" } ?: "Daily.co room (minted per booking)"
    TYPE_IN_PERSON -> location.address?.ifBlank { null }?.let { "Address: $it" } ?: "In-person meeting"
    TYPE_PHONE -> location.phone?.ifBlank { null }?.let { "Phone: $it" } ?: "Phone call"
    else -> location.type
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

@Composable
private fun TimezonePickerDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = remember(query) {
        if (query.isBlank()) COMMON_TIMEZONES
        else COMMON_TIMEZONES.filter { it.contains(query.trim(), ignoreCase = true) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Default timezone") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search zones…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(320.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(filtered.size) { index ->
                        val tz = filtered[index]
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
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

/** Edit one booking-default location: its own Label + value (room URL,
 *  address, or phone), with an explicit "set as default" action. */
@Composable
private fun LocationEditDialog(
    type: String,
    current: LocationDto?,
    isDefault: Boolean,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (LocationDto?, makeDefault: Boolean) -> Unit,
    onMakeDefault: () -> Unit
) {
    var label by remember(type) { mutableStateOf(current?.label ?: "") }
    var value by remember(type) {
        mutableStateOf(
            when (type) {
                TYPE_DAILY -> current?.url ?: ""
                TYPE_IN_PERSON -> current?.address ?: ""
                else -> current?.phone ?: ""
            }
        )
    }
    var makeDefault by remember(type) { mutableStateOf(isDefault) }
    val typeDisplay = when (type) {
        TYPE_DAILY -> "Video (Daily.co)"
        TYPE_IN_PERSON -> "In person"
        else -> "Phone"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(typeDisplay) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label") },
                    placeholder = { Text(if (type == TYPE_DAILY) "My Daily Room" else if (type == TYPE_IN_PERSON) "Office" else "Cell") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(when (type) {
                        TYPE_DAILY -> "Room URL (optional — blank mints per-booking rooms)"
                        TYPE_IN_PERSON -> "Address"
                        else -> "Phone number"
                    }) },
                    singleLine = true
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(UpcomingTokens.RadiusMedium)
                        .clickable { makeDefault = !makeDefault }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = makeDefault,
                        onClick = { makeDefault = true },
                        colors = RadioButtonDefaults.colors(selectedColor = UpcomingTokens.BrandPrimary)
                    )
                    Text("Use as default location", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving,
                onClick = {
                    val location = when (type) {
                        TYPE_DAILY -> LocationDto(
                            type = type,
                            label = label.trim().ifBlank { "Video (Daily.co)" },
                            url = value.trim().ifBlank { null }
                        )
                        TYPE_IN_PERSON -> LocationDto(
                            type = type,
                            label = label.trim().ifBlank { "In person" },
                            address = value.trim().ifBlank { null }
                        )
                        else -> LocationDto(
                            type = type,
                            label = label.trim().ifBlank { "Phone call" },
                            phone = value.trim().ifBlank { null }
                        )
                    }
                    onSave(location, makeDefault)
                }
            ) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = {
            Row {
                if (!isDefault && current != null) {
                    TextButton(onClick = onMakeDefault) { Text("Make default") }
                }
                TextButton(onClick = { onSave(null, false) }) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

/** Add/store/remove a bring-your-own credential. Keys render masked; URLs
 *  render as plain text. The server stores them encrypted and only ever
 *  returns a "••••1234"-style hint back. */
@Composable
private fun CredentialDialog(
    spec: CredentialSpec,
    currentHint: String?,
    saving: Boolean,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onRemove: () -> Unit
) {
    var value by remember(spec.type) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(spec.label) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = spec.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (currentHint != null) {
                    Text(
                        text = "Stored: $currentHint",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = DmMonoFontFamily),
                        color = SemanticSuccess
                    )
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text(if (currentHint != null) "Replace with new value" else if (spec.secret) "Paste secret value" else "Paste URL") },
                    visualTransformation = if (spec.secret) PasswordVisualTransformation() else VisualTransformation.None,
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !saving && value.isNotBlank(),
                onClick = { onSave(value.trim()) }
            ) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = {
            Row {
                if (currentHint != null) {
                    TextButton(onClick = onRemove) { Text("Remove", color = SemanticError) }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}
