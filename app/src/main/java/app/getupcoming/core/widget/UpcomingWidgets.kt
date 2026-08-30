package app.getupcoming.core.widget

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.layout.size
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import app.getupcoming.MainActivity

// Palette mirrors the in-app design system (CanvasCream / PrimaryCoral / Ink).
private val WidgetCanvas = ColorProvider(Color(0xFFFAF9F5))
private val WidgetCoral = ColorProvider(Color(0xFFCC785C))
private val WidgetInk = ColorProvider(Color(0xFF1A1A1A))
private val WidgetMuted = ColorProvider(Color(0xFF6F6A63))
private val WidgetWhite = ColorProvider(Color(0xFFFFFFFF))

/** Opens the Booking Detail screen for the tapped booking (falls back to
 *  plain app launch when no uid is attached). */
class OpenBookingAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        parameters[bookingUidKey]?.let { intent.putExtra(EXTRA_WIDGET_BOOKING_UID, it) }
        context.startActivity(intent)
    }

    companion object {
        val bookingUidKey = ActionParameters.Key<String>("bookingUid")
    }
}

/** Tap-anywhere fallback: opens the app. */
class OpenAppAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        context.startActivity(
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private fun openAppAction() = actionRunCallback<OpenAppAction>()

/** Whole-row tap: open the Booking Detail screen for [uid]. */
private fun openBookingAction(uid: String?) =
    if (uid.isNullOrBlank()) {
        actionRunCallback<OpenBookingAction>(actionParametersOf())
    } else {
        actionRunCallback<OpenBookingAction>(
            actionParametersOf(OpenBookingAction.bookingUidKey to uid)
        )
    }

/** 2x2 "Upcoming Widget" — the home-screen twin of the dashboard banner. */
class UpcomingWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (next, list) = WidgetSnapshotStore.load(context)
        val upcomingCount = (list.ifEmpty { listOfNotNull(next) }).size
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(WidgetCanvas)
                        .cornerRadius(16.dp)
                        .clickable(openAppAction())
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = GlanceModifier
                                .size(30.dp)
                                .background(WidgetInk)
                                .cornerRadius(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("U", style = TextStyle(color = WidgetWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp))
                        }
                        Spacer(GlanceModifier.width(8.dp))
                        Column {
                            Text(
                                "UPCOMING WIDGET",
                                style = TextStyle(color = WidgetInk, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                            )
                            Text(
                                if (upcomingCount > 0) "$upcomingCount meetings scheduled"
                                else "No meetings scheduled",
                                style = TextStyle(color = WidgetInk, fontSize = 11.sp),
                                maxLines = 1
                            )
                        }
                    }

                    next?.let { booking ->
                        Spacer(GlanceModifier.height(8.dp))
                        Column(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(WidgetWhite)
                                .cornerRadius(10.dp)
                                .clickable(openBookingAction(booking.uid))
                                .padding(10.dp)
                        ) {
                            Text(
                                booking.eventTitle,
                                style = TextStyle(color = WidgetCoral, fontSize = 11.sp, fontWeight = FontWeight.Medium),
                                maxLines = 1
                            )
                            Text(
                                booking.attendeeName,
                                style = TextStyle(color = WidgetInk, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                maxLines = 1
                            )
                            Text(
                                booking.timeLabel,
                                style = TextStyle(color = WidgetMuted, fontSize = 10.sp),
                                maxLines = 1
                            )
                        }
                    } ?: Spacer(GlanceModifier.height(8.dp))
                }
            }
        }
    }
}

class UpcomingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpcomingWidget()
}

/** 4x2 widget: the next three bookings. */
class UpcomingListWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val (_, list) = WidgetSnapshotStore.load(context)
        provideContent {
            GlanceTheme {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .background(WidgetCanvas)
                        .cornerRadius(16.dp)
                        .clickable(openAppAction())
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = GlanceModifier
                                .size(22.dp)
                                .background(WidgetInk)
                                .cornerRadius(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("U", style = TextStyle(color = WidgetWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp))
                        }
                        Spacer(GlanceModifier.width(8.dp))
                        Text(
                            "UPCOMING",
                            style = TextStyle(color = WidgetInk, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        )
                    }
                    Spacer(GlanceModifier.height(8.dp))
                    if (list.isEmpty()) {
                        Text(
                            "No meetings scheduled",
                            style = TextStyle(color = WidgetMuted, fontSize = 12.sp)
                        )
                    } else {
                        list.take(3).forEach { booking ->
                            Row(
                                modifier = GlanceModifier
                                    .fillMaxWidth()
                                    .background(WidgetWhite)
                                    .cornerRadius(8.dp)
                                    .clickable(openBookingAction(booking.uid))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = GlanceModifier.defaultWeight()) {
                                    Text(
                                        booking.eventTitle,
                                        style = TextStyle(color = WidgetCoral, fontSize = 10.sp, fontWeight = FontWeight.Medium),
                                        maxLines = 1
                                    )
                                    Text(
                                        "${booking.attendeeName} • ${booking.timeLabel}",
                                        style = TextStyle(color = WidgetInk, fontSize = 11.sp),
                                        maxLines = 1
                                    )
                                }
                            }
                            Spacer(GlanceModifier.height(6.dp))
                        }
                    }
                }
            }
        }
    }
}

class UpcomingListWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = UpcomingListWidget()
}
