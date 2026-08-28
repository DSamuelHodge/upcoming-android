package com.example.core.repository

import android.content.Context
import com.example.core.database.UpcomingDatabase
import com.example.core.database.entity.*
import com.example.core.engine.*
import com.example.core.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.*

class UpcomingRepository(
    private val database: UpcomingDatabase,
    private val context: Context
) {
    private val userDao = database.userDao()
    private val scheduleDao = database.scheduleDao()
    private val availabilityDao = database.availabilityDao()
    private val eventTypeDao = database.eventTypeDao()
    private val bookingDao = database.bookingDao()
    private val reminderDao = database.reminderDao()

    val allEventTypes: Flow<List<EventType>> = eventTypeDao.getAllEventTypesFlow().map { list ->
        list.map { it.toDomain() }
    }

    val upcomingBookings: Flow<List<Booking>> = bookingDao.getUpcomingBookingsFlow().map { list ->
        list.map { it.toDomain() }
    }

    val allBookings: Flow<List<Booking>> = bookingDao.getAllBookingsFlow().map { list ->
        list.map { it.toDomain() }
    }

    val allReminders: Flow<List<NotificationReminder>> = reminderDao.getAllRemindersFlow().map { list ->
        list.map { it.toDomain() }
    }

    suspend fun getPrimaryUser(): User {
        val userEntity = userDao.getUserById(1)
        return userEntity?.toDomain() ?: User(
            id = 1,
            email = "alex.rivera@upcoming.io",
            username = "alex",
            displayName = "Alex Rivera",
            timezone = "America/New_York"
        )
    }

    fun getPrimaryUserFlow(): Flow<User?> {
        return userDao.getUserByIdFlow(1).map { it?.toDomain() }
    }

    suspend fun getScheduleForUser(userId: Long): Schedule {
        val entity = scheduleDao.getScheduleByUserId(userId)
        return entity?.toDomain() ?: Schedule(userId = userId)
    }

    suspend fun getAvailabilityRules(scheduleId: Long): List<AvailabilityRule> {
        return availabilityDao.getAvailabilityByScheduleId(scheduleId).map { it.toDomain() }
    }

    fun getAvailabilityRulesFlow(scheduleId: Long): Flow<List<AvailabilityRule>> {
        return availabilityDao.getAvailabilityByScheduleIdFlow(scheduleId).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun updateScheduleTimezone(scheduleId: Long, userId: Long, newTimezone: String) {
        val existing = scheduleDao.getScheduleByUserId(userId)
        if (existing != null) {
            scheduleDao.updateSchedule(existing.copy(timezone = newTimezone))
        } else {
            scheduleDao.insertSchedule(ScheduleEntity(id = scheduleId, userId = userId, name = "Working Hours", timezone = newTimezone))
        }
        val user = userDao.getUserById(userId)
        if (user != null) {
            userDao.updateUser(user.copy(timezone = newTimezone))
        }
    }

    suspend fun saveWeeklyAvailability(scheduleId: Long, rules: List<AvailabilityRule>) {
        availabilityDao.deleteAllByScheduleId(scheduleId)
        val entities = rules.map {
            AvailabilityEntity(
                scheduleId = scheduleId,
                dayOfWeek = it.dayOfWeek,
                dateOverride = it.dateOverride,
                startTime = it.startTime,
                endTime = it.endTime
            )
        }
        availabilityDao.insertAll(entities)
    }

    suspend fun addDateOverride(scheduleId: Long, dateOverride: String, startTime: String, endTime: String) {
        availabilityDao.insertAvailability(
            AvailabilityEntity(
                scheduleId = scheduleId,
                dayOfWeek = null,
                dateOverride = dateOverride,
                startTime = startTime,
                endTime = endTime
            )
        )
    }

    suspend fun removeAvailabilityRule(ruleId: Long) {
        availabilityDao.deleteAvailability(AvailabilityEntity(id = ruleId, scheduleId = 0, dayOfWeek = null, dateOverride = null, startTime = "", endTime = ""))
    }

    suspend fun getEventTypeById(id: Long): EventType? {
        return eventTypeDao.getEventTypeById(id)?.toDomain()
    }

    suspend fun getEventTypeBySlug(slug: String): EventType? {
        return eventTypeDao.getEventTypeBySlug(slug)?.toDomain()
    }

    suspend fun saveEventType(eventType: EventType): Long {
        val entity = eventType.toEntity()
        return if (eventType.id == 0L) {
            val newId = eventTypeDao.insertEventType(entity)
            eventTypeDao.insertEventTypeHost(
                EventTypeHostEntity(
                    eventTypeId = newId,
                    hostUserId = eventType.ownerUserId,
                    priority = 0
                )
            )
            newId
        } else {
            eventTypeDao.updateEventType(entity)
            eventType.id
        }
    }

    suspend fun deleteEventType(id: Long) {
        eventTypeDao.deleteEventTypeById(id)
        eventTypeDao.deleteHostsByEventTypeId(id)
    }

    suspend fun getBookingByUid(uid: String): Booking? {
        return bookingDao.getBookingByUid(uid)?.toDomain()
    }

    suspend fun getAttendeeForBooking(bookingId: Long): Attendee? {
        return bookingDao.getAttendeeForBooking(bookingId)?.toDomain()
    }

    suspend fun computeAvailableSlots(
        eventType: EventType,
        rangeStartUtc: Date,
        rangeEndUtc: Date,
        inviteeTimezone: String = "America/New_York"
    ): List<OfferedSlot> = withContext(Dispatchers.IO) {
        val hosts = userDao.getAllUsers().map { it.toDomain() }
        val schedulesMap = mutableMapOf<Long, Schedule>()
        val availabilityRulesMap = mutableMapOf<Long, List<AvailabilityRule>>()

        for (host in hosts) {
            val sched = scheduleDao.getScheduleByUserId(host.id)?.toDomain()
                ?: Schedule(userId = host.id, timezone = host.timezone)
            schedulesMap[host.id] = sched
            val rules = availabilityDao.getAvailabilityByScheduleId(sched.id).map { it.toDomain() }
            availabilityRulesMap[sched.id] = rules
        }

        val activeBookings = bookingDao.getAllActiveBookings().map { it.toDomain() }

        val rawSlots = SchedulingEngine.computeAvailability(
            eventType = eventType,
            hosts = hosts,
            schedules = schedulesMap,
            availabilityRules = availabilityRulesMap,
            existingBookings = activeBookings,
            rangeStartUtc = rangeStartUtc,
            rangeEndUtc = rangeEndUtc,
            now = Date()
        )

        // Format for display
        val outTz = TimeZone.getTimeZone(inviteeTimezone)
        val timeDisplayFormat = java.text.SimpleDateFormat("h:mm a", Locale.US).apply {
            timeZone = outTz
        }

        rawSlots.map { slot ->
            val d = SchedulingEngine.parseIsoUtc(slot.startUtc)
            slot.copy(displayLocalTime = timeDisplayFormat.format(d))
        }
    }

    suspend fun createBooking(
        eventTypeId: Long,
        slotStartUtc: String,
        slotEndUtc: String,
        locationJson: String,
        attendeeEmail: String,
        attendeeName: String?,
        attendeePhone: String?,
        attendeeNotes: String?,
        attendeeTimezone: String?,
        idempotencyKey: String,
        paid: Boolean = false,
        paymentIntentId: String? = null
    ): Result<Booking> = withContext(Dispatchers.IO) {
        try {
            // Check idempotency first (replay)
            val existing = bookingDao.getBookingByIdempotencyKey(idempotencyKey)
            if (existing != null) {
                return@withContext Result.success(existing.toDomain())
            }

            val eventType = eventTypeDao.getEventTypeById(eventTypeId)?.toDomain()
                ?: return@withContext Result.failure(Exception("Event type not found"))

            val hostId = eventType.ownerUserId
            val uid = UUID.randomUUID().toString().replace("-", "").take(16)

            val bookingEntity = BookingEntity(
                uid = uid,
                eventTypeId = eventTypeId,
                hostUserId = hostId,
                startTimeUtc = slotStartUtc,
                endTimeUtc = slotEndUtc,
                bufferBefore = eventType.bufferBefore,
                bufferAfter = eventType.bufferAfter,
                status = "accepted",
                cancelledAt = null,
                idempotencyKey = idempotencyKey,
                locationJson = locationJson,
                paid = paid,
                paymentIntentId = paymentIntentId,
                createdAtUtc = SchedulingEngine.formatIsoUtc(Date())
            )

            val bookingId = bookingDao.insertBooking(bookingEntity)

            val attendeeEntity = AttendeeEntity(
                bookingId = bookingId,
                email = attendeeEmail,
                name = attendeeName,
                timezone = attendeeTimezone,
                phone = attendeePhone,
                notes = attendeeNotes
            )
            bookingDao.insertAttendee(attendeeEntity)

            val createdBooking = bookingEntity.copy(id = bookingId).toDomain()

            // Schedule exact alarm reminder 15 mins prior
            NotificationAndReminderManager.scheduleExactAlarm(
                context = context,
                booking = createdBooking,
                eventType = eventType,
                attendeeName = attendeeName,
                reminderMinutesBefore = 15
            )

            // Record reminder in DB
            val reminderTriggerTimeMs = SchedulingEngine.parseIsoUtc(slotStartUtc).time - (15 * 60 * 1000L)
            reminderDao.insertReminder(
                ReminderEntity(
                    bookingId = bookingId,
                    type = "exact_alarm",
                    triggerTimeUtc = SchedulingEngine.formatIsoUtc(Date(reminderTriggerTimeMs)),
                    title = "Upcoming: ${eventType.title}",
                    body = "Meeting with ${attendeeName ?: attendeeEmail} starting in 15 minutes",
                    status = "scheduled",
                    isFired = false,
                    createdTimeUtc = SchedulingEngine.formatIsoUtc(Date())
                )
            )

            // Trigger simulated real-time FCM notification
            NotificationAndReminderManager.triggerFcmNotification(
                context = context,
                title = "New Booking Confirmed!",
                body = "${attendeeName ?: attendeeEmail} booked ${eventType.title} on ${slotStartUtc.take(10)}",
                bookingUid = uid
            )

            Result.success(createdBooking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelBooking(uid: String, reason: String = "Cancelled by user"): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val booking = bookingDao.getBookingByUid(uid) ?: return@withContext Result.failure(Exception("Booking not found"))
            val nowUtc = SchedulingEngine.formatIsoUtc(Date())
            bookingDao.cancelBookingByUid(uid, nowUtc)

            // Cancel alarm
            NotificationAndReminderManager.cancelAlarm(context, uid)
            reminderDao.cancelRemindersForBooking(booking.id)

            // Trigger FCM cancellation push
            NotificationAndReminderManager.triggerFcmNotification(
                context = context,
                title = "Meeting Cancelled",
                body = "Booking #${uid.take(8)} has been cancelled. Time slot is now free.",
                bookingUid = uid
            )

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingUsers = userDao.getAllUsers()
        if (existingUsers.isNotEmpty()) return@withContext

        // 1. Primary User & Team
        val primaryUserId = userDao.insertUser(
            UserEntity(
                email = "alex.rivera@upcoming.io",
                username = "alex",
                displayName = "Alex Rivera",
                timezone = "America/New_York",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                metadata = """{"role":"Product Lead","company":"Upcoming Labs"}"""
            )
        )

        val teamUser2 = userDao.insertUser(
            UserEntity(
                email = "sarah.chen@upcoming.io",
                username = "sarah",
                displayName = "Sarah Chen",
                timezone = "America/New_York",
                avatarUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?w=150",
                metadata = """{"role":"Senior Architect"}"""
            )
        )

        // 2. Schedule & Availability
        val scheduleId = scheduleDao.insertSchedule(
            ScheduleEntity(
                userId = primaryUserId,
                name = "Working Hours",
                timezone = "America/New_York"
            )
        )

        val defaultWeeklyRules = listOf(
            AvailabilityEntity(scheduleId = scheduleId, dayOfWeek = 1, dateOverride = null, startTime = "09:00", endTime = "17:00"),
            AvailabilityEntity(scheduleId = scheduleId, dayOfWeek = 2, dateOverride = null, startTime = "09:00", endTime = "17:00"),
            AvailabilityEntity(scheduleId = scheduleId, dayOfWeek = 3, dateOverride = null, startTime = "09:00", endTime = "17:00"),
            AvailabilityEntity(scheduleId = scheduleId, dayOfWeek = 4, dateOverride = null, startTime = "09:00", endTime = "17:00"),
            AvailabilityEntity(scheduleId = scheduleId, dayOfWeek = 5, dateOverride = null, startTime = "09:00", endTime = "16:00")
        )
        availabilityDao.insertAll(defaultWeeklyRules)

        // Schedule for team member 2
        val sched2Id = scheduleDao.insertSchedule(
            ScheduleEntity(
                userId = teamUser2,
                name = "Sarah's Hours",
                timezone = "America/New_York"
            )
        )
        val sched2Rules = listOf(
            AvailabilityEntity(scheduleId = sched2Id, dayOfWeek = 1, dateOverride = null, startTime = "10:00", endTime = "18:00"),
            AvailabilityEntity(scheduleId = sched2Id, dayOfWeek = 2, dateOverride = null, startTime = "10:00", endTime = "18:00"),
            AvailabilityEntity(scheduleId = sched2Id, dayOfWeek = 3, dateOverride = null, startTime = "10:00", endTime = "18:00"),
            AvailabilityEntity(scheduleId = sched2Id, dayOfWeek = 4, dateOverride = null, startTime = "10:00", endTime = "18:00"),
            AvailabilityEntity(scheduleId = sched2Id, dayOfWeek = 5, dateOverride = null, startTime = "10:00", endTime = "18:00")
        )
        availabilityDao.insertAll(sched2Rules)

        // 3. Event Types
        val et1Id = eventTypeDao.insertEventType(
            EventTypeEntity(
                ownerUserId = primaryUserId,
                slug = "15min",
                title = "15 Min Discovery Call",
                description = "Quick informal sync to discuss product requirements, scope, and synergy.",
                lengthMinutes = 15,
                slotIntervalMinutes = 15,
                bufferBefore = 0,
                bufferAfter = 5,
                schedulingType = "individual",
                locationsJson = """[{"type":"integrations:daily","label":"Daily Video Call","url":"https://upcoming.daily.co/discovery-alex"}]""",
                minBookingNotice = 30,
                priceInCents = 0,
                currency = "usd",
                colorHex = "#0B5CFF",
                isActive = true
            )
        )
        eventTypeDao.insertEventTypeHost(EventTypeHostEntity(eventTypeId = et1Id, hostUserId = primaryUserId, priority = 0))

        val et2Id = eventTypeDao.insertEventType(
            EventTypeEntity(
                ownerUserId = primaryUserId,
                slug = "demo-30m",
                title = "30 Min Product Walkthrough",
                description = "Comprehensive walkthrough of the Upcoming booking engine and multi-host scheduling.",
                lengthMinutes = 30,
                slotIntervalMinutes = 30,
                bufferBefore = 5,
                bufferAfter = 10,
                schedulingType = "individual",
                locationsJson = """[{"type":"integrations:daily","label":"Daily Video Call","url":"https://upcoming.daily.co/demo-room"},{"type":"googleMeet","label":"Google Meet"},{"type":"userPhone","label":"Phone Call"}]""",
                minBookingNotice = 60,
                priceInCents = 0,
                currency = "usd",
                colorHex = "#10B981",
                isActive = true
            )
        )
        eventTypeDao.insertEventTypeHost(EventTypeHostEntity(eventTypeId = et2Id, hostUserId = primaryUserId, priority = 0))

        val et3Id = eventTypeDao.insertEventType(
            EventTypeEntity(
                ownerUserId = primaryUserId,
                slug = "deep-dive",
                title = "45 Min Technical Deep Dive",
                description = "Architecture consulting & system review. Requires Stripe payment deposit.",
                lengthMinutes = 45,
                slotIntervalMinutes = 45,
                bufferBefore = 10,
                bufferAfter = 15,
                schedulingType = "individual",
                locationsJson = """[{"type":"integrations:daily","label":"Daily Video Call","url":"https://upcoming.daily.co/consulting-alex"}]""",
                minBookingNotice = 120,
                priceInCents = 7500, // $75.00
                currency = "usd",
                colorHex = "#7C3AED",
                isActive = true
            )
        )
        eventTypeDao.insertEventTypeHost(EventTypeHostEntity(eventTypeId = et3Id, hostUserId = primaryUserId, priority = 0))

        val et4Id = eventTypeDao.insertEventType(
            EventTypeEntity(
                ownerUserId = primaryUserId,
                slug = "strategy-collective",
                title = "60 Min Strategy (Collective)",
                description = "Joint session with Alex & Sarah Chen for end-to-end technical strategy.",
                lengthMinutes = 60,
                slotIntervalMinutes = 60,
                bufferBefore = 15,
                bufferAfter = 15,
                schedulingType = "collective",
                locationsJson = """[{"type":"integrations:daily","label":"Daily Video Call","url":"https://upcoming.daily.co/strategy-collective"}]""",
                minBookingNotice = 240,
                priceInCents = 15000, // $150.00
                currency = "usd",
                colorHex = "#F59E0B",
                isActive = true
            )
        )
        eventTypeDao.insertEventTypeHost(EventTypeHostEntity(eventTypeId = et4Id, hostUserId = primaryUserId, priority = 0))
        eventTypeDao.insertEventTypeHost(EventTypeHostEntity(eventTypeId = et4Id, hostUserId = teamUser2, priority = 1))

        // 4. Sample Upcoming Bookings
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, 14) // 2 PM UTC
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val booking1Start = SchedulingEngine.formatIsoUtc(cal.time)
        cal.add(Calendar.MINUTE, 30)
        val booking1End = SchedulingEngine.formatIsoUtc(cal.time)

        val b1Id = bookingDao.insertBooking(
            BookingEntity(
                uid = "bk_demo_${UUID.randomUUID().toString().take(8)}",
                eventTypeId = et2Id,
                hostUserId = primaryUserId,
                startTimeUtc = booking1Start,
                endTimeUtc = booking1End,
                bufferBefore = 5,
                bufferAfter = 10,
                status = "accepted",
                cancelledAt = null,
                idempotencyKey = "demo_key_001",
                locationJson = """{"type":"integrations:daily","label":"Daily Video Call","url":"https://upcoming.daily.co/demo-room"}""",
                paid = false,
                paymentIntentId = null,
                createdAtUtc = SchedulingEngine.formatIsoUtc(Date())
            )
        )
        bookingDao.insertAttendee(
            AttendeeEntity(
                bookingId = b1Id,
                email = "jordan.taylor@acme.corp",
                name = "Jordan Taylor",
                timezone = "America/New_York",
                phone = "+1 (555) 234-5678",
                notes = "Interested in integrating multi-host round-robin routing."
            )
        )

        // Add 1 reminder
        reminderDao.insertReminder(
            ReminderEntity(
                bookingId = b1Id,
                type = "exact_alarm",
                triggerTimeUtc = booking1Start,
                title = "Upcoming: 30 Min Product Walkthrough",
                body = "Meeting with Jordan Taylor starts in 15 minutes.",
                status = "scheduled",
                isFired = false,
                createdTimeUtc = SchedulingEngine.formatIsoUtc(Date())
            )
        )
    }
}

// Extension converters
fun UserEntity.toDomain() = User(
    id = id,
    email = email,
    username = username,
    displayName = displayName,
    timezone = timezone,
    avatarUrl = avatarUrl,
    metadata = metadata
)

fun ScheduleEntity.toDomain() = Schedule(
    id = id,
    userId = userId,
    name = name,
    timezone = timezone
)

fun AvailabilityEntity.toDomain() = AvailabilityRule(
    id = id,
    scheduleId = scheduleId,
    dayOfWeek = dayOfWeek,
    dateOverride = dateOverride,
    startTime = startTime,
    endTime = endTime
)

fun EventTypeEntity.toDomain() = EventType(
    id = id,
    ownerUserId = ownerUserId,
    slug = slug,
    title = title,
    description = description,
    lengthMinutes = lengthMinutes,
    slotIntervalMinutes = slotIntervalMinutes,
    bufferBefore = bufferBefore,
    bufferAfter = bufferAfter,
    schedulingType = schedulingType,
    locationsJson = locationsJson,
    minBookingNotice = minBookingNotice,
    priceInCents = priceInCents,
    currency = currency,
    colorHex = colorHex,
    isActive = isActive
)

fun EventType.toEntity() = EventTypeEntity(
    id = id,
    ownerUserId = ownerUserId,
    slug = slug,
    title = title,
    description = description,
    lengthMinutes = lengthMinutes,
    slotIntervalMinutes = slotIntervalMinutes,
    bufferBefore = bufferBefore,
    bufferAfter = bufferAfter,
    schedulingType = schedulingType,
    locationsJson = locationsJson,
    minBookingNotice = minBookingNotice,
    priceInCents = priceInCents,
    currency = currency,
    colorHex = colorHex,
    isActive = isActive
)

fun BookingEntity.toDomain() = Booking(
    id = id,
    uid = uid,
    eventTypeId = eventTypeId,
    hostUserId = hostUserId,
    startTimeUtc = startTimeUtc,
    endTimeUtc = endTimeUtc,
    bufferBefore = bufferBefore,
    bufferAfter = bufferAfter,
    status = status,
    cancelledAt = cancelledAt,
    idempotencyKey = idempotencyKey,
    locationJson = locationJson,
    paid = paid,
    paymentIntentId = paymentIntentId,
    createdAtUtc = createdAtUtc
)

fun AttendeeEntity.toDomain() = Attendee(
    id = id,
    bookingId = bookingId,
    email = email,
    name = name,
    timezone = timezone,
    phone = phone,
    notes = notes
)

fun ReminderEntity.toDomain() = NotificationReminder(
    id = id,
    bookingId = bookingId,
    type = type,
    triggerTimeUtc = triggerTimeUtc,
    title = title,
    body = body,
    status = status,
    isFired = isFired,
    createdTimeUtc = createdTimeUtc
)
