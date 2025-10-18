package io.github.farhazulmullick.lenslogger.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.farhazulmullick.lenslogger.modal.NetworkLogs
import io.github.farhazulmullick.lenslogger.modal.Resource
import io.github.farhazulmullick.lenslogger.modal.contentLength
import io.github.farhazulmullick.lenslogger.modal.getRequestedAgoTime
import io.github.farhazulmullick.lenslogger.plugin.network.LensKtorStateManager
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.encodedPath
import lens.lens_logger.generated.resources.Res
import lens.lens_logger.generated.resources.logger_no_data
import org.jetbrains.compose.resources.painterResource
import kotlin.time.ExperimentalTime

@Composable
fun NetLoggingScreen(
    onItemClick: (Int) -> Unit
) {
    val logs: SnapshotStateList<NetworkLogs> = LensKtorStateManager.stateCalls
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        if (logs.isNotEmpty()) listState.scrollToItem(logs.lastIndex)
    }

    LazyColumn(
        reverseLayout = true,
        modifier = Modifier.fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surfaceContainer),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(8.dp),
        state = listState
    ) {
        item {
            AnimatedVisibility(logs.isEmpty()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    VSpacer(24.dp)
                    Image(
                        modifier = Modifier.size(100.dp),
                        painter = painterResource(Res.drawable.logger_no_data), contentDescription = null
                    )

                    VSpacer(12.dp)
                    Text(
                        text = "No, Network Calls Found.",
                        style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        itemsIndexed (logs) {index, item ->
            NetLogCard(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onItemClick(index) }
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(8.dp)
                ,
                netLog = item
            )
        }
    }
}

@OptIn(ExperimentalTime::class)
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
        Row(modifier = Modifier.fillMaxWidth()) {
            when(netLog.response) {
                is Resource.Loading -> {
                    Text("In Progress...", style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace))
                }
                else -> {
                    // Status Code icon
                    netLog.responseData?.apply {
                        StatusCodeIcon(status)
                        // Status Code Text
                        HSpacer(8.dp)
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

        // Endpoint Path
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
                    // Http-method name
                    Text(it.method.value, style = MaterialTheme.typography.labelLarge
                        .copy(fontFamily = FontFamily.Monospace)
                    )
                    // uploaded data.
                    Icon(modifier = Modifier.alpha(0.5f), imageVector = Icons.Outlined.Upload, contentDescription = null)
                    Text(it.contentLength().toString(), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))

                    // downloaded data
                    Icon(modifier = Modifier.alpha(0.5f),  imageVector = Icons.Filled.Download, contentDescription = null)
                    Text(netLog.responseData?.contentLength.toString(), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))

                    // Requested ago
                    netLog.responseData?.getRequestedAgoTime() ?.let {
                        Icon(modifier = Modifier.alpha(0.5f),  imageVector = Icons.Filled.History, contentDescription = null)
                        Text("$it ago", style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
                    }
                }
            }

            // Response Time
            Row (horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically){
                Icon(
                    imageVector = Icons.Outlined.ElectricBolt,
                    modifier = Modifier.alpha(0.5f)
                        .size(16.dp),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface
                )
                netLog.responseTime?.let {
                    Text(it.toString() + "ms",
                        style = MaterialTheme.typography.labelSmall
                        .copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }
        }
    }
}