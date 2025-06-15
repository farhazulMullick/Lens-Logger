package io.github.farhazulmullick.ui

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
import androidx.compose.ui.unit.dp
import io.github.farhazulmullick.lensktor.modal.NetworkLogs
import io.github.farhazulmullick.lensktor.modal.Resource
import io.github.farhazulmullick.lensktor.plugin.network.LensKtorStateManager
import io.ktor.utils.io.InternalAPI
import kotlinx.coroutines.launch

enum class TAB(val index: Int) {
    Request(0), Response(1)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetLoggingInfoScreen(
    index: Int,
    onBackClick: () -> Unit) {
    Scaffold (
        modifier = Modifier,
        topBar = {
            TopAppBar(
                modifier = Modifier,
                title = {
                    Text(text = "Api Details", style = MaterialTheme.typography.titleLarge)
                }, navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Image(imageVector = Icons.AutoMirrored.Filled.ArrowBack, null)
                    }
                }
            )
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
            val netLogs: NetworkLogs? = LensKtorStateManager.stateCalls.getOrNull(index)
            var selectedTab by remember { mutableStateOf(TAB.Request) }
            Row(modifier = Modifier.fillMaxWidth()) {
                TAB.entries.forEach { it ->
                    BoxTab(
                        modifier = Modifier
                            .weight(1f)
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
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${it.status}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        VSpacer(12.dp)
                        // url
                        Row {
                            Text(
                                text = "Url: ", style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = it.request?.url.toString(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // space
                    VSpacer(12.dp)

                    // request header
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
                                            style = MaterialTheme.typography.bodyLarge
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
                                        color = MaterialTheme.colorScheme.onSurface
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