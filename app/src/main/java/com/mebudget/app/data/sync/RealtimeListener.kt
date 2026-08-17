package com.mebudget.app.data.sync

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.Request
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import kotlin.random.Random

/**
 * Listens for PocketBase realtime record changes via Server-Sent Events (SSE).
 *
 * Protocol (https://pocketbase.io/docs/api-realtime/):
 *  1. `GET /api/realtime` opens the persistent stream. The server immediately
 *     sends a `PB_CONNECT` event whose data is `{"clientId":"..."}`.
 *  2. `POST /api/realtime` with `{clientId, subscriptions:[...]}` authorizes
 *     the connection (the shared auth interceptor adds the Bearer token) and
 *     subscribes to the budgets/wallets/transactions topics.
 *  3. Record create/update/delete operations arrive as SSE events with the
 *     JSON payload `{"action":"create|update|delete","record":{...}}`.
 *
 * Each collection's events are exposed on its own [Flow]. Note that the
 * connection must never be established before the user signs in, because the
 * subscriptions POST binds the connection to the authenticated user.
 */
class RealtimeListener(
    private val pocketBaseClient: PocketBaseClient
) {

    private val gson = Gson()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _budgetUpdates = MutableSharedFlow<RealtimeUpdate>(extraBufferCapacity = 64)
    val budgetUpdates: Flow<RealtimeUpdate> = _budgetUpdates.asSharedFlow()

    private val _walletUpdates = MutableSharedFlow<RealtimeUpdate>(extraBufferCapacity = 64)
    val walletUpdates: Flow<RealtimeUpdate> = _walletUpdates.asSharedFlow()

    private val _transactionUpdates = MutableSharedFlow<RealtimeUpdate>(extraBufferCapacity = 64)
    val transactionUpdates: Flow<RealtimeUpdate> = _transactionUpdates.asSharedFlow()

    @Volatile
    private var clientId: String? = null

    @Volatile
    private var listening = false

    private var eventSource: EventSource? = null
    private var reconnectJob: Job? = null

    val isListening: Boolean
        get() = listening

    /** Opens the SSE stream and, once `PB_CONNECT` arrives, subscribes. Idempotent. */
    fun startListening() {
        if (listening) return
        listening = true
        connect()
    }

    /** Closes the stream and cancels any pending reconnect. */
    fun stopListening() {
        listening = false
        reconnectJob?.cancel()
        reconnectJob = null
        eventSource?.cancel()
        eventSource = null
        clientId = null
    }

    private fun connect() {
        reconnectJob = null
        val request = Request.Builder()
            .url(pocketBaseClient.baseUrl.trimEnd('/') + "/api/realtime")
            .build()
        eventSource = EventSources
            .createFactory(pocketBaseClient.newSseHttpClient())
            .newEventSource(request, listener)
    }

    private fun scheduleReconnect() {
        if (!listening) return
        reconnectJob = scope.launch {
            delay(RANDOM.nextLong(1_000L, 5_000L))
            if (listening) connect()
        }
    }

    private val listener = object : EventSourceListener() {
        override fun onOpen(eventSource: EventSource, response: Response) = Unit

        override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
            when (type) {
                CONNECT_EVENT -> handleConnect(data)
                PING_EVENT -> Unit // server keep-alive
                else -> handleRecordEvent(data)
            }
        }

        override fun onClosed(eventSource: EventSource) {
            if (listening) scheduleReconnect()
        }

        override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
            if (listening) scheduleReconnect()
        }
    }

    /** Stores the client id and POSTs the subscription topics for this connection. */
    private fun handleConnect(data: String) {
        val parsedId = runCatching {
            gson.fromJson(data, JsonObject::class.java).get("clientId")
        }.getOrNull()
        val connectedClientId = (parsedId as? JsonPrimitive)?.asString ?: return
        clientId = connectedClientId

        scope.launch {
            runCatching {
                val body = JsonObject().apply {
                    addProperty("clientId", connectedClientId)
                    val topics = com.google.gson.JsonArray().apply {
                        SUBSCRIPTIONS.forEach { add(it) }
                    }
                    add("subscriptions", topics)
                }
                pocketBaseClient.api.setRealtimeSubscriptions(body)
            }
        }
    }

    /** Parses a record event and routes it to the matching collection flow. */
    private fun handleRecordEvent(data: String) {
        val payload = runCatching { gson.fromJson(data, JsonObject::class.java) }.getOrNull() ?: return
        val action = (payload.get("action") as? JsonPrimitive)?.asString ?: return
        val record = runCatching { payload.getAsJsonObject("record") }.getOrNull() ?: return
        val update = RealtimeUpdate(action = action, record = record)

        val collection = (record.get("collectionName") as? JsonPrimitive)?.asString
        when (collection) {
            "budgets" -> _budgetUpdates.tryEmit(update)
            "wallets" -> _walletUpdates.tryEmit(update)
            "transactions" -> _transactionUpdates.tryEmit(update)
        }
    }

    companion object {
        const val CONNECT_EVENT = "PB_CONNECT"
        const val PING_EVENT = "PB_PING"
        val SUBSCRIPTIONS = listOf("budgets/*", "wallets/*", "transactions/*")
        private val RANDOM = Random.Default
    }
}

/**
 * A single realtime record change from the server.
 *
 * [record] is the complete server-side record (including the system fields
 * `id`, `updated`, `created` and the synced payload columns).
 *
 * @param action One of `create`, `update` or `delete`.
 */
data class RealtimeUpdate(
    val action: String,
    val record: JsonObject
)