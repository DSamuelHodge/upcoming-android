package app.getupcoming.feature.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.getupcoming.core.model.Booking
import app.getupcoming.core.prefs.NotificationPrefs
import app.getupcoming.core.repository.UpcomingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One concrete upcoming reminder: a booking crossed with a reminder offset. */
data class UpcomingReminder(
    val booking: Booking,
    val bookingTitle: String,
    val minutesBefore: Int,
    val triggerTimeMs: Long
)

data class NotificationUiState(
    val prefs: NotificationPrefs = NotificationPrefs(),
    val upcomingReminders: List<UpcomingReminder> = emptyList(),
    val saving: Boolean = false,
    val error: String? = null,
    val savedTick: Long = 0
)

class NotificationsViewModel(private val repository: UpcomingRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    init {
        // Booking titles for the upcoming-reminders preview.
        viewModelScope.launch {
            repository.allEventTypes.collect { types ->
                titleMap = types.associate { it.id to it.title }
                recompute()
            }
        }
        viewModelScope.launch {
            combine(
                repository.notificationPrefs,
                repository.upcomingBookings
            ) { prefs, bookings -> prefs to bookings }
                .collect { (prefs, bookings) ->
                    latestPrefs = prefs
                    latestBookings = bookings
                    recompute()
                }
        }
    }

    private var latestPrefs: NotificationPrefs = NotificationPrefs()
    private var latestBookings: List<Booking> = emptyList()
    private var titleMap: Map<Long, String> = emptyMap()

    private fun recompute() {
        val now = System.currentTimeMillis()
        val reminders = if (!latestPrefs.remindersEnabled) emptyList() else {
            latestBookings.flatMap { booking ->
                val startMs = runCatching {
                    app.getupcoming.core.engine.SchedulingEngine.parseIsoUtc(booking.startTimeUtc).time
                }.getOrNull() ?: return@flatMap emptyList()
                latestPrefs.reminderOffsets.mapNotNull { offset ->
                    val trigger = startMs - offset * 60_000L
                    if (trigger <= now) null
                    else UpcomingReminder(
                        booking = booking,
                        bookingTitle = titleMap[booking.eventTypeId] ?: "Meeting",
                        minutesBefore = offset,
                        triggerTimeMs = trigger
                    )
                }
            }.sortedBy { it.triggerTimeMs }
        }
        _uiState.update { it.copy(prefs = latestPrefs, upcomingReminders = reminders) }
    }

    fun setPushAlerts(enabled: Boolean) {
        viewModelScope.launch {
            repository.setPushAlertsEnabled(enabled)
        }
    }

    fun setSoundVibration(enabled: Boolean) {
        viewModelScope.launch {
            repository.setSoundVibrationEnabled(enabled)
        }
    }

    fun setReminderSettings(enabled: Boolean, offsets: List<Int>) {
        viewModelScope.launch { runSave { repository.updateReminderSettings(enabled, offsets) } }
    }

    private suspend fun runSave(block: suspend () -> Unit) {
        _uiState.update { it.copy(saving = true, error = null) }
        try {
            block()
            _uiState.update { it.copy(saving = false, savedTick = System.currentTimeMillis()) }
        } catch (e: Exception) {
            _uiState.update { it.copy(saving = false, error = e.message ?: "Save failed") }
        }
    }
}
