package io.github.farhazulmullick.lenslogger.ui

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Holds resolved [DataStore] instances for [LensActivity] when using [LensEntryMode.PERSISTENT_NOTIFICATION].
 */
internal object LensRuntimeState {

    @Volatile
    private var cached: List<DataStore<Preferences>> = emptyList()

    fun dataStores(): List<DataStore<Preferences>> = cached

    fun installFrom(application: Application, configuration: LensInstallConfiguration) {
        synchronized(this) {
            cached = configuration.dataStoresProvider(application.applicationContext)
        }
    }

    fun clear() {
        synchronized(this) {
            cached = emptyList()
        }
    }
}
