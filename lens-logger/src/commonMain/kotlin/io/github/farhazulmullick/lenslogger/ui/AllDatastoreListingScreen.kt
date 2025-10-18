package io.github.farhazulmullick.lenslogger.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.farhazulmullick.lenslogger.plugin.datastore.DataType
import io.github.farhazulmullick.lenslogger.plugin.datastore.LensDatastoreStateManager
import kotlinx.coroutines.launch
import lens.lens_logger.generated.resources.Res
import lens.lens_logger.generated.resources.no_data_prefs
import org.jetbrains.compose.resources.painterResource

@Composable
fun AllDatastoreListingScreen(
    dataStores: List<DataStore<Preferences>> = emptyList(),
    onClick: (Int) -> Unit
) {
    LaunchedEffect(Unit) {
        LensDatastoreStateManager.setDataStores(dataStores)
    }
    val scope = rememberCoroutineScope ()

    Scaffold(modifier = Modifier.padding(16.dp)) {
        Column(modifier = Modifier.padding(it)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    AnimatedVisibility(LensDatastoreStateManager.currentDataStoreEntry.isEmpty()) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            VSpacer(24.dp)
                            Image(
                                modifier = Modifier.size(130.dp),
                                painter = painterResource(Res.drawable.no_data_prefs), contentDescription = null
                            )
                            VSpacer(12.dp)
                            Text(
                                text = if(dataStores.isEmpty()) "No, store files found." else "No, data found.",
                                style = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                itemsIndexed(items = LensDatastoreStateManager.currentDataStoreEntry) { index , item ->
                    var wasFocused by remember { mutableStateOf(false) }
                    Column (
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ){
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "${index.plus(1)}")
                            OutlinedTextField(
                                modifier = Modifier.onFocusChanged {focusState ->
                                    if (focusState.isFocused) wasFocused = true
                                    if (!focusState.isFocused && wasFocused) {
                                        // ✅ Action when focus is lost
                                        scope.launch {
                                            LensDatastoreStateManager.saveChangesAt(index, item.value)
                                            wasFocused = false
                                        }
                                    }
                                },
                                value = item.value ?: null.toString(),
                                onValueChange = { change ->
                                    LensDatastoreStateManager.writingAt(index = index, value = change)
                                },
                                label = {
                                    Text(text = item.keyNameWithType)
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType =
                                        if (
                                            arrayOf(
                                                DataType.DOUBLE,
                                                DataType.FLOAT
                                            ).contains(item.dataType)
                                        ) KeyboardType.Number

                                        else if (item.dataType == DataType.INT)
                                            KeyboardType.Decimal
                                        else
                                            KeyboardType.Text
                                )
                            )
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        LensDatastoreStateManager.saveChangesAt(index, item.value)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Check, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.4f))
                    }
                }
            }
        }
    }
}