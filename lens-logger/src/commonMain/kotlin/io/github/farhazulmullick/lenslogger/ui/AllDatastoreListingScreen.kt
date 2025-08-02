package io.github.farhazulmullick.lenslogger.ui

import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

@Composable
fun AllDatastoreListingScreen(
    dataStores: List<DataStore<Preferences>> = emptyList(),
    onClick: (Int) -> Unit
) {
    Scaffold {
        Text(text = "DataStore")
    }
}