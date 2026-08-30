package app.getupcoming.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["email"], unique = true),
        Index(value = ["username"], unique = true)
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    val username: String,
    val displayName: String,
    val timezone: String,
    val avatarUrl: String,
    val metadata: String
)

@Entity(
    tableName = "schedules",
    indices = [
        Index(value = ["userId"], unique = true)
    ]
)
data class ScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val name: String,
    val timezone: String
)

@Entity(
    tableName = "availability",
    indices = [
        Index(value = ["scheduleId"])
    ]
)
data class AvailabilityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scheduleId: Long,
    val dayOfWeek: Int?, // 0 = Sun, 1 = Mon, ..., 6 = Sat
    val dateOverride: String?, // "YYYY-MM-DD"
    val startTime: String, // "HH:MM"
    val endTime: String // "HH:MM"
)

@Entity(
    tableName = "event_types",
    indices = [
        Index(value = ["ownerUserId", "slug"], unique = true)
    ]
)
data class EventTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ownerUserId: Long,
    val slug: String,
    val title: String,
    val description: String,
    val lengthMinutes: Int,
    val slotIntervalMinutes: Int?,
    val bufferBefore: Int,
    val bufferAfter: Int,
    val schedulingType: String,
    val locationsJson: String,
    val minBookingNotice: Int,
    val priceInCents: Int,
    val currency: String,
    val colorHex: String,
    val isActive: Boolean
)

@Entity(
    tableName = "event_type_hosts",
    indices = [
        Index(value = ["eventTypeId", "hostUserId"], unique = true),
        Index(value = ["eventTypeId"])
    ]
)
data class EventTypeHostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eventTypeId: Long,
    val hostUserId: Long,
    val priority: Int
)

@Entity(
    tableName = "bookings",
    indices = [
        Index(value = ["uid"], unique = true),
        Index(value = ["idempotencyKey"], unique = true),
        Index(value = ["hostUserId", "startTimeUtc", "endTimeUtc"])
    ]
)
data class BookingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uid: String,
    val eventTypeId: Long,
    val hostUserId: Long,
    val startTimeUtc: String,
    val endTimeUtc: String,
    val bufferBefore: Int,
    val bufferAfter: Int,
    val status: String,
    val cancelledAt: String?,
    val idempotencyKey: String,
    val locationJson: String?,
    val paid: Boolean,
    val paymentIntentId: String?,
    val createdAtUtc: String
)

@Entity(
    tableName = "attendees",
    indices = [
        Index(value = ["bookingId"])
    ]
)
data class AttendeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookingId: Long,
    val email: String,
    val name: String?,
    val timezone: String?,
    val phone: String?,
    val notes: String?
)

@Entity(
    tableName = "notification_reminders",
    indices = [
        Index(value = ["bookingId"])
    ]
)
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookingId: Long,
    val type: String,
    val triggerTimeUtc: String,
    val title: String,
    val body: String,
    val status: String,
    val isFired: Boolean,
    val createdTimeUtc: String
)
