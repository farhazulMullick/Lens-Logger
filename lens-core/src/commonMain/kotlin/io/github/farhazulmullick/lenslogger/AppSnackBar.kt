package io.github.farhazulmullick.lenslogger

import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Global snackbar events; the Compose UI layer collects [snackBarMsgFlow] and shows them.
 */
object AppSnackBar {
    val scope = CoroutineScope(Dispatchers.Main)

    enum class SnackBarActionType {
        CROSS,
        DISMISS,
    }

    data class SnackBarData(
        val message: String,
        val duration: SnackbarDuration = SnackbarDuration.Short,
        val snackBarActionType: SnackBarActionType = SnackBarActionType.DISMISS,
    )

    private val _snackBarMsgFlow = MutableSharedFlow<SnackBarData>()
    val snackBarMsgFlow: SharedFlow<SnackBarData> = _snackBarMsgFlow.asSharedFlow()

    fun showSnackBar(snackBarData: SnackBarData) {
        scope.launch {
            _snackBarMsgFlow.emit(snackBarData)
        }
    }

    fun showSnackBar(
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short,
        snackBarActionType: SnackBarActionType = SnackBarActionType.DISMISS,
    ) {
        showSnackBar(SnackBarData(message, duration, snackBarActionType))
    }
}
