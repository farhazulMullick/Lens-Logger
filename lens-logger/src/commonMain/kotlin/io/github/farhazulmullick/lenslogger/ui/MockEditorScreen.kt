package io.github.farhazulmullick.lenslogger.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.farhazulmullick.lenslogger.modal.NetworkLogs
import io.github.farhazulmullick.lenslogger.plugin.network.LensKtorStateManager
import io.github.farhazulmullick.lenslogger.plugin.network.LensMockingStateManager
import io.github.farhazulmullick.lenslogger.plugin.network.MockRule

private data class HeaderEntry(var key: String, var value: String)

/**
 * Form for creating or editing a [MockRule].
 *
 * The screen is opened in two ways:
 * 1. From a logged request (tap "Mock this" in the request detail screen) - prefilled from
 *    that request/response and saves a new rule (or upserts onto an existing one matching
 *    the same url+method).
 * 2. From the Mocks tab list (tap a row) - prefilled from the existing [MockRule].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockEditorScreen(
    sourceLogIndex: Int?,
    ruleId: String?,
    onDone: () -> Unit
) {
    val existing: MockRule? = ruleId?.let { LensMockingStateManager.findById(it) }
    val sourceLog: NetworkLogs? = sourceLogIndex?.let {
        LensKtorStateManager.stateCalls.getOrNull(it)
    }

    val initialUrl = remember(existing, sourceLog) {
        existing?.url
            ?: sourceLog?.requestData?.url?.buildString()
            ?: ""
    }
    val initialMethod = remember(existing, sourceLog) {
        existing?.method
            ?: sourceLog?.requestData?.method?.value
            ?: "GET"
    }
    val initialStatus = remember(existing, sourceLog) {
        existing?.statusCode
            ?: sourceLog?.responseData?.status?.value
            ?: 200
    }
    val initialBody = remember(existing, sourceLog) {
        existing?.body
            ?: sourceLog?.responseData?.body
            ?: ""
    }
    val initialHeaders: List<HeaderEntry> = remember(existing, sourceLog) {
        val src = existing?.headers
            ?: sourceLog?.responseData?.headers
            ?: emptyMap()
        src.map { HeaderEntry(it.key, it.value) }
    }

    var url by remember { mutableStateOf(initialUrl) }
    var method by remember { mutableStateOf(initialMethod) }
    var enabled by remember { mutableStateOf(existing?.enabled ?: true) }
    var simulateFailure by remember { mutableStateOf(existing?.simulateFailure ?: false) }
    var statusText by remember { mutableStateOf(initialStatus.toString()) }
    var delayText by remember { mutableStateOf((existing?.delayMs ?: 0L).toString()) }
    var body by remember { mutableStateOf(initialBody) }
    val headers = remember { mutableStateListOf<HeaderEntry>().apply { addAll(initialHeaders) } }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDone) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
                Text(
                    text = if (existing == null) "New Mock" else "Edit Mock",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        bottomBar = {
            Button(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                onClick = {
                    val rule = MockRule(
                        id = existing?.id ?: LensMockingStateManager.newId(),
                        url = url.trim(),
                        method = method.trim().ifBlank { "GET" }.uppercase(),
                        enabled = enabled,
                        delayMs = delayText.toLongOrNull()?.coerceAtLeast(0L) ?: 0L,
                        simulateFailure = simulateFailure,
                        statusCode = statusText.toIntOrNull()?.coerceIn(100, 599) ?: 200,
                        headers = headers
                            .filter { it.key.isNotBlank() }
                            .associate { it.key.trim() to it.value },
                        body = body
                    )
                    LensMockingStateManager.upsert(rule)
                    onDone()
                }
            ) {
                Text(if (existing == null) "Create mock" else "Save changes")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ToggleRow(label = "Enabled", checked = enabled, onCheckedChange = { enabled = it })
            ToggleRow(
                label = "Simulate network failure",
                checked = simulateFailure,
                onCheckedChange = { simulateFailure = it }
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = url,
                onValueChange = { url = it },
                label = { Text("URL (exact match)") },
                singleLine = true
            )
            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = method,
                onValueChange = { method = it.uppercase() },
                label = { Text("HTTP method") },
                singleLine = true
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = delayText,
                onValueChange = { new -> delayText = new.filter { it.isDigit() } },
                label = { Text("Response delay (ms)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = statusText,
                onValueChange = { new -> statusText = new.filter { it.isDigit() }.take(3) },
                label = { Text("Status code") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                enabled = !simulateFailure
            )

            HeadersSection(
                headers = headers,
                onAdd = { headers.add(HeaderEntry("", "")) },
                onRemove = { idx -> if (idx in headers.indices) headers.removeAt(idx) },
                onChange = { idx, key, value ->
                    if (idx in headers.indices) headers[idx] = HeaderEntry(key, value)
                },
                enabled = !simulateFailure
            )

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = body,
                onValueChange = { body = it },
                label = { Text("Response body") },
                minLines = 4,
                enabled = !simulateFailure
            )
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun HeadersSection(
    headers: List<HeaderEntry>,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
    onChange: (Int, String, String) -> Unit,
    enabled: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Response headers",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground
        )
        IconButton(onClick = onAdd, enabled = enabled) {
            Icon(Icons.Outlined.Add, contentDescription = "Add header")
        }
    }
    headers.forEachIndexed { idx, entry ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = entry.key,
                onValueChange = { onChange(idx, it, entry.value) },
                label = { Text("Name") },
                singleLine = true,
                enabled = enabled
            )
            OutlinedTextField(
                modifier = Modifier.weight(1.4f),
                value = entry.value,
                onValueChange = { onChange(idx, entry.key, it) },
                label = { Text("Value") },
                singleLine = true,
                enabled = enabled
            )
            IconButton(onClick = { onRemove(idx) }, enabled = enabled) {
                Icon(Icons.Outlined.Delete, contentDescription = "Remove header")
            }
        }
    }
    if (headers.isEmpty()) {
        Text(
            text = "No headers configured.",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
