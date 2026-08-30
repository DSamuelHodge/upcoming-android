package com.example.feature.scheduling

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.CopyAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.designsystem.GeometricSectionHeader
import com.example.core.designsystem.UpcomingTopBar
import com.example.core.network.SingleUseLinkDto
import com.example.core.util.generateQrBitmap
import com.example.ui.theme.Hairline
import com.example.ui.theme.HairlineSoft
import com.example.ui.theme.Ink
import com.example.ui.theme.MutedText
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCreamStrong
import kotlinx.coroutines.launch

@Composable
fun SchedulingScreen(
    viewModel: SchedulingViewModel,
    onOpenSettings: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val user by viewModel.user.collectAsState()
    val eventTypes by viewModel.eventTypes.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var qrTarget by remember { mutableStateOf<String?>(null) }

    val copyToClipboard: (String, String) -> Unit = { label, value ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
        scope.launch { snackbar.showSnackbar("$label copied to clipboard") }
    }
    val shareUrl: (String) -> Unit = { url ->
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareMessage(url, user?.displayName))
        }
        context.startActivity(Intent.createChooser(intent, "Share booking link"))
    }

    Scaffold(
        topBar = {
            UpcomingTopBar(
                title = "Scheduling",
                subtitle = "Share how people book with you",
                userInitials = user?.displayName?.split(" ")?.mapNotNull { it.firstOrNull()?.toString() }?.take(2)?.joinToString("") ?: "ME",
                onAvatarClick = onOpenSettings,
                actions = {}
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Personal booking link.
            item {
                PersonalLinkCard(
                    username = user?.username,
                    onCopy = { copyToClipboard("Booking page link", personalLink(user?.username)) },
                    onShare = { shareUrl(personalLink(user?.username)) },
                    onQr = { qrTarget = personalLink(user?.username) }
                )
            }

            // 2. Event type links + single-use links.
            item { GeometricSectionHeader(title = "Event Type Links") }
            items(eventTypes.size) { index ->
                val et = eventTypes[index]
                EventTypeLinkCard(
                    eventType = et,
                    username = user?.username,
                    links = uiState.linksByEventType[et.id] ?: emptyList(),
                    busy = et.id in uiState.linkGeneration,
                    onCopy = { copyToClipboard("${et.title} link", eventLink(user?.username, et.slug)) },
                    onShare = { shareUrl(eventLink(user?.username, et.slug)) },
                    onQr = { qrTarget = eventLink(user?.username, et.slug) },
                    onCreateLinks = { viewModel.createLinks(et.id, count = 1) },
                    onRevoke = { linkId -> viewModel.revokeLink(et.id, linkId) },
                    onLoadLinks = { viewModel.loadLinks(et.id) },
                    onCopyLink = { link -> copyToClipboard("Single-use link", link.url) }
                )
            }

            // 3. Embed snippet.
            item {
                EmbedCard(
                    username = user?.username,
                    onCopy = {
                        copyToClipboard("Embed snippet", embedSnippet(user?.username))
                    }
                )
            }
        }
    }

    qrTarget?.let { url ->
        AlertDialog(
            onDismissRequest = { qrTarget = null },
            confirmButton = {
                TextButton(onClick = { qrTarget = null }) { Text("Done") }
            },
            title = { Text("Scan to book") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Image(
                        bitmap = remember(url) { generateQrBitmap(url) },
                        contentDescription = "QR code for $url",
                        modifier = Modifier.size(220.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(url, style = MaterialTheme.typography.bodySmall, color = MutedText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        )
    }
}

@Composable
private fun PersonalLinkCard(
    username: String?,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onQr: () -> Unit
) {
    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, HairlineSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("YOUR BOOKING PAGE", style = MaterialTheme.typography.labelSmall, color = MutedText)
            Spacer(Modifier.height(6.dp))
            Text(
                personalLink(username),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = Ink
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActionChip(icon = { Icon(Icons.Outlined.CopyAll, null, Modifier.size(16.dp)) }, label = "Copy", onClick = onCopy, modifier = Modifier.weight(1f))
                ActionChip(icon = { Icon(Icons.Outlined.Share, null, Modifier.size(16.dp)) }, label = "Share", onClick = onShare, modifier = Modifier.weight(1f))
                ActionChip(icon = { Icon(Icons.Outlined.QrCode2, null, Modifier.size(16.dp)) }, label = "QR", onClick = onQr, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun EventTypeLinkCard(
    eventType: com.example.core.model.EventType,
    username: String?,
    links: List<SingleUseLinkDto>,
    busy: Boolean,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onQr: () -> Unit,
    onCreateLinks: () -> Unit,
    onRevoke: (Long) -> Unit,
    onLoadLinks: () -> Unit,
    onCopyLink: (SingleUseLinkDto) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, HairlineSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(10.dp)
                        .background(
                            runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(eventType.colorHex)) }
                                .getOrDefault(SurfaceCreamStrong),
                            CircleShape
                        )
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(eventType.title.ifBlank { eventType.slug }, style = MaterialTheme.typography.titleSmall, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${eventType.lengthMinutes} min", style = MaterialTheme.typography.bodySmall, color = MutedText)
                }
                IconButton(onClick = onQr) { Icon(Icons.Outlined.QrCode2, "Show QR", Modifier.size(20.dp)) }
                IconButton(onClick = onCopy) { Icon(Icons.Outlined.CopyAll, "Copy link", Modifier.size(18.dp)) }
                IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, "Share link", Modifier.size(18.dp)) }
            }
            Text(
                eventLink(username, eventType.slug),
                style = MaterialTheme.typography.bodySmall, color = MutedText,
                maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(10.dp))

            // Single-use links (Calendly-style).
            HorizontalDivider(color = Hairline)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Single-use links", style = MaterialTheme.typography.labelLarge, color = Ink, modifier = Modifier.weight(1f))
                if (!expanded) {
                    TextButton(onClick = {
                        expanded = true
                        onLoadLinks()
                    }) { Text("Show") }
                } else {
                    OutlinedButton(onClick = onCreateLinks, enabled = !busy) {
                        if (busy) {
                            androidx.compose.material3.CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                        } else {
                            Text("+ New link")
                        }
                    }
                }
            }
            if (expanded) {
                if (links.isEmpty() && !busy) {
                    Text("No single-use links yet. Create one — it can only be used once.", style = MaterialTheme.typography.bodySmall, color = MutedText)
                }
                links.forEach { link ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)) {
                        LinkStatusBadge(status = link.status)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            link.url.substringAfter("lid="),
                            style = MaterialTheme.typography.bodySmall, color = MutedText,
                            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                        )
                        if (link.status == "unused") {
                            TextButton(onClick = { onCopyLink(link) }) { Text("Copy") }
                            TextButton(onClick = { onRevoke(link.id) }, enabled = !busy) { Text("Revoke") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkStatusBadge(status: String) {
    val (label, color) = when (status) {
        "unused" -> "UNUSED" to com.example.ui.theme.SemanticSuccess
        "used" -> "USED" to MutedText
        "expired" -> "EXPIRED" to MutedText
        else -> "REVOKED" to MaterialTheme.colorScheme.error
    }
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.12f)) {
        Text(label, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = color, fontSize = 9.sp)
    }
}

@Composable
private fun EmbedCard(
    username: String?,
    onCopy: () -> Unit
) {
    Surface(
        color = SurfaceCard,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, HairlineSoft),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("ADD TO YOUR WEBSITE", style = MaterialTheme.typography.labelSmall, color = MutedText)
            Spacer(Modifier.height(6.dp))
            Text(
                "Embed your booking page on your profile or site with one line of HTML.",
                style = MaterialTheme.typography.bodySmall, color = MutedText
            )
            Spacer(Modifier.height(10.dp))
            Surface(color = MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Hairline)) {
                Text(
                    embedSnippet(username),
                    style = MaterialTheme.typography.bodySmall,
                    color = Ink,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = onCopy, colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Text("Copy embed code")
            }
        }
    }
}

@Composable
private fun ActionChip(
    icon: @Composable () -> Unit,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, Hairline),
        modifier = modifier.clickable { onClick() }
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            icon()
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = Ink)
        }
    }
}

private fun embedSnippet(username: String?): String =
    """<iframe src="${personalLink(username)}?embed=true" style="border:0" width="100%" height="600"></iframe>"""
