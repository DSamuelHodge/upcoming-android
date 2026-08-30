package com.example.core.auth

import com.example.core.network.LoginRequest
import com.example.core.network.MeResponseDto
import com.example.core.network.RefreshRequest
import com.example.core.network.SignUpRequest
import com.example.core.network.UpcomingApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/** Owns the auth lifecycle: restore-on-launch, signup/login/logout, and the
 *  [AuthState] the splash gate routes on. Server errors surface as Result
 *  failures with the API's message (e.g. "email already registered"). */
class AuthRepository(
    private val api: UpcomingApi,
    private val tokens: AuthTokenManager
) {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        _authState.value = when {
            tokens.isLoggedIn() -> AuthState.LoggedIn(tokens.lastUserId())
            tokens.isDemo() -> AuthState.Demo
            else -> AuthState.LoggedOut
        }
    }

    suspend fun signUp(
        email: String,
        password: String,
        username: String,
        displayName: String?,
        timezone: String?
    ): Result<MeResponseDto> = withContext(Dispatchers.IO) {
        runCatching {
            val auth = api.signUp(SignUpRequest(email.trim(), password, username.trim(), displayName?.trim(), timezone))
            tokens.save(auth)
            _authState.value = AuthState.LoggedIn(auth.user.id)
            auth.user
        }
    }

    suspend fun login(email: String, password: String): Result<MeResponseDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val auth = api.login(LoginRequest(email.trim(), password))
                tokens.save(auth)
                _authState.value = AuthState.LoggedIn(auth.user.id)
                auth.user
            }
        }

    /** Demo mode: local seed data via the legacy shared secret, no account. */
    fun enterDemoMode() {
        tokens.enterDemoMode()
        _authState.value = AuthState.Demo
    }

    /** Best-effort server-side revocation, then always clear local state so
     *  the app lands back on the auth screen. */
    suspend fun logout(): Unit = withContext(Dispatchers.IO) {
        try {
            tokens.refreshToken()?.let { api.logout(RefreshRequest(it)) }
        } catch (_: Exception) {
            // Even if the network call fails, the local session is dead.
        }
        tokens.clear()
        _authState.value = AuthState.LoggedOut
    }
}
