package io.github.farhazulmullick.lensktor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.aakira.napier.Napier
import io.github.farhazulmullick.lensktor.generateCurl
import io.github.farhazulmullick.lensktor.modal.NetworkLogs
import io.github.farhazulmullick.lensktor.modal.Resource
import io.github.farhazulmullick.lensktor.plugin.network.LensKtorStateManager
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.launch

enum class TAB(val index: Int) {
    Request(0), Response(1)
}

private const val TAG = "NetLoggingInfoScreen"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetLoggingInfoScreen(
    index: Int,
    onBackClick: () -> Unit) {
    val netLogs: NetworkLogs? = LensKtorStateManager.stateCalls.getOrNull(index)
    Scaffold (
        modifier = Modifier,
        topBar = {
            TopAppBar(
                modifier = Modifier,
                title = {
                    Text(
                        text = "${netLogs?.responseData?.status?.toString()}", style = MaterialTheme.typography.titleLarge,
                        fontFamily = FontFamily.Monospace
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Image(imageVector = Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
        },
        bottomBar = {
            // using deprecated api since new clipboard api in compose 1.8 is trivial to use.
            val clipboard: ClipboardManager = LocalClipboardManager.current
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                onClick = {
                    val curl = netLogs?.requestData?.generateCurl()
                    curl ?.let { clipboard.setText(AnnotatedString(curl)) }
                    Napier.d(tag = TAG){"$TAG :: cURL :: ${netLogs?.requestData?.generateCurl()}"}
                }
            ) {
                Text("Copy cURL")
            }
        }
    ){
        Column(
            modifier = Modifier
                .padding(it)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            val pagerState = rememberPagerState() { 2 }
            val scope = rememberCoroutineScope()
            var selectedTab by remember { mutableStateOf(TAB.Request) }
            Row(modifier = Modifier.fillMaxWidth()) {
                TAB.entries.forEach { it ->
                    BoxTab(
                        modifier = Modifier
                            .weight(0.5f)
                            .clickable {
                                scope.launch {
                                    pagerState.animateScrollToPage(it.index)
                                    selectedTab = it
                                }
                            },
                        tabTitle = it.name,
                        isSelected = selectedTab == it
                    )
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

            LaunchedEffect(pagerState) {
                // Collect from the a snapshotFlow reading the currentPage
                snapshotFlow { pagerState.currentPage }.collect { page ->
                    when(page) {
                        0 -> selectedTab = TAB.Request
                        1 -> selectedTab = TAB.Response
                    }
                }
            }
        }
    }
}

@Composable
fun BoxTab(
    modifier: Modifier = Modifier,
    tabTitle: String,
    isSelected: Boolean = false
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(100))
            .background(
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surface
            )
            .padding(4.dp)
            .then(modifier)
    ) {
        Text(tabTitle, modifier = Modifier,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = FontFamily.Monospace,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        )
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
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(shape = RoundedCornerShape(8.dp))
                            .background(color = MaterialTheme.colorScheme.surfaceContainer)
                            .padding(16.dp)
                    ) {
                        Row {
                            Text(
                                text = "Status: ",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${it.status?.value}",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }

                        VSpacer(12.dp)
                        // url
                        Row {
                            Text(
                                text = "URL: ",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    ),
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = it.request?.url.toString(),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }

                    // space
                    VSpacer(12.dp)

                    // response header
                    var isRequestExpanded by remember { mutableStateOf(false) }
                    ExpandableCard(
                        title = "Response Headers",
                        isExpanded = isRequestExpanded,
                        onClick = {
                            isRequestExpanded = !isRequestExpanded
                        },
                        content = {
                            SelectionContainer {
                                Column (Modifier.fillMaxWidth()){
                                    VSpacer(16.dp)
                                    it.headers?.forEach { entry ->
                                        Text(text = "${entry.key} : ${entry.value}",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    )

                    VSpacer(12.dp)
                    // body
                    var isResponseExpanded by remember { mutableStateOf(true) }
                    ExpandableCard(
                        title = "Body",
                        isExpanded = isResponseExpanded,
                        onClick = {
                            isResponseExpanded = !isResponseExpanded
                        },
                        content = {
                            SelectionContainer {
                                Column (Modifier.fillMaxWidth()){
                                    VSpacer(16.dp)
                                    Text(
                                        modifier = Modifier.animateContentSize(), text = it.body ?: "No body found",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontFamily = FontFamily.Monospace,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    )
                }
            }
            is Resource.Failed -> {}
            else -> {}

        }
    }
}

@Composable
fun ExpandableCard(
    title: String,
    modifier: Modifier = Modifier,
    isExpanded: Boolean = false,
    onClick: () -> Unit = {},
    content: @Composable () -> Unit,
){
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(shape = RoundedCornerShape(8.dp))
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp)
            .then(modifier)
    ) {
        Row (modifier = Modifier.fillMaxWidth()
            .clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ){
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Icon(imageVector = if (!isExpanded) Icons.Outlined.KeyboardArrowDown
            else Icons.Outlined.KeyboardArrowUp,
                contentDescription = null,
                modifier = Modifier.clickable { onClick() }
            )
        }
        AnimatedVisibility(isExpanded) {
            content()
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
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(shape = RoundedCornerShape(8.dp))
                    .background(color = MaterialTheme.colorScheme.surfaceContainer)
                    .padding(16.dp)
            ) {
                // url
                Row {
                    Text(
                        text = "URL: ",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    SelectionContainer {
                        Text(
                            text = it.url.toString(),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            // space
            VSpacer(12.dp)

            // request header
            var isRequestExpanded by remember { mutableStateOf(true) }
            ExpandableCard(
                title = "Request Headers",
                isExpanded = isRequestExpanded,
                onClick = {
                    isRequestExpanded = !isRequestExpanded
                },
                content = {
                    SelectionContainer {
                        Column (Modifier.fillMaxWidth()){
                            VSpacer(16.dp)
                            it.headers.entries().forEach { entry ->
                                Text(text = "${entry.key} : ${entry.value}",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                    ),
                                )
                            }
                        }
                    }
                }
            )
        }
    }
}