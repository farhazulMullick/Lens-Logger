package io.github.farhazulmullick.lensktor.modal

import io.github.aakira.napier.Napier
import io.github.farhazulmullick.lensktor.plugin.network.tryReadText
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.charset
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

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

fun HttpRequestBuilder.contentLength(): Long? {
    return this.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        ?: this.body.let { body ->
            when (body) {
                is OutgoingContent.NoContent -> 0L
                is OutgoingContent -> body.contentLength
                else -> null
            }
        }
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
        status = this?.status?.value,
        headers = this?.headers?.entries()?.associate { it.key to it.value.joinToString() },
        body = prettyJson,
        request = this?.request,
        contentLength = (bodyAsText?.toByteArray(Charsets.UTF_8)?.size ?: 0).let {
            when {
                it >= 1024f * 1024f -> "${it / (1024f * 1024f)} MB"
                it >= 1024f -> "${(it / 1024f)} KB"
                else -> "$it B"
            }
        }
    )
}

data class ResponseData(
    val status: Int? = null,
    val headers: Map<String, String>? = null,
    val body: String? = null,
    val request: HttpRequest? = null,
    val contentLength: String? = null,
)