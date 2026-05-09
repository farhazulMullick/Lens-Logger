package io.github.farhazulmullick.lenslogger.plugin.network

import io.github.farhazulmullick.lenslogger.modal.LensHttpRequestSnapshot
import io.github.farhazulmullick.lenslogger.modal.contentLength
import io.ktor.client.request.HttpRequestBuilder

/** Path + query segment from a full URL string (Ktor URLBuilder.encodedPath is not on all targets). */
internal fun encodedPathFromFullUrl(fullUrl: String): String {
    val afterScheme = fullUrl.substringAfter("://", fullUrl)
    val pathStart = afterScheme.indexOf('/')
    if (pathStart < 0) return "/"
    val pathAndMaybeQuery = afterScheme.substring(pathStart)
    val q = pathAndMaybeQuery.indexOf('?')
    val end = if (q < 0) pathAndMaybeQuery.length else q
    return pathAndMaybeQuery.substring(0, end).ifEmpty { "/" }
}

internal fun HttpRequestBuilder.toLensHttpRequestSnapshot(bodyText: String?): LensHttpRequestSnapshot {
    val full = url.buildString()
    return LensHttpRequestSnapshot(
        method = method.value,
        url = full,
        encodedPath = encodedPathFromFullUrl(full),
        headers = headers.entries().associate { e -> e.key to e.value.joinToString() },
        bodyText = bodyText,
        contentLengthDisplay = runCatching { contentLength() }.getOrNull(),
    )
}
