package io.github.farhazulmullick.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

val STORE_FILE_1 = "store_file_1.preferences.pb"
val STORE_FILE_2 = "store_file_2.preferences.pb"
val STORE_FILE_3 = "store_file_3.preferences.pb"

fun createDataStore(
    producePath: () -> String
): DataStore<Preferences> =

    PreferenceDataStoreFactory.createWithPath(
        corruptionHandler = null,
        migrations = emptyList(),
        produceFile = { producePath().toPath() },
    )
