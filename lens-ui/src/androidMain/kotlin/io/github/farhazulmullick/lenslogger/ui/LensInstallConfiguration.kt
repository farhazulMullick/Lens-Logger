package io.github.farhazulmullick.lenslogger.ui

import android.app.Activity
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Configuration for [LensAndroid.install].
 */
class LensInstallConfiguration(
    /**
     * How the Lens UI is shown. [LensEntryMode.WINDOW_OVERLAY] attaches a per-activity overlay;
     * [LensEntryMode.PERSISTENT_NOTIFICATION] shows an ongoing notification that opens [LensActivity].
     */
    val entryMode: LensEntryMode = LensEntryMode.WINDOW_OVERLAY,
    /**
     * Supplies DataStore instances for the DataStore tab. Prefer [Context.getApplicationContext]
     * inside the lambda if you cache results, to avoid retaining an [Activity].
     */
    val dataStoresProvider: (Context) -> List<DataStore<Preferences>> = { emptyList() },
    val showLensFAB: Boolean = true,
    val sheetGesturesEnabled: Boolean = false,
    /**
     * Return false to skip installing the overlay (e.g. splash or third-party activities).
     * Used only when [entryMode] is [LensEntryMode.WINDOW_OVERLAY].
     */
    val activityFilter: (Activity) -> Boolean = { true },
)
