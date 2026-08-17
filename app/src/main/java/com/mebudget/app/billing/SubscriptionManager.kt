package com.mebudget.app.billing

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.JsonObject
import com.mebudget.app.data.auth.AuthManager
import com.mebudget.app.data.auth.AuthState
import com.mebudget.app.data.sync.PocketBaseClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

private val Context.subscriptionDataStore by preferencesDataStore(name = "subscription_prefs")

/**
 * Reads the user's Pro subscription from the server `subscriptions` collection
 * and exposes it as [isPro]. Offline grace: the last-known expiry is persisted
 * via [SubscriptionCache] so Pro survives restarts and temporary outages until
 * the expiry date actually passes.
 *
 * The server webhook owns the `subscriptions` collection; the app is read-only
 * (the collection's create/update rules deny everything but the superuser).
 */
class SubscriptionManager(
    private val pocketBaseClient: PocketBaseClient,
    private val authManager: AuthManager,
    private val cache: SubscriptionCache,
    private val now: () -> Long = { System.currentTimeMillis() },
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) {
    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    @Volatile
    private var cachedExpiryMillis: Long? = null

    init {
        // Best-effort restore of the persisted expiry so a cold launch on flaky
        // network still honors the offline grace period.
        scope.launch {
            cachedExpiryMillis = cache.loadExpiryMillis()
            _isPro.value = isActive(cachedExpiryMillis)
        }
    }

    /**
     * Re-evaluates Pro from the server. On network failure falls back to the
     * cached expiry (offline grace). Never throws.
     */
    suspend fun refresh() {
        val auth = authManager.authState.first()
        if (auth !is AuthState.SignedIn) {
            apply(expiryMillis = null, preserveOnFailure = false)
            return
        }
        try {
            val response = pocketBaseClient.api.getList(
                collection = "subscriptions",
                page = 1,
                perPage = 1,
                filter = "status = 'active'",
                sort = "-endDate"
            )
            val expiry = response.items.firstOrNull()?.let { parsePocketBaseDate(it) }
            apply(expiryMillis = expiry, preserveOnFailure = false)
        } catch (_: Exception) {
            apply(expiryMillis = null, preserveOnFailure = true)
        }
    }

    private fun apply(expiryMillis: Long?, preserveOnFailure: Boolean) {
        val effective = when {
            expiryMillis != null -> expiryMillis
            preserveOnFailure -> cachedExpiryMillis
            else -> null
        }
        val active = isActive(effective)
        cachedExpiryMillis = effective
        _isPro.value = active
        if (effective != null) {
            scope.launch { cache.saveExpiryMillis(effective) }
        } else if (!preserveOnFailure) {
            scope.launch { cache.saveExpiryMillis(null) }
        }
    }

    private fun isActive(expiryMillis: Long?): Boolean =
        expiryMillis != null && expiryMillis > now()

    private fun parsePocketBaseDate(record: JsonObject): Long? {
        val raw = record.get("endDate")?.takeIf { !it.isJsonNull }?.asString ?: return null
        return runCatching {
            val normalized = raw.replace('T', ' ').removeSuffix("Z").trim()
            val pattern = when {
                normalized.length >= 23 -> "yyyy-MM-dd HH:mm:ss.SSS"
                normalized.length >= 19 -> "yyyy-MM-dd HH:mm:ss"
                else -> return null
            }
            val sdf = SimpleDateFormat(pattern, Locale.US)
            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
            sdf.parse(normalized)?.time
        }.getOrNull()
    }
}

/** Persistence seam for the offline-grace cache. */
interface SubscriptionCache {
    suspend fun loadExpiryMillis(): Long?
    suspend fun saveExpiryMillis(expiryMillis: Long?)
}

/** DataStore-backed [SubscriptionCache]. */
class DataStoreSubscriptionCache(private val context: Context) : SubscriptionCache {
    override suspend fun loadExpiryMillis(): Long? =
        context.subscriptionDataStore.data.first()[EXPIRY_KEY]

    override suspend fun saveExpiryMillis(expiryMillis: Long?) {
        context.subscriptionDataStore.edit { prefs ->
            if (expiryMillis != null) prefs[EXPIRY_KEY] = expiryMillis
            else prefs.remove(EXPIRY_KEY)
        }
    }

    companion object {
        private val EXPIRY_KEY = longPreferencesKey("pro_expires_at_millis")
    }
}