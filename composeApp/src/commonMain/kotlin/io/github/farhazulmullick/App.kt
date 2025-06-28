package io.github.farhazulmullick

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.farhazulmullick.lenslogger.ui.LensApp
import io.github.farhazulmullick.modal.Comment
import io.github.farhazulmullick.network.HttpKtorClient
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.HeadersBuilder
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.path
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    LensApp(showLensFAB = true){
        AppContent()
    }
}

@Composable
fun AppContent() {
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
    Scaffold() {
        Column(modifier = Modifier.padding(it).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = {
                scope.launch {
//                    val response = client.get(urlString = "https://jsonplaceholder.typicode.com/comments?postId=100")
//                    val body = response.body<String>()
//                    client.post(urlString = "https://jsonplaceholder.typicode.com/posts", block = {
//                        contentType(type = io.ktor.http.ContentType.Application.Json)
//                        setBody(
//                            Comment(
//                                postId = 100,
//                                id = null,
//                                name = "Sample Name",
//                                email = "",
//                                body = "Sample Body"
//                            )
//                        )
//                    })
                    client.request {
                        method = HttpMethod.Post
                        contentType(io.ktor.http.ContentType.Application.Json)
                        setBody(Comment(
                            postId = 100,
                            id = null,
                            name = "Sample Name",
                            email = "",
                            body = "Sample Body"
                        ))
                        url { path("/posts") }
                    }
                }
            }) {
                Text("Make request!")
            }
        }
    }
}