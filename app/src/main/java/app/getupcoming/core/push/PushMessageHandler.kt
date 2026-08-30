package app.getupcoming.core.push

import android.content.Context
import app.getupcoming.core.engine.NotificationAndReminderManager
import com.google.firebase.messaging.RemoteMessage

/**
 * Maps a backend push (api-contract §4.4) onto local notification behavior.
 * Foreground + `booking.reminder` stays silent — the local exact alarm owns
 * visible reminder timing and the server sweep is only a backstop for missed
 * alarms; double-firing while the user is looking at the screen is noise.
 * Lifecycle actions (created/cancelled/paid) always surface.
 */
object PushMessageHandler {

    const val ACTION_REMINDER = "booking.reminder"

    /** True while MainActivity is resumed. Set from the activity lifecycle. */
    @Volatile
    var appInForeground: Boolean = false

    fun handle(context: Context, message: RemoteMessage) {
        val action = message.data["action"] ?: return
        val bookingUid = message.data["bookingUid"] ?: ""

        if (action == ACTION_REMINDER && appInForeground) return

        val notification = message.notification
        val title = notification?.title ?: message.data["title"] ?: defaultTitle(action) ?: return
        val body = notification?.body ?: message.data["body"] ?: ""

        NotificationAndReminderManager.triggerFcmNotification(
            context, title, body, bookingUid
        )
    }

    private fun defaultTitle(action: String): String? = when (action) {
        "booking.created" -> "New booking"
        "booking.cancelled" -> "Booking cancelled"
        "booking.paid" -> "Payment received"
        else -> null
    }
}
