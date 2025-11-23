package io.github.farhazulmullick.lenslogger.plugin.datastore

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.aakira.napier.Napier
import io.github.farhazulmullick.lenslogger.AppSnackBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext

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
){
    val keyNameWithType by lazy { key.plus(" (").plus(dataType.name).plus(")") }
}

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
    private val allDataStores: SnapshotStateList<BaseDataStorePrefs> by lazy { mutableStateListOf() }
    val currentDataStoreEntry: SnapshotStateList<StoreEntryItem> by lazy { mutableStateListOf() }

    suspend fun setDataStores(dataStore: List<DataStore<Preferences>>) {
        if (allDataStores.isEmpty()){
            allDataStores.addAll(dataStore.map { BaseDataStorePrefs(it) })
        }
        currentDataStoreEntry.clear()
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

    suspend fun saveChangesAt(index: Int, someValue: String?): Boolean {
        val value = someValue?.trim()
        var isSaved = true
        if (index in currentDataStoreEntry.indices) {
            val storeEntry: StoreEntryItem = currentDataStoreEntry[index]
            val prefs: BaseDataStorePrefs = storeEntry.dataStore
            val dataType: DataType = storeEntry.dataType

            // read value from store if content identical pre exit.
            if (storeEntry.value == prefs.getValuesWithDataTypeFromPrefs(storeEntry.key, dataType))
                return true

            try {
                withContext(Dispatchers.IO) {
                    when(dataType) {
                        DataType.INT -> prefs.setInt(storeEntry.key, value?.toInt())
                        DataType.LONG -> prefs.setLong(storeEntry.key, value?.toLong())
                        DataType.FLOAT -> prefs.setFloat(storeEntry.key, value?.toFloat())
                        DataType.DOUBLE -> prefs.setDouble(storeEntry.key, value?.toDouble())
                        DataType.BOOLEAN -> prefs.setBoolean(storeEntry.key, value?.toBooleanStrict())
                        DataType.STRING -> prefs.setString(storeEntry.key, value)
                        DataType.NONE -> {
                            throw IllegalStateException("Unknown DataType")
                        }
                    }
                }
            } catch (e: IllegalStateException){
                isSaved = !isSaved
                AppSnackBar.showSnackBar(message = "Unknown dataType of value: $value for key ${storeEntry.key} of type: $dataType")
            } catch (e: Exception) {
                isSaved = !isSaved
                Napier.w(throwable = e) { "Please Pass value of type $dataType. Cause ${e.cause}" }
                e.printStackTrace()
                AppSnackBar.showSnackBar(message = "Unsupported dataType of value: $value for key ${storeEntry.key} of type: $dataType")
            }

            // Read latest value from the data-store.
            currentDataStoreEntry[index] = storeEntry.copy(
                value = prefs.getValuesWithDataTypeFromPrefs(storeEntry.key, dataType),
                editAction = EditAction.Commited
            )
            if (isSaved) AppSnackBar.showSnackBar(message = "Updated value for key ${storeEntry.key}")
        }
        return isSaved
    }

    suspend fun BaseDataStorePrefs.getValuesWithDataTypeFromPrefs(key: String, dataType: DataType): String? {
        return withContext(Dispatchers.IO){
            when(dataType) {
                DataType.INT -> {
                    getIntNullable(key = key).firstOrNull().toString()
                }
                DataType.LONG -> {
                    getLongNullable(key = key).firstOrNull().toString()
                }
                DataType.FLOAT -> {
                    getFloatNullable(key = key).firstOrNull().toString()
                }
                DataType.DOUBLE -> {
                    getDoubleNullable(key = key).firstOrNull().toString()
                }
                DataType.STRING -> {
                    getStringNullable(key = key).firstOrNull().toString()
                }
                DataType.BOOLEAN -> {
                    getBooleanNullable(key = key).firstOrNull().toString()
                }
                else -> null
            }
        }
    }
}

