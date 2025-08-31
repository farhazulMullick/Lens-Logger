package io.github.farhazulmullick.lenslogger.plugin.datastore

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.aakira.napier.Napier
import kotlinx.coroutines.flow.firstOrNull

sealed class EditAction {
    object Idle: EditAction()
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
    val allDataStores: SnapshotStateList<BaseDataStorePrefs> = mutableStateListOf()

    val currentDataStoreEntry: SnapshotStateList<StoreEntryItem> = mutableStateListOf()

    suspend fun setDataStores(dataStore: List<DataStore<Preferences>>) {
        if (allDataStores.isEmpty())
        allDataStores.addAll(dataStore.map { BaseDataStorePrefs(it) })
        allDataStores.loadAllEntries()
    }

    suspend fun List<BaseDataStorePrefs>.loadAllEntries() = forEach {
        it.loadEntriesFromPrefs()
    }

    suspend fun BaseDataStorePrefs?.loadEntriesFromPrefs() {
        this?.getEntriesAsMap()?.forEach { entry ->

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
                dataStore = this
            )
            currentDataStoreEntry.add(storeEntryItem)
        }
    }

    suspend fun startChangesAt(index: Int) {
        if (index in currentDataStoreEntry.indices) {
            if (currentDataStoreEntry.any { it.editAction == EditAction.Staging }) {
                cancelChangesAt(index)
            }
            currentDataStoreEntry[index] = currentDataStoreEntry[index].copy(
                editAction = EditAction.Staging
            )
        }
    }

    fun writingAt(index: Int, value: String) {
        if (index in currentDataStoreEntry.indices) {
            currentDataStoreEntry[index] = currentDataStoreEntry[index].copy(
                value = value,
                editAction = EditAction.Idle
            )
        }
    }

    suspend fun cancelChangesAt(index: Int){
        if (index in currentDataStoreEntry.indices){
            val storeEntry: StoreEntryItem = currentDataStoreEntry[index]
            val prefs: BaseDataStorePrefs = storeEntry.dataStore
            if (storeEntry.editAction == EditAction.Staging){
                currentDataStoreEntry[index] = currentDataStoreEntry[index].copy(
                    value = prefs.getStringNullable(key = storeEntry.key).firstOrNull(),
                    editAction = EditAction.Idle
                )
            }
        }
    }

    suspend fun saveChangesAt(index: Int, someValue: String?) {
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

