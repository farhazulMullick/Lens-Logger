package io.github.farhazulmullick.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LensBottomSheet(
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    sheetShape: Shape = RoundedCornerShape(
        topStart = 16.dp,
        topEnd = 16.dp
    ),
    sheetElevation: Dp = 0.dp,
    sheetState: SheetState = rememberModalBottomSheetState(),
    scrimColor:Color = MaterialTheme.colorScheme.scrim,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    containerColor:Color = Color.Transparent,
    contentColor:Color = Color.Transparent,
    showCross: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val windowInsets = BottomSheetDefaults.windowInsets
    val scope = rememberCoroutineScope()
    ModalBottomSheet(
        scrimColor = scrimColor,
        onDismissRequest = {
            scope.launch { sheetState.hide() }.invokeOnCompletion {
                onDismiss()
            }
        },
        sheetState = sheetState,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    /**
                     * Google is focused on providing edge-to-edge experience with ModalBottomSheet.
                     * So the content of sheet by default overlaps with the navigation bar.
                     * In order to fix this overlapping they provided [Modifier.navigationBarsPadding()]
                     * Which works for both 3-button and gestures-bar.
                     */
                    .systemBarsPadding()
                    .imePadding(), horizontalAlignment = Alignment.CenterHorizontally
            ) {

                if(showCross){
                    CrossButton(
                        sheetState = sheetState,
                        onDismiss = onDismiss,
                        backgroundColor = backgroundColor
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                }

                Column(
                    modifier = Modifier
                        .clip(sheetShape)
                        .background(backgroundColor)
                        .fillMaxWidth() then modifier,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SheetHandle()
                    Spacer(modifier = Modifier.height(24.dp))
                    Box { content() }
                }
            }
        },
        contentColor = contentColor,
        containerColor = containerColor,
        tonalElevation = sheetElevation,
        dragHandle = null,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true)
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
private fun CrossButton(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    backgroundColor: Color,
) {
    lightColorScheme()
    val coroutineScope = rememberCoroutineScope()
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .background(
                shape = CircleShape,
                color = backgroundColor
            )
            .clickable(
                interactionSource = MutableInteractionSource(),
                indication = ripple(radius = 16.dp)
            ) {
                coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                    onDismiss()
                }
            }
    ) {
        Image(
            imageVector = Icons.Outlined.Close,
            contentDescription = null,
            modifier = Modifier,
        )
    }
}

@Composable
private fun SheetHandle() =
    Box(
        modifier = Modifier
            .background(
                MaterialTheme.colorScheme.onBackground,
                RoundedCornerShape(6.dp)
            )
            .width(32.dp)
            .height(3.dp)
    )
