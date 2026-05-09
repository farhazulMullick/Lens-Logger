package io.github.farhazulmullick.lenslogger.plugin.network

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

/**
 * Captures the application [Context] at app startup using the standard ContentProvider trick,
 * so the lens-logger library can construct a DataStore without requiring callers to pass a
 * Context to [io.github.farhazulmullick.lenslogger.ui.LensApp].
 */
internal class LensContextProvider : ContentProvider() {
    override fun onCreate(): Boolean {
        appContext = context?.applicationContext
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    companion object {
        @Volatile
        internal var appContext: Context? = null
    }
}

private val storeInstance: DataStore<Preferences> by lazy {
    val ctx = LensContextProvider.appContext
        ?: error(
            "LensContextProvider was not initialized. Ensure the lens-logger AAR's manifest " +
                "is merged into the host app (manifest merger handles this automatically)."
        )
    val file = java.io.File(ctx.filesDir, "datastore/lens_mocks.preferences_pb").apply {
        parentFile?.mkdirs()
    }
    PreferenceDataStoreFactory.createWithPath { file.absolutePath.toPath() }
}

internal actual fun lensInternalDataStore(): DataStore<Preferences> = storeInstance
