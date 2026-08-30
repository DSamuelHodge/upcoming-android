package com.example.feature.scheduling

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.EventType
import com.example.core.model.User
import com.example.core.network.SingleUseLinkDto
import com.example.core.repository.UpcomingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Base URL for every shareable booking link (official domain). */
const val BOOKING_BASE_URL = "https://getupcoming.app"

fun personalLink(username: String?): String = "$BOOKING_BASE_URL/${username.orEmpty()}"

fun eventLink(username: String?, slug: String): String = "$BOOKING_BASE_URL/${username.orEmpty()}/$slug"

/** Default message pre-filled when a link is shared via the system sheet. */
fun shareMessage(url: String, hostName: String?): String =
    "Hi! You can book time with me${hostName?.takeIf { it.isNotBlank() }?.let { " — $it" } ?: ""} here: $url"

data class SchedulingUiState(
    val user: User? = null,
    val eventTypes: List<EventType> = emptyList(),
    // Single-use links fetched per event type id.
    val linksByEventType: Map<Long, List<SingleUseLinkDto>> = emptyMap(),
    val linkGeneration: Set<Long> = emptySet(), // event type ids with a create/revoke in flight
    val isLoading: Boolean = false,
    val error: String? = null
)

class SchedulingViewModel(
    private val repository: UpcomingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SchedulingUiState())
    val uiState: StateFlow<SchedulingUiState> = _uiState.asStateFlow()

    val user: StateFlow<User?> = repository.getPrimaryUserFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val eventTypes: StateFlow<List<EventType>> = repository.allEventTypes
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val types = eventTypes.value.ifEmpty { repository.allEventTypes.first() }
            val links = mutableMapOf<Long, List<SingleUseLinkDto>>()
            for (et in types) {
                repository.getSingleUseLinks(et.id)?.let { links[et.id] = it }
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                eventTypes = types,
                linksByEventType = links
            )
        }
    }

    fun createLinks(eventTypeId: Long, count: Int = 1, expiresInDays: Int? = null) {
        viewModelScope.launch {
            setGenerating(eventTypeId, true)
            val created = repository.createSingleUseLinks(eventTypeId, count, expiresInDays)
            setGenerating(eventTypeId, false)
            if (created == null) {
                _uiState.value = _uiState.value.copy(error = "Couldn't create single-use links")
                return@launch
            }
            _uiState.value = _uiState.value.copy(error = null)
            loadLinks(eventTypeId)
        }
    }

    fun revokeLink(eventTypeId: Long, linkId: Long) {
        viewModelScope.launch {
            setGenerating(eventTypeId, true)
            repository.revokeSingleUseLink(linkId)
            setGenerating(eventTypeId, false)
            loadLinks(eventTypeId)
        }
    }

    fun loadLinks(eventTypeId: Long) {
        viewModelScope.launch {
            val links = repository.getSingleUseLinks(eventTypeId)
            if (links != null) {
                _uiState.value = _uiState.value.copy(
                    linksByEventType = _uiState.value.linksByEventType + (eventTypeId to links)
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    private fun setGenerating(eventTypeId: Long, busy: Boolean) {
        _uiState.value = _uiState.value.copy(
            linkGeneration = if (busy) _uiState.value.linkGeneration + eventTypeId
            else _uiState.value.linkGeneration - eventTypeId
        )
    }
}
