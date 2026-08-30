package com.example.core.network

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
