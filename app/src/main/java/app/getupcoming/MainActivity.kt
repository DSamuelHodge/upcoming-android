package app.getupcoming

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import app.getupcoming.core.auth.AuthRepository
import app.getupcoming.core.engine.NotificationAndReminderManager
import app.getupcoming.core.repository.UpcomingRepository
import app.getupcoming.core.widget.EXTRA_WIDGET_BOOKING_UID
import app.getupcoming.navigation.UpcomingNavHost
import app.getupcoming.ui.theme.UpcomingTheme

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

        val appContainer = (application as UpcomingApplication).container
        repository = appContainer.repository
        authRepository = appContainer.authRepository

        // One frame is enough for the gate to pick its start destination.
        androidx.core.os.HandlerCompat.createAsync(android.os.Looper.getMainLooper()).postDelayed(
            { keepSplash = false },
            300
        )

        NotificationAndReminderManager.setupChannels(applicationContext)

        // FCM token registration (soft-fail; no-op signed out / demo).
        app.getupcoming.core.push.PushRegistrar.registerAsync(applicationContext)

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

    override fun onResume() {
        super.onResume()
        app.getupcoming.core.push.PushMessageHandler.appInForeground = true
    }

    override fun onPause() {
        super.onPause()
        app.getupcoming.core.push.PushMessageHandler.appInForeground = false
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        // singleTask launch: a widget tap while the app is open lands here.
        setIntent(intent)
        pendingBookingUid.value = intent.getStringExtra(EXTRA_WIDGET_BOOKING_UID)
    }
}
