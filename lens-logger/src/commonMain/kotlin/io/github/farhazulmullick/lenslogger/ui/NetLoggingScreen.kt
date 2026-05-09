package io.github.farhazulmullick.lenslogger.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.ElectricBolt
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Surface
import androidx.compose.material3.TextFieldDefaults
import io.github.farhazulmullick.lenslogger.modal.LensHttpRequestSnapshot
import io.github.farhazulmullick.lenslogger.modal.NetworkLogs
import io.github.farhazulmullick.lenslogger.modal.Resource
import io.github.farhazulmullick.lenslogger.modal.formatResponseTimeHuman
import io.github.farhazulmullick.lenslogger.modal.getRequestedAgoTime
import io.github.farhazulmullick.lenslogger.plugin.network.LensNetworkLogStore
import lens.lens_logger.generated.resources.Res
import lens.lens_logger.generated.resources.logger_no_data
import org.jetbrains.compose.resources.painterResource
import kotlin.time.ExperimentalTime

@Composable
fun NetLoggingScreen(
    onItemClick: (Int) -> Unit
) {
    val logs: SnapshotStateList<NetworkLogs> = LensNetworkLogStore.stateCalls
    val listState = rememberLazyListState()
    var searchQuery by remember { mutableStateOf("") }
    val queryTrimmed = searchQuery.trim()

    val filteredLogs = logs.mapIndexed { index, log -> index to log }
        .filter { (_, log) ->
            if (queryTrimmed.isEmpty()) true
            else log.requestData?.encodedPath?.contains(queryTrimmed, ignoreCase = true) == true
        }

    LaunchedEffect(logs.size, queryTrimmed) {
        if (logs.isNotEmpty() && queryTrimmed.isEmpty() && filteredLogs.isNotEmpty()) {
            listState.scrollToItem(filteredLogs.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            placeholder = {
                Text(
                    "Search by endpoint",
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
        )

        when {
            logs.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Image(
                            modifier = Modifier.size(100.dp),
                            painter = painterResource(Res.drawable.logger_no_data),
                            contentDescription = null
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
            filteredLogs.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No endpoints match \"$queryTrimmed\"",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }
            }
            else -> {
                LazyColumn(
                    reverseLayout = true,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = PaddingValues(8.dp),
                    state = listState
                ) {
                    itemsIndexed(filteredLogs) { _, (originalIndex, item) ->
                        NetLogCard(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onItemClick(originalIndex) }
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(8.dp),
                            netLog = item
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun MockedBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.tertiary
    ) {
        Text(
            text = "MOCK",
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = MaterialTheme.colorScheme.onTertiary,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace)
        )
    }
}

@Composable
internal fun HttpMethodBadge(
    httpMethod: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            text = httpMethod.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun NetMetricInline(
    icon: ImageVector,
    label: String,
    tint: Color,
) {
    Row(
        modifier = Modifier.heightIn(min = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            modifier = Modifier
                .alpha(0.6f)
                .size(16.dp),
            imageVector = icon,
            contentDescription = null,
            tint = tint,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@OptIn(ExperimentalTime::class, ExperimentalLayoutApi::class)
@Composable
fun NetLogCard(
    modifier: Modifier = Modifier,
    netLog: NetworkLogs
) {
    val request: LensHttpRequestSnapshot? = netLog.requestData

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
    ) {
        // Top Row: Status Code and Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            if (netLog.isMocked) {
                HSpacer(8.dp)
                MockedBadge()
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Endpoint Path
        request?.encodedPath?.let {
            Text(it, style = MaterialTheme.typography.bodyLarge
                .copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurface)
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (request != null || netLog.responseData != null || netLog.responseTime != null) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                request?.let { req ->
                    HttpMethodBadge(req.method)
                    NetMetricInline(
                        icon = Icons.Outlined.Upload,
                        label = req.contentLengthDisplay ?: "0 B",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                netLog.responseData?.contentLength?.let { received ->
                    NetMetricInline(
                        icon = Icons.Filled.Download,
                        label = received,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                netLog.responseData?.getRequestedAgoTime()?.let { ago ->
                    NetMetricInline(
                        icon = Icons.Filled.History,
                        label = "$ago ago",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }

                netLog.responseTime?.let { ms ->
                    NetMetricInline(
                        icon = Icons.Outlined.ElectricBolt,
                        label = ms.formatResponseTimeHuman(),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}