package io.github.farhazulmullick.lenslogger

import androidx.annotation.UiThread
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

val LocalSnackBarHostState =
    staticCompositionLocalOf<SnackbarHostState> { error("No ScaffoldState Provided") }

/**
 * Can be called inside a Composable fn
 * @param message message
 * @param duration snack-bar duration
 */
@Composable
fun showSnackBar(
    message: String, duration: SnackbarDuration = SnackbarDuration.Short
) {
    val scope = rememberCoroutineScope()
    val scaffoldState = LocalSnackBarHostState.current

    scope.launch {
        scaffoldState.showSnackbar(message = message, duration = duration)
    }
}

/**
 * Avoid using this method in your Compose-ui LaunchEffect.
 * If your snack-bar is showing message and at the same time you are attempting to navigate then
 * c-scope will be destroyed which will dismiss the snack-bar. This method must be called at AppTheme level.
 * Better use [AppSnackBar.showSnackBar]
 * to show toast on any composable-ui.
 *
 * @param message message
 * @param duration snack-bar duration
 * @param snackbarHostState pass scaffold state,
 * @param scope coroutine scope
 */
@UiThread
internal fun showSnackBar(
    message: String,
    duration: SnackbarDuration = SnackbarDuration.Short,
    snackBarHostState: SnackbarHostState,
    actionLabel: String? = null,
    scope: CoroutineScope
) {
    scope.launch {
        snackBarHostState.showSnackbar(message = message, duration = duration, actionLabel = actionLabel)
    }
}

/**
 * Can be called inside a Composable fn
 * @param message string from Compose resources
 * @param duration snack-bar duration
 */
@Composable
internal fun showSnackBar(
    message: StringResource, duration: SnackbarDuration = SnackbarDuration.Short
) {
    showSnackBar(message = stringResource(message), duration)
}
