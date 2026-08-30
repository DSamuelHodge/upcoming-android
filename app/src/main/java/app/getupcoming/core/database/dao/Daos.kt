package app.getupcoming.core.database.dao

import androidx.room.*
import app.getupcoming.core.database.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    fun getUserByIdFlow(id: Long): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: Long): UserEntity?

    @Query("SELECT * FROM users")
    fun getAllUsersFlow(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users")
    suspend fun getAllUsers(): List<UserEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity): Long

    @Update
    suspend fun updateUser(user: UserEntity)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM schedules WHERE userId = :userId LIMIT 1")
    fun getScheduleByUserIdFlow(userId: Long): Flow<ScheduleEntity?>

    @Query("SELECT * FROM schedules WHERE userId = :userId LIMIT 1")
    suspend fun getScheduleByUserId(userId: Long): ScheduleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: ScheduleEntity): Long

    @Update
    suspend fun updateSchedule(schedule: ScheduleEntity)
}

@Dao
interface AvailabilityDao {
    @Query("SELECT * FROM availability WHERE scheduleId = :scheduleId")
    fun getAvailabilityByScheduleIdFlow(scheduleId: Long): Flow<List<AvailabilityEntity>>

    @Query("SELECT * FROM availability WHERE scheduleId = :scheduleId")
    suspend fun getAvailabilityByScheduleId(scheduleId: Long): List<AvailabilityEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAvailability(availability: AvailabilityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<AvailabilityEntity>)

    @Delete
    suspend fun deleteAvailability(availability: AvailabilityEntity)

    @Query("DELETE FROM availability WHERE scheduleId = :scheduleId")
    suspend fun deleteAllByScheduleId(scheduleId: Long)
}

@Dao
interface EventTypeDao {
    @Query("SELECT * FROM event_types ORDER BY id ASC")
    fun getAllEventTypesFlow(): Flow<List<EventTypeEntity>>

    @Query("SELECT * FROM event_types WHERE id = :id LIMIT 1")
    fun getEventTypeByIdFlow(id: Long): Flow<EventTypeEntity?>

    @Query("SELECT * FROM event_types WHERE id = :id LIMIT 1")
    suspend fun getEventTypeById(id: Long): EventTypeEntity?

    @Query("SELECT * FROM event_types WHERE slug = :slug LIMIT 1")
    suspend fun getEventTypeBySlug(slug: String): EventTypeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventType(eventType: EventTypeEntity): Long

    @Update
    suspend fun updateEventType(eventType: EventTypeEntity)

    @Query("DELETE FROM event_types WHERE id = :id")
    suspend fun deleteEventTypeById(id: Long)

    @Query("SELECT * FROM event_type_hosts WHERE eventTypeId = :eventTypeId")
    suspend fun getHostsForEventType(eventTypeId: Long): List<EventTypeHostEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEventTypeHost(host: EventTypeHostEntity): Long

    @Query("DELETE FROM event_type_hosts WHERE eventTypeId = :eventTypeId")
    suspend fun deleteHostsByEventTypeId(eventTypeId: Long)
}

@Dao
interface BookingDao {
    @Query("SELECT * FROM bookings ORDER BY startTimeUtc DESC")
    fun getAllBookingsFlow(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE status = 'accepted' ORDER BY startTimeUtc ASC")
    fun getUpcomingBookingsFlow(): Flow<List<BookingEntity>>

    @Query("SELECT * FROM bookings WHERE id = :id LIMIT 1")
    fun getBookingByIdFlow(id: Long): Flow<BookingEntity?>

    @Query("SELECT * FROM bookings WHERE id = :id LIMIT 1")
    suspend fun getBookingById(id: Long): BookingEntity?

    @Query("SELECT * FROM bookings WHERE uid = :uid LIMIT 1")
    suspend fun getBookingByUid(uid: String): BookingEntity?

    @Query("SELECT * FROM bookings WHERE idempotencyKey = :key LIMIT 1")
    suspend fun getBookingByIdempotencyKey(key: String): BookingEntity?

    @Query("SELECT * FROM bookings WHERE hostUserId = :hostUserId AND status != 'cancelled'")
    suspend fun getActiveBookingsForHost(hostUserId: Long): List<BookingEntity>

    @Query("SELECT * FROM bookings WHERE status != 'cancelled'")
    suspend fun getAllActiveBookings(): List<BookingEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertBooking(booking: BookingEntity): Long

    @Update
    suspend fun updateBooking(booking: BookingEntity)

    @Query("UPDATE bookings SET status = 'cancelled', cancelledAt = :cancelledAt WHERE uid = :uid")
    suspend fun cancelBookingByUid(uid: String, cancelledAt: String)

    // Attendees
    @Query("SELECT * FROM attendees WHERE bookingId = :bookingId LIMIT 1")
    fun getAttendeeForBookingFlow(bookingId: Long): Flow<AttendeeEntity?>

    @Query("SELECT * FROM attendees WHERE bookingId = :bookingId LIMIT 1")
    suspend fun getAttendeeForBooking(bookingId: Long): AttendeeEntity?

    @Query("SELECT * FROM attendees WHERE bookingId IN (:bookingIds)")
    suspend fun getAttendeesForBookings(bookingIds: List<Long>): List<AttendeeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendee(attendee: AttendeeEntity): Long
}

@Dao
interface ReminderDao {
    @Query("SELECT * FROM notification_reminders ORDER BY triggerTimeUtc DESC")
    fun getAllRemindersFlow(): Flow<List<ReminderEntity>>

    @Query("SELECT * FROM notification_reminders WHERE status = 'scheduled' AND isFired = 0 ORDER BY triggerTimeUtc ASC")
    fun getScheduledRemindersFlow(): Flow<List<ReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ReminderEntity): Long

    @Query("UPDATE notification_reminders SET isFired = 1, status = 'delivered' WHERE id = :id")
    suspend fun markDelivered(id: Long)

    @Query("UPDATE notification_reminders SET status = 'cancelled' WHERE bookingId = :bookingId")
    suspend fun cancelRemindersForBooking(bookingId: Long)
}
