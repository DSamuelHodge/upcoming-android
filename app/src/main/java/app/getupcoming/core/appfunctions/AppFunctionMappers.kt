package app.getupcoming.core.appfunctions

import app.getupcoming.core.model.Attendee
import app.getupcoming.core.model.Booking
import app.getupcoming.core.model.EventType
import app.getupcoming.core.model.OfferedSlot
import app.getupcoming.core.network.SingleUseLinkDto

internal fun EventType.toSummary() = EventTypeSummary(
    id = id,
    slug = slug,
    title = title,
    description = description,
    durationMinutes = lengthMinutes,
    priceCents = priceInCents,
)

internal fun Booking.toSummary(eventTypeTitle: String?, attendee: Attendee?) = BookingSummary(
    uid = uid,
    eventTypeTitle = eventTypeTitle ?: "Unknown meeting type",
    startTimeUtc = startTimeUtc,
    endTimeUtc = endTimeUtc,
    status = status,
    attendeeName = attendee?.name,
    attendeeEmail = attendee?.email,
)

internal fun OfferedSlot.toSummary() = SlotSummary(startUtc = startUtc, endUtc = endUtc)

internal fun SingleUseLinkDto.toResult() =
    SingleUseLinkResult(url = url, linkId = id, expiresAt = expiresAt)
