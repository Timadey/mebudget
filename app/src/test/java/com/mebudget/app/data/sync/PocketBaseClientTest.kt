package com.mebudget.app.data.sync

import com.google.gson.JsonObject
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class PocketBaseClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: PocketBaseClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = PocketBaseClient(server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `client initializes with base url and no auth token`() {
        assertEquals(client.baseUrl, server.url("/").toString())
        assertNull(client.authToken)
    }

    @Test
    fun `auth-with-password parses token and user record`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "token": "abc123",
                  "record": {
                    "id": "user_1",
                    "email": "test@example.com",
                    "name": "Test User"
                  }
                }
                """.trimIndent()
            )
        )

        val response = client.api.authWithPassword(
            AuthWithPasswordRequest(identity = "test@example.com", password = "secret")
        )

        assertEquals("abc123", response.token)
        assertEquals("user_1", response.record.id)
        assertEquals("test@example.com", response.record.email)

        val recordedRequest = server.takeRequest()
        assertEquals("/api/collections/users/auth-with-password", recordedRequest.path)
    }

    @Test
    fun `getList parses paginated response`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """
                {
                  "page": 1,
                  "perPage": 200,
                  "totalItems": 1,
                  "totalPages": 1,
                  "items": [
                    {"id": "rec_1", "name": "Groceries"}
                  ]
                }
                """.trimIndent()
            )
        )

        val response = client.api.getList(collection = "budgets")

        assertEquals(1, response.totalItems)
        assertEquals(1, response.items.size)
        assertEquals("rec_1", response.items[0].get("id").asString)
        assertEquals("Groceries", response.items[0].get("name").asString)
    }

    @Test
    fun `create sends body as json`() = runTest {
        server.enqueue(
            MockResponse().setResponseCode(200).setBody(
                """{"id": "rec_new", "name": "Groceries"}"""
            )
        )

        val response = client.api.create(
            collection = "budgets",
            body = JsonObject().apply {
                addProperty("name", "Groceries")
                addProperty("deleted", false)
            }
        )

        assertEquals("rec_new", response["id"].asString)

        val recordedRequest = server.takeRequest()
        assertEquals("/api/collections/budgets/records", recordedRequest.path)
        val bodyBuffer = Buffer()
        recordedRequest.body.writeTo(bodyBuffer.outputStream())
        assertEquals("""{"name":"Groceries","deleted":false}""", bodyBuffer.readUtf8())
    }

    @Test
    fun `auth token is attached to record requests`() = runTest {
        client.authToken = "token_xyz"
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"id":"rec_1"}"""))

        client.api.getOne(collection = "budgets", id = "rec_1")

        val recordedRequest = server.takeRequest()
        assertEquals("Bearer token_xyz", recordedRequest.getHeader("Authorization"))
    }
}
