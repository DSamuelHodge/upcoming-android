package app.getupcoming.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.getupcoming.core.auth.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val username: String = "",
    val displayName: String = "",
    val agreedToTerms: Boolean = false,
    val isSignUp: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val canSubmitLogin: Boolean
        get() = email.contains('@') && password.length >= 8 && !isLoading
    val canSubmitSignUp: Boolean
        get() = canSubmitLogin && username.length >= 2 && agreedToTerms
}

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun update(transform: (AuthUiState) -> AuthUiState) {
        _uiState.update(transform)
    }

    fun toggleMode() {
        _uiState.update { it.copy(isSignUp = !it.isSignUp, error = null) }
    }

    fun login(onSuccess: () -> Unit) {
        submit {
            // Only navigate when auth actually succeeded — otherwise the gate
            // (bottom bar, sync) never engages because authState stays LoggedOut.
            authRepository.login(it.email, it.password)
                .onSuccess { onSuccess() }
                .onFailure { throw it }
        }
    }

    fun signUp(onSuccess: () -> Unit) {
        submit {
            authRepository.signUp(it.email, it.password, it.username, it.displayName, null)
                .onSuccess { onSuccess() }
                .onFailure { throw it }
        }
    }

    fun enterDemo(onDone: () -> Unit) {
        authRepository.enterDemoMode()
        onDone()
    }

    private fun submit(block: suspend (AuthUiState) -> Unit) {
        val state = _uiState.value
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                block(state)
                _uiState.update { it.copy(isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message ?: "Something went wrong") }
            }
        }
    }
}
