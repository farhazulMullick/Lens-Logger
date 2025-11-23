package io.github.farhazulmullick

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.aakira.napier.Napier
import io.github.farhazulmullick.datastore.STORE_FILE_1
import io.github.farhazulmullick.datastore.STORE_FILE_2
import io.github.farhazulmullick.datastore.STORE_FILE_3
import io.github.farhazulmullick.datastore.createDataStore
import io.github.farhazulmullick.lenslogger.plugin.datastore.BaseDataStorePrefs
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import java.nio.file.Paths

fun main() = application {
    val workingDir = System.getProperty("user.home")
    val filePath = Paths.get(workingDir).toAbsolutePath()
    Napier.d("File path: $filePath")

    val stores = listOf(
        createDataStore { "$filePath/$STORE_FILE_1" },
        createDataStore { "$filePath/$STORE_FILE_2" },
        createDataStore { "$filePath/$STORE_FILE_3" }
    )

    MainScope().launch {
        BaseDataStorePrefs(stores[0])
            .setString("project_name", "LensLogger")
    }
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
            App(stores)
        }
    }
}