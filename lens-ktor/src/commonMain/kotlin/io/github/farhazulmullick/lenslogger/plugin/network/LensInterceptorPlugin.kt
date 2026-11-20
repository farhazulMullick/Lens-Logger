package io.github.farhazulmullick.lenslogger.plugin.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.call.HttpClientCall
import io.ktor.client.plugins.api.ClientHook
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LoggingConfig
import io.ktor.client.plugins.observer.ResponseHandler
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.HttpResponseData
import io.ktor.client.request.HttpSendPipeline
import io.ktor.client.statement.HttpReceivePipeline
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpResponseContainer
import io.ktor.client.statement.HttpResponsePipeline
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpProtocolVersion
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.util.AttributeKey
import io.ktor.util.date.GMTDate
import io.ktor.util.pipeline.PipelineContext
import io.ktor.util.pipeline.PipelinePhase
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import kotlinx.io.IOException
import kotlinx.coroutines.delay


internal val LensCallLoggingKey = AttributeKey<Int>("LensCallLoggingKey")
internal val DisableLogging = AttributeKey<Unit>("LensDisableLogging")
/** Marks a request as having been satisfied by a [MockRule] instead of the real network. */
internal val LensIsMockedKey = AttributeKey<Boolean>("LensIsMockedKey")

class LensConfig {
    internal var filters = mutableListOf<(HttpRequestBuilder) -> Boolean>()

    private var _logger: Logger? = null

    /**
     * Specifies a [Logger] instance.
     */
    public var logger: Logger
        get() = _logger ?: Logger.DEFAULT
        set(value) {
            _logger = value
        }

    public var level: LogLevel = LogLevel.HEADERS

    public fun filter(predicate: (HttpRequestBuilder) -> Boolean) {
        filters.add(predicate)
    }
}

/**
 * A Client Plugin for logging HTTP requests and responses.
 *
 * This plugin allows you to configure the level of logging detail:
 * - `LogLevel.ALL`: Logs request and response bodies.
 * - `LogLevel.HEADERS`: Logs only request and response headers.
 * - `LogLevel.BODY`: Logs request and response bodies along with headers.
 * - `LogLevel.INFO`: Logs informational messages about requests and responses (e.g., URL, method, status).
 * - `LogLevel.NONE`: Disables logging.
 *
 * It intercepts requests and responses at various stages (Send, Response, Receive)
 * to log relevant information. It also handles exceptions during these stages and
 * logs them appropriately.
 *
 * The plugin can be configured using the `LoggingConfig` class, where you specify
 * the desired `LogLevel`.
 *
 * **Usage Example:**
 * ```kotlin
 * val client = HttpClient(CIO) {
 *     install(LensHttpLogger) {
 *         level = LogLevel.ALL
 *     }
 * }
 * ```
 */
public val LensHttpLogger: ClientPlugin<LoggingConfig> = createClientPlugin("LensHttpLogger", ::LoggingConfig) {
    val level: LogLevel = pluginConfig.level

    fun shouldBeLogged(): Boolean = level != LogLevel.NONE

    on(SendHook) { request ->
        if (!shouldBeLogged()) {
            request.attributes.put(DisableLogging, Unit)
            return@on
        }
        val loggedRequest = try {
            val bodyStr = if (level.body) {
                try {
                    request.requestBody()
                } catch (e: Exception) {
                    "[request body error: ${e.message}]"
                }
            } else {
                null
            }
            val snapshot = request.toLensHttpRequestSnapshot(bodyStr)
            val callId = LensNetworkLogStore.beginCall(snapshot)
            request.attributes.put(LensCallLoggingKey, callId)
            request.body as OutgoingContent
        } catch (_: Throwable) {
            null
        }

        try {
            proceedWith(loggedRequest ?: request.body)
        } catch (cause: Throwable) {
            request.attributes.getOrNull(LensCallLoggingKey)?.let { id ->
                LensNetworkLogStore.recordOutboundFailure(id, cause)
            }
            throw cause
        } finally {
        }
    }

    on(ResponseHook) { response ->
        if (level == LogLevel.NONE || response.call.attributes.contains(DisableLogging)) return@on

        try {
            proceed()
        } catch (cause: Throwable) {
            response.call.request.attributes.getOrNull(LensCallLoggingKey)?.let { id ->
                LensNetworkLogStore.recordResponseFailure(id, cause)
            }
            throw cause
        } finally { }
    }

    on(ReceiveHook) { call ->
        if (call.attributes.contains(DisableLogging)) {
            return@on
        }

        try {
            proceed()
        } catch (cause: Throwable) {
            call.request.attributes.getOrNull(LensCallLoggingKey)?.let { id ->
                LensNetworkLogStore.recordResponseFailure(id, cause)
            }
            throw cause
        }
    }

    // Install mocking *before* the engine runs. We insert a custom phase right before
    // [HttpSendPipeline.Engine] so the existing logging interceptors (which run in
    // [HttpSendPipeline.Monitoring]) still fire, but the engine itself is bypassed.
    installLensMocking(client)

    if (!level.body) return@createClientPlugin

    @OptIn(InternalAPI::class)
    val observer: ResponseHandler = observer@ {
        if (it.call.attributes.contains(DisableLogging)) {
            return@observer
        }

        try {
            val callId = it.call.request.attributes.getOrNull(LensCallLoggingKey) ?: return@observer
            LensNetworkLogStore.recordResponseSuccess(callId, it.toResponseData())
        } catch (_: Throwable) {
            print("EXCEPTION")
        } finally { }
    }

    // observe response.
    ResponseObserver.install(ResponseObserver.prepare { onResponse(observer) }, client)
}

private val LensMockPhase = PipelinePhase("LensMockPhase")

@OptIn(InternalAPI::class)
private fun installLensMocking(client: HttpClient) {
    LensMockingStateManager.ensureInitialized()
    client.sendPipeline.insertPhaseBefore(HttpSendPipeline.Engine, LensMockPhase)
    client.sendPipeline.intercept(LensMockPhase) {
        val request: HttpRequestBuilder = context
        val rule = LensMockingStateManager.findActive(
            url = request.url.buildString(),
            method = request.method.value
        ) ?: return@intercept

        request.attributes.put(LensIsMockedKey, true)
        val callId = request.attributes.getOrNull(LensCallLoggingKey)
        if (callId != null) {
            LensNetworkLogStore.markMocked(callId)
        }

        val mockStartMs = currentEpochMs()

        if (rule.delayMs > 0) {
            delay(rule.delayMs)
        }

        if (rule.simulateFailure) {
            val failure = IOException(
                "Lens mock: simulated failure for ${request.method.value} ${request.url.buildString()}"
            )
            if (callId != null) {
                LensNetworkLogStore.logMockedFailure(callId, failure)
            }
            throw failure
        }

        val bodyBytes = rule.body.encodeToByteArray()

        // Strip framing headers from the user-supplied set: the rule's headers are typically
        // seeded from a captured live response, so a stale Content-Length / Transfer-Encoding
        // would cause the engine to throw IllegalStateException when the mocked body length
        // differs from the original. Re-derive Content-Length from the actual body.
        val responseHeaders = HeadersBuilder().apply {
            rule.headers.forEach { (key, value) ->
                if (key.equals(HttpHeaders.ContentLength, ignoreCase = true)) return@forEach
                if (key.equals(HttpHeaders.TransferEncoding, ignoreCase = true)) return@forEach
                append(key, value)
            }
            append(HttpHeaders.ContentLength, bodyBytes.size.toString())
        }.build()

        val responseData = HttpResponseData(
            statusCode = HttpStatusCode.fromValue(rule.statusCode),
            requestTime = GMTDate(),
            headers = responseHeaders,
            version = HttpProtocolVersion.HTTP_1_1,
            body = ByteReadChannel(bodyBytes),
            callContext = request.executionContext
        )

        val mockedCall = HttpClientCall(
            client = client,
            requestData = request.build(),
            responseData = responseData
        )

        // The ResponseObserver-driven logging path only runs when LogLevel.body is true and is
        // unreliable for synthesized calls, so we record the mocked response directly here.
        if (callId != null) {
            LensNetworkLogStore.logMockedResponse(
                callId = callId,
                rule = rule,
                responseTimeMs = currentEpochMs() - mockStartMs,
            )
        }

        // Skip the engine phase entirely - the response is fully synthesized.
        subject = mockedCall
        finish()
    }
}

@OptIn(kotlin.time.ExperimentalTime::class)
private fun currentEpochMs(): Long = kotlin.time.Clock.System.now().toEpochMilliseconds()


/**
 * Installs the LensHttpLogger and the standard Ktor Logging feature.
 *
 * This extension function simplifies the setup of HTTP logging for Ktor HTTP clients
 * by installing both the `LensHttpLogger` (presumably a custom logger for Lens)
 * and the standard Ktor `Logging` feature.
 *
 * It allows for configuring both loggers using the same `LoggingConfig` block.
 *
 * @receiver HttpClientConfig<*> The Ktor HTTP client configuration block.
 * @param block A lambda function with `LoggingConfig` as its receiver, used to configure
 *              the logging behavior for both `LensHttpLogger` and Ktor `Logging`.
 *              Defaults to an empty block if no specific configuration is needed.
 *
 * @see LensHttpLogger Ktor feature for Lens-specific HTTP logging.
 * @see Logging Ktor feature for standard HTTP logging.
 * @see LoggingConfig Configuration options for Ktor logging.
 *
 * Example usage:
 * ```kotlin
 * val client = HttpClient(CIO) {
 *     LensHttpLogger {
 *         logger = Logger.DEFAULT
 *         level = LogLevel.ALL
 *     }
 * }
 * ```
 */
fun HttpClientConfig<*>.LensHttpLogger(block: LoggingConfig.() -> Unit = {}) {
    install(LensHttpLogger, block)
    install(Logging, block)
}

/**
 * A hook that is triggered when the server sends a raw HTTP response.
 *
 * This hook allows you to inspect or modify the `HttpResponse` before it is further processed
 * by the client's receive pipeline.
 *
 * Example Usage:
 * ```kotlin
 * client.ResponseHook { response ->
 *     // Log the response status
 *     println("Received response with status: ${response.status}")
 *
 *     // You can choose to proceed with the original response
 *     proceed()
 *
 *     // Or, if you were to modify the response (though less common at this stage),
 *     // you would typically do so before proceeding or by replacing the subject
 *     // in a more advanced interceptor. For simple inspection, `proceed()` is sufficient.
 * }
 * ```
 */
private object ResponseHook : ClientHook<suspend ResponseHook.Context.(response: HttpResponse) -> Unit> {

    class Context(private val context: PipelineContext<HttpResponse, Unit>) {
        suspend fun proceed() = context.proceed()
    }

    override fun install(
        client: HttpClient,
        handler: suspend Context.(response: HttpResponse) -> Unit
    ) {
        client.receivePipeline.intercept(HttpReceivePipeline.State) {
            handler(Context(this), subject)
        }
    }
}

/**
 * A ClientHook that allows intercepting and modifying an [HttpRequestBuilder] before it is sent.
 *
 * This hook is installed in the [HttpSendPipeline.Monitoring] phase of the HTTP client's send pipeline.
 *
 * ## Usage Example:
 * ```kotlin
 * val client = HttpClient {
 *     install(SendHook) { requestBuilder ->
 *         // Modify the requestBuilder here, for example, add a header
 *         requestBuilder.headers.append("X-Custom-Header", "MyValue")
 *
 *         // To proceed with the modified request:
 *         // proceedWith(requestBuilder) // Not needed if you only modify the request
 *
 *         // To replace the request entirely with a new one (less common):
 *         // val newRequest = HttpRequestBuilder().apply {
 *         //     url("http://new.example.com")
 *         // }
 *         // proceedWith(newRequest)
 *     }
 * }
 * ```
 */
private object SendHook : ClientHook<suspend SendHook.Context.(response: HttpRequestBuilder) -> Unit> {

    class Context(private val context: PipelineContext<Any, HttpRequestBuilder>) {
        suspend fun proceedWith(content: Any) = context.proceedWith(content)
    }

    override fun install(
        client: HttpClient,
        handler: suspend Context.(request: HttpRequestBuilder) -> Unit
    ) {
        client.sendPipeline.intercept(HttpSendPipeline.Monitoring) {
            handler(Context(this), context)
        }
    }
}

private object ReceiveHook : ClientHook<suspend ReceiveHook.Context.(call: HttpClientCall) -> Unit> {

    class Context(private val context: PipelineContext<HttpResponseContainer, HttpClientCall>) {
        suspend fun proceed() = context.proceed()
    }

    override fun install(
        client: HttpClient,
        handler: suspend Context.(call: HttpClientCall) -> Unit
    ) {
        client.responsePipeline.intercept(HttpResponsePipeline.Receive) {
            handler(Context(this), context)
        }
    }
}
