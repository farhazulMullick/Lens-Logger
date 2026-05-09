package io.github.farhazulmullick.lenslogger

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

expect fun platform(): String

object Platform {
    val snackBarState = SnackbarHostState()
    private val scope = MainScope()

    fun showSnackBar(message: String, time: SnackbarDuration = SnackbarDuration.Short) {
        scope.launch {
            snackBarState.showSnackbar(message, duration = time)
        }
    }
}