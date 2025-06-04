package io.github.farhazulmullick.network

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.github.farhazulmullick.lensktor.plugin.network.LensHttpLogger
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestRetryConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.LoggingConfig
import io.ktor.client.request.header
import io.ktor.http.HeadersBuilder
import io.ktor.http.URLProtocol
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

const val TIMEOUT_DURATION: Long = 30_000

/**
 * @param hostURL: Its domain. Ex api.penpencil.co
 * @param urlProtocol: By default https.
 * @param refreshTokenPlugin: Use [PWRefreshTokenPlugin] for Authentication and Refresh-token
 * @param loginConfigs: Add your custom logging configs. By default uses [DefaultLogging]
 * @param httpClientEngine: Add your custom ClientEngine.
 * For android use okhttp client-engine to addInterception
 */
fun HttpKtorClient(
    hostURL: String,
    urlProtocol: URLProtocol = URLProtocol.HTTPS,
    headers: HeadersBuilder.() -> Unit, // default headers are added.
    refreshTokenPlugin: HttpClientConfig<*>.() -> Unit = {},
    jsonConfigs: Json = JSON,
    timeoutDuration: Long = TIMEOUT_DURATION,
    enableHttpLogging: Boolean = true,
    loginConfigs: LoggingConfig.() -> Unit = { DefaultLogging() },
    httpRetryConfigs: HttpRequestRetryConfig.() -> Unit = { DefaultHttpRetryConfigs() },
): HttpClient {
    val httpClient = HttpClient(getHttpClientEngine()) {

        // converts response into dto objects
        install(ContentNegotiation) {
            json(json = jsonConfigs)
        }

        install(HttpRequestRetry) {
            httpRetryConfigs()
        }

        install(DefaultRequest) {
            apply {
                url {
                    // Protocol is also configurable please reach-kmm team for change requests.
                    protocol = urlProtocol
                    host = hostURL
                }
                header("X-Custom-Header", "Hello")
            }
        }

        // Auth plugin to refresh access-token
        refreshTokenPlugin()

        install(HttpTimeout) {
            requestTimeoutMillis = timeoutDuration
            connectTimeoutMillis = timeoutDuration
            socketTimeoutMillis = timeoutDuration
        }

        // Log HTTP request and response
        if (enableHttpLogging)
            LensHttpLogger {
                loginConfigs()
            }.also { Napier.base(DebugAntilog()) }
    }
    return httpClient
}


internal fun HttpRequestRetryConfig.DefaultHttpRetryConfigs() {
    retryOnException(retryOnTimeout = true, maxRetries = 5)
    retryOnServerErrors(3)
    exponentialDelay()
}


fun LoggingConfig.DefaultLogging() {
    level = LogLevel.ALL
    logger = object : Logger {
        override fun log(message: String) {
            Napier.d(message = message)
        }
    }
}