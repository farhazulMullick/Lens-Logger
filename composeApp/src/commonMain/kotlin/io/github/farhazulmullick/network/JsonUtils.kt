package io.github.farhazulmullick.network

import kotlinx.serialization.json.Json

val JSON = Json {
    isLenient = true
    /** if a field is defined as nullable in DTO and not present in JSON object from backend then that field will be skipped **/
    explicitNulls = false
    prettyPrint = true
    ignoreUnknownKeys = true
}
