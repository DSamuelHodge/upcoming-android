package com.example.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.User
import com.example.core.network.LocationDto
import com.example.core.network.UserMetadataDto
import com.example.core.network.UserPrefsDto
import com.example.core.repository.UpcomingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val isLoading: Boolean = true,
    val user: User? = null,
    val scheduleName: String? = null,
    val scheduleTimezone: String? = null,
    val defaultLocation: LocationDto? = null,
    val timeFormat: String = "12h",
    val saving: Boolean = false,
    val error: String? = null,
    val savedTick: Long = 0
)

class SettingsViewModel(private val repository: UpcomingRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // A failed refresh must never prevent the Room-backed flow below
            // from rendering (offline-safe).
            runCatching { repository.refreshMe() }
            repository.getPrimaryUserFlow().collect { user ->
                if (user != null) {
                    val metadata = parseMetadata(user.metadata)
                    val schedule = repository.getScheduleForUser(user.id)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            user = user,
                            scheduleName = schedule.name.ifBlank { "Working Hours" },
                            scheduleTimezone = schedule.timezone,
                            defaultLocation = metadata.defaultLocation,
                            timeFormat = metadata.prefs?.timeFormat ?: "12h"
                        )
                    }
                }
            }
        }
    }

    fun updateProfile(displayName: String, email: String, username: String, avatarUrl: String) {
        viewModelScope.launch { runSave { repository.updateProfile(displayName = displayName, email = email, username = username, avatarUrl = avatarUrl) } }
    }

    fun updateTimezone(timezone: String) {
        viewModelScope.launch { runSave { repository.updateTimezone(timezone) } }
    }

    fun setTimeFormat(timeFormat: String) {
        viewModelScope.launch { runSave { repository.setTimeFormatPref(timeFormat) } }
    }

    fun setDefaultLocation(location: LocationDto?) {
        viewModelScope.launch { runSave { repository.updateDefaultLocation(location) } }
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

    private fun parseMetadata(raw: String): UserMetadataDto {
        return runCatching {
            com.example.core.network.UpcomingApiClient.moshi
                .adapter(UserMetadataDto::class.java).fromJson(raw)
        }.getOrNull() ?: UserMetadataDto(prefs = UserPrefsDto(timeFormat = "12h"))
    }
}
