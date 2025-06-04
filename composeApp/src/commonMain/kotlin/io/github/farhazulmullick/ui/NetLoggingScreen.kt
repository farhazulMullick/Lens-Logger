package io.github.farhazulmullick.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.farhazulmullick.lensktor.modal.NetworkLogs
import io.github.farhazulmullick.lensktor.modal.Resource
import io.github.farhazulmullick.lensktor.plugin.network.LensKtorStateManager
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.encodedPath

@Composable
fun NetLoggingScreen(
    onItemClick: (Int) -> Unit
) {
    val logs: SnapshotStateList<NetworkLogs> = LensKtorStateManager.stateCalls
    LazyColumn(
        reverseLayout = true,
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        itemsIndexed (logs) {index, item ->
            NetLogCard(
                modifier = Modifier
                    .clickable { onItemClick(index) }
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(8.dp),
                netLog = item
            )
        }
    }
}

@Composable
fun NetLogCard(
    modifier: Modifier = Modifier,
    netLog: NetworkLogs
) {
    val request: HttpRequestBuilder? = netLog.requestData

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        // Top Row: Status Code and Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            when(netLog.response) {
                is Resource.Loading -> {
                    Text("In Progress...", style = MaterialTheme.typography.titleLarge)
                }
                else -> {
                    netLog.responseData?.apply {
                        Text(
                            status.toString(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Path
        request?.url?.encodedPath?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            request?.let {
                Row {
                    // method
                    Text(it.method.value, style = MaterialTheme.typography.labelLarge)
                    // uploaded data.
                    Text("upload", style = MaterialTheme.typography.labelLarge)
                    // downloaded data
                    Text("download", style = MaterialTheme.typography.labelLarge)
                }
            }

            netLog.responseTime?.let {
                // time ago
                Text(it.toString(), fontWeight = FontWeight.Thin)
            }
        }
    }
}