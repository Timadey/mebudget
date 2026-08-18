package com.mebudget.app.data.sync

import com.google.gson.JsonObject
import com.mebudget.app.data.sync.models.CheckoutResponse
import com.mebudget.app.data.sync.models.PocketBaseAuthResponse
import com.mebudget.app.data.sync.models.PocketBaseListResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the PocketBase Web API (v0.23+ / v0.39.x).
 * Auth token is injected via OkHttp interceptor in [PocketBaseClient].
 */
interface PocketBaseApi {

    // --- Auth -----------------------------------------------------------------

    @POST("api/collections/users/auth-with-password")
    suspend fun authWithPassword(
        @Body body: AuthWithPasswordRequest
    ): PocketBaseAuthResponse

    @POST("api/subscriptions/checkout")
    suspend fun createCheckout(
        @Body body: JsonObject
    ): CheckoutResponse

    @POST("api/collections/users/refresh")
    suspend fun refreshAuth(): PocketBaseAuthResponse

    // --- Records ---------------------------------------------------------------

    @GET("api/collections/{collection}/records")
    suspend fun getList(
        @Path("collection") collection: String,
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 200,
        @Query("filter") filter: String? = null,
        @Query("sort") sort: String? = null
    ): PocketBaseListResponse<JsonObject>

    @GET("api/collections/{collection}/records/{id}")
    suspend fun getOne(
        @Path("collection") collection: String,
        @Path("id") id: String
    ): JsonObject

    @POST("api/collections/{collection}/records")
    suspend fun create(
        @Path("collection") collection: String,
        @Body body: JsonObject
    ): JsonObject

    @PATCH("api/collections/{collection}/records/{id}")
    suspend fun update(
        @Path("collection") collection: String,
        @Path("id") id: String,
        @Body body: JsonObject
    ): JsonObject

    @DELETE("api/collections/{collection}/records/{id}")
    suspend fun delete(
        @Path("collection") collection: String,
        @Path("id") id: String
    )

    // --- Realtime (SSE) --------------------------------------------------------
    //
    // The SSE GET /api/realtime stream is opened with an okhttp-sse EventSource
    // (it needs a persistent connection, which Retrofit cannot represent).
    // After the server emits PB_CONNECT, the listener POSTs the client id and
    // subscriptions here to authorize and subscribe the connection.

    @POST("api/realtime")
    suspend fun setRealtimeSubscriptions(
        @Body body: JsonObject
    )
}

data class AuthWithPasswordRequest(
    val identity: String,
    val password: String
)
