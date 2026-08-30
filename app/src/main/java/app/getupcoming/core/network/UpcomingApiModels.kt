package app.getupcoming.core.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ---------------------------------------------------------------------------
// Wire models for the upcoming-db HTTP layer (see docs/api-contract.md).
// Field names match the JSON exactly; Moshi (reflection) does the rest.
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = false)
data class LocationDto(
    val type: String,
    val label: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val displayPhone: String? = null,
    val url: String? = null,
    val dailyRoomName: String? = null
)

@JsonClass(generateAdapter = false)
data class AttendeeWireDto(
    val email: String,
    val name: String? = null,
    val timezone: String? = null,
    val phone: String? = null
)

@JsonClass(generateAdapter = false)
data class CreateBookingRequest(
    val eventTypeId: Long,
    @Json(name = "slotStartUtc") val slotStartUtc: String,
    @Json(name = "slotEndUtc") val slotEndUtc: String,
    val location: LocationDto,
    val attendee: AttendeeWireDto,
    val idempotencyKey: String
)

@JsonClass(generateAdapter = false)
data class BookingResultDto(
    val uid: String,
    val eventTypeId: Long,
    val hostUserId: Long,
    @Json(name = "attendingHostUserIds") val attendingHostUserIds: List<Long> = emptyList(),
    val startUtc: String,
    val endUtc: String,
    val status: String,
    val replay: Boolean = false,
    val location: LocationDto? = null,
    val attendee: AttendeeWireDto? = null
)

@JsonClass(generateAdapter = false)
data class CancelBookingRequest(
    val uid: String? = null,
    val idempotencyKey: String? = null
)

@JsonClass(generateAdapter = false)
data class EventTypeDto(
    val id: Long,
    val ownerUserId: Long,
    val slug: String,
    val lengthMinutes: Int,
    val slotIntervalMinutes: Int? = null,
    val bufferBefore: Int = 0,
    val bufferAfter: Int = 0,
    val schedulingType: String = "individual",
    val locations: String = "[]",
    val minBookingNotice: Int = 0,
    val title: String = "",
    val description: String = "",
    val priceInCents: Int = 0,
    val currency: String = "usd",
    val colorHex: String = "#CC785C",
    val isActive: Boolean = true,
    val hostUserIds: List<Long> = emptyList()
)

@JsonClass(generateAdapter = false)
data class OfferedSlotDto(
    val startUtc: String,
    val endUtc: String,
    val schedulingType: String,
    val attendingHostUserIds: List<Long>? = null
)

@JsonClass(generateAdapter = false)
data class AvailabilityResponseDto(
    val eventTypeId: Long,
    val slots: List<OfferedSlotDto>
)

// Raw booking row from GET /bookings (drizzle column names, camelCase).
@JsonClass(generateAdapter = false)
data class BookingRowDto(
    val id: Long = 0,
    val uid: String,
    val eventTypeId: Long,
    val hostUserId: Long,
    @Json(name = "startTime") val startTimeUtc: String,
    @Json(name = "endTime") val endTimeUtc: String,
    val bufferBefore: Int = 0,
    val bufferAfter: Int = 0,
    val status: String = "accepted",
    val cancelledAt: String? = null,
    val idempotencyKey: String = "",
    val location: String? = null,
    val paid: Boolean = false,
    val paymentIntentId: String? = null,
    val createdAt: String? = null
)

@JsonClass(generateAdapter = false)
data class AttendeeDetailDto(
    val id: Long = 0,
    val bookingId: Long = 0,
    val email: String,
    val name: String? = null,
    val timezone: String? = null,
    val phone: String? = null,
    val notes: String? = null
)

@JsonClass(generateAdapter = false)
data class BookingDetailDto(
    val id: Long = 0,
    val uid: String,
    val eventTypeId: Long,
    val hostUserId: Long,
    @Json(name = "startTime") val startTimeUtc: String,
    @Json(name = "endTime") val endTimeUtc: String,
    val bufferBefore: Int = 0,
    val bufferAfter: Int = 0,
    val status: String = "accepted",
    val cancelledAt: String? = null,
    val idempotencyKey: String = "",
    val location: String? = null,
    val paid: Boolean = false,
    val paymentIntentId: String? = null,
    val createdAt: String? = null,
    val eventType: EventTypeDto? = null,
    val attendee: AttendeeDetailDto? = null,
    val hostUserIds: List<Long> = emptyList()
)

// ---------------------------------------------------------------------------
// Payments
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = false)
data class CreateIntentRequest(val eventTypeId: Long)

@JsonClass(generateAdapter = false)
data class CreateIntentResponse(
    val paymentIntentId: String,
    val clientSecret: String,
    val amount: Int,
    val currency: String
)

@JsonClass(generateAdapter = false)
data class MarkPaidRequest(
    val uid: String,
    val paymentIntentId: String
)

@JsonClass(generateAdapter = false)
data class MarkPaidResponse(
    val uid: String,
    val paid: Boolean,
    val paymentIntentId: String
)

@JsonClass(generateAdapter = false)
data class ApiErrorDto(val error: String? = null)

// ---------------------------------------------------------------------------
// User settings (/me)
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = false)
data class UserMetadataDto(
    val defaultLocation: LocationDto? = null,
    val locations: LocationsMapDto? = null,
    val defaultLocationType: String? = null,
    val prefs: UserPrefsDto? = null,
    val role: String? = null,
    val company: String? = null
)

// One configured location per type (each with its own label + value) for
// booking defaults; keys use the wire name "integrations:daily".
@JsonClass(generateAdapter = false)
data class LocationsMapDto(
    @Json(name = "integrations:daily") val daily: LocationDto? = null,
    val inPerson: LocationDto? = null,
    val userPhone: LocationDto? = null
) {
    fun entryFor(type: String): LocationDto? = when (type) {
        "integrations:daily" -> daily
        "inPerson" -> inPerson
        "userPhone" -> userPhone
        else -> null
    }
}

@JsonClass(generateAdapter = false)
data class UserPrefsDto(
    val timeFormat: String? = null,
    // Pre-meeting reminder lead times in minutes (server normalizes to
    // sorted-ascending, max 5).
    val reminderOffsets: List<Int>? = null
)

@JsonClass(generateAdapter = false)
data class ScheduleDto(
    val id: Long = 0,
    val name: String = "",
    val timezone: String = ""
)

@JsonClass(generateAdapter = false)
data class MeResponseDto(
    val id: Long,
    val email: String,
    val username: String,
    val timezone: String,
    val displayName: String = "",
    val avatarUrl: String = "",
    val metadata: UserMetadataDto = UserMetadataDto(),
    val schedule: ScheduleDto? = null
)

@JsonClass(generateAdapter = false)
data class PatchMeRequest(
    val displayName: String? = null,
    val avatarUrl: String? = null,
    val email: String? = null,
    val username: String? = null,
    val timezone: String? = null,
    val metadata: UserMetadataDto? = null
)

// ---------------------------------------------------------------------------
// Auth (/auth/*) — JWT pair + the user it authenticates.
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = false)
data class SignUpRequest(
    val email: String,
    val password: String,
    val username: String,
    val displayName: String? = null,
    val timezone: String? = null
)

@JsonClass(generateAdapter = false)
data class LoginRequest(val email: String, val password: String)

@JsonClass(generateAdapter = false)
data class RefreshRequest(val refreshToken: String)

@JsonClass(generateAdapter = false)
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: MeResponseDto
)

@JsonClass(generateAdapter = false)
data class PatchScheduleRequest(
    val name: String? = null,
    val timezone: String? = null
)

// ---------------------------------------------------------------------------
// User credentials (/me/credentials) — masked hints only; plaintext values
// are write-only from the client's perspective.
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = false)
data class CredentialHintDto(
    val type: String,
    val hint: String
)

@JsonClass(generateAdapter = false)
data class PutCredentialRequest(val value: String)

@JsonClass(generateAdapter = false)
data class DeleteCredentialResponse(val deleted: String? = null)

// ---------------------------------------------------------------------------
// Single-use booking links (/single-use-links) — Calendly-style one-time
// links burned when a booking lands carrying their token.
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = false)
data class SingleUseLinkDto(
    val id: Long,
    val token: String,
    val url: String,
    val eventTypeId: Long,
    val createdAt: String? = null,
    val expiresAt: String? = null,
    val usedAt: String? = null,
    val revokedAt: String? = null,
    val status: String = "unused"
)

@JsonClass(generateAdapter = false)
data class CreateSingleUseLinksRequest(
    val eventTypeId: Long,
    val count: Int = 1,
    val expiresInDays: Int? = null
)

@JsonClass(generateAdapter = false)
data class RevokeSingleUseLinkResponse(
    val id: Long,
    val status: String? = null,
    val url: String? = null
)

// ---------------------------------------------------------------------------
// Event-type mutations (/event-types) — create/update are full-body writes
// (the editor always sends every field); nulls are omitted from the JSON so
// PATCH bodies stay partial. `locations` is the JSON-encoded menu string the
// app caches; the server accepts a real array or the encoded string.
// ---------------------------------------------------------------------------

@JsonClass(generateAdapter = false)
data class CreateEventTypeRequest(
    val slug: String,
    val title: String,
    val description: String? = null,
    val lengthMinutes: Int,
    val slotIntervalMinutes: Int? = null,
    val bufferBefore: Int? = null,
    val bufferAfter: Int? = null,
    val schedulingType: String? = null,
    val locations: String? = null,
    val minBookingNotice: Int? = null,
    val priceInCents: Int? = null,
    val currency: String? = null,
    val colorHex: String? = null,
    val isActive: Boolean? = null
)

@JsonClass(generateAdapter = false)
data class UpdateEventTypeRequest(
    val slug: String? = null,
    val title: String? = null,
    val description: String? = null,
    val lengthMinutes: Int? = null,
    val slotIntervalMinutes: Int? = null,
    val bufferBefore: Int? = null,
    val bufferAfter: Int? = null,
    val schedulingType: String? = null,
    val locations: String? = null,
    val minBookingNotice: Int? = null,
    val priceInCents: Int? = null,
    val currency: String? = null,
    val colorHex: String? = null,
    val isActive: Boolean? = null
)

@JsonClass(generateAdapter = false)
data class DeleteEventTypeResponse(val ok: Boolean = false)
