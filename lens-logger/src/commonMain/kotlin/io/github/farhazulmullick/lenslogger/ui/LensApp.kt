package io.github.farhazulmullick.lenslogger.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeGesturesPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.farhazulmullick.lenslogger.navigation.LensRoute
import io.github.farhazulmullick.lenslogger.navigation.TabDestination

private const val TAG = "LensApp"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LensApp(
    modifier: Modifier = Modifier,
    dataStores: List<DataStore<Preferences>> = emptyList(),
    showLensFAB: Boolean = true,
    content: @Composable () -> Unit = {},
) {
    var showContent by remember { mutableStateOf(false) }
    MaterialTheme(
        colorScheme = if(isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    ) {
        Box(
            modifier = Modifier
                .zIndex(Float.MAX_VALUE)
                .fillMaxSize()
                .safeGesturesPadding()
                .safeContentPadding()
                .then(modifier)
        ) {
            // Lens FAB to show bottom sheet.
            if (showLensFAB) {
                LensFAB(modifier = Modifier) {
                    showContent = !showContent
                }
            }
        }

        if (showContent){
            LensBottomSheet(onDismiss = { showContent = !showContent }) {
                LensContent(dataStores)
            }
        }
    }
    content()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LensContent(
    dataStores: List<DataStore<Preferences>> = emptyList(),
){
    val navController: NavHostController = rememberNavController()
    val startDestination = TabDestination.Network
    var selectedDestination by rememberSaveable { mutableIntStateOf(startDestination.ordinal) }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            PrimaryTabRow(selectedTabIndex = selectedDestination) {
                TabDestination.entries.forEachIndexed { index, destination ->
                    Tab(
                        selected = selectedDestination == index,
                        onClick = {
                            navController.navigate(route = destination.route)
                            selectedDestination = index
                        },
                        text = {
                            Text(
                                text = destination.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }) { contentPadding ->
        AppNavHost(
            modifier = Modifier.padding(contentPadding),
            startDestination = startDestination,
            navController = navController,
            dataStores = dataStores
        )
    }
}

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    startDestination: TabDestination,
    dataStores: List<DataStore<Preferences>> = emptyList(),
) {
    NavHost(
        modifier = modifier,
        startDestination = startDestination.route,
        navController = navController
    ) {
        composable(TabDestination.Network.route) {
            NetLoggingScreen() {
                // navigate to details screen.
                navController.navigate(LensRoute.NetLogInfoScreen(index = it))
            }
        }

        composable(TabDestination.DataStore.route) {
            AllDatastoreListingScreen(dataStores) {}
        }

        composable<LensRoute.NetLogInfoScreen> { entry ->
            val data: LensRoute.NetLogInfoScreen = entry.toRoute<LensRoute.NetLogInfoScreen>()
            NetLoggingInfoScreen(index = data.index) {
                navController.navigateUp()
            }
        }

        composable<LensRoute.DataStoreLogInfoScreen> { entry ->
            val data: LensRoute.DataStoreLogInfoScreen = entry.toRoute()
            DatastoreLoggingInfoScreen(index = data.index){
                navController.navigateUp()
            }
        }
    }
}