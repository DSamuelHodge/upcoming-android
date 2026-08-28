package com.example.feature.availability

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.model.AvailabilityRule
import com.example.core.model.Schedule
import com.example.core.model.User
import com.example.core.repository.UpcomingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DayScheduleState(
    val dayOfWeek: Int, // 0 = Sun, 1 = Mon, ..., 6 = Sat
    val dayName: String,
    val isEnabled: Boolean,
    val startTime: String = "09:00",
    val endTime: String = "17:00"
)

data class AvailabilityUiState(
    val schedule: Schedule? = null,
    val user: User? = null,
    val days: List<DayScheduleState> = emptyList(),
    val dateOverrides: List<AvailabilityRule> = emptyList(),
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false
)

class AvailabilityViewModel(
    private val repository: UpcomingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AvailabilityUiState())
    val uiState: StateFlow<AvailabilityUiState> = _uiState.asStateFlow()

    init {
        loadAvailability()
    }

    private fun loadAvailability() {
        viewModelScope.launch {
            val user = repository.getPrimaryUser()
            val sched = repository.getScheduleForUser(user.id)

            repository.getAvailabilityRulesFlow(sched.id).collect { rules ->
                val dayNames = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
                val recurringDays = (0..6).map { dayIdx ->
                    val rule = rules.find { it.dayOfWeek == dayIdx && it.dateOverride == null }
                    DayScheduleState(
                        dayOfWeek = dayIdx,
                        dayName = dayNames[dayIdx],
                        isEnabled = rule != null,
                        startTime = rule?.startTime ?: "09:00",
                        endTime = rule?.endTime ?: "17:00"
                    )
                }

                val overrides = rules.filter { it.dateOverride != null }

                _uiState.value = AvailabilityUiState(
                    schedule = sched,
                    user = user,
                    days = recurringDays,
                    dateOverrides = overrides
                )
            }
        }
    }

    fun toggleDay(dayOfWeek: Int, isEnabled: Boolean) {
        val currentDays = _uiState.value.days.toMutableList()
        val idx = currentDays.indexOfFirst { it.dayOfWeek == dayOfWeek }
        if (idx >= 0) {
            currentDays[idx] = currentDays[idx].copy(isEnabled = isEnabled)
            _uiState.value = _uiState.value.copy(days = currentDays)
        }
    }

    fun updateDayTimes(dayOfWeek: Int, startTime: String, endTime: String) {
        val currentDays = _uiState.value.days.toMutableList()
        val idx = currentDays.indexOfFirst { it.dayOfWeek == dayOfWeek }
        if (idx >= 0) {
            currentDays[idx] = currentDays[idx].copy(startTime = startTime, endTime = endTime)
            _uiState.value = _uiState.value.copy(days = currentDays)
        }
    }

    fun updateTimezone(newTimezone: String) {
        val sched = _uiState.value.schedule ?: return
        val user = _uiState.value.user ?: return
        viewModelScope.launch {
            repository.updateScheduleTimezone(sched.id, user.id, newTimezone)
            _uiState.value = _uiState.value.copy(schedule = sched.copy(timezone = newTimezone))
        }
    }

    fun saveWeeklyRules(onSuccess: () -> Unit) {
        val sched = _uiState.value.schedule ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true)

            val activeRules = mutableListOf<AvailabilityRule>()
            // Add recurring days
            for (day in _uiState.value.days) {
                if (day.isEnabled) {
                    activeRules.add(
                        AvailabilityRule(
                            scheduleId = sched.id,
                            dayOfWeek = day.dayOfWeek,
                            dateOverride = null,
                            startTime = day.startTime,
                            endTime = day.endTime
                        )
                    )
                }
            }
            // Retain overrides
            for (override in _uiState.value.dateOverrides) {
                activeRules.add(override)
            }

            repository.saveWeeklyAvailability(sched.id, activeRules)
            _uiState.value = _uiState.value.copy(isSaving = false, saveSuccess = true)
            onSuccess()
        }
    }

    fun addDateOverride(dateStr: String, startTime: String, endTime: String) {
        val sched = _uiState.value.schedule ?: return
        viewModelScope.launch {
            repository.addDateOverride(sched.id, dateStr, startTime, endTime)
        }
    }

    fun removeOverride(ruleId: Long) {
        viewModelScope.launch {
            repository.removeAvailabilityRule(ruleId)
        }
    }
}
