package io.github.farhazulmullick.network

import io.ktor.client.engine.darwin.Darwin

actual fun getHttpClientEngine() = Darwin.create()
