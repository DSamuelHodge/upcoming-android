package app.getupcoming.core.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Entry point for FCM (manifest intent filter below).
 * Token refresh → PATCH /me metadata.fcmToken (soft-fail); foreground
 * message mapping lives in [PushMessageHandler].
 */
class UpcomingFcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        PushRegistrar.registerTokenAsync(applicationContext, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        PushMessageHandler.handle(applicationContext, message)
    }
}
