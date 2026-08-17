package com.mebudget.app.data.sync

import com.google.gson.JsonObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RealtimeListenerTest {

    private lateinit var server: MockWebServer
    private lateinit var client: PocketBaseClient
    private lateinit var listener: RealtimeListener

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = PocketBaseClient(server.url("/").toString())
        listener = RealtimeListener(client)
    }

    @After
    fun tearDown() {
        listener.stopListening()
        server.shutdown()
    }

    /** Builds a standard SSE event frame, mirroring PocketBase's output. */
    private fun sseFrame(id: String, event: String, data: String): String =
        "id:$id\nevent:$event\ndata:$data\n\n"

    private fun connectFrame(clientId: String): String =
        sseFrame(clientId, RealtimeListener.CONNECT_EVENT, """{"clientId":"$clientId"}""")

    private fun recordFrame(collection: String, action: String, recordId: String): String =
        sseFrame(
            id = recordId,
            event = collection,
            data = """{"action":"$action","record":{"id":"$recordId","collectionName":"$collection","name":"x"}}"""
        )

    @Test
    fun `startListening opens a GET to api realtime and connects`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(connectFrame("client_abc"))
        )

        listener.startListening()

        val request = server.takeRequest()
        assertEquals("/api/realtime", request.path)
        assertEquals("GET", request.method)
        assertTrue(listener.isListening)
    }

    @Test
    fun `after PB_CONNECT posts subscriptions with client id`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(connectFrame("client_abc") + recordFrame("budgets", "create", "rec1"))
        )
        server.enqueue(MockResponse().setResponseCode(204))

        listener.startListening()

        // wait until the subscriptions POST has been received
        val post = awaitRequestMatching("/api/realtime", "POST")
        assertEquals("/api/realtime", post.path)
        assertEquals("POST", post.method)
        val body = post.body.readUtf8()
        assertTrue(body.contains("client_abc"))
        assertTrue(body.contains("budgets/*"))
        assertTrue(body.contains("wallets/*"))
        assertTrue(body.contains("transactions/*"))
    }

    @Test
    fun `record events are routed to their collection flows`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(
                    connectFrame("client_abc") +
                        recordFrame("budgets", "create", "b1") +
                        recordFrame("wallets", "update", "w1") +
                        recordFrame("transactions", "delete", "t1")
                )
        )
        server.enqueue(MockResponse().setResponseCode(204))

        // Rebuild the listener pointed at the mock server.
        listener.stopListening()
        listener = RealtimeListener(client)

        kotlinx.coroutines.runBlocking {
            val budgetF = async { listener.budgetUpdates.first() }
            val walletF = async { listener.walletUpdates.first() }
            val transactionF = async { listener.transactionUpdates.first() }

            // Let the collectors subscribe before opening the stream so no
            // update is dropped (replay buffer is 0).
            kotlinx.coroutines.delay(50)

            listener.startListening()

            val budget = withTimeout(5_000) { budgetF.await() }
            val wallet = withTimeout(5_000) { walletF.await() }
            val transaction = withTimeout(5_000) { transactionF.await() }

            listener.stopListening()

            assertEquals("create", budget.action)
            assertEquals("b1", budget.record.get("id").asString)
            assertEquals("budgets", budget.record.get("collectionName").asString)

            assertEquals("update", wallet.action)
            assertEquals("w1", wallet.record.get("id").asString)

            assertEquals("delete", transaction.action)
            assertEquals("t1", transaction.record.get("id").asString)
        }
    }

    @Test
    fun `stopListening closes the stream`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "text/event-stream")
                .setBody(connectFrame("client_abc"))
        )

        listener.startListening()
        awaitCondition { listener.isListening }

        listener.stopListening()
        assertEquals(false, listener.isListening)
    }

    /** Polls until [block] returns true or a timeout elapses. */
    private suspend fun awaitCondition(block: () -> Boolean) {
        var attempts = 0
        while (!block() && attempts < 200) {
            kotlinx.coroutines.delay(25)
            attempts++
        }
        assertTrue("Condition not met within timeout", block())
    }

    private suspend fun awaitRequestMatching(path: String, method: String): okhttp3.mockwebserver.RecordedRequest {
        var attempts = 0
        while (attempts < 200) {
            val req = server.takeRequest(25, java.util.concurrent.TimeUnit.MILLISECONDS)
            if (req != null && req.path == path && req.method == method) return req
            attempts++
        }
        throw AssertionError("Request $method $path was not received")
    }
}