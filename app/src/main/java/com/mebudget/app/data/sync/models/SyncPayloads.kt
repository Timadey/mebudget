package com.mebudget.app.data.sync.models

import com.google.gson.annotations.SerializedName

/**
 * Payloads sent to / received from the synced PocketBase collections.
 * Field names match the collection schema in pb_migrations/001_initial.js.
 */

data class PocketBudgetPayload(
    val userId: String,
    val name: String,
    @SerializedName("startDateEpochDay") val startDateEpochDay: Long? = null,
    @SerializedName("endDateEpochDay") val endDateEpochDay: Long? = null,
    @SerializedName("negativeBalanceRule") val negativeBalanceRule: String,
    @SerializedName("createdAtMillis") val createdAtMillis: Long,
    @SerializedName("updatedAtMillis") val updatedAtMillis: Long,
    val deleted: Boolean = false
)

data class PocketWalletPayload(
    val userId: String,
    @SerializedName("budgetId") val budgetId: String,
    val name: String,
    @SerializedName("plannedAmount") val plannedAmount: Long,
    @SerializedName("sortOrder") val sortOrder: Int,
    val archived: Boolean = false,
    @SerializedName("updatedAtMillis") val updatedAtMillis: Long,
    val deleted: Boolean = false
)

data class PocketTransactionPayload(
    val userId: String,
    @SerializedName("budgetId") val budgetId: String,
    val type: String,
    val amount: Long,
    @SerializedName("dateEpochDay") val dateEpochDay: Long,
    @SerializedName("sourceWalletId") val sourceWalletId: String? = null,
    @SerializedName("destinationWalletId") val destinationWalletId: String? = null,
    val note: String? = null,
    @SerializedName("createdAtMillis") val createdAtMillis: Long,
    @SerializedName("updatedAtMillis") val updatedAtMillis: Long,
    val deleted: Boolean = false
)
