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
    val locations: com.example.core.network.LocationsMapDto = com.example.core.network.LocationsMapDto(),
    val defaultLocationType: String? = null,
    val timeFormat: String = "12h",
    val credentialHints: Map<String, String> = emptyMap(),
    val saving: Boolean = false,
    val error: String? = null,
    val savedTick: Long = 0
)

class SettingsViewModel(
    private val repository: UpcomingRepository,
    private val authRepository: com.example.core.auth.AuthRepository? = null
) : ViewModel() {

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
                            locations = metadata.locations ?: com.example.core.network.LocationsMapDto(),
                            defaultLocationType = metadata.defaultLocationType,
                            timeFormat = metadata.prefs?.timeFormat ?: "12h"
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            runCatching {
                _uiState.update { state ->
                    state.copy(credentialHints = repository.credentialHints().associate { it.type to it.hint })
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

    /** Store/replace one configured location (its own label + value) and
     *  optionally make it the default. */
    fun saveLocationDefault(type: String, location: LocationDto?, makeDefault: Boolean = false) {
        viewModelScope.launch { runSave { repository.updateLocationDefault(type, location, makeDefault) } }
    }

    fun setDefaultLocationType(type: String) {
        viewModelScope.launch { runSave { repository.setDefaultLocationType(type) } }
    }

    fun putCredential(type: String, value: String) {
        viewModelScope.launch {
            runSave {
                val hint = repository.putCredential(type, value)
                _uiState.update { it.copy(credentialHints = it.credentialHints + (hint.type to hint.hint)) }
            }
        }
    }

    /** Revoke the session server-side, clear local state, exit to auth. */
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository?.logout()
            onDone()
        }
    }

    fun deleteCredential(type: String) {
        viewModelScope.launch {
            runSave {
                repository.deleteCredential(type)
                _uiState.update { it.copy(credentialHints = it.credentialHints - type) }
            }
        }
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
