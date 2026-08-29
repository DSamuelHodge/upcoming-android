package com.example.core.engine

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.core.model.Booking
import com.example.core.model.EventType
import java.util.Date

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Upcoming Meeting"
        val body = intent.getStringExtra("body") ?: "Your scheduled event is starting soon!"
        val bookingUid = intent.getStringExtra("bookingUid") ?: ""

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "upcoming_reminders_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Upcoming Meeting Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Exact alarm reminders for upcoming bookings"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("target_booking_uid", bookingUid)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            bookingUid.hashCode(),
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}

object NotificationAndReminderManager {

    private const val CHANNEL_PUSH_ID = "upcoming_push_channel"
    private const val CHANNEL_ALARM_ID = "upcoming_reminders_channel"

    fun setupChannels(context: Context, soundVibrationEnabled: Boolean = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val pushChannel = NotificationChannel(
                CHANNEL_PUSH_ID,
                "Upcoming Booking Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Booking confirmations and cancellations"
                if (soundVibrationEnabled) enableVibration(true) else enableVibration(false)
                if (!soundVibrationEnabled) setSound(null, null)
            }

            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM_ID,
                "Meeting Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders before meetings start"
                if (soundVibrationEnabled) enableVibration(true) else enableVibration(false)
                if (!soundVibrationEnabled) setSound(null, null)
            }

            notificationManager.createNotificationChannel(pushChannel)
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    fun triggerFcmNotification(
        context: Context,
        title: String,
        body: String,
        bookingUid: String = ""
    ) {
        setupChannels(context)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("target_booking_uid", bookingUid)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            bookingUid.hashCode() + 1,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_PUSH_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }

    /** Schedules one exact alarm per reminder offset (minutes before start). */
    fun scheduleBookingReminders(
        context: Context,
        booking: Booking,
        eventType: EventType,
        attendeeName: String?,
        offsetsMinutes: List<Int>
    ) {
        for (offset in offsetsMinutes) {
            scheduleExactAlarm(context, booking, eventType, attendeeName, offset)
        }
    }

    /** Cancels every alarm armed for this booking. Pass a superset of the
     *  offsets that may have been armed (current settings + presets). */
    fun cancelBookingReminders(context: Context, bookingUid: String, offsetsMinutes: List<Int>) {
        for (offset in offsetsMinutes) {
            cancelAlarm(context, bookingUid, offset)
        }
    }

    fun scheduleExactAlarm(
        context: Context,
        booking: Booking,
        eventType: EventType,
        attendeeName: String?,
        reminderMinutesBefore: Int
    ) {
        setupChannels(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val bookingStartTimeMs = SchedulingEngine.parseIsoUtc(booking.startTimeUtc).time
        val triggerTimeMs = bookingStartTimeMs - (reminderMinutesBefore * 60 * 1000L)

        // Only schedule if in future
        if (triggerTimeMs <= System.currentTimeMillis()) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("title", "Upcoming: ${eventType.title}")
            val nameText = if (!attendeeName.isNullOrBlank()) "with $attendeeName " else ""
            putExtra("body", "Your meeting ${nameText}starts in ${formatOffset(reminderMinutesBefore)}.")
            putExtra("bookingUid", booking.uid)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmRequestCode(booking.uid, reminderMinutesBefore),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        }
    }

    fun cancelAlarm(context: Context, bookingUid: String, reminderMinutesBefore: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmRequestCode(bookingUid, reminderMinutesBefore),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /** Stable per-(booking, offset) request code so each reminder offset owns
     *  its own alarm slot and can be replaced or cancelled independently. */
    private fun alarmRequestCode(bookingUid: String, minutesBefore: Int): Int =
        bookingUid.hashCode() * 31 + minutesBefore

    /** "10 minutes" / "2 hours" / "1 day" — used in alarm bodies. */
    fun formatOffset(minutesBefore: Int): String = when {
        minutesBefore >= 1440 && minutesBefore % 1440 == 0 -> {
            val d = minutesBefore / 1440
            if (d == 1) "1 day" else "$d days"
        }
        minutesBefore >= 60 && minutesBefore % 60 == 0 -> {
            val h = minutesBefore / 60
            if (h == 1) "1 hour" else "$h hours"
        }
        minutesBefore == 1 -> "1 minute"
        else -> "$minutesBefore minutes"
    }
}
