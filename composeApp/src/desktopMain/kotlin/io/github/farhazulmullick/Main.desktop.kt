package io.github.farhazulmullick

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Lens Logger Desktop",
    ) {
        MaterialTheme(
            colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme())
                androidx.compose.material3.darkColorScheme()
            else
                androidx.compose.material3.lightColorScheme()
        ) {
            App()
        }
    }
}