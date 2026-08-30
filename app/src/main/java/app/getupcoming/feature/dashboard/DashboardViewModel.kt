package app.getupcoming.feature.dashboard

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.getupcoming.core.model.*
import androidx.glance.appwidget.updateAll
import app.getupcoming.core.repository.UpcomingRepository
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
    private val repository: UpcomingRepository,
    private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            if (repository.isDemoSession()) {
                // Demo persona only: seed the showcase data.
                repository.seedInitialDataIfEmpty()
            } else {
                // Signed-in session: purge any demo rows from the cache and
                // re-point identity at the real account before syncing.
                runCatching { repository.onSessionEstablished() }
            }
            // Network-first sync: API → Room. Room keeps serving if offline.
            runCatching { repository.refreshEventTypes() }
            runCatching { repository.refreshBookings() }
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
                viewModelScope.launch { updateWidgetSnapshot(state) }
            }
        }
    }

    /** Keeps the home-screen Glance widgets (2x2 + 4x2) in sync with the
     *  dashboard's upcoming-bookings snapshot. */
    private suspend fun updateWidgetSnapshot(state: DashboardUiState) {
        val fmt = java.text.SimpleDateFormat("EEE, MMM d • h:mm a", java.util.Locale.US).apply {
            timeZone = java.util.TimeZone.getDefault()
        }
        fun label(iso: String): String = try {
            fmt.format(java.util.Date.from(java.time.Instant.parse(iso)))
        } catch (_: Exception) {
            iso
        }
        fun joinUrl(booking: Booking): String? = try {
            booking.locationJson?.let {
                val loc = org.json.JSONObject(it)
                loc.optString("url").takeIf { u -> u.isNotBlank() }
            }
        } catch (_: Exception) {
            null
        }
        suspend fun row(booking: Booking): app.getupcoming.core.widget.WidgetBooking =
            app.getupcoming.core.widget.WidgetBooking(
                uid = booking.uid,
                attendeeName = repository.getAttendeeForBooking(booking.id)?.name ?: "Invitee",
                eventTitle = state.eventTypes.find { it.id == booking.eventTypeId }?.title ?: "Meeting",
                timeLabel = label(booking.startTimeUtc),
                joinUrl = joinUrl(booking)
            )
        val list = buildList {
            for (booking in state.upcomingBookings.take(3)) add(row(booking))
        }
        app.getupcoming.core.widget.WidgetSnapshotStore.save(appContext, state.nextBooking?.let { row(it) }, list)
        app.getupcoming.core.widget.UpcomingWidget().updateAll(appContext)
        app.getupcoming.core.widget.UpcomingListWidget().updateAll(appContext)
    }

    fun toggleEventType(eventType: EventType) {
        viewModelScope.launch {
            repository.saveEventType(eventType.copy(isActive = !eventType.isActive))
        }
    }
}
