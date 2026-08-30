package app.getupcoming.core.appfunctions

import app.getupcoming.core.model.Attendee
import app.getupcoming.core.model.Booking
import app.getupcoming.core.model.EventType
import app.getupcoming.core.model.OfferedSlot
import app.getupcoming.core.network.SingleUseLinkDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppFunctionMappersTest {

    private val eventType = EventType(
        id = 15,
        slug = "intro-call",
        title = "Intro Call",
        description = "Get to know each other",
        lengthMinutes = 30,
        priceInCents = 0,
    )

    private val booking = Booking(
        id = 7,
        uid = "bk-123",
        eventTypeId = 15,
        hostUserId = 38,
        startTimeUtc = "2026-09-01T15:00:00Z",
        endTimeUtc = "2026-09-01T15:30:00Z",
        status = "accepted",
        idempotencyKey = "idem",
    )

    private val attendee = Attendee(
        id = 9,
        bookingId = 7,
        email = "invitee@example.com",
        name = "Sam Smith",
    )

    @Test
    fun `event type maps to summary`() {
        val s = eventType.toSummary()
        assertEquals(15L, s.id)
        assertEquals("intro-call", s.slug)
        assertEquals("Intro Call", s.title)
        assertEquals("Get to know each other", s.description)
        assertEquals(30, s.durationMinutes)
        assertEquals(0, s.priceCents)
    }

    @Test
    fun `booking maps with attendee and title`() {
        val s = booking.toSummary("Intro Call", attendee)
        assertEquals("bk-123", s.uid)
        assertEquals("Intro Call", s.eventTypeTitle)
        assertEquals("2026-09-01T15:00:00Z", s.startTimeUtc)
        assertEquals("accepted", s.status)
        assertEquals("Sam Smith", s.attendeeName)
        assertEquals("invitee@example.com", s.attendeeEmail)
    }

    @Test
    fun `booking without attendee yields nulls`() {
        val s = booking.toSummary(null, null)
        assertEquals("Unknown meeting type", s.eventTypeTitle)
        assertNull(s.attendeeName)
        assertNull(s.attendeeEmail)
    }

    @Test
    fun `offered slot maps to summary`() {
        val s = OfferedSlot(
            startUtc = "2026-09-02T10:00:00Z",
            endUtc = "2026-09-02T10:30:00Z",
            schedulingType = "individual",
        ).toSummary()
        assertEquals("2026-09-02T10:00:00Z", s.startUtc)
        assertEquals("2026-09-02T10:30:00Z", s.endUtc)
    }

    @Test
    fun `single use link maps to result`() {
        val dto = SingleUseLinkDto(
            id = 42,
            token = "tok",
            url = "https://getupcoming.app/derrick/intro-call?lid=tok",
            eventTypeId = 15,
            expiresAt = "2026-09-30T00:00:00Z",
        )
        val s = dto.toResult()
        assertEquals(42L, s.linkId)
        assertEquals("https://getupcoming.app/derrick/intro-call?lid=tok", s.url)
        assertEquals("2026-09-30T00:00:00Z", s.expiresAt)
    }
}
