package io.github.farhazulmullick.lenslogger.ui

import android.app.Activity
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Configuration for [LensAndroid.install], applied whenever an overlay is attached to an activity.
 */
class LensInstallConfiguration(
    /**
     * Supplies DataStore instances for the DataStore tab. Prefer [Context.getApplicationContext]
     * inside the lambda if you cache results, to avoid retaining an [Activity].
     */
    val dataStoresProvider: (Context) -> List<DataStore<Preferences>> = { emptyList() },
    val showLensFAB: Boolean = true,
    val sheetGesturesEnabled: Boolean = false,
    /**
     * Return false to skip installing the overlay (e.g. splash or third-party activities).
     */
    val activityFilter: (Activity) -> Boolean = { true },
)
