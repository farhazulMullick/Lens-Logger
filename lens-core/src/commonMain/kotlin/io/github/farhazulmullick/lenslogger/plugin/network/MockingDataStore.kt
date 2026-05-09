package io.github.farhazulmullick.lenslogger.plugin.network

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

/**
 * Provides an internal `DataStore<Preferences>` used by [LensMockingStateManager] to persist
 * user-defined mock rules across app restarts. Each platform supplies its own file location.
 */
internal expect fun lensInternalDataStore(): DataStore<Preferences>
