package com.mebudget.app.data.sync

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.mebudget.app.billing.FeatureLimits
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Loads free-tier limits from the server `config` collection.
 *
 * Defaults to [FeatureLimits.DEFAULT] until the server is reachable, then caches
 * the fetched limits for [cacheDurationMillis] so the app does not hammer the
 * server on every gate check.
 */
class LimitsConfigManager(
    private val pocketBaseClient: PocketBaseClient
) {
    private val _limits = MutableStateFlow(FeatureLimits.DEFAULT)
    val limits: StateFlow<FeatureLimits> = _limits.asStateFlow()

    private var lastFetchTime: Long = 0
    private val cacheDurationMillis = 24 * 60 * 60 * 1000L

    /** Fetches fresh limits if the cache has expired. Never throws. */
    suspend fun refreshLimits() {
        val now = System.currentTimeMillis()
        if (now - lastFetchTime < cacheDurationMillis) return

        try {
            val response = pocketBaseClient.api.getList(
                collection = "config",
                page = 1,
                perPage = 1
            )
            val record = response.items.firstOrNull() ?: return
            _limits.value = FeatureLimits.fromServerConfig(record.toConfigMap())
            lastFetchTime = now
        } catch (_: Exception) {
        }
    }

    private fun JsonObject.toConfigMap(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        entrySet().forEach { (key, value) ->
            if (!value.isJsonNull) {
                map[key] = value.toSimpleValue()
            }
        }
        return map
    }

    private fun JsonElement.toSimpleValue(): Any = when {
        isJsonPrimitive -> {
            val primitive = asJsonPrimitive
            when {
                primitive.isNumber -> primitive.asDouble
                primitive.isBoolean -> primitive.asBoolean
                else -> asString
            }
        }
        else -> toString()
    }
}