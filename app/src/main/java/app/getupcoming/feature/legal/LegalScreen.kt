package app.getupcoming.feature.legal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.getupcoming.core.designsystem.UpcomingTopBar

/** In-app Terms of Use / Privacy Policy. Content is DRAFT placeholder text —
 *  replace with reviewed legal copy before public release. */
@Composable
fun LegalScreen(
    title: String,
    isTerms: Boolean,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        topBar = {
            UpcomingTopBar(
                title = title,
                subtitle = "Last updated: August 29, 2026 · Draft",
                navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                onNavigationClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (isTerms) TermsBody() else PrivacyBody()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Section(heading: String) {
    Text(
        heading,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.onSurface
    )
}

@Composable
private fun TermsBody() {
    Section("1. Acceptance")
    Text(
        "By creating an account or using Upcoming you agree to these Terms. " +
            "THIS TEXT IS A PLACEHOLDER and has not been reviewed by counsel.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Section("2. The Service")
    Text(
        "Upcoming provides scheduling tools: event types, availability rules, booking pages, " +
            "video room creation, reminders, and payment collection. Features may change or be " +
            "discontinued; we aim to give reasonable notice for breaking changes.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Section("3. Your Account")
    Text(
        "You are responsible for the activity on your account and for keeping your password " +
            "secure. Do not share booking links you do not intend to honor; cancelled bookings " +
            "release the slot to others immediately.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Section("4. Payments")
    Text(
        "Paid bookings are processed by Stripe. Upcoming does not store full card details. " +
            "Refunds for paid bookings are handled between you and your invitee unless stated otherwise.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Section("5. Acceptable Use")
    Text(
        "Do not use Upcoming to send spam, book fraudulently, infringe others' rights, or " +
            "attempt to access data that is not yours. Accounts used abusively may be suspended.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Section("6. Disclaimers")
    Text(
        "The service is provided \"as is\" without warranties of any kind. We are not liable " +
            "for missed meetings, lost revenue, or damages arising from service interruptions.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun PrivacyBody() {
    Section("1. What we store")
    Text(
        "Your email, username, display name, avatar, timezone, scheduling settings, and the " +
            "bookings invitees make with you. Booking confirmations and reminders are generated " +
            "from this data.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Section("2. Credentials you provide")
    Text(
        "Integration keys (Daily.co, iCal/CalDAV URLs, Stripe) are encrypted at rest " +
            "(AES-256-GCM) and are never returned in full to any client — only masked hints. " +
            "You can remove them at any time in Settings → Integrations.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Section("3. What stays on your device")
    Text(
        "Your session tokens are stored in encrypted storage on this device. Reminder " +
            "preferences, alarms, and cached bookings live locally and are never sold or shared.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Section("4. What we never do")
    Text(
        "We do not sell your data, we do not read your calendar contents beyond the feeds you " +
            "explicitly connect, and we do not email your invitees anything beyond the booking " +
            "flow they initiate.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Section("5. Deleting your data")
    Text(
        "Logging out removes your session from this device. Account deletion and data export " +
            "are available on request. THIS POLICY IS A PLACEHOLDER pending legal review.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
