package io.github.farhazulmullick.lenslogger.plugin.network

import io.github.aakira.napier.Napier
import io.github.farhazulmullick.lenslogger.modal.ResponseData
import io.github.farhazulmullick.lenslogger.modal.formatDataPacket
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.charset
import io.ktor.http.content.OutgoingContent
import io.ktor.http.contentType
import io.ktor.util.copyToBoth
import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.core.toByteArray
import io.ktor.utils.io.readRemaining
import io.ktor.utils.io.writeFully
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

private const val TAG = "KtorNetworkCapture"

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

    else -> {
        log.flushAndClose()
    }
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
        val element = json.parseToJsonElement(bodyAsText ?: "")
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

suspend inline fun ByteReadChannel.tryReadText(charset: Charset?): String? = try {
    readRemaining().readText(charset = charset ?: Charsets.UTF_8)
} catch (cause: Throwable) {
    null
}
