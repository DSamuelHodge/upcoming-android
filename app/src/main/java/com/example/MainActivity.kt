package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.core.auth.AuthRepository
import com.example.core.auth.AuthTokenManager
import com.example.core.database.UpcomingDatabase
import com.example.core.engine.NotificationAndReminderManager
import com.example.core.network.UpcomingApiClient
import com.example.core.repository.UpcomingRepository
import com.example.core.widget.EXTRA_WIDGET_BOOKING_UID
import com.example.navigation.UpcomingNavHost
import com.example.ui.theme.UpcomingTheme

class MainActivity : ComponentActivity() {

    private lateinit var repository: UpcomingRepository
    private lateinit var authRepository: AuthRepository

    /** Widget tap → Booking Detail deep link (see core/widget). */
    private val pendingBookingUid = androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingBookingUid.value = intent?.getStringExtra(EXTRA_WIDGET_BOOKING_UID)

        // Keep the system splash up while the auth gate resolves (token
        // restore is local + fast; the first network sync happens after).
        var keepSplash = true
        splash.setKeepOnScreenCondition { keepSplash }

        val db = UpcomingDatabase.getInstance(applicationContext)
        val tokens = AuthTokenManager(applicationContext)
        val api = UpcomingApiClient.create(auth = tokens)
        repository = UpcomingRepository(
            database = db,
            context = applicationContext,
            api = api,
            authTokens = tokens
        )
        authRepository = AuthRepository(
            api = api,
            tokens = tokens,
            // A real session must never inherit the demo persona's data.
            onSessionEstablished = { repository.onSessionEstablished() }
        )

        // One frame is enough for the gate to pick its start destination.
        androidx.core.os.HandlerCompat.createAsync(android.os.Looper.getMainLooper()).postDelayed(
            { keepSplash = false },
            300
        )

        NotificationAndReminderManager.setupChannels(applicationContext)

        setContent {
            UpcomingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UpcomingNavHost(
                        repository = repository,
                        context = applicationContext,
                        authRepository = authRepository,
                        pendingBookingUid = pendingBookingUid.value,
                        onConsumePendingBooking = { pendingBookingUid.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // singleTask launch: a widget tap while the app is open lands here.
        setIntent(intent)
        pendingBookingUid.value = intent.getStringExtra(EXTRA_WIDGET_BOOKING_UID)
    }
}
