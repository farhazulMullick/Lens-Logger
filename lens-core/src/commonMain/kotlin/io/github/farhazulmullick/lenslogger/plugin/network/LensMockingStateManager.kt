package io.github.farhazulmullick.lenslogger.plugin.network

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.random.Random

/**
 * In-memory + DataStore-backed registry of [MockRule]s.
 *
 * - [rules] is a Compose-observable [SnapshotStateList] that always reflects the source of truth.
 * - [findActive] is a fast non-suspending lookup used by the Ktor plugin on every outbound call.
 * - All mutating ops persist to DataStore asynchronously.
 */
object LensMockingStateManager {

    private const val TAG = "LensMockingStateManager"
    private val RULES_KEY = stringPreferencesKey("lens_mocks_v1")
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val ruleSerializer = ListSerializer(MockRule.serializer())

    val rules: SnapshotStateList<MockRule> = mutableStateListOf()

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var initialized = false

    /**
     * Hydrates [rules] from disk. Safe to call multiple times; subsequent calls are no-ops.
     */
    suspend fun init() {
        mutex.withLock {
            if (initialized) return
            initialized = true
        }
        try {
            val prefs = lensInternalDataStore().data.firstOrNull()
            val raw = prefs?.get(RULES_KEY)
            if (!raw.isNullOrBlank()) {
                val loaded = json.decodeFromString(ruleSerializer, raw)
                rules.clear()
                rules.addAll(loaded)
            }
        } catch (e: Throwable) {
            Napier.w(throwable = e, tag = TAG) { "Failed to load mock rules" }
        }
    }

    /**
     * Fires off [init] in the background. Use this from non-suspend contexts (e.g. plugin install)
     * where you cannot await initialization but want it to happen ASAP.
     */
    fun ensureInitialized() {
        scope.launch { init() }
    }

    /**
     * Returns the first enabled rule whose [MockRule.url] and [MockRule.method]
     * exactly match the given request (method comparison is case-insensitive).
     */
    fun findActive(url: String, method: String): MockRule? =
        rules.firstOrNull {
            it.enabled && it.url == url && it.method.equals(method, ignoreCase = true)
        }

    /**
     * Inserts a new rule (when [MockRule.id] does not yet exist) or updates the existing one.
     */
    fun upsert(rule: MockRule) {
        val idx = rules.indexOfFirst { it.id == rule.id }
        if (idx >= 0) rules[idx] = rule else rules.add(rule)
        persist()
    }

    fun toggle(id: String) {
        val idx = rules.indexOfFirst { it.id == id }
        if (idx >= 0) {
            rules[idx] = rules[idx].copy(enabled = !rules[idx].enabled)
            persist()
        }
    }

    fun delete(id: String) {
        val removed = rules.removeAll { it.id == id }
        if (removed) persist()
    }

    fun findById(id: String): MockRule? = rules.firstOrNull { it.id == id }

    /**
     * Generates a stable id for a new rule. Random hex is sufficient since rules
     * are identified independently of url/method (one URL can have multiple rules; only one
     * is active at a time via the enabled flag).
     */
    fun newId(): String = buildString(16) {
        val chars = "0123456789abcdef"
        repeat(16) { append(chars[Random.nextInt(chars.length)]) }
    }

    private fun persist() {
        val snapshot = rules.toList()
        scope.launch {
            try {
                val encoded = json.encodeToString(ruleSerializer, snapshot)
                lensInternalDataStore().edit { it[RULES_KEY] = encoded }
            } catch (e: Throwable) {
                Napier.w(throwable = e, tag = TAG) { "Failed to persist mock rules" }
            }
        }
    }
}
