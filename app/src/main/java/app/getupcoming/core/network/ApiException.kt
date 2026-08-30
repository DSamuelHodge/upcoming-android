package app.getupcoming.core.network

import java.io.IOException

/** Typed failures surfaced by [UpcomingApiClient]; status codes follow the
 *  api-contract error mapping (409 conflict / 404 not found / 400 validation). */
sealed class ApiException(message: String) : Exception(message) {
    /** Slot taken, off-grid, out of working hours, or inside min-notice. */
    class SlotConflict(message: String) : ApiException(message)

    class NotFound(message: String) : ApiException(message)

    class Validation(message: String) : ApiException(message)

    class Server(message: String) : ApiException(message)

    /** Request never reached the API (offline, DNS, timeout). */
    class Network(cause: IOException) : ApiException("Network error: ${cause.message}")
}

data class ApiError(val status: Int, override val message: String) : Exception(message)
