package io.github.farhazulmullick.lenslogger.plugin.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.farhazulmullick.lenslogger.EMPTY
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

class BaseDataStorePrefs(private val dataStore: DataStore<Preferences>) {

    private fun <T : Any> Preferences.Key<T>.get(defaultValue: T): Flow<T> {
        return dataStore.data.map {
            it[this] ?: defaultValue
        }.distinctUntilChanged()
    }

    private fun <T : Any> Preferences.Key<T>.getNullable(defaultValue: T?): Flow<T?> {
        return dataStore.data.map {
            it[this] ?: defaultValue
        }.distinctUntilChanged()
    }

    private suspend fun <T : Any> Preferences.Key<T>.set(value: T?): Boolean {
        return try {
            dataStore.edit { store ->
                value?.let {
                    store[this] = it
                } ?: store.remove(this)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getEntriesAsMap() = dataStore.data.map { it.asMap() }.distinctUntilChanged().firstOrNull()

    /**
     * Get non-null value as default value.
     */

    fun getBoolean(key: String, defaultValue: Boolean = false) =
        booleanPreferencesKey(key).get(defaultValue)

    fun getInt(key: String, defaultValue: Int = 0) =
        intPreferencesKey(key).get(defaultValue)

    fun getLong(key: String, defaultValue: Long = 0L) =
        longPreferencesKey(key).get(defaultValue)

    fun getDouble(key: String, defaultValue: Double = 0.0) =
        doublePreferencesKey(key).get(defaultValue)

    fun getFloat(key: String, defaultValue: Float = 0F) =
        floatPreferencesKey(key).get(defaultValue)

    fun getString(key: String, defaultValue: String = String.EMPTY) =
        stringPreferencesKey(key).get(defaultValue)

    /**
     * Get nullable values as default value.
     */
    fun getBooleanNullable(key: String, defaultValue: Boolean? = null) =
        booleanPreferencesKey(key).getNullable(defaultValue)

    fun getIntNullable(key: String, defaultValue: Int? = null) =
        intPreferencesKey(key).getNullable(defaultValue)

    fun getLongNullable(key: String, defaultValue: Long? = null) =
        longPreferencesKey(key).getNullable(defaultValue)

    fun getDoubleNullable(key: String, defaultValue: Double? = null) =
        doublePreferencesKey(key).getNullable(defaultValue)

    fun getFloatNullable(key: String, defaultValue: Float? = null) =
        floatPreferencesKey(key).getNullable(defaultValue)

    fun getStringNullable(key: String, defaultValue: String? = null) =
        stringPreferencesKey(key).getNullable(defaultValue)


    suspend fun setBoolean(key: String, value: Boolean?) = booleanPreferencesKey(key).set(value)

    suspend fun setInt(key: String, value: Int?) = intPreferencesKey(key).set(value)

    suspend fun setLong(key: String, value: Long?) = longPreferencesKey(key).set(value)

    suspend fun setDouble(key: String, value: Double?) = doublePreferencesKey(key).set(value)

    suspend fun setFloat(key: String, value: Float?) = floatPreferencesKey(key).set(value)

    suspend fun setString(key: String, value: String?) = stringPreferencesKey(key).set(value)

    suspend fun removeString(key: String) = dataStore.edit {
        it.remove( stringPreferencesKey(key) )
    }


    /** Clear Datastore **/
    suspend fun clearDataStore(){
        dataStore.edit { it.clear() }
    }

}

