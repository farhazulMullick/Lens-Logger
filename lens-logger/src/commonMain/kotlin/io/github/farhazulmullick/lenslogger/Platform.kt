package io.github.farhazulmullick.lenslogger

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

expect fun platform(): String

internal object Platform {
    val snackBarState by mutableStateOf(SnackbarHostState())
    private val scope = MainScope()

    fun showSnackBar(message: String, time: SnackbarDuration = SnackbarDuration.Short) {
        scope.launch {
            snackBarState.showSnackbar(message, duration = time)
        }
    }
}