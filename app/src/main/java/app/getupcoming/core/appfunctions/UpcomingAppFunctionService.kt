package app.getupcoming.core.appfunctions

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionAppUnknownException
import androidx.appfunctions.AppFunctionElementNotFoundException
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionPermissionRequiredException
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import app.getupcoming.AppContainer
import java.time.Instant
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Exposes Upcoming's scheduling capabilities as AppFunctions so agentic
 * assistants can query availability, read bookings, and mint single-use
 * booking links directly — no app UI required (Android 16+).
 *
 * KSP generates the concrete [UpcomingAppFunctionService] and the
 * `upcoming_app_function_service.xml` schema referenced by the manifest.
 */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "UpcomingAppFunctionService",
    appFunctionXmlFileName = "upcoming_app_function_service",
)
abstract class BaseUpcomingAppFunctionService : AppFunctionService() {

    private val container: AppContainer by lazy {
        (applicationContext as app.getupcoming.UpcomingApplication).container
    }

    private fun requireSignedIn() {
        val tokens = container.tokens
        if (!tokens.isLoggedIn() || tokens.isDemo()) {
            throw AppFunctionPermissionRequiredException(
                "The user is not signed in to Upcoming. Ask them to open the app and sign in first."
            )
        }
    }

    /**
     * Lists the user's active meeting types (event types) they can share.
     *
     * @return The list of bookable meeting types.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun listEventTypes(): List<EventTypeSummary> = withContext(Dispatchers.IO) {
        requireSignedIn()
        container.repository.allEventTypes.first()
            .filter { it.isActive }
            .map { it.toSummary() }
    }

    /**
     * Lists the user's upcoming bookings, soonest first.
     *
     * @param limit Maximum number of bookings to return; pass null for the default of 10.
     * @return The upcoming bookings with attendee info.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getUpcomingBookings(limit: Int? = null): List<BookingSummary> =
        withContext(Dispatchers.IO) {
            requireSignedIn()
            val effectiveLimit = limit ?: 10
            if (effectiveLimit <= 0) {
                throw AppFunctionInvalidArgumentException("limit must be positive")
            }
            val repo = container.repository
            repo.upcomingBookings.first()
                .take(effectiveLimit)
                .map { booking ->
                    val title = repo.getEventTypeById(booking.eventTypeId)?.title
                    val attendee = repo.getAttendeeForBooking(booking.id)
                    booking.toSummary(title, attendee)
                }
        }

    /**
     * Returns one booking by its uid, including attendee details.
     *
     * @param bookingUid The uid of the booking, e.g. from getUpcomingBookings.
     * @return The booking with attendee info.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getBooking(bookingUid: String): BookingSummary = withContext(Dispatchers.IO) {
        requireSignedIn()
        val repo = container.repository
        val booking = repo.getBookingByUid(bookingUid)
            ?: throw AppFunctionElementNotFoundException("No booking with uid $bookingUid")
        val title = repo.getEventTypeById(booking.eventTypeId)?.title
        val attendee = repo.getAttendeeForBooking(booking.id)
        booking.toSummary(title, attendee)
    }

    /**
     * Returns bookable time slots for a meeting type within a time range.
     *
     * @param eventTypeId Id of the meeting type, e.g. from listEventTypes.
     * @param rangeStartUtc Start of the search window, RFC 3339 UTC, e.g. 2026-09-01T00:00:00Z.
     * @param rangeEndUtc End of the search window, RFC 3339 UTC.
     * @param inviteeTimezone IANA timezone of the invitee, e.g. "America/New_York"; pass null for UTC.
     * @return The bookable slots inside the window.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun checkAvailability(
        eventTypeId: Long,
        rangeStartUtc: Instant,
        rangeEndUtc: Instant,
        inviteeTimezone: String? = null,
    ): List<SlotSummary> = withContext(Dispatchers.IO) {
        requireSignedIn()
        if (!rangeEndUtc.isAfter(rangeStartUtc)) {
            throw AppFunctionInvalidArgumentException("rangeEndUtc must be after rangeStartUtc")
        }
        val repo = container.repository
        val eventType = repo.getEventTypeById(eventTypeId)
            ?: throw AppFunctionElementNotFoundException("No event type with id $eventTypeId")
        repo.computeAvailableSlots(
            eventType = eventType,
            rangeStartUtc = Date.from(rangeStartUtc),
            rangeEndUtc = Date.from(rangeEndUtc),
            inviteeTimezone = inviteeTimezone ?: "UTC",
        ).map { it.toSummary() }
    }

    /**
     * Creates a single-use booking link for a meeting type. The link books
     * one meeting, then expires. Share the returned URL with the invitee.
     *
     * @param eventTypeId Id of the meeting type, e.g. from listEventTypes.
     * @param expiresInDays Days until the link expires, if desired.
     * @return The single-use booking link including its shareable URL.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun createSingleUseBookingLink(
        eventTypeId: Long,
        expiresInDays: Int? = null,
    ): SingleUseLinkResult = withContext(Dispatchers.IO) {
        requireSignedIn()
        if (expiresInDays != null && expiresInDays <= 0) {
            throw AppFunctionInvalidArgumentException("expiresInDays must be positive")
        }
        val links = container.repository.createSingleUseLinks(
            eventTypeId = eventTypeId,
            count = 1,
            expiresInDays = expiresInDays,
        )
        val link = links?.firstOrNull()
            ?: throw AppFunctionAppUnknownException(
                "Could not create a single-use link for event type $eventTypeId. " +
                    "Check that the id exists and the device is online."
            )
        link.toResult()
    }

    /**
     * Returns the user's personal booking page URL, where invitees can see
     * all meeting types and book any of them.
     *
     * @return The personal booking page link.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun getPersonalShareLink(): PersonalShareLink = withContext(Dispatchers.IO) {
        requireSignedIn()
        val user = container.repository.getPrimaryUser()
        PersonalShareLink(
            url = "https://getupcoming.app/${user.username}",
            hostName = user.displayName.ifBlank { user.username },
        )
    }
}
