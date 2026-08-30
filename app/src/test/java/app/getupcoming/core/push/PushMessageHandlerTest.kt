package app.getupcoming.core.push

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.messaging.RemoteMessage
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PushMessageHandlerTest {

    private val context: Context =
        ApplicationProvider.getApplicationContext()

    private fun message(
        action: String,
        title: String? = null,
        body: String? = null,
        bookingUid: String = "bk-1"
    ): RemoteMessage {
        val builder = RemoteMessage.Builder("sender")
            .addData("action", action)
            .addData("bookingUid", bookingUid)
        if (title != null) builder.addData("title", title)
        if (body != null) builder.addData("body", body)
        return builder.build()
    }

    private fun notificationCount(): Int {
        val nm = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as android.app.NotificationManager
        return nm.activeNotifications.size
    }

    @Test
    fun `foreground reminder stays silent - local alarm owns it`() {
        PushMessageHandler.appInForeground = true
        PushMessageHandler.handle(
            context,
            message("booking.reminder", "Reminder", "Intro in 10m")
        )
        assertEquals(0, notificationCount())
        PushMessageHandler.appInForeground = false
    }

    @Test
    fun `background reminder surfaces via push channel`() {
        PushMessageHandler.appInForeground = false
        PushMessageHandler.handle(
            context,
            message("booking.reminder", "Reminder", "Intro in 10m")
        )
        assertEquals(1, notificationCount())
    }

    @Test
    fun `lifecycle push always surfaces even in foreground`() {
        PushMessageHandler.appInForeground = true
        PushMessageHandler.handle(
            context,
            message("booking.cancelled", "Booking cancelled", "Intro was cancelled")
        )
        assertEquals(1, notificationCount())
        PushMessageHandler.appInForeground = false
    }

    @Test
    fun `lifecycle push without notification payload gets default title`() {
        PushMessageHandler.appInForeground = false
        PushMessageHandler.handle(context, message("booking.created"))
        assertEquals(1, notificationCount())
    }

    @Test
    fun `unknown action without payload is dropped`() {
        PushMessageHandler.appInForeground = false
        PushMessageHandler.handle(context, message("something.else"))
        assertEquals(0, notificationCount())
    }
}
