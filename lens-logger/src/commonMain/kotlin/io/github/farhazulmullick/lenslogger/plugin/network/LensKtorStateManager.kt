package io.github.farhazulmullick.lenslogger.plugin.network

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import io.github.farhazulmullick.lenslogger.modal.NetworkLogs
import io.github.farhazulmullick.lenslogger.modal.Resource
import io.github.farhazulmullick.lenslogger.modal.ResponseData
import io.github.farhazulmullick.lenslogger.modal.toResponseData
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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

    fun clear() = stateCalls.clear()
}