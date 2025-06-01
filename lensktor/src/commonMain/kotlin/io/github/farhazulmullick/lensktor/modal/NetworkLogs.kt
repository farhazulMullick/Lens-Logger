package io.github.farhazulmullick.lensktor.modal

import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse

data class NetworkLogs(
    val logLevel: LogLevel? = null,
    val request: Resource<HttpRequestBuilder>? = Resource.Loading(),
    val response: Resource<HttpResponse>? = Resource.Loading(),
) {
    val requestData = if (request is Resource.Success) request.data else null
    val responseData = if (response is Resource.Success) response.data else null
}