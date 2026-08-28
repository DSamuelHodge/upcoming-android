package com.example.core.designsystem

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

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
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = (-0.25).sp
                            ),
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
                        // Geometric Hamburger/Square Menu Icon
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
                            color = GeoAvatarBg,
                            border = BorderStroke(1.dp, GeoAvatarBorder),
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userInitials,
                                    color = GeoAvatarText,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
            // Geometric Subtle Divider
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
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.2.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!tag.isNullOrBlank()) {
            Surface(
                shape = UpcomingTokens.RadiusFull,
                color = GeoPrimaryContainerLight,
                border = BorderStroke(1.dp, GeoBorderSubtleLight)
            ) {
                Text(
                    text = tag,
                    color = GeoPrimary,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
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
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
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
        shape = UpcomingTokens.RadiusLarge,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.4f),
            disabledContentColor = contentColor.copy(alpha = 0.7f)
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
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
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
        shape = UpcomingTokens.RadiusLarge,
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
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
        )
    }
}

@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, textLabel) = when (status.lowercase()) {
        "accepted" -> Triple(GeoGreenLight, GeoGreen, "Confirmed")
        "pending" -> Triple(GeoAmberLight, GeoAmber, "Pending")
        "cancelled" -> Triple(GeoRoseLight, GeoRose, "Cancelled")
        "individual" -> Triple(GeoPrimaryContainerLight, GeoPrimary, "1-on-1")
        "round_robin" -> Triple(GeoPurpleLight, GeoPurple, "Round Robin")
        "collective" -> Triple(GeoAmberLight, GeoAmber, "Collective")
        else -> Triple(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant, status)
    }

    Surface(
        modifier = modifier,
        shape = UpcomingTokens.RadiusFull,
        color = bgColor
    ) {
        Text(
            text = textLabel,
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
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
        locationType.contains("daily", ignoreCase = true) -> Triple(Icons.Default.Videocam, UpcomingTokens.DailyVideoPurple, "Daily.co Video")
        locationType.contains("meet", ignoreCase = true) -> Triple(Icons.Default.VideoCall, UpcomingTokens.GoogleMeetGreen, "Google Meet")
        locationType.contains("phone", ignoreCase = true) -> Triple(Icons.Default.Phone, UpcomingTokens.PhoneOrange, "Phone Call")
        locationType.contains("person", ignoreCase = true) -> Triple(Icons.Default.Place, UpcomingTokens.InPersonTeal, "In Person")
        else -> Triple(Icons.Default.Link, UpcomingTokens.BrandBlue, "Virtual Meeting")
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
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
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
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = "Copy link",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

