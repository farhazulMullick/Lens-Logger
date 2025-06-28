package io.github.farhazulmullick.lenslogger.modal

import io.github.aakira.napier.Napier
import io.github.farhazulmullick.lenslogger.plugin.network.tryReadText
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.charset
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.util.copyToBoth
import io.ktor.util.date.GMTDate
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.time.Clock.System
import kotlin.time.ExperimentalTime

private const val TAG = "NetworkLogs"
data class NetworkLogs(
    val logLevel: LogLevel? = null,
    val request: Resource<HttpRequestBuilder>? = Resource.Loading(),
    val response: Resource<ResponseData>? = Resource.Loading(),
    val responseTime: Long? = null
) {
    val requestData = when (request) {
        is Resource.Success -> request.data
        is Resource.Failed -> request.data
        else -> null
    }
    val responseData = if (response is Resource.Success) response.data else null
}

@OptIn(InternalAPI::class)
suspend fun HttpRequestBuilder.requestBody(): String? {
    val content = this.body as OutgoingContent

    val requestLog = StringBuilder()
    val charset = content.contentType?.charset() ?: Charsets.UTF_8

    val channel = ByteChannel()
    content.observe(channel)
    return withContext(Dispatchers.Default) {
        try {
            val text = channel.tryReadText(charset) ?: "No request body"
            requestLog.appendLine(text)
            Napier.d(tag = TAG) { "REQUEST_BODY: ${requestLog}" }
        } catch (e: Exception) {
            Napier.e(tag = TAG) { "Error reading request body: ${e.message}" }
            requestLog.appendLine("[request body error: ${e.message}]")
        }
        requestLog.toString()
    }
}

internal suspend fun OutgoingContent.observe(log: ByteWriteChannel) = when (this) {
    is OutgoingContent.ByteArrayContent -> {
        log.writeFully(bytes())
        log.flushAndClose()
    }
    is OutgoingContent.ReadChannelContent -> {
        val responseChannel = ByteChannel()
        val content = readFrom()

        content.copyToBoth(log, responseChannel)
    }
    is OutgoingContent.NoContent, is OutgoingContent.ProtocolUpgrade -> {
        log.flushAndClose()
    }

    else -> {log.flushAndClose()}
}


fun HttpRequestBuilder.contentLength(): String? {
    val data = this.headers[HttpHeaders.ContentLength]?.toIntOrNull()?.formatDataPacket()
        ?: this.body.let { body ->
            when (body) {
                is OutgoingContent.NoContent -> 0.formatDataPacket()
                is OutgoingContent -> body.contentLength?.toInt()?.formatDataPacket()
                else -> null
            }
        }

    return data
}

@OptIn(InternalAPI::class)
suspend fun HttpResponse?.toResponseData(): ResponseData {
    val bodyAsText = this?.rawContent?.tryReadText(this.contentType()?.charset())
    val prettyJson = try {
        val json = Json { prettyPrint = true }
        val element = Json.parseToJsonElement(bodyAsText ?: "")
        json.encodeToString(JsonElement.serializer(), element)
    } catch (e: Exception) {
        Napier.d(tag = TAG) { "Exception during retrieving body, ${e.printStackTrace()} and Caused by ${e.cause}" }
        null
    }
    return ResponseData(
        status = HttpStatusCode.allStatusCodes.find { it == this?.status },
        headers = this?.headers?.entries()?.associate { it.key to it.value.joinToString() },
        body = prettyJson,
        request = this?.request,
        requestTime = this?.requestTime,
        responseTime = this?.responseTime,
        contentLength = (bodyAsText?.toByteArray(Charsets.UTF_8)?.size ?: 0).formatDataPacket()
    )
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
    }
    else if (minutes % 60 > 0) {
        return "${minutes % 60} min"
    }
    else if (seconds % 60 > 0) {
        return "${seconds % 60} sec"
    }
    return null
}

fun Int.formatDataPacket(): String? = when {
    this >= 1024f * 1024f -> "${this / (1024f * 1024f)} MB"
    this >= 1024f -> "${(this / 1024f)} KB"
    else -> "$this B"
}

data class ResponseData(
    val status: HttpStatusCode? = null,
    val headers: Map<String, String>? = null,
    val body: String? = null,
    val request: HttpRequest? = null,
    val requestTime: GMTDate? = null,
    val responseTime: GMTDate? = null,
    val contentLength: String? = null,
)