package io.github.farhazulmullick.lensktor.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import io.github.farhazulmullick.lensktor.modal.NetworkLogs
import io.github.farhazulmullick.lensktor.modal.Resource
import io.github.farhazulmullick.lensktor.plugin.network.LensCallLoggingKey
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request

internal object LensKtorStateManager {
    val stateCalls : SnapshotStateList<NetworkLogs> = mutableStateListOf<NetworkLogs>()

    fun logRequest(requestBuilder: HttpRequestBuilder) {
        requestBuilder.attributes.put(LensCallLoggingKey, stateCalls.size)
        val log = NetworkLogs(request = Resource.Success(requestBuilder))

        stateCalls.add(log)
    }

    fun logResponse(response: HttpResponse) {
        val index = response.request.attributes[LensCallLoggingKey]
        if (index >= stateCalls.size) return

        stateCalls[index] = stateCalls[index].copy(
            response = Resource.Success(response)
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

//internal object LensKtorStateManager {
//    val stateCalls: SnapshotStateList<NetworkLogs> = mutableStateListOf()
//
//    fun logRequest(requestBuilder: HttpRequestBuilder) {
//        requestBuilder.attributes.put(LensCallLoggingKey, stateCalls.size)
//        stateCalls.add(
//            NetworkLogs(request = Resource.Success(requestBuilder))
//        )
//    }
//
//    fun logResponse(response: HttpResponse) {
//        updateLogByRequest(response.request) { log ->
//            log.copy(response = Resource.Success(response))
//        }
//    }
//
//    fun logRequestException(request: HttpRequest, cause: Throwable?) {
//        updateLogByRequest(request) { log ->
//            log.copy(
//                request = Resource.Failed(log.requestData, cause),
//                response = null
//            )
//        }
//    }
//
//    fun logResponseException(request: HttpRequest, cause: Throwable?) {
//        updateLogByRequest(request) { log ->
//            log.copy(
//                response = Resource.Failed(log.responseData, cause)
//            )
//        }
//    }
//
//    fun clear() = stateCalls.clear()
//
//    // --- Private helpers ---
//    private fun updateLogByRequest(request: HttpRequest, updater: (old: NetworkLogs) -> NetworkLogs) {
//        val index = request.attributes[LensCallLoggingKey]
//        if (index < stateCalls.size) {
//            stateCalls[index] = updater(stateCalls[index])
//        }
//    }
//}
