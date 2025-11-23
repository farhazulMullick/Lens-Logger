package io.github.farhazulmullick.server

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

actual object ProxyServer {
    private val server = embeddedServer(Netty, 8090) {
        configureProxyServer()
    }

    fun start() {
        server.start(wait = false)
    }

    fun stop() {
        server.stop(1_000, 2_000)
    }
}

fun Application.configureProxyServer() {
    routing {
        get(path = "/my-endpoint") {
            call.respond(
                status = HttpStatusCode.OK,
                message = "Hello from android proxy server!"
            )
        }
        post(path = "/my-endpoint") {
            call.respond(
                status = HttpStatusCode.OK,
                message = "Hello from android proxy server!"
            )
        }
    }
}