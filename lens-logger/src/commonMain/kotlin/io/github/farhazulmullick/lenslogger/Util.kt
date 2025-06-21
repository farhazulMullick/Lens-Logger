package io.github.farhazulmullick.lenslogger

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.content.TextContent
import io.ktor.http.content.ByteArrayContent
import io.ktor.http.content.OutgoingContent.NoContent

fun HttpRequestBuilder.generateCurl(): String {
    val method = method.value
    val url = url.toString()
    val headers = headers.entries().joinToString(" ") { (k, v) ->
        v.joinToString(" ") { value -> "-H \"$k: $value\"" }
    }

    val body = when (val content = body) {
        is io.ktor.client.request.forms.FormDataContent -> {
            content.formData.entries().joinToString(" ") { (key, value) ->
                "-F \"$key=$value\""
            }
        }
        is ByteArrayContent -> {
            // Save to a temp file or use a placeholder
            "--data-binary @yourfile.bin" // Todo: Research here
        }
        is TextContent -> {
            "-d '${content.text}'"
        }
        is NoContent -> ""
        else -> ""
    }

    return "curl -X $method $headers $body \"$url\""
}