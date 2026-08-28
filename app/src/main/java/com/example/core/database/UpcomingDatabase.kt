package com.example.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.core.database.dao.*
import com.example.core.database.entity.*

@Database(
    entities = [
        UserEntity::class,
        ScheduleEntity::class,
        AvailabilityEntity::class,
        EventTypeEntity::class,
        EventTypeHostEntity::class,
        BookingEntity::class,
        AttendeeEntity::class,
        ReminderEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class UpcomingDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun availabilityDao(): AvailabilityDao
    abstract fun eventTypeDao(): EventTypeDao
    abstract fun bookingDao(): BookingDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        @Volatile
        private var INSTANCE: UpcomingDatabase? = null

        fun getInstance(context: Context): UpcomingDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UpcomingDatabase::class.java,
                    "upcoming_scheduling.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
