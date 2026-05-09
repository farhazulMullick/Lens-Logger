package io.github.farhazulmullick.lenslogger.plugin.network

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import io.github.farhazulmullick.lenslogger.modal.LensHttpRequestSnapshot
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

/**
 * In-memory log of HTTP calls. Client adapters record [LensHttpRequestSnapshot] values and
 * correlate completions with a [callId] (list index) returned from [beginCall].
 *
 * Adapters (Ktor plugin, OkHttp interceptor, future URLSession/Swift bridges) should be the only
 * code that touches client-specific types; this store stays transport-agnostic.
 *
 * Publishing note: a later release may split `lens-core` (this model + store + mocks) from
 * `lens-ktor` / `lens-okhttp` artifacts; callers should depend on the snapshot types here rather
 * than on Ktor request builders.
 */
object LensNetworkLogStore {
    val stateCalls: SnapshotStateList<NetworkLogs> = mutableStateListOf()
    val mutex: Mutex = Mutex()

    @OptIn(ExperimentalTime::class)
    suspend fun beginCall(snapshot: LensHttpRequestSnapshot): Int {
        mutex.withLock {
            val id = stateCalls.size
            val startedAt = Clock.System.now().toEpochMilliseconds()
            val log = NetworkLogs(
                request = Resource.Success(snapshot),
                requestStartEpochMs = startedAt,
            )
            stateCalls.add(log)
            return id
        }
    }

    fun markMocked(callId: Int) {
        if (callId !in stateCalls.indices) return
        stateCalls[callId] = stateCalls[callId].copy(isMocked = true)
    }

    @OptIn(ExperimentalTime::class)
    fun logMockedResponse(
        callId: Int,
        rule: MockRule,
        responseTimeMs: Long,
    ) {
        if (callId !in stateCalls.indices) return
        val snapshot = stateCalls[callId].requestData ?: return

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
            sourceRequestUrl = snapshot.url,
        )

        stateCalls[callId] = stateCalls[callId].copy(
            response = Resource.Success(responseData),
            responseTime = responseTimeMs,
            isMocked = true,
        )
    }

    fun logMockedFailure(callId: Int, cause: Throwable?) {
        if (callId !in stateCalls.indices) return
        stateCalls[callId] = stateCalls[callId].copy(
            response = Resource.Failed(stateCalls[callId].responseData, cause),
            isMocked = true,
        )
    }

    @OptIn(ExperimentalTime::class, InternalAPI::class)
    suspend fun logResponse(response: HttpResponse) {
        val index = response.request.attributes.getOrNull(LensCallLoggingKey) ?: return
        val sendTime = stateCalls.getOrNull(index)?.requestStartEpochMs ?: return
        val responseTime: Long = Clock.System.now().toEpochMilliseconds() - sendTime
        if (index >= stateCalls.size) return

        val responseData: ResponseData = response.toResponseData()
        stateCalls[index] = stateCalls[index].copy(
            response = Resource.Success(responseData),
            responseTime = responseTime,
        )
    }

    fun logRequestException(request: HttpRequestBuilder, cause: Throwable?) {
        val index = request.attributes.getOrNull(LensCallLoggingKey) ?: return
        if (index >= stateCalls.size) return

        stateCalls[index] = stateCalls[index].copy(
            request = Resource.Failed(stateCalls[index].requestData, cause),
            response = null,
        )
    }

    fun logResponseException(request: HttpRequest, cause: Throwable?) {
        val index = request.attributes.getOrNull(LensCallLoggingKey) ?: return
        if (index >= stateCalls.size) return

        stateCalls[index] = stateCalls[index].copy(
            response = Resource.Failed(stateCalls[index].responseData, cause),
        )
    }

    fun completeCallWithSuccess(callIndex: Int, responseTimeMs: Long, data: ResponseData) {
        if (callIndex !in stateCalls.indices) return
        stateCalls[callIndex] = stateCalls[callIndex].copy(
            response = Resource.Success(data),
            responseTime = responseTimeMs,
        )
    }

    fun completeCallWithResponseTransportFailure(callIndex: Int, cause: Throwable?) {
        if (callIndex !in stateCalls.indices) return
        stateCalls[callIndex] = stateCalls[callIndex].copy(
            response = Resource.Failed(stateCalls[callIndex].responseData, cause),
        )
    }

    fun completeCallWithOutboundFailure(callIndex: Int, cause: Throwable?) {
        if (callIndex !in stateCalls.indices) return
        stateCalls[callIndex] = stateCalls[callIndex].copy(
            request = Resource.Failed(stateCalls[callIndex].requestData, cause),
            response = null,
        )
    }

    fun clear() = stateCalls.clear()
}

@Deprecated(
    message = "Renamed to LensNetworkLogStore for a client-agnostic API.",
    replaceWith = ReplaceWith("LensNetworkLogStore"),
)
typealias LensKtorStateManager = LensNetworkLogStore
