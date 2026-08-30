package app.getupcoming.feature.permissions

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
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


private data class PermissionRow(
    val key: String,
    val title: String,
    val subtitle: String,
    val rationale: String
)

/** Device-granted permission inspector + request flows. Read-only statuses
 *  are re-checked on every composition/resume of the screen. */
@Composable
fun PermissionsScreen(onNavigateBack: () -> Unit) {
    val context = LocalContext.current

    val rows = listOf(
        PermissionRow(
            key = Manifest.permission.POST_NOTIFICATIONS,
            title = "Notifications",
            subtitle = "Booking confirmations and pre-meeting reminders",
            rationale = "Required on Android 13+ to show any alert. Without it, reminders are silently dropped."
        ),
        PermissionRow(
            key = "SCHEDULE_EXACT_ALARM",
            title = "Exact Alarms",
            subtitle = "Fires reminders at the precise minute, even in Doze",
            rationale = "Android 14 asks users to allow exact alarms for apps that need precise timing."
        )
    )

    // Re-check statuses whenever this screen recomposes after a return from
    // the OS settings page.
    var refreshTick by remember { mutableIntStateOf(0) }
    LaunchedEffect(refreshTick) { /* recomposition trigger only */ }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { refreshTick++ }

    fun isGranted(row: PermissionRow): Boolean = when (row.key) {
        Manifest.permission.POST_NOTIFICATIONS ->
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, row.key) == PackageManager.PERMISSION_GRANTED
        "SCHEDULE_EXACT_ALARM" ->
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                NotificationAndReminderManager.canScheduleExactAlarms(context)
        else -> false
    }

    Scaffold(
        topBar = {
            UpcomingTopBar(
                title = "Device Permissions",
                subtitle = "What Upcoming is allowed to do on this device",
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
            items(rows) { row ->
                val granted = isGranted(row)
                UpcomingCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(row.title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium))
                            Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        StatusBadge(status = if (granted) "granted" else "off")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(row.rationale, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))
                    if (granted) {
                        UpcomingSecondaryButton(
                            text = "Manage in Settings",
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        .setData(android.net.Uri.fromParts("package", context.packageName, null))
                                )
                            }
                        )
                    } else {
                        UpcomingPrimaryButton(
                            text = when (row.key) {
                                Manifest.permission.POST_NOTIFICATIONS -> "Allow notifications"
                                else -> "Allow exact alarms"
                            },
                            onClick = {
                                when (row.key) {
                                    Manifest.permission.POST_NOTIFICATIONS ->
                                        notifLauncher.launch(row.key)
                                    "SCHEDULE_EXACT_ALARM" -> {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                            context.startActivity(
                                                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                                                    .setData(android.net.Uri.fromParts("package", context.packageName, null))
                                            )
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
            item {
                Text(
                    "Vibration is granted automatically with the app install and follows the " +
                        "Sound & Vibration toggle in Notifications & Reminders.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
