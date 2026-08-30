package app.getupcoming.core.engine

import app.getupcoming.core.model.*
import java.text.SimpleDateFormat
import java.util.*

object SchedulingEngine {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    fun parseIsoUtc(isoString: String): Date {
        return try {
            isoFormat.parse(isoString) ?: Date()
        } catch (e: Exception) {
            try {
                // Try format with milliseconds if present
                val alt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }
                alt.parse(isoString) ?: Date()
            } catch (e2: Exception) {
                Date()
            }
        }
    }

    fun formatIsoUtc(date: Date): String {
        return isoFormat.format(date)
    }

    fun computeAvailability(
        eventType: EventType,
        hosts: List<User>,
        schedules: Map<Long, Schedule>,
        availabilityRules: Map<Long, List<AvailabilityRule>>,
        existingBookings: List<Booking>,
        rangeStartUtc: Date,
        rangeEndUtc: Date,
        now: Date = Date()
    ): List<OfferedSlot> {
        val slotLengthMs = eventType.lengthMinutes * 60 * 1000L
        val slotIntervalMs = (eventType.slotIntervalMinutes ?: eventType.lengthMinutes) * 60 * 1000L
        val minNoticeMs = eventType.minBookingNotice * 60 * 1000L
        val earliestAllowedTimeMs = now.time + minNoticeMs

        val candidateSlots = mutableListOf<OfferedSlot>()

        // Generate day by day in range
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            time = rangeStartUtc
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            time = rangeEndUtc
        }

        while (cal.timeInMillis < endCal.timeInMillis) {
            val dayStart = cal.time
            val dayDateStr = dateFormat.format(dayStart)
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1 // 0=Sun, 1=Mon, ..., 6=Sat

            // Find slots for this day across hosts
            val hostSlotsMap = mutableMapOf<Long, List<Pair<Date, Date>>>()

            for (host in hosts) {
                val schedule = schedules[host.id] ?: Schedule(userId = host.id, timezone = host.timezone)
                val hostTz = TimeZone.getTimeZone(schedule.timezone.ifBlank { "America/New_York" })
                val rules = availabilityRules[schedule.id] ?: emptyList()

                // Check date-specific overrides first, else recurring day of week
                val overrideRules = rules.filter { it.dateOverride == dayDateStr }
                val activeRules = if (overrideRules.isNotEmpty()) {
                    overrideRules
                } else {
                    rules.filter { it.dayOfWeek == dayOfWeek }
                }

                val freeSlotsForHost = mutableListOf<Pair<Date, Date>>()

                for (rule in activeRules) {
                    val startParts = rule.startTime.split(":").mapNotNull { it.toIntOrNull() }
                    val endParts = rule.endTime.split(":").mapNotNull { it.toIntOrNull() }
                    if (startParts.size < 2 || endParts.size < 2) continue

                    val hostYear = cal.get(Calendar.YEAR)
                    val hostMonth = cal.get(Calendar.MONTH)
                    val hostDay = cal.get(Calendar.DAY_OF_MONTH)

                    // Build local time in host timezone
                    val hostCal = Calendar.getInstance(hostTz).apply {
                        set(Calendar.YEAR, hostYear)
                        set(Calendar.MONTH, hostMonth)
                        set(Calendar.DAY_OF_MONTH, hostDay)
                        set(Calendar.HOUR_OF_DAY, startParts[0])
                        set(Calendar.MINUTE, startParts[1])
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val windowStartTimeMs = hostCal.timeInMillis

                    val hostEndCal = Calendar.getInstance(hostTz).apply {
                        set(Calendar.YEAR, hostYear)
                        set(Calendar.MONTH, hostMonth)
                        set(Calendar.DAY_OF_MONTH, hostDay)
                        set(Calendar.HOUR_OF_DAY, endParts[0])
                        set(Calendar.MINUTE, endParts[1])
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    val windowEndTimeMs = hostEndCal.timeInMillis

                    var currentSlotStartMs = windowStartTimeMs
                    while (currentSlotStartMs + slotLengthMs <= windowEndTimeMs) {
                        val currentSlotEndMs = currentSlotStartMs + slotLengthMs

                        // Check min booking notice
                        if (currentSlotStartMs >= earliestAllowedTimeMs) {
                            // Check conflict with existing active bookings for this host
                            val hasConflict = checkHostConflict(
                                hostId = host.id,
                                slotStartMs = currentSlotStartMs,
                                slotEndMs = currentSlotEndMs,
                                bufferBeforeMs = eventType.bufferBefore * 60 * 1000L,
                                bufferAfterMs = eventType.bufferAfter * 60 * 1000L,
                                existingBookings = existingBookings
                            )

                            if (!hasConflict) {
                                freeSlotsForHost.add(Pair(Date(currentSlotStartMs), Date(currentSlotEndMs)))
                            }
                        }
                        currentSlotStartMs += slotIntervalMs
                    }
                }
                hostSlotsMap[host.id] = freeSlotsForHost
            }

            // Consolidate slots according to schedulingType
            when (eventType.schedulingType) {
                "individual" -> {
                    val primaryHost = hosts.firstOrNull()
                    if (primaryHost != null) {
                        val slots = hostSlotsMap[primaryHost.id] ?: emptyList()
                        for (slot in slots) {
                            candidateSlots.add(
                                OfferedSlot(
                                    startUtc = formatIsoUtc(slot.first),
                                    endUtc = formatIsoUtc(slot.second),
                                    schedulingType = "individual",
                                    attendingHostUserIds = listOf(primaryHost.id)
                                )
                            )
                        }
                    }
                }
                "round_robin" -> {
                    // Union of all available slots; slot is available if at least one host is free
                    val allSlotTimes = mutableSetOf<Pair<Long, Long>>()
                    for ((_, slots) in hostSlotsMap) {
                        for (s in slots) {
                            allSlotTimes.add(Pair(s.first.time, s.second.time))
                        }
                    }

                    for (slotPair in allSlotTimes.sortedBy { it.first }) {
                        val availableHostIds = hostSlotsMap.filter { entry ->
                            entry.value.any { it.first.time == slotPair.first && it.second.time == slotPair.second }
                        }.keys.toList()

                        candidateSlots.add(
                            OfferedSlot(
                                startUtc = formatIsoUtc(Date(slotPair.first)),
                                endUtc = formatIsoUtc(Date(slotPair.second)),
                                schedulingType = "round_robin",
                                attendingHostUserIds = availableHostIds
                            )
                        )
                    }
                }
                "collective" -> {
                    // Intersection: ALL hosts must be available
                    if (hosts.isNotEmpty()) {
                        val firstHostSlots = hostSlotsMap[hosts.first().id] ?: emptyList()
                        for (slot in firstHostSlots) {
                            val allFree = hosts.all { h ->
                                (hostSlotsMap[h.id] ?: emptyList()).any {
                                    it.first.time == slot.first.time && it.second.time == slot.second.time
                                }
                            }
                            if (allFree) {
                                candidateSlots.add(
                                    OfferedSlot(
                                        startUtc = formatIsoUtc(slot.first),
                                        endUtc = formatIsoUtc(slot.second),
                                        schedulingType = "collective",
                                        attendingHostUserIds = hosts.map { it.id }
                                    )
                                )
                            }
                        }
                    }
                }
            }

            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        return candidateSlots.distinctBy { it.startUtc }
    }

    fun hasConflict(
        hostId: Long,
        slotStartMs: Long,
        slotEndMs: Long,
        bufferBeforeMs: Long,
        bufferAfterMs: Long,
        existingBookings: List<Booking>
    ): Boolean {
        return checkHostConflict(hostId, slotStartMs, slotEndMs, bufferBeforeMs, bufferAfterMs, existingBookings)
    }

    private fun checkHostConflict(
        hostId: Long,
        slotStartMs: Long,
        slotEndMs: Long,
        bufferBeforeMs: Long,
        bufferAfterMs: Long,
        existingBookings: List<Booking>
    ): Boolean {
        val testStartWithBuffer = slotStartMs - bufferBeforeMs
        val testEndWithBuffer = slotEndMs + bufferAfterMs

        for (b in existingBookings) {
            if (b.hostUserId != hostId || b.status == "cancelled" || b.status == "rejected") continue

            val bStart = parseIsoUtc(b.startTimeUtc).time
            val bEnd = parseIsoUtc(b.endTimeUtc).time
            val bStartBuffered = bStart - (b.bufferBefore * 60 * 1000L)
            val bEndBuffered = bEnd + (b.bufferAfter * 60 * 1000L)

            // Overlap condition: start < other.end && end > other.start
            if (testStartWithBuffer < bEndBuffered && testEndWithBuffer > bStartBuffered) {
                return true
            }
        }
        return false
    }
}
