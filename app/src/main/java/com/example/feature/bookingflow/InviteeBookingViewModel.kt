package com.example.feature.bookingflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.engine.SchedulingEngine
import com.example.core.model.*
import com.example.core.repository.UpcomingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

sealed class BookingStep {
    object SelectDateAndTime : BookingStep()
    object EnterDetails : BookingStep()
    object StripePayment : BookingStep()
    data class Confirmation(val booking: Booking, val attendee: Attendee?) : BookingStep()
}

data class InviteeBookingUiState(
    val eventType: EventType? = null,
    val hostUser: User? = null,
    val selectedDate: Date = Date(),
    val availableSlots: List<OfferedSlot> = emptyList(),
    val selectedSlot: OfferedSlot? = null,
    val inviteeTimezone: String = "America/New_York",
    val step: BookingStep = BookingStep.SelectDateAndTime,
    val attendeeName: String = "",
    val attendeeEmail: String = "",
    val attendeePhone: String = "",
    val attendeeNotes: String = "",
    val cardNumber: String = "",
    val expMonthYear: String = "12/28",
    val cvc: String = "123",
    /** Booking already committed server-side while the payment is in flight. */
    val heldBooking: Booking? = null,
    val idempotencyKey: String? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
)

class InviteeBookingViewModel(
    private val repository: UpcomingRepository,
    private val eventTypeId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(InviteeBookingUiState())
    val uiState: StateFlow<InviteeBookingUiState> = _uiState.asStateFlow()

    init {
        loadEventDetails()
    }

    private fun loadEventDetails() {
        viewModelScope.launch {
            val eventType = repository.getEventTypeById(eventTypeId)
            val host = repository.getPrimaryUser()
            _uiState.value = _uiState.value.copy(
                eventType = eventType,
                hostUser = host,
                inviteeTimezone = host.timezone
            )
            computeSlotsForDate(_uiState.value.selectedDate)
        }
    }

    fun selectDate(date: Date) {
        _uiState.value = _uiState.value.copy(selectedDate = date, selectedSlot = null)
        computeSlotsForDate(date)
    }

    fun setInviteeTimezone(tz: String) {
        _uiState.value = _uiState.value.copy(inviteeTimezone = tz)
        computeSlotsForDate(_uiState.value.selectedDate)
    }

    fun selectSlot(slot: OfferedSlot) {
        _uiState.value = _uiState.value.copy(selectedSlot = slot)
    }

    private fun computeSlotsForDate(date: Date) {
        val et = _uiState.value.eventType ?: return
        viewModelScope.launch {
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                time = date
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val rangeStart = cal.time
            cal.add(Calendar.DAY_OF_YEAR, 1)
            val rangeEnd = cal.time

            val slots = repository.computeAvailableSlots(
                eventType = et,
                rangeStartUtc = rangeStart,
                rangeEndUtc = rangeEnd,
                inviteeTimezone = _uiState.value.inviteeTimezone
            )
            _uiState.value = _uiState.value.copy(availableSlots = slots)
        }
    }

    fun proceedToDetails() {
        if (_uiState.value.selectedSlot != null) {
            _uiState.value = _uiState.value.copy(step = BookingStep.EnterDetails, errorMessage = null)
        } else {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select a time slot to continue.")
        }
    }

    fun updateInviteeInfo(name: String, email: String, phone: String, notes: String) {
        _uiState.value = _uiState.value.copy(
            attendeeName = name,
            attendeeEmail = email,
            attendeePhone = phone,
            attendeeNotes = notes
        )
    }

    fun proceedFromDetails() {
        val state = _uiState.value
        if (state.attendeeName.isBlank() || state.attendeeEmail.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter your name and email address.")
            return
        }
        // Book first (server-side, atomic) — the booking itself holds the slot
        // while payment runs. Paid flow then attaches a PaymentIntent; free
        // events go straight to confirmation.
        bookSlot()
    }

    fun updateCardInfo(cardNumber: String, exp: String, cvc: String) {
        _uiState.value = _uiState.value.copy(
            cardNumber = cardNumber,
            expMonthYear = exp,
            cvc = cvc
        )
    }

    fun autofillTestCard() {
        _uiState.value = _uiState.value.copy(
            cardNumber = "4242 4242 4242 4242",
            expMonthYear = "12/28",
            cvc = "456"
        )
    }

    private fun bookSlot() {
        val state = _uiState.value
        val et = state.eventType ?: return
        val slot = state.selectedSlot ?: return

        _uiState.value = state.copy(isSubmitting = true, errorMessage = null)

        viewModelScope.launch {
            val idempotencyKey = state.idempotencyKey
                ?: "idemp_${UUID.randomUUID().toString().replace("-", "").take(24)}"
            val locationJson = et.locationsJson.ifBlank { """[{"type":"integrations:daily","label":"Daily Video Call"}]""" }

            val result = repository.createBooking(
                eventTypeId = et.id,
                slotStartUtc = slot.startUtc,
                slotEndUtc = slot.endUtc,
                locationJson = locationJson,
                attendeeEmail = state.attendeeEmail.trim(),
                attendeeName = state.attendeeName.trim(),
                attendeePhone = state.attendeePhone.trim(),
                attendeeNotes = state.attendeeNotes.trim(),
                attendeeTimezone = state.inviteeTimezone,
                idempotencyKey = idempotencyKey
            )

            result.fold(
                onSuccess = { booking ->
                    if (et.priceInCents > 0) {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            heldBooking = booking,
                            idempotencyKey = idempotencyKey,
                            step = BookingStep.StripePayment
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isSubmitting = false,
                            idempotencyKey = idempotencyKey,
                            step = BookingStep.Confirmation(booking, buildAttendee(booking.id))
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = err.message ?: "Booking failed. Please choose another slot."
                    )
                }
            )
        }
    }

    /** Confirms the card with Stripe (client-side tokenization), then has the
     *  server verify the PaymentIntent and flip `paid`. A definitive payment
     *  failure auto-cancels the held booking so the slot frees immediately;
     *  network failures keep the booking so the user can retry. */
    fun submitStripePaymentAndBook() {
        val state = _uiState.value
        val et = state.eventType ?: return
        val booking = state.heldBooking ?: return
        if (state.cardNumber.isBlank() || state.cvc.isBlank()) {
            _uiState.value = state.copy(errorMessage = "Please enter your card details.")
            return
        }

        _uiState.value = state.copy(isSubmitting = true, errorMessage = null)

        viewModelScope.launch {
            // 1. Server-side PaymentIntent (amount/currency from the event type)
            val intentResult = repository.createPaymentIntent(et.id)
            val intent = intentResult.getOrNull()
            if (intent == null) {
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = "Could not start payment: ${intentResult.exceptionOrNull()?.message}"
                )
                return@launch
            }

            // 2. Confirm with Stripe from the device
            val (expMonth, expYear) = parseExpiry(state.expMonthYear)
            val confirmResult = repository.confirmStripePayment(
                clientSecret = intent.clientSecret,
                cardNumber = state.cardNumber,
                expMonth = expMonth,
                expYear = expYear,
                cvc = state.cvc,
                cardholderName = state.attendeeName.trim()
            )

            val paymentIntentId = confirmResult.getOrNull()
            if (paymentIntentId == null) {
                val err = confirmResult.exceptionOrNull()
                if (err is java.io.IOException) {
                    // Retryable — the booking keeps holding the slot.
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = "Network error — please retry: ${err.message}"
                    )
                } else {
                    // Definitive (declined / invalid card) — free the slot.
                    repository.cancelBooking(booking.uid)
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        heldBooking = null,
                        idempotencyKey = null,
                        errorMessage = "Payment failed: ${err?.message}. The held slot was released — please book again."
                    )
                }
                return@launch
            }

            // 3. Server verifies the PaymentIntent and marks the booking paid
            repository.markBookingPaid(booking.uid, paymentIntentId).fold(
                onSuccess = {
                    val paid = booking.copy(paid = true, paymentIntentId = paymentIntentId)
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        step = BookingStep.Confirmation(paid, buildAttendee(booking.id))
                    )
                },
                onFailure = { err ->
                    // Booking is committed and payment succeeded; marking paid
                    // failed transiently. Keep the booking, surface the error.
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = "Payment succeeded but could not be recorded: ${err.message}"
                    )
                }
            )
        }
    }

    private fun buildAttendee(bookingId: Long): Attendee {
        val state = _uiState.value
        return Attendee(
            bookingId = bookingId,
            email = state.attendeeEmail,
            name = state.attendeeName,
            phone = state.attendeePhone,
            notes = state.attendeeNotes,
            timezone = state.inviteeTimezone
        )
    }

    private fun parseExpiry(expMonthYear: String): Pair<Int, Int> {
        val parts = expMonthYear.split("/").map { it.trim() }
        val month = parts.getOrNull(0)?.toIntOrNull()?.coerceIn(1, 12) ?: 12
        val yearRaw = parts.getOrNull(1)?.toIntOrNull() ?: 28
        val year = if (yearRaw < 100) 2000 + yearRaw else yearRaw
        return month to year
    }

    fun navigateBackStep() {
        when (_uiState.value.step) {
            BookingStep.EnterDetails -> _uiState.value = _uiState.value.copy(step = BookingStep.SelectDateAndTime)
            BookingStep.StripePayment -> releaseHeldBooking()
            else -> {}
        }
    }

    /** Backing out of the payment step releases the held slot. */
    private fun releaseHeldBooking() {
        val state = _uiState.value
        val booking = state.heldBooking
        if (booking != null) {
            viewModelScope.launch { repository.cancelBooking(booking.uid) }
        }
        _uiState.value = state.copy(
            step = BookingStep.EnterDetails,
            heldBooking = null,
            idempotencyKey = null,
            errorMessage = null
        )
    }
}
