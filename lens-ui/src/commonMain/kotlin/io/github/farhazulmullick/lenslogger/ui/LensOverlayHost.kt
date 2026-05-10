package io.github.farhazulmullick.lenslogger.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeGesturesPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetState
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

@Composable
internal fun LensMaterialTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme(),
        content = content,
    )
}

/**
 * Floating Lens FAB and inspector bottom sheet, without wrapping host app content.
 *
 * Used by [LensApp] and by the Android-only `LensAndroid.install` helper, which hosts this
 * composable in a full-window `ComposeView` on each activity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LensOverlayHost(
    modifier: Modifier = Modifier,
    dataStores: List<DataStore<Preferences>> = emptyList(),
    showLensFAB: Boolean = true,
    sheetGesturesEnabled: Boolean = false,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
) {
    var showContent by remember { mutableStateOf(false) }
    LensMaterialTheme {
        Box(
            modifier = Modifier
                .zIndex(Float.MAX_VALUE)
                .fillMaxSize()
                .safeGesturesPadding()
                .safeContentPadding()
                .then(modifier)
        ) {
            if (showLensFAB) {
                LensFAB(modifier = Modifier) {
                    showContent = !showContent
                }
            }
        }

        if (showContent) {
            LensBottomSheet(
                onDismiss = { showContent = false },
                sheetGesturesEnabled = sheetGesturesEnabled,
                sheetState = sheetState,
            ) {
                LensContent(dataStores)
            }
        }
    }
}
