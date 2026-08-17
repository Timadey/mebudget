package com.mebudget.app.data.sync

import android.content.Context
import com.mebudget.app.billing.DataStoreSubscriptionCache
import com.mebudget.app.billing.SubscriptionManager
import com.mebudget.app.data.AppDatabase
import com.mebudget.app.data.auth.AuthManager
import com.mebudget.app.data.auth.UserPreferences

/**
 * Holds the app-wide sync + auth + billing dependency graph so every consumer
 * (UI, workers, realtime) shares a SINGLE [PocketBaseClient]. Sharing is what
 * makes the sign-in token reach the sync/realtime requests — creating a fresh
 * client per call-site would silently drop the bearer token.
 */
data class SyncDependencies(
    val client: PocketBaseClient,
    val authManager: AuthManager,
    val syncEngine: SyncEngine,
    val realtimeListener: RealtimeListener,
    val subscriptionManager: SubscriptionManager
)

private val lock = Any()

@Volatile
private var cachedDependencies: SyncDependencies? = null

/** Returns [SyncDependencies], building it lazily once per process. */
internal fun Context.syncDependencies(): SyncDependencies {
    cachedDependencies?.let { return it }
    synchronized(lock) {
        cachedDependencies?.let { return it }
        val graph = buildGraph(applicationContext)
        cachedDependencies = graph
        return graph
    }
}

private fun buildGraph(context: Context): SyncDependencies {
    val client = PocketBaseClient(PocketBaseConfig.baseUrl)
    val userPreferences = UserPreferences(context)
    val authManager = AuthManager(client, userPreferences)
    val database = AppDatabase.getInstance(context)
    val realtimeListener = RealtimeListener(client)
    val subscriptionManager = SubscriptionManager(
        pocketBaseClient = client,
        authManager = authManager,
        cache = DataStoreSubscriptionCache(context)
    )
    return SyncDependencies(
        client = client,
        authManager = authManager,
        syncEngine = SyncEngine(client, authManager, database, realtimeListener),
        realtimeListener = realtimeListener,
        subscriptionManager = subscriptionManager
    )
}