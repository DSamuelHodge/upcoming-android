package com.example.feature.bookingflow

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.engine.PaymentResult
import com.example.core.engine.SchedulingEngine
import com.example.core.engine.StripeCardInput
import com.example.core.engine.StripePaymentSimulator
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
        val et = state.eventType
        if (et != null && et.priceInCents > 0) {
            _uiState.value = state.copy(step = BookingStep.StripePayment, errorMessage = null)
        } else {
            submitBooking(paid = false, paymentIntentId = null)
        }
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

    fun submitStripePaymentAndBook() {
        val state = _uiState.value
        val et = state.eventType ?: return

        _uiState.value = state.copy(isSubmitting = true, errorMessage = null)

        viewModelScope.launch {
            val paymentResult = StripePaymentSimulator.processPayment(
                amountInCents = et.priceInCents,
                currency = et.currency,
                cardInput = StripeCardInput(
                    cardNumber = state.cardNumber,
                    expMonth = "12",
                    expYear = "2028",
                    cvc = state.cvc,
                    cardholderName = state.attendeeName
                )
            )

            when (paymentResult) {
                is PaymentResult.Success -> {
                    submitBooking(paid = true, paymentIntentId = paymentResult.paymentIntentId)
                }
                is PaymentResult.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        errorMessage = paymentResult.message
                    )
                }
            }
        }
    }

    private fun submitBooking(paid: Boolean, paymentIntentId: String?) {
        val state = _uiState.value
        val et = state.eventType ?: return
        val slot = state.selectedSlot ?: return

        _uiState.value = state.copy(isSubmitting = true, errorMessage = null)

        viewModelScope.launch {
            val idempotencyKey = "idemp_${UUID.randomUUID().toString().replace("-", "").take(16)}"
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
                idempotencyKey = idempotencyKey,
                paid = paid,
                paymentIntentId = paymentIntentId
            )

            result.onSuccess { booking ->
                val attendee = Attendee(
                    bookingId = booking.id,
                    email = state.attendeeEmail,
                    name = state.attendeeName,
                    phone = state.attendeePhone,
                    notes = state.attendeeNotes,
                    timezone = state.inviteeTimezone
                )
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    step = BookingStep.Confirmation(booking, attendee)
                )
            }.onFailure { err ->
                _uiState.value = _uiState.value.copy(
                    isSubmitting = false,
                    errorMessage = err.message ?: "Booking failed. Please choose another slot."
                )
            }
        }
    }

    fun navigateBackStep() {
        when (_uiState.value.step) {
            BookingStep.EnterDetails -> _uiState.value = _uiState.value.copy(step = BookingStep.SelectDateAndTime)
            BookingStep.StripePayment -> _uiState.value = _uiState.value.copy(step = BookingStep.EnterDetails)
            else -> {}
        }
    }
}
