package com.example.core.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.core.network.AuthResponse
import com.example.core.network.UpcomingApiClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** AuthState consumed by the splash/nav gate and auth screens. */
sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data object Demo : AuthState
    data class LoggedIn(val userId: Long) : AuthState
}

/** Encrypted storage + lifecycle for the JWT pair. Access tokens are sent by
 *  the OkHttp interceptor; on 401 the [Authenticator] calls
 *  [refreshAccessToken] (synchronous, single-flight) and retries once. */
class AuthTokenManager(private val context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "upcoming_auth",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    @Volatile private var refreshing = false

    fun accessToken(): String? = prefs.getString(KEY_ACCESS, null)
    fun refreshToken(): String? = prefs.getString(KEY_REFRESH, null)
    fun isLoggedIn(): Boolean = accessToken() != null
    fun isDemo(): Boolean = prefs.getBoolean(KEY_DEMO, false)
    fun lastUserId(): Long = prefs.getLong(KEY_USER_ID, 0L)

    fun save(auth: AuthResponse) {
        prefs.edit()
            .putString(KEY_ACCESS, auth.accessToken)
            .putString(KEY_REFRESH, auth.refreshToken)
            .putLong(KEY_USER_ID, auth.user.id)
            .putBoolean(KEY_DEMO, false)
            .apply()
    }

    fun saveTokens(access: String, refresh: String) {
        prefs.edit()
            .putString(KEY_ACCESS, access)
            .putString(KEY_REFRESH, refresh)
            .putBoolean(KEY_DEMO, false)
            .apply()
    }

    fun enterDemoMode() {
        prefs.edit().clear().putBoolean(KEY_DEMO, true).apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    /** Calls POST /auth/refresh; on success persists the rotated pair and
     *  returns true. Returns false when the refresh token is expired/revoked
     *  (caller should log out locally). */
    @Synchronized
    fun refreshAccessToken(baseUrl: String): Boolean {
        if (refreshing) return false
        refreshing = true
        try {
            val refresh = refreshToken() ?: return false
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
            val body = """{"refreshToken":"$refresh"}"""
                .toRequestBody("application/json; charset=utf-8".toMediaType())
            val url = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
            val request = Request.Builder().url(url + "auth/refresh").post(body).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return false
                val auth = UpcomingApiClient.moshi
                    .adapter(AuthResponse::class.java)
                    .fromJson(response.body?.string() ?: return false) ?: return false
                save(auth)
                return true
            }
        } finally {
            refreshing = false
        }
    }

    private companion object {
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_DEMO = "demo_mode"
    }
}
