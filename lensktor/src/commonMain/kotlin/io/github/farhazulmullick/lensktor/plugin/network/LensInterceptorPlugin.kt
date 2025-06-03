package io.github.farhazulmullick.lensktor.plugin.network

import io.github.farhazulmullick.lensktor.plugin.network.LensKtorStateManager.logRequest
import io.github.farhazulmullick.lensktor.plugin.network.LensKtorStateManager.logRequestException
import io.github.farhazulmullick.lensktor.plugin.network.LensKtorStateManager.logResponse
import io.github.farhazulmullick.lensktor.plugin.network.LensKtorStateManager.logResponseException
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
import io.ktor.client.request.HttpSendPipeline
import io.ktor.client.statement.HttpReceivePipeline
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.HttpResponseContainer
import io.ktor.client.statement.HttpResponsePipeline
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.content.OutgoingContent
import io.ktor.util.AttributeKey
import io.ktor.util.pipeline.PipelineContext
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.readAvailable


internal val LensCallLoggingKey = AttributeKey<Int>("LensCallLoggingKey")
internal val DisableLogging = AttributeKey<Unit>("LensDisableLogging")
internal val CurrentTimeKey = AttributeKey<Long>("CurrentTimeKey")

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

public val LensHttpLogger: ClientPlugin<LoggingConfig> = createClientPlugin("LensHttpLogger", ::LoggingConfig) {
    val logger: Logger = pluginConfig.logger
    val level: LogLevel = pluginConfig.level

    fun shouldBeLogged(): Boolean = level != LogLevel.NONE

    on(SendHook) { request ->
        if (!shouldBeLogged()) {
            request.attributes.put(DisableLogging, Unit)
            return@on
        }
        val loggedRequest = try {
            logRequest(request)
            request.body as OutgoingContent
        } catch (_: Throwable) {
            null
        }

        try {
            proceedWith(loggedRequest ?: request.body)
        } catch (cause: Throwable) {
            logRequestException(request, cause)
            throw cause
        } finally {
        }
    }

    on(ResponseHook) { response ->
        if (level == LogLevel.NONE || response.call.attributes.contains(DisableLogging)) return@on

        var failed = false
        try {
            proceed()
        } catch (cause: Throwable) {
            logResponseException(response.call.request, cause)
            failed = true
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
            logResponseException(call.request, cause)
            throw cause
        }
    }

    if (!level.body) return@createClientPlugin

    @OptIn(InternalAPI::class)
    val observer: ResponseHandler = observer@ {
        if (it.call.attributes.contains(DisableLogging)) {
            return@observer
        }

        try {
            logResponse(it)
        } catch (_: Throwable) {

        } finally { }
    }

    ResponseObserver.install(ResponseObserver.prepare { onResponse(observer) }, client)
}

fun HttpClientConfig<*>.LensHttpLogger(block: LoggingConfig.() -> Unit = {}) {
    install(LensHttpLogger, block)
    install(Logging, block)
}

private suspend fun measureResponseBytes(response: HttpResponse): Long {
    val channel: ByteReadChannel = response.bodyAsChannel()
    // 8KB chunk size.
    val buffer = ByteArray(8192)
    var total = 0L

    while (!channel.isClosedForRead) {
        val bytesRead = channel.readAvailable(buffer, 0, buffer.size)
        if (bytesRead == -1) break
        total += bytesRead
    }
    return total
}

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

