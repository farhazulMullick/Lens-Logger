package io.github.farhazulmullick.lenslogger.plugin.datastore

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.firstOrNull

sealed class EditAction {
    object Staging  : EditAction()
    object Commited   : EditAction()
}

data class StoreEntryItem (
    val key: String,
    val value: String?,
    val dataType: DataType,
    val editAction: EditAction,
    val dataStore: BaseDataStorePrefs
)

enum class DataType {
    INT,
    STRING,
    LONG,
    FLOAT,
    DOUBLE,
    BOOLEAN,
    NONE
}

private const val tag = "LensDatastoreStateManager"

internal object LensDatastoreStateManager {

    /**
     * Master DataStores List. It contains all dataStores.
     */
    val allDataStores: SnapshotStateList<Pair<String, BaseDataStorePrefs>> = mutableStateListOf()

    val currentDataStoreEntry: SnapshotStateList<StoreEntryItem> = mutableStateListOf()

    fun addStore(fileName: String, dataStore: DataStore<Preferences>) {
        allDataStores.add(fileName to BaseDataStorePrefs(dataStore))
    }

    suspend fun loadAllEntriesFromDataStore(index: Int) {
        if (index !in allDataStores.indices) return
        val prefs: BaseDataStorePrefs = allDataStores[index].second

        // clear current list.
        currentDataStoreEntry.clear()

        prefs.getEntriesAsMap()?.forEach { entry ->

            val dataType = when(entry.value) {
                is Int -> DataType.INT
                is Long -> DataType.LONG
                is Float -> DataType.FLOAT
                is Double -> DataType.DOUBLE
                is String -> DataType.STRING
                is Boolean -> DataType.BOOLEAN
                else -> DataType.NONE
            }

            val storeEntryItem = StoreEntryItem(
                key = entry.key.name,
                value = entry.value.toString(),
                dataType = dataType,
                editAction = EditAction.Commited,
                dataStore = prefs
            )
            currentDataStoreEntry.add(storeEntryItem)
        }
    }
    suspend fun startEditingAt(index: Int) {
        if ( index in currentDataStoreEntry.indices) {
            if (currentDataStoreEntry.any { it.editAction != EditAction.Staging }) {
                currentDataStoreEntry[index] = currentDataStoreEntry[index].copy(
                    editAction = EditAction.Staging
                )
            }
        }
    }

    suspend fun saveChanges(index: Int, someValue: String?) {
        val value = someValue?.trim()
        if (index in currentDataStoreEntry.indices) {
            val storeEntry: StoreEntryItem = currentDataStoreEntry[index]
            val prefs: BaseDataStorePrefs = storeEntry.dataStore
            val dataType: DataType = storeEntry.dataType

            try {
                when(dataType) {
                    DataType.INT -> prefs.setInt(storeEntry.key, value?.toIntOrNull())
                    DataType.LONG -> prefs.setLong(storeEntry.key, value?.toLongOrNull())
                    DataType.FLOAT -> prefs.setFloat(storeEntry.key, value?.toFloatOrNull())
                    DataType.DOUBLE -> prefs.setDouble(storeEntry.key, value?.toDoubleOrNull())
                    DataType.BOOLEAN -> prefs.setBoolean(storeEntry.key, value?.toBooleanStrictOrNull())
                    DataType.STRING -> prefs.setString(storeEntry.key, value)
                    DataType.NONE -> {
                        throw IllegalArgumentException("Unknown DataType")
                    }
                }
            } catch (e: Exception) {
                Napier.w(throwable = e) { "Unknown dataType found please, create issue in github if this is valid dataType" }
            }


            currentDataStoreEntry[index] = storeEntry.copy(
                value = prefs.getStringNullable(key = storeEntry.key).firstOrNull(),
                editAction = EditAction.Commited
            )
        }
    }
}

