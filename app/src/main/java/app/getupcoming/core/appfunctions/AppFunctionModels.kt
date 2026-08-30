package app.getupcoming.core.appfunctions

import androidx.appfunctions.AppFunctionSerializable
import app.getupcoming.core.model.Attendee
import app.getupcoming.core.model.Booking
import app.getupcoming.core.model.EventType
import app.getupcoming.core.model.OfferedSlot
import app.getupcoming.core.network.SingleUseLinkDto

/** A bookable meeting type (event type) owned by the signed-in user. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class EventTypeSummary(
    /** Unique numeric id of the event type. */
    val id: Long,
    /** URL slug, e.g. "intro-call". */
    val slug: String,
    /** Display title, e.g. "Intro Call". */
    val title: String,
    /** Short description of what the meeting covers. */
    val description: String,
    /** Meeting length in minutes. */
    val durationMinutes: Int,
    /** Price in minor units (cents); 0 means free. */
    val priceCents: Int,
)

/** A scheduled booking on the user's calendar. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class BookingSummary(
    /** Unique id of the booking (uid). */
    val uid: String,
    /** Title of the event type the booking is for. */
    val eventTypeTitle: String,
    /** Booking start time, ISO-8601 UTC, e.g. 2026-09-01T15:00:00Z. */
    val startTimeUtc: String,
    /** Booking end time, ISO-8601 UTC. */
    val endTimeUtc: String,
    /** Status: accepted, pending, cancelled or rejected. */
    val status: String,
    /** Name of the attendee who booked, if known. */
    val attendeeName: String?,
    /** Email of the attendee who booked, if known. */
    val attendeeEmail: String?,
)

/** One bookable time slot. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SlotSummary(
    /** Slot start time, ISO-8601 UTC. */
    val startUtc: String,
    /** Slot end time, ISO-8601 UTC. */
    val endUtc: String,
)

/** A freshly created single-use booking link. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SingleUseLinkResult(
    /** The full shareable URL to give the invitee, including the single-use link id. */
    val url: String,
    /** Numeric id of the link, needed to revoke it later. */
    val linkId: Long,
    /** When the link expires, ISO-8601 UTC, if an expiry was set. */
    val expiresAt: String?,
)

/** The user's personal booking page. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class PersonalShareLink(
    /** The full shareable URL of the personal booking page. */
    val url: String,
    /** The user's display name. */
    val hostName: String,
)
