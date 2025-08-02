package io.github.farhazulmullick.lenslogger.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializerOrNull
import org.jetbrains.compose.resources.DrawableResource
import kotlin.reflect.KClass

@Serializable
sealed class LensRoute {
    @Serializable
    object NetLogScreen: LensRoute()

    @Serializable
    data class NetLogInfoScreen(val index: Int): LensRoute()

    @Serializable
    object DataStoreLogScreen: LensRoute()

    @Serializable
    data class DataStoreLogInfoScreen(val index: Int): LensRoute()
}


sealed class TabItems(
    val route: String,
    val label: String,
    val icon: DrawableResource? = null,
    val contentDescription: String? = null
) {
    data object Network : TabItems(
        route = getFullyQualifiedName(LensRoute.NetLogScreen::class),
        label = "Network",
    )
}

enum class TabDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val contentDescription: String
) {
    Network("network", "Network", Icons.Default.WifiFind, "Network Tab"),
    DataStore("datastore", "Datastore", Icons.Default.Storage, "Datastore Tab"),
}

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
fun getFullyQualifiedName(clazz: KClass<*>): String {
    return clazz.serializerOrNull()?.run { descriptor.serialName } ?: "Unknown"
}

