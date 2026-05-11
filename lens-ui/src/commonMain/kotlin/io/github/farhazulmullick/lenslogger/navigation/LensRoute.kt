package io.github.farhazulmullick.lenslogger.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
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

    @Serializable
    object MockListScreen: LensRoute()

    /**
     * @param sourceLogIndex index into [io.github.farhazulmullick.lenslogger.plugin.network.LensNetworkLogStore.stateCalls]
     *  used to prefill the editor when creating a new mock from a log entry. -1 if not applicable.
     * @param ruleId existing [io.github.farhazulmullick.lenslogger.plugin.network.MockRule] id when editing; null when creating.
     */
    @Serializable
    data class MockEditorScreen(val sourceLogIndex: Int = -1, val ruleId: String? = null): LensRoute()
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
    Mocks("mocks", "Mocks", Icons.Default.SwapHoriz, "Mocks Tab"),
}

@OptIn(InternalSerializationApi::class, ExperimentalSerializationApi::class)
fun getFullyQualifiedName(clazz: KClass<*>): String {
    return clazz.serializerOrNull()?.run { descriptor.serialName } ?: "Unknown"
}

