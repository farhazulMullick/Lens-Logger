package io.github.farhazulmullick.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.farhazulmullick.lensktor.modal.NetworkLogs
import io.github.farhazulmullick.lensktor.modal.Resource
import io.github.farhazulmullick.lensktor.plugin.network.LensKtorStateManager
import io.ktor.client.call.body
import io.ktor.client.statement.request
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.InternalAPI
import io.ktor.utils.io.charsets.Charset
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetLoggingInfoScreen(index: Int) {
    Scaffold (
        modifier = Modifier,
        topBar = {
            TopAppBar(
                modifier = Modifier
                    .offset(y = (-12).dp),
                title = {
                    Text(text = "Api Details", style = MaterialTheme.typography.titleLarge)
                }, navigationIcon = {
                    IconButton(onClick = {
                    }) {
                        Image(imageVector = Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        }
    ){
        Column (modifier = Modifier.padding(it).fillMaxSize()) {
            val pagerState = rememberPagerState() { 2 }
            val scope = rememberCoroutineScope()
            val netLogs: NetworkLogs? = LensKtorStateManager.stateCalls.getOrNull(index)
            Row(modifier = Modifier.fillMaxWidth()) {
                // Request-tab
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            scope.launch {
                                pagerState.scrollToPage(0)
                            }
                        }
                ) {
                    Text("Request", modifier = Modifier)
                }

                // Response-tab
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            scope.launch {
                                pagerState.scrollToPage(1)
                            }
                        }
                ) {
                    Text("Response")
                }
            }

            HorizontalPager(pagerState,
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxSize()) {page ->
                when(page) {
                    0 -> RequestPageUI(
                        modal = netLogs,
                        modifier = Modifier.padding(16.dp)
                    )
                    1 -> ResponsePageUI(
                        modal = netLogs,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(InternalAPI::class)
@Composable
fun ResponsePageUI(
    modal: NetworkLogs? = null,
    modifier: Modifier = Modifier
) {
    Column (modifier = modifier.verticalScroll(rememberScrollState())){
        val scope = rememberCoroutineScope()
        when(modal?.response) {
            is Resource.Loading -> {
                Text("Please wait...")
            }
            is Resource.Success -> {
                modal.responseData?.let {
                    // status
                    Row {
                        Text(text="Status ->")
                        Text(text = "${it.status}")
                    }

                    // space
                    VSpacer(12.dp)

                    // url
                    Text(text = "Url ->")
                    Text(text = it.request.url.toString())

                    // space
                    VSpacer(12.dp)

                    // request header
                    Text(text = "Response headers ->")
                    Text("{")
                    it.headers.entries().forEach { entry ->
                        Text("      ${entry.key} : ${entry.value}")
                    }
                    Text("}")

                    // body
                    var body by remember { mutableStateOf("") }
                    Text(text = "Response Body ->")
                    scope.launch() {
                        body = it.body<String>()
                    }
                    Text(body)
                }
            }
            is Resource.Failed -> {}

            else -> {}

        }
    }
}

@Composable
fun RequestPageUI(
    modal: NetworkLogs? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState()), ) {
        modal?.requestData?.let {
            // url
            Text(text = "Url ->")
            Text(text = it.url.toString())

            // space
            VSpacer(12.dp)

            // request header
            Text(text = "Request headers ->")

            Text("{")
            it.headers.entries().forEach { entry ->
                Text("      ${entry.key} : ${entry.value}")
            }
            Text("}")
        }
    }
}

suspend inline fun ByteReadChannel.tryReadText(charset: Charset): String? = try {
    readRemaining().readText(charset = charset)
} catch (cause: Throwable) {
    null
}