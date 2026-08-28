package com.example.feature.eventtypes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.EventType
import com.example.core.model.User
import com.example.core.repository.UpcomingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EventTypesUiState(
    val eventTypes: List<EventType> = emptyList(),
    val user: User? = null,
    val searchQuery: String = "",
    val filterType: String = "ALL", // "ALL", "INDIVIDUAL", "COLLECTIVE", "PAID"
    val isLoading: Boolean = true
)

class EventTypesViewModel(
    private val repository: UpcomingRepository
) : ViewModel() {

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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), EventTypesUiState())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(filter: String) {
        _filterType.value = filter
    }

    fun toggleEventTypeActive(eventType: EventType) {
        viewModelScope.launch {
            repository.saveEventType(eventType.copy(isActive = !eventType.isActive))
        }
    }

    fun duplicateEventType(eventType: EventType) {
        viewModelScope.launch {
            val copy = eventType.copy(
                id = 0,
                title = "${eventType.title} (Copy)",
                slug = "${eventType.slug}-copy"
            )
            repository.saveEventType(copy)
        }
    }

    fun deleteEventType(id: Long) {
        viewModelScope.launch {
            repository.deleteEventType(id)
        }
    }

    fun saveEventType(eventType: EventType, onSaved: () -> Unit) {
        viewModelScope.launch {
            repository.saveEventType(eventType)
            onSaved()
        }
    }
}
