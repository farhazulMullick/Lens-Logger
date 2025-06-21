package io.github.farhazulmullick.lensktor.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class LensRoute {
    @Serializable
    object NetLogScreen: LensRoute()

    @Serializable
    data class NetLogInfoScreen(val index: Int): LensRoute()
}