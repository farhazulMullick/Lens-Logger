package io.github.farhazulmullick.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.farhazulmullick.navigation.LensRoute
import io.github.farhazulmullick.network.HttpKtorClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HeadersBuilder
import io.ktor.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LensApp(
    content: @Composable () -> Unit = {}
) {
    var showContent by remember { mutableStateOf(false) }
    Button(onClick = { showContent = !showContent }) {
        Text("Click me!")
    }
    LaunchedEffect(Unit) {
        this.launch (Dispatchers.IO){
            val client = HttpKtorClient(
                hostURL = Url(urlString = "https://jsonplaceholder.typicode.com").host,
                headers = {
                    HeadersBuilder().apply {
                        append("header1", "Data_1")
                        append("header2", "Data_2")
                    }
                }
            )
            for (i in 0..2) {
                launch {
                    client.get(urlString = "https://jsonplaceholder.typicode.com/v1/post/$i")
                }
            }
        }
    }
    content()

    if (showContent)
    LensBottomSheet(onDismiss = {
        showContent = !showContent
    }) {
        LensContent()
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
                    NetLoggingScreen()
                }

                composable<LensRoute.NetLogInfoScreen> {}
            }
        }
    }
}