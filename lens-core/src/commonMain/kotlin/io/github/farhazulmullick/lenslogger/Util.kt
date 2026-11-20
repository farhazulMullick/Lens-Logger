package io.github.farhazulmullick.lenslogger

import io.github.farhazulmullick.lenslogger.modal.LensHttpRequestSnapshot

val String.Companion.EMPTY : String by lazy { "" }

val String?.value get() =  this ?: String.EMPTY

fun LensHttpRequestSnapshot.generateCurl(): String {
    val parts = mutableListOf<String>()
    parts.add("curl")
    parts.add("-X")
    parts.add(method)
    headers.forEach { (k, v) -> parts.add("-H \"$k: $v\"") }
    if (!bodyText.isNullOrBlank()) {
        val escaped = bodyText.replace("'", "'\\''")
        parts.add("-d '$escaped'")
    }
    parts.add("\"$url\"")
    return parts.joinToString(" ")
}
