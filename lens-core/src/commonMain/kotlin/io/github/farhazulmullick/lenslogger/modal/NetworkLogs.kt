package io.github.farhazulmullick.lenslogger.modal

import io.ktor.client.request.HttpRequest
import io.ktor.http.HttpStatusCode
import io.ktor.util.date.GMTDate
import kotlin.time.Clock.System
import kotlin.time.ExperimentalTime

data class NetworkLogs(
    val request: Resource<LensHttpRequestSnapshot>? = Resource.Loading(),
    val response: Resource<ResponseData>? = Resource.Loading(),
    val responseTime: Long? = null,
    val isMocked: Boolean = false,
    /** Monotonic start time for this call; used to compute latency when completing from adapters. */
    val requestStartEpochMs: Long? = null,
) {
    val requestData = when (request) {
        is Resource.Success -> request.data
        is Resource.Failed -> request.data
        else -> null
    }
    val responseData = if (response is Resource.Success) response.data else null
}

@OptIn(ExperimentalTime::class)
fun ResponseData.getRequestedAgoTime(): String? {
    val requestTimeMilli = this.requestTime?.timestamp ?: return null
    val currentTimeMilli = System.now().toEpochMilliseconds()
    val requestedAgoMilli = currentTimeMilli - requestTimeMilli

    val seconds = requestedAgoMilli / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    if (hours > 0) {
        return "$hours hr"
    } else if (minutes % 60 > 0) {
        return "${minutes % 60} min"
    } else if (seconds % 60 > 0) {
        return "${seconds % 60} sec"
    }
    return "0 sec"
}

fun Int.formatDataPacket(): String? = this.toLong().formatBytesHuman()

fun Long.formatBytesHuman(): String {
    return when {
        this < 1024L -> "${this} B"
        this < 1024L * 1024L -> formatUpToTwoDecimalBinaryUnit(this / 1024.0, "KB")
        else -> formatUpToTwoDecimalBinaryUnit(this / (1024.0 * 1024.0), "MB")
    }
}

private fun formatUpToTwoDecimalBinaryUnit(value: Double, unit: String): String {
    val scaledLong = kotlin.math.round(value * 100.0).toLong()
    val whole = scaledLong / 100
    val frac = kotlin.math.abs(scaledLong % 100)
    val body =
        if (frac == 0L) whole.toString()
        else "$whole.${frac.toString().padStart(2, '0').trimEnd('0')}"
    return "$body $unit"
}

fun Long.formatResponseTimeHuman(): String {
    if (this < 1000L) return "${this} ms"
    val seconds = this / 1000.0
    val scaledLong = kotlin.math.round(seconds * 100.0).toLong()
    val whole = scaledLong / 100
    val frac = kotlin.math.abs(scaledLong % 100)
    return if (frac == 0L) "${whole} s"
    else "${whole}.${frac.toString().padStart(2, '0').trimEnd('0')} s"
}

data class ResponseData(
    val status: HttpStatusCode? = null,
    val headers: Map<String, String>? = null,
    val body: String? = null,
    val request: HttpRequest? = null,
    val requestTime: GMTDate? = null,
    val responseTime: GMTDate? = null,
    val contentLength: String? = null,
    /** Present for non-Ktor pipelines (e.g. OkHttp/Retrofit) where [request] may be null. */
    val sourceRequestUrl: String? = null,
)
