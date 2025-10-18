package io.github.farhazulmullick

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.window.ComposeUIViewController
import io.github.farhazulmullick.datastore.STORE_FILE_1
import io.github.farhazulmullick.datastore.STORE_FILE_2
import io.github.farhazulmullick.datastore.STORE_FILE_3
import io.github.farhazulmullick.datastore.createDataStore
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
private val documentDirectoryUrl: NSURL? by lazy {
    NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
}

fun MainViewController() = ComposeUIViewController {
    val dataStores = listOf(
        createDataStore {
            requireNotNull(documentDirectoryUrl).path + "/" + STORE_FILE_1
        },
        createDataStore { requireNotNull(documentDirectoryUrl).path + "/" + STORE_FILE_2 },
        createDataStore { requireNotNull(documentDirectoryUrl).path + "/" + STORE_FILE_3 },
    )
    MaterialTheme(
        colorScheme = if (androidx.compose.foundation.isSystemInDarkTheme())
            androidx.compose.material3.darkColorScheme()
        else
            androidx.compose.material3.lightColorScheme()
    ) {
        App()
    }
}