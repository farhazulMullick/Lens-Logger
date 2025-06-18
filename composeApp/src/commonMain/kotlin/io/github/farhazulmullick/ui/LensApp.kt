package io.github.farhazulmullick.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.farhazulmullick.navigation.LensRoute
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val TAG = "LensApp"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LensApp(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    var showContent by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        LensFAB(){ showContent = !showContent }
        content()
    }

    if (showContent){
        LensBottomSheet(onDismiss = { showContent = !showContent }) {
            LensContent()
        }
    }
}

@Composable
internal fun LensContent(){
    val navController = rememberNavController()
    Scaffold(modifier = Modifier.fillMaxSize()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(
                startDestination = LensRoute.NetLogScreen,
                navController = navController
            ) {
                composable<LensRoute.NetLogScreen> {
                    NetLoggingScreen() {
                        // navigate to details screen.
                        navController.navigate(LensRoute.NetLogInfoScreen(index = it))
                    }
                }
                composable<LensRoute.NetLogInfoScreen> {entry ->
                    val data: LensRoute.NetLogInfoScreen = entry.toRoute<LensRoute.NetLogInfoScreen>()
                    NetLoggingInfoScreen(index = data.index) {
                        navController.navigateUp()
                    }
                }
            }
        }
    }
}

@Serializable
data class Comment(
    @SerialName("postId")
    val postId: Int?,
    @SerialName("id")
    val id: Int?,
    @SerialName("name")
    val name: String?,
    @SerialName("email")
    val email: String?,
    @SerialName("body")
    val body: String?
)