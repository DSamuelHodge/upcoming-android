package com.example.core.designsystem

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import com.example.ui.theme.UpcomingTextStyles

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpcomingTopBar(
    title: String,
    subtitle: String? = null,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    userInitials: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = MaterialTheme.colorScheme.background,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                navigationIcon = {
                    if (navigationIcon != null && onNavigationClick != null) {
                        IconButton(
                            onClick = onNavigationClick,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = navigationIcon,
                                contentDescription = "Back",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(start = 12.dp, end = 4.dp)
                                .size(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(modifier = Modifier.size(width = 20.dp, height = 2.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(1.dp)))
                                Box(modifier = Modifier.size(width = 20.dp, height = 2.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(1.dp)))
                                Box(modifier = Modifier.size(width = 20.dp, height = 2.dp).background(MaterialTheme.colorScheme.onSurfaceVariant, RoundedCornerShape(1.dp)))
                            }
                        }
                    }
                },
                actions = {
                    actions()
                    if (userInitials != null) {
                        Surface(
                            shape = CircleShape,
                            color = SurfaceCard,
                            border = BorderStroke(1.dp, Hairline),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userInitials,
                                    color = Ink,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}

@Composable
fun GeometricSectionHeader(
    title: String,
    tag: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!tag.isNullOrBlank()) {
            Surface(
                shape = UpcomingTokens.RadiusFull,
                color = SurfaceCard,
                border = BorderStroke(1.dp, HairlineSoft)
            ) {
                Text(
                    text = tag,
                    color = Ink,
                    style = UpcomingTextStyles.caption,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun UpcomingCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    borderColor: Color = MaterialTheme.colorScheme.outline,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    padding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) {
        modifier
            .clip(UpcomingTokens.RadiusLarge)
            .clickable(onClick = onClick)
    } else {
        modifier.clip(UpcomingTokens.RadiusLarge)
    }

    Surface(
        modifier = cardModifier,
        shape = UpcomingTokens.RadiusLarge,
        color = backgroundColor,
        border = BorderStroke(UpcomingTokens.BorderWidth, borderColor),
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            content = content
        )
    }
}

@Composable
fun UpcomingPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    leadingIcon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White
) {
    Button(
        onClick = onClick,
        enabled = enabled && !isLoading,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .minimumInteractiveComponentSize(),
        shape = UpcomingTokens.RadiusMedium,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = PrimaryCoralDisabled,
            disabledContentColor = MutedSoftText
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = contentColor,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        } else if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun UpcomingSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .minimumInteractiveComponentSize(),
        shape = UpcomingTokens.RadiusMedium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    // Doc: badge-status = dot + label, DM Mono 12sp
    val dotColor = when (status.lowercase()) {
        "accepted" -> SemanticSuccess
        "granted" -> SemanticSuccess
        "off" -> SemanticError
        "pending" -> AccentAmber
        "cancelled" -> SemanticError
        "individual" -> PrimaryCoral
        "round_robin" -> AccentTeal
        "collective" -> AccentAmber
        else -> MutedSoftText
    }
    val textLabel = when (status.lowercase()) {
        "accepted" -> "Confirmed"
        "granted" -> "Granted"
        "off" -> "Off"
        "individual" -> "1-on-1"
        "round_robin" -> "Round Robin"
        "collective" -> "Collective"
        else -> status.replaceFirstChar { it.uppercase() }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Text(
            text = textLabel,
            color = BodyText,
            style = UpcomingTextStyles.monoLabel
        )
    }
}

@Composable
fun LocationBadge(
    locationType: String,
    label: String? = null,
    modifier: Modifier = Modifier
) {
    val (icon, color, defaultText) = when {
        locationType.contains("daily", ignoreCase = true) -> Triple(Icons.Default.Videocam, UpcomingTokens.DailyVideoAccent, "Daily.co Video")
        locationType.contains("meet", ignoreCase = true) -> Triple(Icons.Default.VideoCall, UpcomingTokens.GoogleMeetAccent, "Google Meet")
        locationType.contains("phone", ignoreCase = true) -> Triple(Icons.Default.Phone, UpcomingTokens.PhoneAccent, "Phone Call")
        locationType.contains("person", ignoreCase = true) -> Triple(Icons.Default.Place, UpcomingTokens.InPersonAccent, "In Person")
        else -> Triple(Icons.Default.Link, UpcomingTokens.VirtualAccent, "Virtual Meeting")
    }

    Surface(
        shape = UpcomingTokens.RadiusSmall,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label?.ifBlank { defaultText } ?: defaultText,
                style = UpcomingTextStyles.caption,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ShareLinkButton(
    url: String,
    context: Context = LocalContext.current,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(UpcomingTokens.RadiusMedium)
            .clickable {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                val clip = ClipData.newPlainText("Upcoming Booking Link", url)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "Link copied to clipboard!", Toast.LENGTH_SHORT).show()
            },
        shape = UpcomingTokens.RadiusMedium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy link",
                tint = PrimaryCoral,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Copy link",
                style = UpcomingTextStyles.caption,
                color = PrimaryCoral
            )
        }
    }
}

/**
 * Time / date label in DM Mono 12sp with tabular numbers (colors.label).
 * Use for every HH:MM, date, and data-metric display.
 */
@Composable
fun TimeLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MutedText,
    textAlign: TextAlign? = null
) {
    Text(
        text = text,
        style = UpcomingTextStyles.monoLabel,
        color = color,
        textAlign = textAlign,
        modifier = modifier
    )
}

/**
 * Editorial callout in Instrument Serif italic 16sp (typography.serif-italic).
 * Rare accent — use only for quotes and editorial moments.
 */
@Composable
fun SerifCallout(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Ink
) {
    Text(
        text = text,
        style = UpcomingTextStyles.serifItalic,
        color = color,
        modifier = modifier
    )
}

/** Circular monogram badge on the brand coral — used for the splash/app
 *  identity and empty-state accents. */
@Composable
fun MonogramBadge(
    letter: String,
    size: androidx.compose.ui.unit.Dp = 48.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(UpcomingTokens.BrandPrimary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = letter,
            color = OnDark,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
        )
    }
}

/** The app's single loading pattern: centered spinner + muted label, on the
 *  card surface. Use it wherever a sync or initial load is in flight. */
@Composable
fun UpcomingLoadingRow(
    label: String,
    modifier: Modifier = Modifier
) {
    UpcomingCard(
        modifier = modifier.fillMaxWidth(),
        padding = PaddingValues(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = UpcomingTokens.BrandPrimary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
