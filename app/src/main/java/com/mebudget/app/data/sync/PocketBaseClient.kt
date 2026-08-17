package com.mebudget.app.data.sync

import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Wraps the [PocketBaseApi] Retrofit instance and injects the bearer auth token.
 * Thread-safe: [authToken] is read from an atomic ref so background sync workers
 * and the UI can share a single client.
 */
class PocketBaseClient(
    val baseUrl: String
) {
    @Volatile
    var authToken: String? = null

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val token = authToken
            val request = if (token != null) {
                chain.request().newBuilder()
                    .addHeader("Authorization", "Bearer $token")
                    .build()
            } else {
                chain.request()
            }
            chain.proceed(request)
        }
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val api: PocketBaseApi = Retrofit.Builder()
        .baseUrl(baseUrl.trimEnd('/') + "/")
        .client(httpClient)
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().create()))
        .build()
        .create(PocketBaseApi::class.java)

    internal fun newHttpClient(): OkHttpClient = httpClient

    /**
     * A client with an effectively-infinite read timeout for the SSE realtime
     * stream (the 30s default would kill a long-lived connection).
     */
    internal fun newSseHttpClient(): OkHttpClient = httpClient.newBuilder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    fun clearAuth() {
        authToken = null
    }
}
