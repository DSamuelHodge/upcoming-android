package app.getupcoming

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import app.getupcoming.core.engine.SchedulingEngine
import app.getupcoming.core.model.AvailabilityRule
import app.getupcoming.core.model.Booking
import app.getupcoming.core.model.EventType
import app.getupcoming.core.model.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar
import java.util.TimeZone

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Upcoming", appName)
  }

  @Test
  fun `test scheduling engine computes available slots within working hours`() {
    val schedule = Schedule(
      id = 1L,
      userId = 1L,
      name = "Working Hours",
      timezone = "America/New_York"
    )

    // Monday to Friday 09:00 - 17:00
    val rules = (1..5).map { day ->
      AvailabilityRule(
        id = day.toLong(),
        scheduleId = 1L,
        dayOfWeek = day,
        dateOverride = null,
        startTime = "09:00",
        endTime = "17:00"
      )
    }

    val eventType = EventType(
      id = 1L,
      ownerUserId = 1L,
      slug = "quick-chat",
      title = "Quick Chat",
      description = "15m sync",
      lengthMinutes = 15,
      slotIntervalMinutes = 15,
      bufferBefore = 0,
      bufferAfter = 0,
      schedulingType = "individual"
    )

    // Wednesday in UTC
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, Calendar.SEPTEMBER)
      set(Calendar.DAY_OF_MONTH, 2) // Wednesday
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val startUtc = cal.time
    cal.add(Calendar.DAY_OF_YEAR, 1)
    val endUtc = cal.time

    val host = app.getupcoming.core.model.User(
      id = 1L,
      email = "alex@example.com",
      username = "alex",
      displayName = "Alex Rivera",
      timezone = "America/New_York"
    )

    val slots = SchedulingEngine.computeAvailability(
      eventType = eventType,
      hosts = listOf(host),
      schedules = mapOf(1L to schedule),
      availabilityRules = mapOf(1L to rules),
      existingBookings = emptyList(),
      rangeStartUtc = startUtc,
      rangeEndUtc = endUtc,
      now = startUtc
    )

    assertTrue("Slots should be generated for Wednesday", slots.isNotEmpty())
  }

  @Test
  fun `test buffer expansion blocks conflicting slots`() {
    val existingBooking = Booking(
      id = 10L,
      uid = "book-123",
      eventTypeId = 1L,
      hostUserId = 1L,
      startTimeUtc = "2026-09-02T14:00:00.000Z",
      endTimeUtc = "2026-09-02T14:30:00.000Z",
      bufferBefore = 10,
      bufferAfter = 15,
      status = "accepted",
      idempotencyKey = "key-1"
    )

    val slotStart = SchedulingEngine.parseIsoUtc("2026-09-02T14:35:00.000Z").time
    val slotEnd = SchedulingEngine.parseIsoUtc("2026-09-02T14:50:00.000Z").time

    val touches = SchedulingEngine.hasConflict(
      hostId = 1L,
      slotStartMs = slotStart,
      slotEndMs = slotEnd,
      bufferBeforeMs = 0L,
      bufferAfterMs = 0L,
      existingBookings = listOf(existingBooking)
    )

    assertTrue("Slot starting at 14:35 touches buffer ending at 14:45", touches)
  }
}

