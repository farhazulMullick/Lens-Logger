package io.github.farhazulmullick.lenslogger.plugin.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath
import java.io.File

private val storeInstance: DataStore<Preferences> by lazy {
    val home = System.getProperty("user.home") ?: "."
    val file = File(home, ".lens-logger/lens_mocks.preferences_pb").apply {
        parentFile?.mkdirs()
    }
    PreferenceDataStoreFactory.createWithPath { file.absolutePath.toPath() }
}

internal actual fun lensInternalDataStore(): DataStore<Preferences> = storeInstance
