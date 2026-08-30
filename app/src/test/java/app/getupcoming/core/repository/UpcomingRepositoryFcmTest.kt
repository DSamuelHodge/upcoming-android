package app.getupcoming.core.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import app.getupcoming.core.auth.AuthTokenManager
import app.getupcoming.core.database.UpcomingDatabase
import app.getupcoming.core.database.entity.UserEntity
import app.getupcoming.core.network.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/** registerFcmToken metadata-wipe guard (CodeRabbit finding): with no cached
 *  user row, the /me refresh hasn't landed yet — the PATCH must carry the
 *  fetched real metadata (or not happen at all), never empty+fcmToken. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class UpcomingRepositoryFcmTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var db: UpcomingDatabase
    private lateinit var auth: AuthTokenManager
    private lateinit var api: FakeUpcomingApi

    private val realMetadata = UserMetadataDto(
        defaultLocation = LocationDto(type = "custom", label = "Office"),
        locations = LocationsMapDto(daily = LocationDto(type = "custom", label = "Daily room")),
        defaultLocationType = "integrations:daily",
        prefs = UserPrefsDto(timeFormat = "24h", reminderOffsets = listOf(10, 60))
    )

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, UpcomingDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        auth = AuthTokenManager(context) {
            context.getSharedPreferences("upcoming_auth_test", Context.MODE_PRIVATE)
        }
        auth.saveTokens("test-access", "test-refresh")
        api = FakeUpcomingApi()
    }

    @After
    fun tearDown() {
        db.close()
        auth.clear()
    }

    private fun repository(): UpcomingRepository =
        UpcomingRepository(db, context, api, auth)

    private suspend fun insertUserWithMetadata(metadata: UserMetadataDto) {
        db.userDao().insertUser(
            UserEntity(
                id = 1,
                email = "alex.rivera@upcoming.io",
                username = "alex",
                displayName = "Alex Rivera",
                timezone = "UTC",
                avatarUrl = "",
                metadata = UpcomingApiClient.moshi
                    .adapter(UserMetadataDto::class.java).toJson(metadata)
            )
        )
    }

    private fun patchedMetadata(): UserMetadataDto? = api.patchRequests.last().metadata

    @Test
    fun `no cached row - refreshes me first and PATCH carries real metadata`() = runTest {
        api.meResponse = MeResponseDto(
            id = 1, email = "alex.rivera@upcoming.io", username = "alex",
            timezone = "UTC", metadata = realMetadata
        )
        assertTrue(repository().registerFcmToken("tok-123"))

        assertEquals(1, api.getMeCalls)
        assertEquals(1, api.patchRequests.size)
        val sent = patchedMetadata()
        assertEquals("tok-123", sent?.fcmToken)
        assertEquals(realMetadata.prefs, sent?.prefs)
        assertEquals(realMetadata.locations, sent?.locations)
        assertEquals(realMetadata.defaultLocation, sent?.defaultLocation)
        assertEquals(realMetadata.defaultLocationType, sent?.defaultLocationType)

        // Room now mirrors the merged metadata.
        val row = db.userDao().getUserById(1)
        assertEquals(
            "tok-123",
            UpcomingApiClient.moshi.adapter(UserMetadataDto::class.java)
                .fromJson(row!!.metadata)?.fcmToken
        )
    }

    @Test
    fun `no cached row and me refresh offline - skips PATCH, never wipes`() = runTest {
        api.getMeError = IOException("offline")
        assertFalse(repository().registerFcmToken("tok-123"))

        assertEquals(1, api.getMeCalls)
        assertTrue(api.patchRequests.isEmpty())
    }

    @Test
    fun `no cached row and me refresh errors - soft-fail, no PATCH`() = runTest {
        api.getMeError = ApiException.Server("boom")
        assertFalse(repository().registerFcmToken("tok-123"))

        assertEquals(1, api.getMeCalls)
        assertTrue(api.patchRequests.isEmpty())
    }

    @Test
    fun `cached row present - PATCH preserves metadata without an extra refresh`() = runTest {
        insertUserWithMetadata(realMetadata)
        assertTrue(repository().registerFcmToken("tok-123"))

        assertEquals(0, api.getMeCalls)
        assertEquals(1, api.patchRequests.size)
        val sent = patchedMetadata()
        assertEquals("tok-123", sent?.fcmToken)
        assertEquals(realMetadata.prefs, sent?.prefs)
        assertEquals(realMetadata.locations, sent?.locations)
    }

    @Test
    fun `server already has the token - dedupe, no PATCH`() = runTest {
        insertUserWithMetadata(realMetadata.copy(fcmToken = "tok-123"))
        assertTrue(repository().registerFcmToken("tok-123"))

        assertEquals(0, api.getMeCalls)
        assertTrue(api.patchRequests.isEmpty())
    }

    @Test
    fun `signed out - no network at all`() = runTest {
        auth.clear()
        assertFalse(repository().registerFcmToken("tok-123"))

        assertEquals(0, api.getMeCalls)
        assertTrue(api.patchRequests.isEmpty())
    }
}

/** Only /me traffic is exercised; everything else must stay untouched. */
private class FakeUpcomingApi : UpcomingApi {
    var meResponse: MeResponseDto? = null
    var getMeError: Throwable? = null
    var getMeCalls = 0
    val patchRequests = mutableListOf<PatchMeRequest>()

    override suspend fun getMe(userId: Long?): MeResponseDto {
        getMeCalls++
        getMeError?.let { throw it }
        return meResponse ?: throw IllegalStateException("no meResponse staged")
    }

    override suspend fun patchMe(body: PatchMeRequest): MeResponseDto {
        patchRequests.add(body)
        return MeResponseDto(
            id = 1,
            email = "alex.rivera@upcoming.io",
            username = "alex",
            timezone = "UTC",
            metadata = body.metadata ?: UserMetadataDto()
        )
    }

    private fun unused(): Nothing = throw UnsupportedOperationException("unused in FCM tests")

    override suspend fun getEventTypes(): List<EventTypeDto> = unused()
    override suspend fun createEventType(body: CreateEventTypeRequest): EventTypeDto = unused()
    override suspend fun updateEventType(id: Long, body: UpdateEventTypeRequest): EventTypeDto = unused()
    override suspend fun deleteEventType(id: Long): DeleteEventTypeResponse = unused()
    override suspend fun getAvailability(
        eventTypeId: Long,
        rangeStartUtc: String,
        rangeEndUtc: String
    ): AvailabilityResponseDto = unused()
    override suspend fun getBookings(
        from: String?,
        to: String?,
        activeOnly: Boolean?
    ): List<BookingRowDto> = unused()
    override suspend fun getBooking(uid: String): BookingDetailDto = unused()
    override suspend fun createBooking(body: CreateBookingRequest): BookingResultDto = unused()
    override suspend fun cancelBooking(body: CancelBookingRequest): BookingResultDto = unused()
    override suspend fun createPaymentIntent(body: CreateIntentRequest): CreateIntentResponse = unused()
    override suspend fun markPaid(body: MarkPaidRequest): MarkPaidResponse = unused()
    override suspend fun patchSchedule(body: PatchScheduleRequest): MeResponseDto = unused()
    override suspend fun getCredentials(): List<CredentialHintDto> = unused()
    override suspend fun putCredential(type: String, body: PutCredentialRequest): CredentialHintDto = unused()
    override suspend fun deleteCredential(type: String): DeleteCredentialResponse = unused()
    override suspend fun signUp(body: SignUpRequest): AuthResponse = unused()
    override suspend fun login(body: LoginRequest): AuthResponse = unused()
    override suspend fun refresh(body: RefreshRequest): AuthResponse = unused()
    override suspend fun logout(body: RefreshRequest): Map<String, Boolean> = unused()
    override suspend fun createSingleUseLinks(
        body: CreateSingleUseLinksRequest
    ): List<SingleUseLinkDto> = unused()
    override suspend fun getSingleUseLinks(eventTypeId: Long): List<SingleUseLinkDto> = unused()
    override suspend fun revokeSingleUseLink(id: Long): RevokeSingleUseLinkResponse = unused()
}
