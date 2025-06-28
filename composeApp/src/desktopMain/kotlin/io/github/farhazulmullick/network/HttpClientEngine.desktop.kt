package io.github.farhazulmullick.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO

actual fun getHttpClientEngine(): HttpClientEngine {
    return CIO.create()
}