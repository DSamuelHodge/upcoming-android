package com.example.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.*
import com.example.core.repository.UpcomingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val user: User? = null,
    val eventTypes: List<EventType> = emptyList(),
    val upcomingBookings: List<Booking> = emptyList(),
    val nextBooking: Booking? = null,
    val nextBookingAttendee: Attendee? = null,
    val nextBookingEventType: EventType? = null,
    val totalRevenueCents: Int = 0,
    val hoursBookedThisMonth: Double = 0.0,
    val isLoading: Boolean = true
)

class DashboardViewModel(
    private val repository: UpcomingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            loadDashboardData()
        }
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            combine(
                repository.getPrimaryUserFlow(),
                repository.allEventTypes,
                repository.upcomingBookings,
                repository.allBookings
            ) { user, eventTypes, upcoming, allBookings ->
                val next = upcoming.firstOrNull()
                val nextAttendee = next?.let { repository.getAttendeeForBooking(it.id) }
                val nextEventType = next?.let { repository.getEventTypeById(it.eventTypeId) }

                val totalRevenue = allBookings.filter { it.status == "accepted" && it.paid }
                    .sumOf { booking ->
                        val et = eventTypes.find { it.id == booking.eventTypeId }
                        et?.priceInCents ?: 0
                    }

                val totalMinutes = allBookings.filter { it.status == "accepted" }
                    .sumOf { booking ->
                        val et = eventTypes.find { it.id == booking.eventTypeId }
                        et?.lengthMinutes ?: 30
                    }

                DashboardUiState(
                    user = user,
                    eventTypes = eventTypes,
                    upcomingBookings = upcoming,
                    nextBooking = next,
                    nextBookingAttendee = nextAttendee,
                    nextBookingEventType = nextEventType,
                    totalRevenueCents = totalRevenue,
                    hoursBookedThisMonth = totalMinutes / 60.0,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleEventType(eventType: EventType) {
        viewModelScope.launch {
            repository.saveEventType(eventType.copy(isActive = !eventType.isActive))
        }
    }
}
