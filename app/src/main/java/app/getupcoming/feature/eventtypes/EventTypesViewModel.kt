package app.getupcoming.feature.eventtypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.getupcoming.core.model.EventType
import app.getupcoming.core.model.User
import app.getupcoming.core.repository.UpcomingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EventTypesUiState(
    val eventTypes: List<EventType> = emptyList(),
    val user: User? = null,
    val searchQuery: String = "",
    val filterType: String = "ALL", // "ALL", "INDIVIDUAL", "COLLECTIVE", "PAID"
    val isLoading: Boolean = true,
    // Server sync in flight (spinner); isLoading alone only tracks Room.
    val isRefreshing: Boolean = false,
    val syncError: String? = null
)

class EventTypesViewModel(
    private val repository: UpcomingRepository
) : ViewModel() {

    private val _isRefreshing = MutableStateFlow(false)
    private val _syncError = MutableStateFlow<String?>(null)

    init {
        refresh()
    }

    /** Pull event types from the server; surfaces progress + failure to the UI. */
    fun refresh() {
        if (_isRefreshing.value) return
        _isRefreshing.value = true
        _syncError.value = null
        viewModelScope.launch {
            try {
                repository.refreshEventTypes()
            } catch (e: Exception) {
                if (!e.isNetworkError()) _syncError.value = e.message ?: "Sync failed"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    /** User-level default location from Settings — prefills new event types. */
    private val _defaultLocation =
        MutableStateFlow<app.getupcoming.core.network.LocationDto?>(null)
    val defaultLocation: StateFlow<app.getupcoming.core.network.LocationDto?> = _defaultLocation.asStateFlow()

    init {
        viewModelScope.launch {
            _defaultLocation.value = runCatching { repository.defaultLocation() }.getOrNull()
        }
    }

    private val _searchQuery = MutableStateFlow("")
    private val _filterType = MutableStateFlow("ALL")

    val uiState: StateFlow<EventTypesUiState> = combine(
        repository.allEventTypes,
        repository.getPrimaryUserFlow(),
        _searchQuery,
        _filterType
    ) { types, user, query, filter ->
        val filtered = types.filter { et ->
            val matchesQuery = query.isBlank() || et.title.contains(query, ignoreCase = true) || et.slug.contains(query, ignoreCase = true)
            val matchesFilter = when (filter) {
                "INDIVIDUAL" -> et.schedulingType == "individual"
                "COLLECTIVE" -> et.schedulingType == "collective" || et.schedulingType == "round_robin"
                "PAID" -> et.priceInCents > 0
                else -> true
            }
            matchesQuery && matchesFilter
        }
        EventTypesUiState(
            eventTypes = filtered,
            user = user,
            searchQuery = query,
            filterType = filter,
            isLoading = false
        )
    }.combine(_isRefreshing) { state, refreshing -> state.copy(isRefreshing = refreshing) }
        .combine(_syncError) { state, err -> state.copy(syncError = err) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EventTypesUiState())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(filter: String) {
        _filterType.value = filter
    }

    private fun Exception.isNetworkError(): Boolean =
        this is java.io.IOException || this is java.net.UnknownHostException || message?.contains("Unable to resolve host", ignoreCase = true) == true

    fun toggleEventTypeActive(eventType: EventType) {
        viewModelScope.launch {
            repository.saveEventType(eventType.copy(isActive = !eventType.isActive))
                .onFailure { _syncError.value = it.message ?: "Could not update event type" }
        }
    }

    fun duplicateEventType(eventType: EventType) {
        viewModelScope.launch {
            val base = eventType.copy(id = 0, title = "${eventType.title} (Copy)", slug = "${eventType.slug}-copy")
            val result = repository.saveEventType(base)
            if (result.isFailure) {
                // Most likely a slug conflict with an earlier copy — retry
                // once with a numbered suffix.
                repository.saveEventType(base.copy(slug = "${eventType.slug}-copy-2"))
                    .onFailure { _syncError.value = it.message ?: "Could not duplicate event type" }
            }
        }
    }

    fun deleteEventType(id: Long) {
        viewModelScope.launch {
            repository.deleteEventType(id)
                .onFailure { _syncError.value = it.message ?: "Could not delete event type" }
        }
    }

    fun saveEventType(eventType: EventType, onSaved: () -> Unit, onError: (String) -> Unit = { _syncError.value = it }) {
        viewModelScope.launch {
            repository.saveEventType(eventType)
                .onSuccess { onSaved() }
                .onFailure { onError(it.message ?: "Could not save event type") }
        }
    }
}
