package app.getupcoming.core.repository

import android.content.Context
import app.getupcoming.core.database.UpcomingDatabase
import app.getupcoming.core.database.entity.*
import app.getupcoming.core.engine.*
import app.getupcoming.core.model.*
import app.getupcoming.core.network.ApiException
import app.getupcoming.core.network.AttendeeWireDto
import app.getupcoming.core.network.BookingResultDto
import app.getupcoming.core.network.BookingRowDto
import app.getupcoming.core.network.CreateBookingRequest
import app.getupcoming.core.network.CreateEventTypeRequest
import app.getupcoming.core.network.EventTypeDto
import app.getupcoming.core.network.LocationDto
import app.getupcoming.core.network.MeResponseDto
import app.getupcoming.core.network.PatchMeRequest
import app.getupcoming.core.network.PatchScheduleRequest
import app.getupcoming.core.network.SingleUseLinkDto
import app.getupcoming.core.network.CreateSingleUseLinksRequest
import app.getupcoming.core.network.UpcomingApi
import app.getupcoming.core.network.UpcomingApiClient
import app.getupcoming.core.network.UpdateEventTypeRequest
import app.getupcoming.core.network.UserMetadataDto
import app.getupcoming.core.network.UserPrefsDto
import app.getupcoming.core.network.apiCall
import app.getupcoming.core.network.isNetworkError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.*

class UpcomingRepository(
    private val database: UpcomingDatabase,
    private val context: Context,
    private val api: UpcomingApi? = null,
    private val authTokens: app.getupcoming.core.auth.AuthTokenManager? = null
) {
    private val userDao = database.userDao()
    private val scheduleDao = database.scheduleDao()
    private val availabilityDao = database.availabilityDao()
    private val eventTypeDao = database.eventTypeDao()
    private val bookingDao = database.bookingDao()
    private val reminderDao = database.reminderDao()
    private val userPreferences = app.getupcoming.core.prefs.UserPreferences(context)

    /** True when the app is running the demo persona (no signed-in account).
     *  Demo seed data (Alex Rivera & co.) must never exist in a real user's
     *  session, so seeding and demo fallbacks are gated on this. */
    fun isDemoSession(): Boolean = authTokens?.let { it.isDemo() && !it.isLoggedIn() } ?: true

    /** The server identity. Starts at the local seed id and re-points to the
     *  /me user as soon as a refresh lands, so every "primary user" consumer
     *  follows the real account. */
    private val primaryUserId = kotlinx.coroutines.flow.MutableStateFlow(1L)

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

    // -----------------------------------------------------------------------
    // Remote refresh — network-first, Room as the offline cache. Flows above
    // stay Room-backed, so the UI keeps rendering from cache when offline.
    // -----------------------------------------------------------------------

    /** Pull event types from the API into Room (full-replace: local leftovers
     *  not known remotely are deleted, so stale locally-seeded ids can never
     *  reach the server). Returns true when fresh data landed. */
    suspend fun refreshEventTypes(): Boolean = withContext(Dispatchers.IO) {
        val api = api ?: return@withContext false
        try {
            val remote = apiCall { api.getEventTypes() }
            val remoteIds = remote.map { it.id }.toSet()
            for (dto in remote) {
                val existing = eventTypeDao.getEventTypeById(dto.id)
                val entity = dto.toEntity()
                if (existing != null) {
                    eventTypeDao.updateEventType(entity)
                } else {
                    eventTypeDao.insertEventType(entity)
                }
            }
            // Purge locally-seeded leftovers whose ids the server doesn't know.
            val localIds = eventTypeDao.getAllEventTypesFlow().first().map { it.id }
            for (id in localIds) {
                if (id !in remoteIds) eventTypeDao.deleteEventTypeById(id)
            }
            true
        } catch (e: Exception) {
            if (!e.isNetworkError()) throw e
            false
        }
    }

    /** Pull bookings from the API into Room (upsert by uid). */
    suspend fun refreshBookings(): Boolean = withContext(Dispatchers.IO) {
        val api = api ?: return@withContext false
        try {
            val remote = apiCall { api.getBookings() }
            for (row in remote) {
                upsertBookingRow(row)
            }
            true
        } catch (e: Exception) {
            if (!e.isNetworkError()) throw e
            false
        }
    }

    /** Pull the user settings profile from the API into Room (upsert by id).
     *  Returns true when fresh data landed. */
    /** Called right after a real sign-up/login (and on cold start of a
     *  restored session). Purges any demo data that leaked into the cache
     *  (flagged, or detected from earlier builds) and re-points identity at
     *  the authenticated user. */
    suspend fun onSessionEstablished() = withContext(Dispatchers.IO) {
        val demoRowsPresent = userDao.getAllUsers().any {
            it.email == "alex.rivera@upcoming.io" || it.email == "sarah.chen@upcoming.io"
        }
        if (authTokens?.demoDataSeeded() == true || demoRowsPresent) {
            database.clearAllTables()
            authTokens?.markDemoDataSeeded(false)
        }
        authTokens?.lastUserId()?.takeIf { it > 0 }?.let { primaryUserId.value = it }
        runCatching { refreshMe() }
    }

    suspend fun refreshMe(): Boolean = withContext(Dispatchers.IO) {
        val api = api ?: return@withContext false
        try {
            val me = apiCall { api.getMe() }
            storeRemoteUser(me)
            primaryUserId.value = me.id
            me.metadata.prefs?.reminderOffsets?.let { hydrateReminderOffsets(it) }
            true
        } catch (e: Exception) {
            if (!e.isNetworkError()) throw e
            false
        }
    }

    private suspend fun storeRemoteUser(me: MeResponseDto) {
        val existing = userDao.getUserById(me.id)
        userDao.insertUser(
            UserEntity(
                id = me.id,
                email = me.email,
                username = me.username,
                displayName = me.displayName,
                timezone = me.timezone,
                avatarUrl = me.avatarUrl,
                metadata = UpcomingApiClient.moshi
                    .adapter(UserMetadataDto::class.java)
                    .toJson(me.metadata)
            )
        )
        me.schedule?.let { schedule ->
            val local = scheduleDao.getScheduleByUserId(me.id)
            val entity = ScheduleEntity(
                id = local?.id ?: schedule.id,
                userId = me.id,
                name = schedule.name,
                timezone = schedule.timezone
            )
            if (local != null) scheduleDao.updateSchedule(entity) else scheduleDao.insertSchedule(entity)
        }
    }

    /** Registers/refreshes the FCM push token in metadata.fcmToken
     *  (api-contract 4.4). No-op when signed out, in demo, or when
     *  the server already has this token. Soft-fail: never throws. */
    suspend fun registerFcmToken(token: String): Boolean =
        withContext(Dispatchers.IO) {
            val tokens = authTokens ?: return@withContext false
            if (!tokens.isLoggedIn() || tokens.isDemo()) return@withContext false
            val current = currentMetadata()
            if (current.fcmToken == token) return@withContext true
            runCatching {
                updateProfile(metadata = current.copy(fcmToken = token))
            }.isSuccess
        }

    /** Server-backed profile update: PATCH /me then mirror into Room. */
    suspend fun updateProfile(
        displayName: String? = null,
        avatarUrl: String? = null,
        email: String? = null,
        username: String? = null,
        metadata: UserMetadataDto? = null
    ): User = withContext(Dispatchers.IO) {
        val api = api ?: throw ApiException.Server("offline")
        val me = apiCall { api.patchMe(PatchMeRequest(displayName, avatarUrl, email, username, null, metadata)) }
        storeRemoteUser(me)
        me.toDomainUser()
    }

    /** Timezone change: PATCH /me/schedule keeps schedules.timezone (the
     *  availability source of truth) and users.timezone in lockstep
     *  server-side; we mirror both writes into Room. */
    suspend fun updateTimezone(newTimezone: String): User = withContext(Dispatchers.IO) {
        val api = api ?: throw ApiException.Server("offline")
        val me = apiCall { api.patchSchedule(PatchScheduleRequest(timezone = newTimezone)) }
        storeRemoteUser(me)
        me.toDomainUser()
    }

    /** Convenience wrapper: user-level default location lives in metadata. */
    suspend fun updateDefaultLocation(location: LocationDto?): User =
        withContext(Dispatchers.IO) {
            val current = currentMetadata()
            updateProfile(metadata = current.copy(defaultLocation = location))
        }

    /** Booking defaults: store/replace one configured location per type
     *  (each with its own label + value) and/or pick the default type. */
    suspend fun updateLocationDefault(
        type: String,
        location: LocationDto?,
        makeDefault: Boolean = false
    ): User = withContext(Dispatchers.IO) {
        val current = currentMetadata()
        val map = current.locations ?: app.getupcoming.core.network.LocationsMapDto()
        val updatedMap = when (type) {
            "integrations:daily" -> map.copy(daily = location)
            "inPerson" -> map.copy(inPerson = location)
            "userPhone" -> map.copy(userPhone = location)
            else -> map
        }
        updateProfile(
            metadata = current.copy(
                locations = updatedMap,
                defaultLocationType = if (makeDefault && location != null) type else current.defaultLocationType
            )
        )
    }

    suspend fun setDefaultLocationType(type: String): User =
        withContext(Dispatchers.IO) {
            val current = currentMetadata()
            updateProfile(metadata = current.copy(defaultLocationType = type))
        }

    suspend fun currentTimeFormatPref(): String? =
        runCatching { currentMetadata().prefs?.timeFormat }.getOrNull()

    /** User-level default location (from metadata) for prefilling new event
     *  types' location menus; null when none is set. Prefers the per-type
     *  defaults map, falls back to the legacy single defaultLocation field. */
    suspend fun defaultLocation(): LocationDto? = runCatching {
        val meta = currentMetadata()
        meta.defaultLocationType?.let { type -> meta.locations?.entryFor(type) } ?: meta.defaultLocation
    }.getOrNull()

    // --- Credentials (bring-your-own API keys / private URLs) ---------------

    suspend fun credentialHints(): List<app.getupcoming.core.network.CredentialHintDto> =
        withContext(Dispatchers.IO) {
            val api = api ?: return@withContext emptyList()
            try {
                apiCall { api.getCredentials() }
            } catch (e: Exception) {
                if (!e.isNetworkError()) throw e
                emptyList()
            }
        }

    suspend fun putCredential(type: String, value: String): app.getupcoming.core.network.CredentialHintDto =
        withContext(Dispatchers.IO) {
            val api = api ?: throw ApiException.Server("offline")
            apiCall { api.putCredential(type, app.getupcoming.core.network.PutCredentialRequest(value)) }
        }

    suspend fun deleteCredential(type: String) = withContext(Dispatchers.IO) {
        val api = api ?: throw ApiException.Server("offline")
        apiCall { api.deleteCredential(type) }
    }

    suspend fun setTimeFormatPref(timeFormat: String): User =
        updateProfile(
            metadata = currentMetadata().copy(
                prefs = (currentMetadata().prefs ?: UserPrefsDto(timeFormat)).copy(timeFormat = timeFormat)
            )
        )

    // --- Reminder settings (DataStore + metadata sync + alarm re-arm) -------

    /** Reminder settings change: persist device-locally (instant), re-arm all
     *  alarms for upcoming bookings, then best-effort sync the offsets into
     *  users.metadata.prefs so they follow the account across reinstalls. */
    suspend fun updateReminderSettings(enabled: Boolean, offsets: List<Int>): Boolean =
        withContext(Dispatchers.IO) {
            val normalized = offsets.filter { it in 1..10080 }.distinct().sorted().take(5)
                .ifEmpty { app.getupcoming.core.prefs.DEFAULT_REMINDER_OFFSETS }
            userPreferences.setReminderSettings(enabled, normalized)
            rescheduleAllReminders()
            try {
                val current = currentMetadata()
                val prefs = (current.prefs ?: UserPrefsDto("12h")).copy(reminderOffsets = normalized)
                updateProfile(metadata = current.copy(prefs = prefs))
                true
            } catch (e: Exception) {
                if (!e.isNetworkError()) throw e
                false
            }
        }

    /** Server → device: apply reminder offsets carried in metadata.prefs. */
    private suspend fun hydrateReminderOffsets(offsets: List<Int>) {
        if (offsets.isEmpty()) return
        userPreferences.setReminderSettings(
            userPreferences.notificationPrefs.first().remindersEnabled,
            offsets
        )
    }

    /** Cancel + re-arm every alarm for upcoming bookings under the current
     *  reminder settings (called after settings change or alarm-affecting
     *  data changes). */
    suspend fun rescheduleAllReminders() = withContext(Dispatchers.IO) {
        val prefs = userPreferences.notificationPrefs.first()
        val upcoming = bookingDao.getUpcomingBookingsFlow().first()
        for (entity in upcoming) {
            val booking = entity.toDomain()
            NotificationAndReminderManager.cancelBookingReminders(
                context, booking.uid,
                (prefs.reminderOffsets + app.getupcoming.core.prefs.REMINDER_PRESETS + listOf(15)).distinct()
            )
            if (!prefs.remindersEnabled) continue
            val eventType = eventTypeDao.getEventTypeById(entity.eventTypeId)?.toDomain() ?: continue
            val attendee = bookingDao.getAttendeeForBooking(entity.id)?.name
            NotificationAndReminderManager.scheduleBookingReminders(
                context = context,
                booking = booking,
                eventType = eventType,
                attendeeName = attendee,
                offsetsMinutes = prefs.reminderOffsets
            )
        }
    }

    // Device-local notification toggles (push + sound/vibration).

    val notificationPrefs: Flow<app.getupcoming.core.prefs.NotificationPrefs>
        get() = userPreferences.notificationPrefs

    suspend fun setPushAlertsEnabled(enabled: Boolean) =
        userPreferences.setPushAlertsEnabled(enabled)

    /** Sound & vibration reconfigures the notification channels, which is
     *  where Android actually applies it (per-toggle, not per-notification). */
    suspend fun setSoundVibrationEnabled(enabled: Boolean) {
        userPreferences.setSoundVibrateEnabled(enabled)
        NotificationAndReminderManager.setupChannels(context, enabled)
    }

    private suspend fun currentMetadata(): UserMetadataDto {
        val user = userDao.getUserById(primaryUserId.value)
        val raw = user?.metadata ?: "{}"
        return runCatching {
            UpcomingApiClient.moshi
                .adapter(UserMetadataDto::class.java).fromJson(raw)
        }.getOrNull() ?: UserMetadataDto()
    }

    private fun app.getupcoming.core.network.MeResponseDto.toDomainUser(): User = User(
        id = id,
        email = email,
        username = username,
        displayName = displayName,
        timezone = timezone,
        avatarUrl = avatarUrl,
        metadata = UpcomingApiClient.moshi
            .adapter(UserMetadataDto::class.java).toJson(metadata)
    )

    private suspend fun upsertBookingRow(row: BookingRowDto) {
        val existing = bookingDao.getBookingByUid(row.uid)
        val entity = BookingEntity(
            id = existing?.id ?: 0,
            uid = row.uid,
            eventTypeId = row.eventTypeId,
            hostUserId = row.hostUserId,
            startTimeUtc = row.startTimeUtc,
            endTimeUtc = row.endTimeUtc,
            bufferBefore = row.bufferBefore,
            bufferAfter = row.bufferAfter,
            status = row.status,
            cancelledAt = row.cancelledAt,
            idempotencyKey = row.idempotencyKey.ifBlank { existing?.idempotencyKey ?: row.uid },
            locationJson = row.location ?: existing?.locationJson,
            paid = row.paid,
            paymentIntentId = row.paymentIntentId,
            createdAtUtc = row.createdAt ?: existing?.createdAtUtc ?: ""
        )
        if (existing != null) {
            bookingDao.updateBooking(entity)
        } else {
            bookingDao.insertBooking(entity)
        }
    }

    private fun EventTypeDto.toEntity(): EventTypeEntity = EventTypeEntity(
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
        locationsJson = locations,
        minBookingNotice = minBookingNotice,
        priceInCents = priceInCents,
        currency = currency,
        colorHex = colorHex,
        isActive = isActive
    )

    suspend fun getPrimaryUser(): User {
        val userEntity = userDao.getUserById(primaryUserId.value)
        // Neutral placeholder, never a demo persona — a signed-in user must
        // not see Alex Rivera leaked from the seed data.
        return userEntity?.toDomain() ?: User(
            id = primaryUserId.value,
            email = "",
            username = "",
            displayName = "",
            timezone = "UTC"
        )
    }

    @kotlinx.coroutines.ExperimentalCoroutinesApi
    fun getPrimaryUserFlow(): Flow<User?> {
        return primaryUserId.flatMapLatest { id ->
            userDao.getUserByIdFlow(id).map { it?.toDomain() }
        }
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

    /**
     * Create/update an event type. Remote-first when an API client is
     * configured: the server owns ids, slug-uniqueness (409 on conflict) and
     * the uniform host row, and the stored DTO mirrors back into Room.
     * Demo/offline sessions (no API) write Room only. Returns the stored id.
     */
    suspend fun saveEventType(eventType: EventType): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val api = api ?: run {
                // Demo path: local-only write, exactly as before remote sync.
                val entity = eventType.toEntity()
                val id = if (eventType.id == 0L) {
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
                return@withContext Result.success(id)
            }

            val dto = if (eventType.id == 0L) {
                apiCall {
                    api.createEventType(
                        CreateEventTypeRequest(
                            slug = eventType.slug,
                            title = eventType.title,
                            description = eventType.description,
                            lengthMinutes = eventType.lengthMinutes,
                            slotIntervalMinutes = eventType.slotIntervalMinutes,
                            bufferBefore = eventType.bufferBefore,
                            bufferAfter = eventType.bufferAfter,
                            schedulingType = eventType.schedulingType,
                            locations = eventType.locationsJson,
                            minBookingNotice = eventType.minBookingNotice,
                            priceInCents = eventType.priceInCents,
                            currency = eventType.currency,
                            colorHex = eventType.colorHex,
                            isActive = eventType.isActive
                        )
                    )
                }
            } else {
                apiCall {
                    api.updateEventType(
                        eventType.id,
                        UpdateEventTypeRequest(
                            slug = eventType.slug,
                            title = eventType.title,
                            description = eventType.description,
                            lengthMinutes = eventType.lengthMinutes,
                            slotIntervalMinutes = eventType.slotIntervalMinutes,
                            bufferBefore = eventType.bufferBefore,
                            bufferAfter = eventType.bufferAfter,
                            schedulingType = eventType.schedulingType,
                            locations = eventType.locationsJson,
                            minBookingNotice = eventType.minBookingNotice,
                            priceInCents = eventType.priceInCents,
                            currency = eventType.currency,
                            colorHex = eventType.colorHex,
                            isActive = eventType.isActive
                        )
                    )
                }
            }
            Result.success(storeRemoteEventType(dto))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Upserts a server event-type DTO into Room and returns its id. */
    private suspend fun storeRemoteEventType(dto: EventTypeDto): Long {
        val entity = dto.toEntity()
        val existing = eventTypeDao.getEventTypeById(dto.id)
        if (existing != null) {
            eventTypeDao.updateEventType(entity)
        } else {
            eventTypeDao.insertEventType(entity)
        }
        // Keep local host rows in parity with the server's uniform host model.
        for (hostId in dto.hostUserIds) {
            val already = eventTypeDao.getHostsForEventType(dto.id).any { it.hostUserId == hostId }
            if (!already) {
                eventTypeDao.insertEventTypeHost(
                    EventTypeHostEntity(eventTypeId = dto.id, hostUserId = hostId, priority = 0)
                )
            }
        }
        return dto.id
    }

    /**
     * Deletes an event type. Remote-first: the server refuses (409) when
     * bookings reference the type — surface that instead of deleting locally;
     * a 404 (already gone server-side) still reconciles the local cache.
     * Demo/offline sessions delete Room only.
     */
    suspend fun deleteEventType(id: Long): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            if (api != null) {
                try {
                    apiCall { api.deleteEventType(id) }
                } catch (e: ApiException.NotFound) {
                    // Server never knew it (or it was already deleted) — the
                    // local row is stale either way, so purge it below.
                }
            }
            eventTypeDao.deleteEventTypeById(id)
            eventTypeDao.deleteHostsByEventTypeId(id)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
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
        val outTz = TimeZone.getTimeZone(inviteeTimezone)
        val timeDisplayFormat = java.text.SimpleDateFormat("h:mm a", Locale.US).apply {
            timeZone = outTz
        }

        // Network-first: the server's availability engine owns DST/min-notice/
        // slot-grid logic; Room only serves as fallback when the API is down.
        if (api != null) {
            try {
                val response = apiCall {
                    api.getAvailability(
                        eventTypeId = eventType.id,
                        rangeStartUtc = SchedulingEngine.formatIsoUtc(rangeStartUtc),
                        rangeEndUtc = SchedulingEngine.formatIsoUtc(rangeEndUtc)
                    )
                }
                return@withContext response.slots.map { slot ->
                    OfferedSlot(
                        startUtc = slot.startUtc,
                        endUtc = slot.endUtc,
                        schedulingType = slot.schedulingType,
                        attendingHostUserIds = slot.attendingHostUserIds ?: emptyList(),
                        displayLocalTime = timeDisplayFormat.format(SchedulingEngine.parseIsoUtc(slot.startUtc))
                    )
                }
            } catch (e: Exception) {
                if (!e.isNetworkError()) {
                    // A definitive server answer (validation, not found) should
                    // not silently degrade to the local engine's view.
                    throw e
                }
                // else: fall through to offline fallback below
            }
        }

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
            // Replay within the local cache first (works offline).
            val existing = bookingDao.getBookingByIdempotencyKey(idempotencyKey)
            if (existing != null) {
                return@withContext Result.success(existing.toDomain())
            }

            val eventType = eventTypeDao.getEventTypeById(eventTypeId)?.toDomain()
                ?: return@withContext Result.failure(Exception("Event type not found"))

            // Remote-first write: the handler owns slot-grid/min-notice/buffer
            // conflict logic and Daily room minting. Local writes are only
            // used when no API client is configured.
            if (api != null) {
                val result = apiCall {
                    api.createBooking(
                        CreateBookingRequest(
                            eventTypeId = eventTypeId,
                            slotStartUtc = slotStartUtc,
                            slotEndUtc = slotEndUtc,
                            location = parseChosenLocation(locationJson),
                            attendee = AttendeeWireDto(
                                email = attendeeEmail,
                                name = attendeeName,
                                timezone = attendeeTimezone,
                                phone = attendeePhone
                            ),
                            idempotencyKey = idempotencyKey
                        )
                    )
                }
                val createdBooking = storeRemoteBooking(result, eventType)
                schedulePostBookingNotifications(createdBooking, eventType, attendeeName, attendeeEmail)
                return@withContext Result.success(createdBooking)
            }

            val uid = UUID.randomUUID().toString().replace("-", "").take(16)

            val bookingEntity = BookingEntity(
                uid = uid,
                eventTypeId = eventTypeId,
                hostUserId = eventType.ownerUserId,
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
            schedulePostBookingNotifications(createdBooking, eventType, attendeeName, attendeeEmail)

            Result.success(createdBooking)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Alarm + reminder-row + FCM-style notification, shared by the remote and
     *  local booking paths. */
    private suspend fun schedulePostBookingNotifications(
        booking: Booking,
        eventType: EventType,
        attendeeName: String?,
        attendeeEmail: String
    ) {
        val prefs = userPreferences.notificationPrefs.first()
        // One exact alarm per configured reminder offset (honors the device
        // reminder settings from Settings → Notifications).
        if (prefs.remindersEnabled) {
            NotificationAndReminderManager.scheduleBookingReminders(
                context = context,
                booking = booking,
                eventType = eventType,
                attendeeName = attendeeName,
                offsetsMinutes = prefs.reminderOffsets
            )

            // Record one reminder row per offset (bookkeeping for the
            // reminders list; AlarmManager is the actual trigger).
            val startMs = SchedulingEngine.parseIsoUtc(booking.startTimeUtc).time
            for (offset in prefs.reminderOffsets) {
                reminderDao.insertReminder(
                    ReminderEntity(
                        bookingId = booking.id,
                        type = "exact_alarm",
                        triggerTimeUtc = SchedulingEngine.formatIsoUtc(Date(startMs - (offset * 60 * 1000L))),
                        title = "Upcoming: ${eventType.title}",
                        body = "Meeting with ${attendeeName ?: attendeeEmail} starting in ${NotificationAndReminderManager.formatOffset(offset)}",
                        status = "scheduled",
                        isFired = false,
                        createdTimeUtc = SchedulingEngine.formatIsoUtc(Date())
                    )
                )
            }
        }

        // Trigger simulated real-time FCM notification
        if (prefs.pushAlertsEnabled) {
            NotificationAndReminderManager.triggerFcmNotification(
                context = context,
                title = "New Booking Confirmed!",
                body = "${attendeeName ?: attendeeEmail} booked ${eventType.title} on ${booking.startTimeUtc.take(10)}",
                bookingUid = booking.uid
            )
        }
    }

    /** Persists a handler BookingResult into Room (booking + attendee rows) and
     *  returns it as a domain Booking. */
    private suspend fun storeRemoteBooking(
        result: BookingResultDto,
        eventType: EventType
    ): Booking {
        val existing = bookingDao.getBookingByUid(result.uid)
        val entity = BookingEntity(
            id = existing?.id ?: 0,
            uid = result.uid,
            eventTypeId = result.eventTypeId,
            hostUserId = result.hostUserId,
            startTimeUtc = result.startUtc,
            endTimeUtc = result.endUtc,
            bufferBefore = eventType.bufferBefore,
            bufferAfter = eventType.bufferAfter,
            status = result.status,
            cancelledAt = null,
            idempotencyKey = existing?.idempotencyKey ?: result.uid,
            locationJson = result.location?.let { locationJson(it) } ?: existing?.locationJson,
            paid = existing?.paid ?: false,
            paymentIntentId = existing?.paymentIntentId,
            createdAtUtc = existing?.createdAtUtc ?: SchedulingEngine.formatIsoUtc(Date())
        )
        val bookingId = if (existing != null) {
            bookingDao.updateBooking(entity)
            existing.id
        } else {
            bookingDao.insertBooking(entity)
        }

        result.attendee?.let { attendee ->
            val existingAttendee = bookingDao.getAttendeeForBooking(bookingId)
            if (existingAttendee == null) {
                bookingDao.insertAttendee(
                    AttendeeEntity(
                        bookingId = bookingId,
                        email = attendee.email,
                        name = attendee.name,
                        timezone = attendee.timezone,
                        phone = attendee.phone,
                        notes = null
                    )
                )
            }
        }

        return entity.copy(id = bookingId).toDomain()
    }

    /** Parses the CHOSEN-location JSON (a single object) into the wire shape. */
    private fun parseChosenLocation(locationJson: String): LocationDto {
        val adapter = UpcomingApiClient.moshi
            .adapter(LocationDto::class.java).lenient()
        return adapter.fromJson(locationJson)
            ?: throw IllegalArgumentException("Invalid location JSON: $locationJson")
    }

    private fun locationJson(location: LocationDto): String {
        val adapter = UpcomingApiClient.moshi
            .adapter(LocationDto::class.java).lenient()
        return adapter.toJson(location)
    }

    suspend fun cancelBooking(uid: String, reason: String = "Cancelled by user"): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val booking = bookingDao.getBookingByUid(uid) ?: return@withContext Result.failure(Exception("Booking not found"))

            // Remote-first: the handler prunes occupancy ticks and deletes
            // minted Daily rooms in the same transaction. 404 means the
            // booking never existed server-side (or was already pruned) —
            // safe to reconcile the local cache either way.
            if (api != null) {
                try {
                    apiCall { api.cancelBooking(app.getupcoming.core.network.CancelBookingRequest(uid = uid)) }
                } catch (e: ApiException.NotFound) {
                    // fall through to local reconcile
                }
            }

            val nowUtc = SchedulingEngine.formatIsoUtc(Date())
            bookingDao.cancelBookingByUid(uid, nowUtc)

            // Cancel every alarm armed for this booking (superset of offsets
            // that may have been used when it was created).
            NotificationAndReminderManager.cancelBookingReminders(
                context, uid,
                (userPreferences.notificationPrefs.first().reminderOffsets +
                    app.getupcoming.core.prefs.REMINDER_PRESETS + listOf(15)).distinct()
            )
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

    // -----------------------------------------------------------------------
    // Payments (book → pay → mark-paid). Card data goes device → Stripe
    // directly (publishable key only); the server verifies the PaymentIntent
    // before flipping `paid`.
    // -----------------------------------------------------------------------

    suspend fun createPaymentIntent(eventTypeId: Long): Result<app.getupcoming.core.network.CreateIntentResponse> =
        withContext(Dispatchers.IO) {
            runCatching { apiCall { api!!.createPaymentIntent(app.getupcoming.core.network.CreateIntentRequest(eventTypeId)) } }
        }

    /** Creates a PaymentMethod from raw card fields (tokenized client-side by
     *  the Stripe SDK) and confirms the PaymentIntent. Returns the PI id. */
    suspend fun confirmStripePayment(
        clientSecret: String,
        cardNumber: String,
        expMonth: Int,
        expYear: Int,
        cvc: String,
        cardholderName: String?
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val stripe = com.stripe.android.Stripe(context, app.getupcoming.BuildConfig.STRIPE_PUBLISHABLE_KEY)
            val card = com.stripe.android.model.PaymentMethodCreateParams.Card.Builder()
                .setNumber(cardNumber.replace(" ", ""))
                .setExpiryMonth(expMonth)
                .setExpiryYear(expYear)
                .setCvc(cvc)
                .build()
            val params = com.stripe.android.model.PaymentMethodCreateParams.create(
                card = card,
                billingDetails = com.stripe.android.model.PaymentMethod.BillingDetails(name = cardholderName)
            )
            val paymentMethod = stripe.createPaymentMethodSynchronous(params)
            val confirmParams = com.stripe.android.model.ConfirmPaymentIntentParams
                .createWithPaymentMethodId(paymentMethod.id!!, clientSecret)
            val intent = stripe.confirmPaymentIntentSynchronous(confirmParams)
            if (intent.status != com.stripe.android.model.StripeIntent.Status.Succeeded) {
                throw IllegalStateException("Payment not completed (status: ${intent.status})")
            }
            intent.id ?: throw IllegalStateException("Payment succeeded without an id")
        }
    }

    suspend fun markBookingPaid(uid: String, paymentIntentId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                apiCall { api!!.markBookingPaidInternal(uid, paymentIntentId) }
                val booking = bookingDao.getBookingByUid(uid)
                if (booking != null) {
                    bookingDao.updateBooking(
                        booking.copy(paid = true, paymentIntentId = paymentIntentId)
                    )
                }
            }
        }

    private suspend fun app.getupcoming.core.network.UpcomingApi.markBookingPaidInternal(uid: String, paymentIntentId: String) =
        markPaid(app.getupcoming.core.network.MarkPaidRequest(uid, paymentIntentId))

    suspend fun seedInitialDataIfEmpty() = withContext(Dispatchers.IO) {
        // Demo content only ever exists in a demo session. A signed-in user
        // gets a clean, server-synced cache — no Alex Rivera, ever.
        if (!isDemoSession()) return@withContext
        val existingUsers = userDao.getAllUsers()
        if (existingUsers.isNotEmpty()) return@withContext
        authTokens?.markDemoDataSeeded(true)

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

        // Demo reminder row (bookkeeping only — real reminders arm via
        // schedulePostBookingNotifications)
        reminderDao.insertReminder(
            ReminderEntity(
                bookingId = b1Id,
                type = "exact_alarm",
                triggerTimeUtc = booking1Start,
                title = "Upcoming: 30 Min Product Walkthrough",
                body = "Meeting with Jordan Taylor starts in ${NotificationAndReminderManager.formatOffset(15)}.",
                status = "scheduled",
                isFired = false,
                createdTimeUtc = SchedulingEngine.formatIsoUtc(Date())
            )
        )
    }

    // -----------------------------------------------------------------------
    // Single-use booking links (Calendly-style). Server state only — no Room
    // cache, the Scheduling screen always fetches fresh.
    // -----------------------------------------------------------------------

    suspend fun createSingleUseLinks(
        eventTypeId: Long,
        count: Int = 1,
        expiresInDays: Int? = null
    ): List<SingleUseLinkDto>? = withContext(Dispatchers.IO) {
        val api = api ?: return@withContext null
        try {
            apiCall { api.createSingleUseLinks(CreateSingleUseLinksRequest(eventTypeId, count, expiresInDays)) }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getSingleUseLinks(eventTypeId: Long): List<SingleUseLinkDto>? =
        withContext(Dispatchers.IO) {
            val api = api ?: return@withContext null
            try {
                apiCall { api.getSingleUseLinks(eventTypeId) }
            } catch (e: Exception) {
                null
            }
        }

    suspend fun revokeSingleUseLink(id: Long): Boolean = withContext(Dispatchers.IO) {
        val api = api ?: return@withContext false
        try {
            apiCall { api.revokeSingleUseLink(id) }
            true
        } catch (e: Exception) {
            false
        }
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
