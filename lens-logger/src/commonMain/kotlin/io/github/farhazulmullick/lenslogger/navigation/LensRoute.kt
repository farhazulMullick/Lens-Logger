package io.github.farhazulmullick.lenslogger.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class LensRoute {
    @Serializable
    object NetLogScreen: LensRoute()

    @Serializable
    data class NetLogInfoScreen(val index: Int): LensRoute()
}