package io.github.farhazulmullick.network

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp


actual fun getHttpClientEngine(): HttpClientEngine = OkHttp.create()