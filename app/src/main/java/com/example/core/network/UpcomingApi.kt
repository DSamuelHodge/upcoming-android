package com.example.core.network

import com.example.BuildConfig
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
}

/** Maps non-2xx responses onto the contract's error semantics. */
class ErrorMappingInterceptor(private val moshi: Moshi) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val response = chain.proceed(chain.request())
        if (response.isSuccessful) return response

        val body = response.body?.string().orEmpty()
        val message = runCatching {
            moshi.adapter(ApiErrorDto::class.java).lenient().fromJson(body)?.error
        }.getOrNull()

        val messageOrFallback = message ?: "HTTP ${response.code}"
        throw when (response.code) {
            409 -> ApiException.SlotConflict(messageOrFallback)
            404 -> ApiException.NotFound(messageOrFallback)
            400 -> ApiException.Validation(messageOrFallback)
            else -> ApiException.Server(messageOrFallback)
        }
    }
}

object UpcomingApiClient {
    val moshi: Moshi = Moshi.Builder().build()

    fun create(
        baseUrl: String = BuildConfig.UPCOMING_API_BASE_URL,
        apiSecret: String = BuildConfig.UPCOMING_API_SECRET
    ): UpcomingApi {
        require(baseUrl.isNotBlank()) { "UPCOMING_API_BASE_URL is not configured (.env)" }
        require(apiSecret.isNotBlank()) { "UPCOMING_API_SECRET is not configured (.env)" }

        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(ErrorMappingInterceptor(moshi))
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("Authorization", "Bearer $apiSecret")
                        .build()
                )
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(UpcomingApi::class.java)
    }
}

/** True when the failure means "the API was unreachable" — callers use this
 *  to fall back to the offline Room cache. */
fun Throwable.isNetworkError(): Boolean = this is ApiException.Network || this is IOException
