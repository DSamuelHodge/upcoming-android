package app.getupcoming.feature.bookingflow

import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.getupcoming.core.designsystem.*
import app.getupcoming.core.engine.SchedulingEngine
import app.getupcoming.core.engine.StripePaymentSimulator
import app.getupcoming.core.model.OfferedSlot
import app.getupcoming.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InviteeBookingScreen(
    viewModel: InviteeBookingViewModel,
    onNavigateBack: () -> Unit,
    onFinishBooking: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val eventType = uiState.eventType

    Scaffold(
        topBar = {
            UpcomingTopBar(
                title = if (uiState.step is BookingStep.Confirmation) "Booking Confirmed" else (eventType?.title ?: "Select Date & Time"),
                subtitle = "with ${uiState.hostUser?.displayName ?: "Alex Rivera"}",
                navigationIcon = if (uiState.step !is BookingStep.Confirmation) Icons.AutoMirrored.Filled.ArrowBack else null,
                onNavigationClick = {
                    if (uiState.step == BookingStep.SelectDateAndTime) {
                        onNavigateBack()
                    } else {
                        viewModel.navigateBackStep()
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        if (eventType == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = UpcomingTokens.BrandPrimary)
            }
        } else {
            when (val step = uiState.step) {
                is BookingStep.SelectDateAndTime -> {
                    DateAndTimeStepView(
                        modifier = Modifier.padding(innerPadding),
                        uiState = uiState,
                        onDateSelected = { viewModel.selectDate(it) },
                        onSlotSelected = { viewModel.selectSlot(it) },
                        onTimezoneChanged = { viewModel.setInviteeTimezone(it) },
                        onContinue = { viewModel.proceedToDetails() }
                    )
                }
                is BookingStep.EnterDetails -> {
                    InviteeDetailsStepView(
                        modifier = Modifier.padding(innerPadding),
                        uiState = uiState,
                        onNameChange = { name -> viewModel.updateInviteeInfo(name, uiState.attendeeEmail, uiState.attendeePhone, uiState.attendeeNotes) },
                        onEmailChange = { email -> viewModel.updateInviteeInfo(uiState.attendeeName, email, uiState.attendeePhone, uiState.attendeeNotes) },
                        onPhoneChange = { phone -> viewModel.updateInviteeInfo(uiState.attendeeName, uiState.attendeeEmail, phone, uiState.attendeeNotes) },
                        onNotesChange = { notes -> viewModel.updateInviteeInfo(uiState.attendeeName, uiState.attendeeEmail, uiState.attendeePhone, notes) },
                        onProceed = { viewModel.proceedFromDetails() }
                    )
                }
                is BookingStep.StripePayment -> {
                    StripePaymentStepView(
                        modifier = Modifier.padding(innerPadding),
                        uiState = uiState,
                        onCardNumberChange = { num -> viewModel.updateCardInfo(num, uiState.expMonthYear, uiState.cvc) },
                        onExpChange = { exp -> viewModel.updateCardInfo(uiState.cardNumber, exp, uiState.cvc) },
                        onCvcChange = { cvc -> viewModel.updateCardInfo(uiState.cardNumber, uiState.expMonthYear, cvc) },
                        onAutofillTest = { viewModel.autofillTestCard() },
                        onSubmitPayment = { viewModel.submitStripePaymentAndBook() }
                    )
                }
                is BookingStep.Confirmation -> {
                    BookingConfirmationStepView(
                        modifier = Modifier.padding(innerPadding),
                        booking = step.booking,
                        attendee = step.attendee,
                        eventType = eventType,
                        hostUser = uiState.hostUser,
                        onFinish = onFinishBooking
                    )
                }
            }
        }
    }
}

@Composable
fun DateAndTimeStepView(
    modifier: Modifier = Modifier,
    uiState: InviteeBookingUiState,
    onDateSelected: (Date) -> Unit,
    onSlotSelected: (OfferedSlot) -> Unit,
    onTimezoneChanged: (String) -> Unit,
    onContinue: () -> Unit
) {
    val eventType = uiState.eventType ?: return
    var showTzPicker by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Event summary header
        item {
            UpcomingCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = eventType.title,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Outlined.Timer, contentDescription = null, tint = UpcomingTokens.BrandPrimary, modifier = Modifier.size(14.dp))
                                Text("${eventType.lengthMinutes} min", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                            }
                            if (eventType.priceInCents > 0) {
                                Text("•", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    StripePaymentSimulator.formatPrice(eventType.priceInCents),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AccentTeal
                                )
                            }
                        }
                    }
                    StatusBadge(status = eventType.schedulingType)
                }

                if (eventType.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = eventType.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Date selection carousel
        item {
            Text(
                text = "Select a Date",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            val datesList = remember {
                val cal = Calendar.getInstance()
                (0..13).map {
                    val d = cal.time
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    d
                }
            }

            val dayOfWeekFmt = SimpleDateFormat("EEE", Locale.US)
            val dayNumFmt = SimpleDateFormat("d", Locale.US)
            val monthFmt = SimpleDateFormat("MMM", Locale.US)

            val isSameDay = { d1: Date, d2: Date ->
                val c1 = Calendar.getInstance().apply { time = d1 }
                val c2 = Calendar.getInstance().apply { time = d2 }
                c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
            }

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(datesList) { date ->
                    val selected = isSameDay(date, uiState.selectedDate)
                    Surface(
                        modifier = Modifier
                            .width(62.dp)
                            .clip(UpcomingTokens.RadiusMedium)
                            .clickable { onDateSelected(date) },
                        shape = UpcomingTokens.RadiusMedium,
                        color = if (selected) UpcomingTokens.BrandPrimary else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (selected) UpcomingTokens.BrandPrimary else MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = dayOfWeekFmt.format(date).uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = dayNumFmt.format(date),
                                style = UpcomingTextStyles.monoData,
                                color = if (selected) Color.White else Ink
                            )
                            Text(
                                text = monthFmt.format(date),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Timezone switcher
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showTzPicker = true }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Outlined.Public, contentDescription = null, tint = UpcomingTokens.BrandPrimary, modifier = Modifier.size(16.dp))
                Text(
                    text = "Time zone: ${uiState.inviteeTimezone}",
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = UpcomingTokens.BrandPrimary
                )
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = UpcomingTokens.BrandPrimary, modifier = Modifier.size(16.dp))
            }
        }

        // Available slots
        item {
            val dateTitleFmt = SimpleDateFormat("EEEE, MMMM d", Locale.US)
            Text(
                text = "Available Slots for ${dateTitleFmt.format(uiState.selectedDate)}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (uiState.availableSlots.isEmpty()) {
            item {
                UpcomingCard(modifier = Modifier.fillMaxWidth(), padding = PaddingValues(24.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(32.dp))
                        Text(
                            text = "No open slots on this date",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Please choose another date or contact host for alternate availability.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(uiState.availableSlots) { slot ->
                val selected = uiState.selectedSlot?.startUtc == slot.startUtc
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(UpcomingTokens.RadiusMedium)
                        .clickable { onSlotSelected(slot) },
                    shape = UpcomingTokens.RadiusMedium,
                    color = if (selected) UpcomingTokens.SelectedBg else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.5.dp,
                        if (selected) UpcomingTokens.BrandPrimary else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = slot.displayLocalTime,
                            style = UpcomingTextStyles.monoData,
                            color = if (selected) UpcomingTokens.BrandPrimary else Ink
                        )
                        if (selected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = UpcomingTokens.BrandPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        if (uiState.errorMessage != null) {
            item {
                Text(
                    text = uiState.errorMessage,
                    color = SemanticError,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item {
            UpcomingPrimaryButton(
                text = "Next: Enter Details",
                onClick = onContinue,
                enabled = uiState.selectedSlot != null,
                leadingIcon = Icons.AutoMirrored.Filled.ArrowForward
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showTzPicker) {
        val timezones = listOf(
            "America/New_York", "America/Chicago", "America/Denver",
            "America/Los_Angeles", "Europe/London", "Europe/Paris", "Asia/Tokyo", "UTC"
        )
        AlertDialog(
            onDismissRequest = { showTzPicker = false },
            title = { Text("Select Your Timezone") },
            text = {
                LazyColumn {
                    items(timezones) { tz ->
                        Text(
                            text = tz,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onTimezoneChanged(tz)
                                    showTzPicker = false
                                }
                                .padding(12.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTzPicker = false }) { Text("Close") } }
        )
    }
}

@Composable
fun InviteeDetailsStepView(
    modifier: Modifier = Modifier,
    uiState: InviteeBookingUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    onProceed: () -> Unit
) {
    val eventType = uiState.eventType ?: return
    val slot = uiState.selectedSlot

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            UpcomingCard(backgroundColor = MaterialTheme.colorScheme.surfaceVariant) {
                Text(
                    text = "Selected Appointment",
                    style = MaterialTheme.typography.labelSmall,
                    color = UpcomingTokens.BrandPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = eventType.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (slot != null) {
                    Text(
                        text = "${slot.displayLocalTime} (${uiState.inviteeTimezone})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            UpcomingCard {
                Text(
                    text = "Your Information",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = uiState.attendeeName,
                    onValueChange = onNameChange,
                    label = { Text("Your Full Name *") },
                    placeholder = { Text("e.g. Jordan Taylor") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = UpcomingTokens.RadiusMedium,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.attendeeEmail,
                    onValueChange = onEmailChange,
                    label = { Text("Email Address *") },
                    placeholder = { Text("jordan@example.com") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = UpcomingTokens.RadiusMedium,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.attendeePhone,
                    onValueChange = onPhoneChange,
                    label = { Text("Phone Number (Optional)") },
                    placeholder = { Text("+1 (555) 000-0000") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = UpcomingTokens.RadiusMedium,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.attendeeNotes,
                    onValueChange = onNotesChange,
                    label = { Text("Please share anything that will help prepare for our meeting:") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = UpcomingTokens.RadiusMedium
                )
            }
        }

        if (uiState.errorMessage != null) {
            item {
                Text(
                    text = uiState.errorMessage,
                    color = SemanticError,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item {
            UpcomingPrimaryButton(
                text = if (eventType.priceInCents > 0) "Proceed to Payment (${StripePaymentSimulator.formatPrice(eventType.priceInCents)})" else "Schedule Event",
                onClick = onProceed,
                isLoading = uiState.isSubmitting,
                leadingIcon = if (eventType.priceInCents > 0) Icons.Default.CreditCard else Icons.Default.Check
            )
        }
    }
}

@Composable
fun StripePaymentStepView(
    modifier: Modifier = Modifier,
    uiState: InviteeBookingUiState,
    onCardNumberChange: (String) -> Unit,
    onExpChange: (String) -> Unit,
    onCvcChange: (String) -> Unit,
    onAutofillTest: () -> Unit,
    onSubmitPayment: () -> Unit
) {
    val eventType = uiState.eventType ?: return
    val priceStr = StripePaymentSimulator.formatPrice(eventType.priceInCents)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            UpcomingCard(
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                borderColor = AccentTeal.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Stripe Secure Checkout", style = MaterialTheme.typography.labelSmall, color = AccentTeal)
                        Text(eventType.title, style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = priceStr,
                        style = MaterialTheme.typography.headlineSmall,
                        color = AccentTeal
                    )
                }
            }
        }

        item {
            UpcomingCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Payment Method", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = onAutofillTest) {
                        Text("Autofill Test Card", color = UpcomingTokens.BrandPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = uiState.cardNumber,
                    onValueChange = onCardNumberChange,
                    label = { Text("Card Number") },
                    placeholder = { Text("4242 4242 4242 4242") },
                    leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    shape = UpcomingTokens.RadiusMedium,
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = uiState.expMonthYear,
                        onValueChange = onExpChange,
                        label = { Text("MM/YY") },
                        placeholder = { Text("12/28") },
                        modifier = Modifier.weight(1f),
                        shape = UpcomingTokens.RadiusMedium,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = uiState.cvc,
                        onValueChange = onCvcChange,
                        label = { Text("CVC") },
                        placeholder = { Text("123") },
                        modifier = Modifier.weight(1f),
                        shape = UpcomingTokens.RadiusMedium,
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                    Text("Encrypted with 256-bit SSL via Stripe API simulation", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        if (uiState.errorMessage != null) {
            item {
                Text(
                    text = uiState.errorMessage,
                    color = SemanticError,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        item {
            UpcomingPrimaryButton(
                text = "Pay $priceStr & Confirm Booking",
                onClick = onSubmitPayment,
                isLoading = uiState.isSubmitting,
                containerColor = AccentTeal,
                leadingIcon = Icons.Default.Lock
            )
        }
    }
}

@Composable
fun BookingConfirmationStepView(
    modifier: Modifier = Modifier,
    booking: app.getupcoming.core.model.Booking,
    attendee: app.getupcoming.core.model.Attendee?,
    eventType: app.getupcoming.core.model.EventType,
    hostUser: app.getupcoming.core.model.User?,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val startUtc = SchedulingEngine.parseIsoUtc(booking.startTimeUtc)
    val endUtc = SchedulingEngine.parseIsoUtc(booking.endTimeUtc)

    val dateFmt = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("America/New_York")
    }
    val timeFmt = SimpleDateFormat("h:mm a", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("America/New_York")
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(SurfaceSoft),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Confirmed",
                    tint = SemanticSuccess,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "You are scheduled!",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "A calendar invitation has been sent to ${attendee?.email ?: "your email"}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            UpcomingCard(
                modifier = Modifier.fillMaxWidth(),
                padding = PaddingValues(16.dp)
            ) {
                Text(
                    text = eventType.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, tint = UpcomingTokens.BrandPrimary, modifier = Modifier.size(16.dp))
                    Text(
                        text = dateFmt.format(startUtc),
                        style = UpcomingTextStyles.monoData,
                        color = Ink
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Schedule, contentDescription = null, tint = UpcomingTokens.BrandPrimary, modifier = Modifier.size(16.dp))
                    Text(
                        text = "${timeFmt.format(startUtc)} – ${timeFmt.format(endUtc)} EDT",
                        style = UpcomingTextStyles.monoData,
                        color = Ink
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, tint = UpcomingTokens.DailyVideoAccent, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Web conferencing details provided upon entry",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            UpcomingPrimaryButton(
                text = "Join Daily Video Room",
                onClick = {
                    val url = "https://upcoming.daily.co/meeting-${booking.uid.take(6)}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                },
                leadingIcon = Icons.Default.Videocam
            )
        }

        item {
            UpcomingSecondaryButton(
                text = "Add to Google Calendar",
                onClick = {
                    val calIntent = Intent(Intent.ACTION_INSERT).apply {
                        data = CalendarContract.Events.CONTENT_URI
                        putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startUtc.time)
                        putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endUtc.time)
                        putExtra(CalendarContract.Events.TITLE, eventType.title)
                        putExtra(CalendarContract.Events.DESCRIPTION, "Meeting with ${hostUser?.displayName ?: "Alex Rivera"}")
                        putExtra(CalendarContract.Events.EVENT_LOCATION, "Daily.co Video Call")
                    }
                    context.startActivity(calIntent)
                },
                leadingIcon = Icons.Default.EventAvailable
            )
        }

        item {
            TextButton(onClick = onFinish) {
                Text("Return to Dashboard", color = UpcomingTokens.BrandPrimary)
            }
        }
    }
}
