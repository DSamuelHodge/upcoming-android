package app.getupcoming.core.widget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** One row of the home-screen widgets: the next booking(s). */
data class WidgetBooking(
    val uid: String?,
    val attendeeName: String,
    val eventTitle: String,
    val timeLabel: String,
    val joinUrl: String?
)

private const val PREFS = "upcoming_widget_state"
private const val KEY_NEXT = "next"
private const val KEY_LIST = "list"

/** Widget → app deep link extra: tapping a widget row opens the Booking
 *  Detail screen for this booking uid. */
const val EXTRA_WIDGET_BOOKING_UID = "app.getupcoming.widget.BOOKING_UID"

/** Tiny persistence bridge: the app writes a snapshot on every dashboard
 *  refresh; the Glance widgets read it in provideGlance. */
object WidgetSnapshotStore {

    fun save(context: Context, next: WidgetBooking?, list: List<WidgetBooking>) {
        val json = JSONObject()
        json.put(KEY_NEXT, next?.let { toJson(it) } ?: JSONObject.NULL)
        val arr = JSONArray()
        list.take(3).forEach { arr.put(toJson(it)) }
        json.put(KEY_LIST, arr)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_NEXT, json.toString()).apply()
    }

    fun load(context: Context): Pair<WidgetBooking?, List<WidgetBooking>> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_NEXT, null)
            ?: return null to emptyList()
        return try {
            val json = JSONObject(raw)
            val next = if (json.isNull(KEY_NEXT)) null else fromJson(json.getJSONObject(KEY_NEXT))
            val arr = json.getJSONArray(KEY_LIST)
            val list = buildList(arr.length()) {
                for (i in 0 until arr.length()) add(fromJson(arr.getJSONObject(i)))
            }
            next to list
        } catch (_: Exception) {
            null to emptyList()
        }
    }

    private fun toJson(b: WidgetBooking): JSONObject = JSONObject().apply {
        put("uid", b.uid ?: JSONObject.NULL)
        put("attendeeName", b.attendeeName)
        put("eventTitle", b.eventTitle)
        put("timeLabel", b.timeLabel)
        put("joinUrl", b.joinUrl ?: JSONObject.NULL)
    }

    private fun fromJson(o: JSONObject): WidgetBooking = WidgetBooking(
        uid = if (o.isNull("uid")) null else o.optString("uid"),
        attendeeName = o.optString("attendeeName"),
        eventTitle = o.optString("eventTitle"),
        timeLabel = o.optString("timeLabel"),
        joinUrl = if (o.isNull("joinUrl")) null else o.optString("joinUrl")
    )
}
