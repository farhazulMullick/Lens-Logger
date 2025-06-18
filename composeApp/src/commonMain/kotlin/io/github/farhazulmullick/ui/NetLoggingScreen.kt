package io.github.farhazulmullick.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import io.github.farhazulmullick.lensktor.modal.NetworkLogs
import io.github.farhazulmullick.lensktor.modal.Resource
import io.github.farhazulmullick.lensktor.modal.contentLength
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
                    Text("In Progress...", style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace))
                }
                else -> {
                    netLog.responseData?.apply {
                        Text(
                            status.toString(),
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Path
        request?.url?.encodedPath?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge
                .copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            request?.let {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    // method
                    Text(it.method.value, style = MaterialTheme.typography.labelLarge
                        .copy(fontFamily = FontFamily.Monospace)
                    )
                    // uploaded data.
                    Icon(modifier = Modifier.alpha(0.5f), imageVector = Icons.Outlined.Upload, contentDescription = null)
                    Text(it.contentLength().toString(), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
                    // downloaded data
                    Icon(modifier = Modifier.alpha(0.5f),  imageVector = Icons.Filled.Download, contentDescription = null)
                    Text(netLog.responseData?.contentLength.toString(), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
                }
            }

            Row (horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically){
                Image(imageVector = Icons.Outlined.NetworkCheck,
                    modifier = Modifier.alpha(0.5f)
                        .size(16.dp),
                    contentDescription = null
                )
                netLog.responseTime?.let {
                    // time ago
                    Text(it.toString() + "ms",
                        style = MaterialTheme.typography.labelSmall
                        .copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }
        }
    }
}