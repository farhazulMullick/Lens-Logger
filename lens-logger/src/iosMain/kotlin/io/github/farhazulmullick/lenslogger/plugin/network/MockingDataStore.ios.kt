package io.github.farhazulmullick.lenslogger.plugin.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
private val storeInstance: DataStore<Preferences> by lazy {
    val docsUrl: NSURL = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null
    ) ?: error("Could not resolve iOS Documents directory")
    val path = (docsUrl.path ?: error("NSURL.path was null")) + "/lens_mocks.preferences_pb"
    PreferenceDataStoreFactory.createWithPath { path.toPath() }
}

internal actual fun lensInternalDataStore(): DataStore<Preferences> = storeInstance
