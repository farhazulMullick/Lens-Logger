package io.github.farhazulmullick.lenslogger.plugin.network

import kotlinx.serialization.Serializable

/**
 * A user-authored rule that, when [enabled], causes the LensHttpLogger plugin to short-circuit
 * outbound Ktor requests matching [url] + [method] and return a synthetic response.
 *
 * Matching is exact: full request URL string and HTTP method name (case-insensitive).
 *
 * Behaviors:
 * - [delayMs] - if > 0, suspend that many ms before responding.
 * - [simulateFailure] - throw an IOException-style failure instead of producing a response.
 * - [statusCode], [headers], [body] - shape of the synthetic response.
 */
@Serializable
data class MockRule(
    val id: String,
    val url: String,
    val method: String,
    val enabled: Boolean = true,
    val delayMs: Long = 0L,
    val simulateFailure: Boolean = false,
    val statusCode: Int = 200,
    val headers: Map<String, String> = emptyMap(),
    val body: String = ""
)
