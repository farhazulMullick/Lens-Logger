package io.github.farhazulmullick.lenslogger.plugin.network

import io.github.farhazulmullick.lenslogger.modal.ResponseData
import io.github.farhazulmullick.lenslogger.modal.formatDataPacket
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.client.request.url as ktorUrl
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.ByteArrayContent
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import java.io.IOException

private const val DEFAULT_MOCK_MEDIA_TYPE = "application/json; charset=utf-8"

/** Cap for response body capture in the Lens UI (full body may still be consumed by the app). */
private const val MAX_PEEK_BYTES = 8L * 1024L * 1024L

internal data class PreparedOkHttpRequest(
    val request: Request,
    val bodyBytes: ByteArray?,
    val bodyContentType: okhttp3.MediaType?,
)

/**
 * Reads a one-shot request body once and returns a request with a replayable body plus the bytes
 * used for Ktor-style request logging.
 */
internal fun prepareOkHttpRequest(original: Request): PreparedOkHttpRequest {
    val body = original.body ?: return PreparedOkHttpRequest(original, null, null)
    val buffer = Buffer()
    body.writeTo(buffer)
    val bytes = buffer.readByteArray()
    val contentType = body.contentType()
    val replayable = original.newBuilder()
        .method(original.method, bytes.toRequestBody(contentType))
        .build()
    return PreparedOkHttpRequest(replayable, bytes, contentType)
}

internal fun toLensHttpRequestBuilder(
    prepared: PreparedOkHttpRequest,
): HttpRequestBuilder {
    val req = prepared.request
    return HttpRequestBuilder().apply {
        ktorUrl(req.url.toString())
        method = HttpMethod(req.method)
        for (i in 0 until req.headers.size) {
            headers.append(req.headers.name(i), req.headers.value(i))
        }
        val rawBytes = prepared.bodyBytes
        if (rawBytes != null) {
            val ctString = prepared.bodyContentType?.toString()
            val ct = ctString?.let { ContentType.parse(it) }
            setBody(ByteArrayContent(rawBytes, contentType = ct))
        }
    }
}

private fun okhttpHeadersToSingleMap(headers: okhttp3.Headers): Map<String, String> {
    val out = LinkedHashMap<String, String>()
    for (i in 0 until headers.size) {
        out[headers.name(i)] = headers.value(i)
    }
    return out
}

private fun prettyJsonBody(raw: String?): String? {
    if (raw.isNullOrBlank()) return raw
    return try {
        val json = Json { prettyPrint = true }
        val element = Json.parseToJsonElement(raw)
        json.encodeToString(JsonElement.serializer(), element)
    } catch (_: Throwable) {
        raw
    }
}

private fun fallbackHttpReason(code: Int): String = when (code) {
    200 -> "OK"
    201 -> "Created"
    204 -> "No Content"
    304 -> "Not Modified"
    400 -> "Bad Request"
    401 -> "Unauthorized"
    403 -> "Forbidden"
    404 -> "Not Found"
    500 -> "Internal Server Error"
    502 -> "Bad Gateway"
    503 -> "Service Unavailable"
    else -> "HTTP $code"
}

/**
 * Logs outbound calls into [LensKtorStateManager] and honors [LensMockingStateManager] (same URLs
 * and methods as Ktor uses). Install on the **application** interceptor list of your
 * Retrofit-bound [okhttp3.OkHttpClient].
 *
 * Uses [runBlocking] for the mutex-guarded log entry; Retrofit invokes interceptors off the main
 * thread — avoid attaching this interceptor to clients used on the UI thread synchronously.
 */
class LensOkHttpInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        LensMockingStateManager.ensureInitialized()

        val prepared = prepareOkHttpRequest(chain.request())
        val request = prepared.request
        val lensBuilder = toLensHttpRequestBuilder(prepared)

        val callIndex = runBlocking {
            LensKtorStateManager.logRequest(lensBuilder)
            lensBuilder.attributes.getOrNull(LensCallLoggingKey)
                ?: error("LensOkHttpInterceptor: missing call index after logRequest")
        }

        val mockRule = LensMockingStateManager.findActive(request.url.toString(), request.method)
        if (mockRule != null) {
            return handleMock(request, lensBuilder, mockRule)
        }

        val sendTime = System.currentTimeMillis()
        val response = try {
            chain.proceed(request)
        } catch (e: IOException) {
            LensKtorStateManager.completeCallWithResponseTransportFailure(callIndex, e)
            throw e
        }
        val elapsed = System.currentTimeMillis() - sendTime

        val bodyString = try {
            val body = response.body ?: run {
                val data = LensOkHttpCapturedResponse.responseDataWithoutBody(response, bodyLengthLabel = null)
                LensKtorStateManager.completeCallWithSuccess(callIndex, elapsed, data)
                return response
            }
            response.peekBody(MAX_PEEK_BYTES).string()
        } catch (e: Throwable) {
            LensKtorStateManager.completeCallWithResponseTransportFailure(callIndex, e)
            throw e
        }

        val contentLengthLabel = try {
            response.body?.contentLength()?.takeIf { it >= 0 }?.toInt()?.formatDataPacket()
        } catch (_: Throwable) {
            null
        }

        val data = ResponseData(
            status = HttpStatusCode.fromValue(response.code),
            headers = okhttpHeadersToSingleMap(response.headers),
            body = prettyJsonBody(bodyString),
            request = null,
            requestTime = null,
            responseTime = null,
            contentLength = contentLengthLabel
                ?: bodyString.encodeToByteArray().size.formatDataPacket(),
            sourceRequestUrl = request.url.toString(),
        )
        LensKtorStateManager.completeCallWithSuccess(callIndex, elapsed, data)

        return response
    }

    private fun handleMock(
        request: Request,
        lensBuilder: HttpRequestBuilder,
        rule: MockRule,
    ): Response {
        LensKtorStateManager.markMocked(lensBuilder)

        val mockStart = System.currentTimeMillis()
        if (rule.delayMs > 0L) {
            Thread.sleep(rule.delayMs)
        }
        val urlDesc = "${request.method} ${request.url}"
        if (rule.simulateFailure) {
            val failure = IOException("Lens mock: simulated failure for $urlDesc")
            LensKtorStateManager.logMockedFailure(lensBuilder, failure)
            throw failure
        }

        val bodyBytes = rule.body.encodeToByteArray()
        val headerBuilder = Headers.Builder()
        rule.headers.forEach { (key, value) ->
            if (key.equals("Content-Length", ignoreCase = true)) return@forEach
            if (key.equals("Transfer-Encoding", ignoreCase = true)) return@forEach
            headerBuilder.add(key, value)
        }

        val mediaType =
            rule.headers.entries.find { it.key.equals("Content-Type", ignoreCase = true) }?.value
                ?.toMediaTypeOrNull()
                ?: DEFAULT_MOCK_MEDIA_TYPE.toMediaTypeOrNull()

        val responseBody = bodyBytes.toResponseBody(mediaType)
        val took = System.currentTimeMillis() - mockStart
        LensKtorStateManager.logMockedResponse(lensBuilder, rule, took)

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(rule.statusCode)
            .message(fallbackHttpReason(rule.statusCode))
            .headers(headerBuilder.build())
            .body(responseBody)
            .sentRequestAtMillis(java.lang.System.currentTimeMillis())
            .receivedResponseAtMillis(java.lang.System.currentTimeMillis())
            .build()
    }
}

private object LensOkHttpCapturedResponse {
    fun responseDataWithoutBody(
        response: Response,
        bodyLengthLabel: String?,
    ): ResponseData = ResponseData(
        status = HttpStatusCode.fromValue(response.code),
        headers = okhttpHeadersToSingleMap(response.headers),
        body = null,
        request = null,
        requestTime = null,
        responseTime = null,
        contentLength = bodyLengthLabel,
        sourceRequestUrl = response.request.url.toString(),
    )
}

/**
 * Adds [LensOkHttpInterceptor] for Retrofit / raw OkHttp clients on JVM (Android + desktop).
 */
fun okhttp3.OkHttpClient.Builder.installLensInterceptor(): okhttp3.OkHttpClient.Builder =
    addInterceptor(LensOkHttpInterceptor())
