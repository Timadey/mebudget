package com.mebudget.app.data.sync.models

/**
 * PocketBase REST API response models.
 *
 * These mirror the current PocketBase v0.23+ (v0.39.x) Web API record and auth
 * shapes. Field names match the server JSON exactly.
 */

data class PocketBaseListResponse<T>(
    val page: Int,
    val perPage: Int,
    val totalItems: Int,
    val totalPages: Int,
    val items: List<T>
)

/**
 * Every PocketBase record has these system fields.
 * `expand` and relation fields are only present when requested.
 */
data class PocketBaseRecord(
    val id: String,
    val created: String,
    val updated: String,
    val collectionId: String,
    val collectionName: String,
    val expand: Map<String, Any>? = null
)

/** Auth response returned by auth-with-password / auth-via-oauth2 / refresh. */
data class PocketBaseAuthResponse(
    val token: String,
    val record: PocketBaseUserRecord
)

/** The users auth collection record. */
data class PocketBaseUserRecord(
    val id: String,
    val email: String,
    val name: String,
    val avatar: String? = null
)
