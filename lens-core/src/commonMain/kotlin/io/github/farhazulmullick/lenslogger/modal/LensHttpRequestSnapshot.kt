package io.github.farhazulmullick.lenslogger.modal

/**
 * Transport-neutral capture of an outbound HTTP request for logging and tooling.
 * Populated by client adapters (Ktor, OkHttp, etc.); the log store and UI depend only on this type.
 */
data class LensHttpRequestSnapshot(
    val method: String,
    val url: String,
    val encodedPath: String,
    val headers: Map<String, String>,
    val bodyText: String?,
    val contentLengthDisplay: String?,
)
