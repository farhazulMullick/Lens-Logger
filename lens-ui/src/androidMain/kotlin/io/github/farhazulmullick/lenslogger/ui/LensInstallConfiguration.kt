package io.github.farhazulmullick.lenslogger.ui

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Configuration for [LensAndroid.install].
 */
class LensInstallConfiguration(
    /**
     * Supplies DataStore instances for the DataStore tab in [LensActivity].
     * Prefer [Context.getApplicationContext] inside the lambda if you cache results.
     */
    val dataStoresProvider: (Context) -> List<DataStore<Preferences>> = { emptyList() },
)
