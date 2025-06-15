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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.aakira.napier.Napier
import io.github.farhazulmullick.navigation.LensRoute
import io.github.farhazulmullick.network.HttpKtorClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HeadersBuilder
import io.ktor.http.Url
import kotlinx.coroutines.launch
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
    val scope = rememberCoroutineScope()
    val client = remember {
        HttpKtorClient(
            hostURL = Url(urlString = "https://jsonplaceholder.typicode.com").host,
            headers = {
                HeadersBuilder().apply {
                    append("header1", "Data_1")
                    append("header2", "Data_2")
                }
            }
        )
    }

    Box(modifier = modifier) {
        LensFAB(){
            showContent = !showContent
            if(showContent) {
                scope.launch {
                    val a = client.get(urlString = "https://jsonplaceholder.typicode.com/comments?postId=1")
                    val body = a.body<String>()
                    Napier.d(tag = TAG) { " response :: hashcode :: ${a.hashCode()}, body :: $body" }
                }
            }
        }
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