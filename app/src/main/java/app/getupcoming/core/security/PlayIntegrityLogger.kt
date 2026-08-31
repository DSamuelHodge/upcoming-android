package app.getupcoming.core.security

import android.content.Context
import android.util.Log
import com.google.android.play.core.integrity.IntegrityManager
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Phase 0.6 — Play Integrity integration, **logging-only MVP** (roadmap:
 * "Non-blocking (logging only at MVP)").
 *
 * After a successful signup/login we request a standard integrity token.
 * This build NEVER blocks a session on the verdict and the token is not
 * yet forwarded anywhere: server-side verification needs a verdict
 * endpoint on upcoming-db (Google Play Integrity API + a decryption key),
 * which is a backend-lane follow-up. Until then this only measures that
 * the device can produce tokens at all; failures are swallowed with a
 * warning so auth UX is never affected.
 *
 * Stricter policy (reject non-certified devices) is a deliberate NO for
 * MVP — wire it in the same place once the verdict lands server-side.
 */
object PlayIntegrityLogger {

    private const val TAG = "PlayIntegrity"
    private const val CLOUD_PROJECT_NUMBER: Long = 189422075971L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Fire-and-forget; safe to call from any thread after auth succeeds. */
    fun logAfterAuth(context: Context) {
        try {
            val manager: IntegrityManager = IntegrityManagerFactory.create(context.applicationContext)
            val request: IntegrityTokenRequest = IntegrityTokenRequest.builder()
                .setNonce(UUID.randomUUID().toString())
                .setCloudProjectNumber(CLOUD_PROJECT_NUMBER)
                .build()
            scope.launch {
                try {
                    manager.requestIntegrityToken(request)
                        .addOnSuccessListener { response ->
                            // Logging-only MVP: the token proves the pipeline works.
                            // Deliberately NOT logged itself (it is a credential).
                            Log.i(TAG, "integrity token acquired (${response.token().length} chars)")
                        }
                        .addOnFailureListener { e ->
                            Log.w(TAG, "integrity token request failed", e)
                        }
                } catch (e: Exception) {
                    Log.w(TAG, "integrity pipeline unavailable", e)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "integrity manager unavailable (no Play Services?)", e)
        }
    }
}
