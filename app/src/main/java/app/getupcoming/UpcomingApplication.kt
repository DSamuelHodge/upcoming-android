package app.getupcoming

import android.app.Application
import android.content.Context
import app.getupcoming.core.auth.AuthRepository
import app.getupcoming.core.auth.AuthTokenManager
import app.getupcoming.core.database.UpcomingDatabase
import app.getupcoming.core.network.UpcomingApi
import app.getupcoming.core.network.UpcomingApiClient
import app.getupcoming.core.repository.UpcomingRepository

/**
 * Process-wide singleton graph. MainActivity and the AppFunctions service both
 * resolve dependencies from here so every component shares one
 * [AuthTokenManager] (and therefore one session) plus one repository/api.
 */
class AppContainer(context: Context) {

    val tokens: AuthTokenManager = AuthTokenManager(context)

    val api: UpcomingApi = UpcomingApiClient.create(auth = tokens)

    val repository: UpcomingRepository = UpcomingRepository(
        database = UpcomingDatabase.getInstance(context),
        context = context,
        api = api,
        authTokens = tokens
    )

    val authRepository: AuthRepository = AuthRepository(
        api = api,
        tokens = tokens,
        // A real session must never inherit the demo persona's data.
        context = context,
        onSessionEstablished = { repository.onSessionEstablished() }
    )
}

class UpcomingApplication : Application() {

    // Lazy so a Robolectric/JVM environment never initializes the Keystore-backed
    // AuthTokenManager just by instantiating the application.
    val container: AppContainer by lazy { AppContainer(this) }
}
