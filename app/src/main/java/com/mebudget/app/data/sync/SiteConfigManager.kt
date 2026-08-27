package com.mebudget.app.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SiteConfigManager(
    private val pocketBaseClient: PocketBaseClient
) {
    companion object {
        const val DEFAULT_SITE_URL = "https://mebudget.blackshade.site"
        private const val PRIVACY_PATH = "/privacy"
    }

    private val _siteUrl = MutableStateFlow(DEFAULT_SITE_URL)
    val siteUrl: StateFlow<String> = _siteUrl.asStateFlow()

    private var lastFetchTime: Long = 0
    private val cacheDurationMillis = 24 * 60 * 60 * 1000L

    val privacyPolicyUrl: String
        get() = "${_siteUrl.value}$PRIVACY_PATH"

    suspend fun refreshSiteUrl() {
        val now = System.currentTimeMillis()
        if (now - lastFetchTime < cacheDurationMillis) return

        try {
            val response = pocketBaseClient.api.getList(
                collection = "config",
                page = 1,
                perPage = 1,
                filter = "key = 'site_url'"
            )
            val record = response.items.firstOrNull() ?: return
            val value = record.get("value")?.takeIf { !it.isJsonNull }?.asJsonObject ?: return
            val url = value.get("siteUrl")?.asString ?: return
            if (url.isNotBlank()) {
                _siteUrl.value = url.trimEnd('/')
            }
            lastFetchTime = now
        } catch (_: Exception) {
        }
    }
}
