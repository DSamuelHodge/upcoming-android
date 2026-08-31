package app.getupcoming.core.network

import app.getupcoming.BuildConfig
import com.squareup.moshi.Moshi
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

interface UpcomingApi {
    @retrofit2.http.GET("event-types")
    suspend fun getEventTypes(): List<EventTypeDto>

    @retrofit2.http.POST("event-types")
    suspend fun createEventType(@retrofit2.http.Body body: CreateEventTypeRequest): EventTypeDto

    @retrofit2.http.PATCH("event-types/{id}")
    suspend fun updateEventType(
        @retrofit2.http.Path("id") id: Long,
        @retrofit2.http.Body body: UpdateEventTypeRequest
    ): EventTypeDto

    @retrofit2.http.DELETE("event-types/{id}")
    suspend fun deleteEventType(@retrofit2.http.Path("id") id: Long): DeleteEventTypeResponse

    @retrofit2.http.GET("availability")
    suspend fun getAvailability(
        @retrofit2.http.Query("eventTypeId") eventTypeId: Long,
        @retrofit2.http.Query("rangeStartUtc") rangeStartUtc: String,
        @retrofit2.http.Query("rangeEndUtc") rangeEndUtc: String
    ): AvailabilityResponseDto

    @retrofit2.http.GET("bookings")
    suspend fun getBookings(
        @retrofit2.http.Query("from") from: String? = null,
        @retrofit2.http.Query("to") to: String? = null,
        @retrofit2.http.Query("activeOnly") activeOnly: Boolean? = null
    ): List<BookingRowDto>

    @retrofit2.http.GET("bookings/{uid}")
    suspend fun getBooking(@retrofit2.http.Path("uid") uid: String): BookingDetailDto

    @retrofit2.http.POST("bookings")
    suspend fun createBooking(@retrofit2.http.Body body: CreateBookingRequest): BookingResultDto

    @retrofit2.http.POST("bookings/cancel")
    suspend fun cancelBooking(@retrofit2.http.Body body: CancelBookingRequest): BookingResultDto

    @retrofit2.http.POST("payments/create-intent")
    suspend fun createPaymentIntent(@retrofit2.http.Body body: CreateIntentRequest): CreateIntentResponse

    @retrofit2.http.POST("payments/mark-paid")
    suspend fun markPaid(@retrofit2.http.Body body: MarkPaidRequest): MarkPaidResponse

    @retrofit2.http.GET("me")
    suspend fun getMe(@retrofit2.http.Query("userId") userId: Long? = null): MeResponseDto

    @retrofit2.http.PATCH("me")
    suspend fun patchMe(@retrofit2.http.Body body: PatchMeRequest): MeResponseDto

    @retrofit2.http.PATCH("me/schedule")
    suspend fun patchSchedule(@retrofit2.http.Body body: PatchScheduleRequest): MeResponseDto

    @retrofit2.http.GET("me/credentials")
    suspend fun getCredentials(): List<CredentialHintDto>

    @retrofit2.http.PUT("me/credentials/{type}")
    suspend fun putCredential(
        @retrofit2.http.Path("type") type: String,
        @retrofit2.http.Body body: PutCredentialRequest
    ): CredentialHintDto

    @retrofit2.http.DELETE("me/credentials/{type}")
    suspend fun deleteCredential(@retrofit2.http.Path("type") type: String): DeleteCredentialResponse

    // Auth — open routes minting the JWT pair.

    @retrofit2.http.POST("auth/signup")
    suspend fun signUp(@retrofit2.http.Body body: SignUpRequest): AuthResponse

    @retrofit2.http.POST("auth/login")
    suspend fun login(@retrofit2.http.Body body: LoginRequest): AuthResponse

    @retrofit2.http.POST("auth/refresh")
    suspend fun refresh(@retrofit2.http.Body body: RefreshRequest): AuthResponse

    @retrofit2.http.POST("auth/logout")
    suspend fun logout(@retrofit2.http.Body body: RefreshRequest): Map<String, Boolean>

    // Single-use booking links (Calendly-style one-time links).

    @retrofit2.http.POST("single-use-links")
    suspend fun createSingleUseLinks(@retrofit2.http.Body body: CreateSingleUseLinksRequest): List<SingleUseLinkDto>

    @retrofit2.http.GET("single-use-links")
    suspend fun getSingleUseLinks(@retrofit2.http.Query("eventTypeId") eventTypeId: Long): List<SingleUseLinkDto>

    @retrofit2.http.DELETE("single-use-links/{id}")
    suspend fun revokeSingleUseLink(@retrofit2.http.Path("id") id: Long): RevokeSingleUseLinkResponse
}

object UpcomingApiClient {
    val moshi: Moshi = Moshi.Builder()
        .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
        .build()

    fun create(
        baseUrl: String = BuildConfig.UPCOMING_API_BASE_URL,
        auth: app.getupcoming.core.auth.AuthTokenManager? = null
    ): UpcomingApi {
        require(baseUrl.isNotBlank()) { "UPCOMING_API_BASE_URL is not configured (.env)" }

        // JWT-only (Phase 0.2): the legacy shared API secret never ships in the
        // app. Requests without a live token carry no Authorization header —
        // the only such calls are the server's open /auth/* routes. Demo mode
        // is fully local (UpcomingRepository.isDemoSession()) and never dials
        // the network.
        val normalizedBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        val authenticator = auth?.let { mgr ->
            okhttp3.Authenticator { _, response ->
                val hadJwt = response.request.header("Authorization")?.startsWith("Bearer ey") == true
                if (!hadJwt || response.priorResponse != null) return@Authenticator null
                if (!mgr.refreshAccessToken(normalizedBaseUrl)) return@Authenticator null
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${mgr.accessToken()}")
                    .build()
            }
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                // Attach a header only for a real JWT; never fall back to a
                // shared secret (none exists in the app anymore).
                auth?.accessToken()?.takeIf { it.isNotBlank() }?.let { token ->
                    request.header("Authorization", "Bearer $token")
                }
                chain.proceed(request.build())
            }
            .apply { authenticator?.let { authenticator(it) } }
            .build()

        return Retrofit.Builder()
            .baseUrl(normalizedBaseUrl)
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(UpcomingApi::class.java)
    }
}

/**
 * Wraps a suspend API call so failures surface as [ApiException] **inside the
 * coroutine** instead of on OkHttp's dispatcher thread. Throwing non-IO
 * exceptions from an OkHttp interceptor escapes Retrofit's translation and
 * kills the process; here Retrofit's HttpException/IOException are translated
 * safely at the call site.
 */
suspend fun <T> apiCall(
    block: suspend () -> T
): T = try {
    block()
} catch (e: retrofit2.HttpException) {
    val body = e.response()?.errorBody()?.string().orEmpty()
    val message = runCatching {
        UpcomingApiClient.moshi.adapter(ApiErrorDto::class.java).lenient().fromJson(body)?.error
    }.getOrNull() ?: "HTTP ${e.code()}"
    throw when (e.code()) {
        409 -> ApiException.SlotConflict(message)
        404 -> ApiException.NotFound(message)
        400 -> ApiException.Validation(message)
        else -> ApiException.Server(message)
    }
} catch (e: IOException) {
    throw ApiException.Network(e)
}

/** True when the failure means "the API was unreachable" — callers use this
 *  to fall back to the offline Room cache. */
fun Throwable.isNetworkError(): Boolean = this is ApiException.Network || this is IOException
