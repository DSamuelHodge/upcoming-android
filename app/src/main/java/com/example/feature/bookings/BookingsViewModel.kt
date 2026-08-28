package com.example.feature.bookings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.engine.NotificationAndReminderManager
import com.example.core.engine.SchedulingEngine
import com.example.core.model.Attendee
import com.example.core.model.Booking
import com.example.core.model.EventType
import com.example.core.repository.UpcomingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

data class BookingWithDetails(
    val booking: Booking,
    val attendee: Attendee?,
    val eventType: EventType?
)

data class BookingsUiState(
    val bookings: List<BookingWithDetails> = emptyList(),
    val selectedTab: Int = 0, // 0 = Upcoming, 1 = Past, 2 = Cancelled
    val searchQuery: String = "",
    val isLoading: Boolean = true
)

class BookingsViewModel(
    private val repository: UpcomingRepository,
    private val context: Context
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    private val _searchQuery = MutableStateFlow("")

    val uiState: StateFlow<BookingsUiState> = combine(
        repository.allBookings,
        repository.allEventTypes,
        _selectedTab,
        _searchQuery
    ) { allBookings, allTypes, tab, query ->
        val now = Date()
        val detailed = allBookings.map { booking ->
            val attendee = repository.getAttendeeForBooking(booking.id)
            val eventType = allTypes.find { it.id == booking.eventTypeId }
            BookingWithDetails(booking, attendee, eventType)
        }

        val filtered = detailed.filter { item ->
            val b = item.booking
            val startDate = SchedulingEngine.parseIsoUtc(b.startTimeUtc)
            val isUpcoming = b.status != "cancelled" && startDate.after(now)
            val isPast = b.status != "cancelled" && !startDate.after(now)
            val isCancelled = b.status == "cancelled"

            val matchesTab = when (tab) {
                0 -> isUpcoming
                1 -> isPast
                2 -> isCancelled
                else -> true
            }

            val matchesQuery = query.isBlank() ||
                    (item.attendee?.name?.contains(query, ignoreCase = true) == true) ||
                    (item.attendee?.email?.contains(query, ignoreCase = true) == true) ||
                    (item.eventType?.title?.contains(query, ignoreCase = true) == true) ||
                    b.uid.contains(query, ignoreCase = true)

            matchesTab && matchesQuery
        }

        BookingsUiState(
            bookings = filtered,
            selectedTab = tab,
            searchQuery = query,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), BookingsUiState())

    fun setSelectedTab(tab: Int) {
        _selectedTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun cancelBooking(uid: String, onCompleted: () -> Unit) {
        viewModelScope.launch {
            repository.cancelBooking(uid)
            onCompleted()
        }
    }

    fun triggerTestReminder(booking: Booking, eventType: EventType?, attendee: Attendee?) {
        NotificationAndReminderManager.triggerFcmNotification(
            context = context,
            title = "Reminder: ${eventType?.title ?: "Meeting"}",
            body = "Meeting with ${attendee?.name ?: "Guest"} is starting soon.",
            bookingUid = booking.uid
        )
    }
}
