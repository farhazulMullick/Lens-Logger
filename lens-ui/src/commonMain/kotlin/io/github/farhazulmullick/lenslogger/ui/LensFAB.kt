package io.github.farhazulmullick.lenslogger.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LensFAB(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { /* No-op */ }
) {
    BoxWithConstraints(modifier) {
        val nudgeSize = 56.dp
        val maxWidthPx = with(LocalDensity.current) { maxWidth.toPx() }
        val maxHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

        var maxWidthPxState by remember { mutableStateOf(maxWidthPx) }
        var maxHeightPxState by remember { mutableStateOf(maxHeightPx)}

        val nudgeSizePx = with(LocalDensity.current) { nudgeSize.toPx() }
        val scope = rememberCoroutineScope()

        val offsetX: Animatable<Float, AnimationVector1D> by remember { derivedStateOf { Animatable(maxWidthPx-nudgeSizePx)} }
        val offsetY: Animatable<Float, AnimationVector1D> by remember { derivedStateOf { Animatable(maxHeightPx / 3 - nudgeSizePx) } }

        //println("LensFAB: maxWidthPx = $maxWidthPx, maxHeightPx = $maxHeightPx, nudgeSizePx = $nudgeSizePx")
        LaunchedEffect(maxHeightPx, maxWidthPx) {
            maxHeightPxState = maxHeightPx
            maxWidthPxState = maxWidthPx
            val targetX = if (offsetX.value < (maxWidthPx - nudgeSizePx) / 2) 0f else (maxWidthPx - nudgeSizePx)
            scope.launch {
                offsetX.animateTo(targetX, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), offsetY.value.roundToInt()) }
                .clickable(interactionSource = MutableInteractionSource(), indication = null) { onClick() }
                .size(nudgeSize)
                .clip(RoundedCornerShape(100))
                .background(color = MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(100))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val newX = (offsetX.value + dragAmount.x).coerceIn(0f, maxWidthPxState - nudgeSizePx)
                            val newY = (offsetY.value + dragAmount.y).coerceIn(0f, maxHeightPxState - nudgeSizePx)
                            scope.launch {
                                offsetX.snapTo(newX)
                                offsetY.snapTo(newY)
                            }
                        },
                        onDragEnd = {
                            // Stick to nearest horizontal edge
                            //println("LensFAB: onDragEnd maxWidthPx = $maxWidthPx, maxHeightPx = $maxHeightPx, nudgeSizePx = $nudgeSizePx")
                            val targetX = if (offsetX.value < (maxWidthPxState - nudgeSizePx) / 2) 0f else (maxWidthPxState - nudgeSizePx)
                            scope.launch {
                                offsetX.animateTo(targetX, animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium))
                            }
                        }
                    )
                }
        ){
            Icon(
                imageVector = Icons.Outlined.NetworkCheck,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}