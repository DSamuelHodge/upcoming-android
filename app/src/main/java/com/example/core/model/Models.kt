package com.example.core.model

data class User(
    val id: Long = 0,
    val email: String,
    val username: String,
    val displayName: String = "",
    val timezone: String = "America/New_York",
    val avatarUrl: String = "",
    val metadata: String = "{}"
)

data class Schedule(
    val id: Long = 0,
    val userId: Long,
    val name: String = "Working Hours",
    val timezone: String = "America/New_York"
)

data class AvailabilityRule(
    val id: Long = 0,
    val scheduleId: Long,
    val dayOfWeek: Int? = null, // 0 = Sun, 1 = Mon, ..., 6 = Sat
    val dateOverride: String? = null, // "YYYY-MM-DD"
    val startTime: String, // "09:00"
    val endTime: String // "17:00"
)

data class LocationOption(
    val type: String, // "integrations:daily" | "inPerson" | "userPhone" | "googleMeet"
    val label: String = "",
    val address: String? = null,
    val phone: String? = null,
    val displayPhone: String? = null,
    val url: String? = null, // Daily room url
    val dailyRoomName: String? = null
)

data class EventType(
    val id: Long = 0,
    val ownerUserId: Long = 1,
    val slug: String,
    val title: String,
    val description: String = "",
    val lengthMinutes: Int = 30,
    val slotIntervalMinutes: Int? = null,
    val bufferBefore: Int = 0,
    val bufferAfter: Int = 0,
    val schedulingType: String = "individual", // "individual" | "round_robin" | "collective"
    val locationsJson: String = "[]",
    val minBookingNotice: Int = 60, // minutes
    val priceInCents: Int = 0, // 0 = free, 5000 = $50.00
    val currency: String = "usd",
    val colorHex: String = "#0B5CFF",
    val isActive: Boolean = true
)

data class EventTypeHost(
    val id: Long = 0,
    val eventTypeId: Long,
    val hostUserId: Long,
    val priority: Int = 0
)

data class Booking(
    val id: Long = 0,
    val uid: String,
    val eventTypeId: Long,
    val hostUserId: Long,
    val startTimeUtc: String,
    val endTimeUtc: String,
    val bufferBefore: Int = 0,
    val bufferAfter: Int = 0,
    val status: String = "accepted", // "accepted" | "pending" | "cancelled" | "rejected"
    val cancelledAt: String? = null,
    val idempotencyKey: String,
    val locationJson: String? = null,
    val paid: Boolean = false,
    val paymentIntentId: String? = null,
    val createdAtUtc: String = ""
)

data class Attendee(
    val id: Long = 0,
    val bookingId: Long,
    val email: String,
    val name: String? = null,
    val timezone: String? = null,
    val phone: String? = null,
    val notes: String? = null
)

data class NotificationReminder(
    val id: Long = 0,
    val bookingId: Long,
    val type: String = "exact_alarm", // "exact_alarm" | "fcm_push" | "email"
    val triggerTimeUtc: String,
    val title: String,
    val body: String,
    val status: String = "scheduled", // "scheduled" | "delivered" | "cancelled"
    val isFired: Boolean = false,
    val createdTimeUtc: String = ""
)

data class OfferedSlot(
    val startUtc: String,
    val endUtc: String,
    val schedulingType: String,
    val attendingHostUserIds: List<Long> = emptyList(),
    val displayLocalTime: String = ""
)
