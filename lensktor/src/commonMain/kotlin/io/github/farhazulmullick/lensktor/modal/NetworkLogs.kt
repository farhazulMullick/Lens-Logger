package io.github.farhazulmullick.lensktor.modal

import io.github.aakira.napier.Napier
import io.github.farhazulmullick.lensktor.plugin.network.tryReadText
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.request.HttpRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.request
import io.ktor.http.charset
import io.ktor.http.contentType
import io.ktor.utils.io.InternalAPI

private const val TAG = "NetworkLogs"
data class NetworkLogs(
    val logLevel: LogLevel? = null,
    val request: Resource<HttpRequestBuilder>? = Resource.Loading(),
    val response: Resource<ResponseData>? = Resource.Loading(),
    val responseTime: Long? = null
) {
    val requestData = when (request) {
        is Resource.Success -> request.data
        is Resource.Failed -> request.data
        else -> null
    }
    val responseData = if (response is Resource.Success) response.data else null
}

@OptIn(InternalAPI::class)
suspend fun HttpResponse?.toResponseData(): ResponseData {
    val bodyAsText = this?.rawContent?.tryReadText(this.contentType()?.charset())
    Napier.d(tag = TAG){" toResponseData :: body :: $bodyAsText"}
    return ResponseData(
        status = this?.status?.value,
        headers = this?.headers?.entries()?.associate { it.key to it.value.joinToString() },
        body = bodyAsText,
        request = this?.request
    )
}

data class ResponseData(
    val status: Int? = null,
    val headers: Map<String, String>? = null,
    val body: String? = null,
    val request: HttpRequest? = null
)