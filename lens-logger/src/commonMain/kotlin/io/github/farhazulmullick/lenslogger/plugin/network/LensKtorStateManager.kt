package io.github.farhazulmullick.lenslogger.plugin.network

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import io.github.farhazulmullick.lenslogger.modal.NetworkLogs
import io.github.farhazulmullick.lenslogger.modal.Resource
import io.github.farhazulmullick.lenslogger.modal.ResponseData
import io.github.farhazulmullick.lenslogger.modal.formatDataPacket
import io.github.farhazulmullick.lenslogger.modal.toResponseData
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.charsets.Charsets
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

object LensKtorStateManager {
    val stateCalls : SnapshotStateList<NetworkLogs> = mutableStateListOf<NetworkLogs>()
    val mutex: Mutex = Mutex()
    private val TAG = "LensKtorStateManager"

    @OptIn(ExperimentalTime::class)
    suspend fun logRequest(requestBuilder: HttpRequestBuilder) {
        mutex.withLock {
            requestBuilder.attributes.put(LensCallLoggingKey, stateCalls.size)
            requestBuilder.attributes.put(CurrentTimeKey, Clock.System.now().toEpochMilliseconds())
            val log = NetworkLogs(request = Resource.Success(requestBuilder))

            stateCalls.add(log)
        }
    }

    /**
     * Marks the [NetworkLogs] entry corresponding to this in-flight [requestBuilder] as having
     * been satisfied by a [MockRule]. Called from the mocking interceptor right after the rule
     * is matched. Safe to call before [logResponse].
     */
    fun markMocked(requestBuilder: HttpRequestBuilder) {
        val index = requestBuilder.attributes.getOrNull(LensCallLoggingKey) ?: return
        if (index !in stateCalls.indices) return
        stateCalls[index] = stateCalls[index].copy(isMocked = true)
    }

    /**
     * Records a synthesized success response for a mocked request directly from a [MockRule],
     * without depending on the [io.ktor.client.plugins.observer.ResponseObserver] (which only
     * runs when the configured [io.ktor.client.plugins.logging.LogLevel] includes a body).
     */
    @OptIn(ExperimentalTime::class)
    fun logMockedResponse(
        requestBuilder: HttpRequestBuilder,
        rule: MockRule,
        responseTimeMs: Long
    ) {
        val index = requestBuilder.attributes.getOrNull(LensCallLoggingKey) ?: return
        if (index !in stateCalls.indices) return

        val prettyBody = if (rule.body.isBlank()) {
            rule.body
        } else {
            try {
                val pretty = Json { prettyPrint = true }
                pretty.encodeToString(JsonElement.serializer(), Json.parseToJsonElement(rule.body))
            } catch (_: Throwable) {
                rule.body
            }
        }

        val responseData = ResponseData(
            status = HttpStatusCode.fromValue(rule.statusCode),
            headers = rule.headers,
            body = prettyBody,
            request = null,
            requestTime = null,
            responseTime = null,
            contentLength = rule.body.toByteArray(Charsets.UTF_8).size.formatDataPacket(),
            sourceRequestUrl = requestBuilder.url.buildString(),
        )

        stateCalls[index] = stateCalls[index].copy(
            response = Resource.Success(responseData),
            responseTime = responseTimeMs,
            isMocked = true
        )
    }

    /**
     * Records a synthesized failure response for a mocked request configured with
     * [MockRule.simulateFailure].
     */
    fun logMockedFailure(requestBuilder: HttpRequestBuilder, cause: Throwable?) {
        val index = requestBuilder.attributes.getOrNull(LensCallLoggingKey) ?: return
        if (index !in stateCalls.indices) return
        stateCalls[index] = stateCalls[index].copy(
            response = Resource.Failed(stateCalls[index].responseData, cause),
            isMocked = true
        )
    }

    @OptIn(ExperimentalTime::class, InternalAPI::class)
    suspend fun logResponse(response: HttpResponse) {
        val index = response.request.attributes[LensCallLoggingKey]
        val sendTime: Long = response.request.attributes[CurrentTimeKey]
        val responseTime: Long = Clock.System.now().toEpochMilliseconds() - sendTime
        if (index >= stateCalls.size) return

        val response : ResponseData = response.toResponseData()
        stateCalls[index] = stateCalls[index].copy(
            response = Resource.Success(response),
            responseTime = responseTime
        )
    }

    fun logRequestException(request: HttpRequestBuilder, cause: Throwable?) {
        val index = request.attributes[LensCallLoggingKey]
        if (index >= stateCalls.size) return

        stateCalls[index] = stateCalls[index].copy(
            request = Resource.Failed(stateCalls[index].requestData, cause),
            response = null,
        )
    }

    fun logResponseException(request: HttpRequest, cause: Throwable?) {
        val index = request.attributes[LensCallLoggingKey]
        if (index >= stateCalls.size) return

        stateCalls[index] = stateCalls[index].copy(
            response = Resource.Failed(stateCalls[index].responseData, cause),
        )
    }

    /**
     * Completes an in-flight call (e.g. OkHttp / Retrofit) with a captured success [ResponseData].
     */
    fun completeCallWithSuccess(callIndex: Int, responseTimeMs: Long, data: ResponseData) {
        if (callIndex !in stateCalls.indices) return
        stateCalls[callIndex] = stateCalls[callIndex].copy(
            response = Resource.Success(data),
            responseTime = responseTimeMs
        )
    }

    /** Transport failed before a response line was produced (mirror of [logResponseException]). */
    fun completeCallWithResponseTransportFailure(callIndex: Int, cause: Throwable?) {
        if (callIndex !in stateCalls.indices) return
        stateCalls[callIndex] = stateCalls[callIndex].copy(
            response = Resource.Failed(stateCalls[callIndex].responseData, cause),
        )
    }

    /** Request never reached the backend (mirror of [logRequestException]). */
    fun completeCallWithOutboundFailure(callIndex: Int, cause: Throwable?) {
        if (callIndex !in stateCalls.indices) return
        stateCalls[callIndex] = stateCalls[callIndex].copy(
            request = Resource.Failed(stateCalls[callIndex].requestData, cause),
            response = null,
        )
    }

    fun clear() = stateCalls.clear()
}