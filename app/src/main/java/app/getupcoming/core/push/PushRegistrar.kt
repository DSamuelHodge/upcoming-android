package app.getupcoming.core.push

import android.content.Context
import app.getupcoming.UpcomingApplication
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Soft-fail FCM token plumbing (roadmap Phase 0 "FCM setup"): push must
 * never crash or block the app, so every Firebase touch is wrapped — absent
 * Google Play services simply yields no token and no registration.
 */
object PushRegistrar {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Fire-and-forget registration (app resume). */
    fun registerAsync(context: Context) {
        scope.launch { register(context) }
    }

    /** Fire-and-forget with a known token (FirebaseMessagingService.onNewToken). */
    fun registerTokenAsync(context: Context, token: String) {
        scope.launch { registerToken(context, token) }
    }

    suspend fun register(context: Context): Boolean {
        val token = currentToken() ?: return false
        return registerToken(context, token)
    }

    suspend fun registerToken(context: Context, token: String): Boolean {
        val container = runCatching {
            (context.applicationContext as UpcomingApplication).container
        }.getOrNull() ?: return false
        if (!container.tokens.isLoggedIn() || container.tokens.isDemo()) return false
        return runCatching { container.repository.registerFcmToken(token) }
            .getOrDefault(false)
    }

    /** Current FCM registration token, or null when Play services are absent. */
    suspend fun currentToken(): String? = withContext(Dispatchers.IO) {
        runCatching {
            suspendCoroutine<String?> { cont ->
                try {
                    FirebaseMessaging.getInstance().token
                        .addOnSuccessListener { cont.resume(it) }
                        .addOnFailureListener { cont.resume(null) }
                } catch (t: Throwable) {
                    cont.resume(null)
                }
            }
        }.getOrNull()
    }
}
