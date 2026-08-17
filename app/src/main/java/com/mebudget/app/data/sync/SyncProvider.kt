package com.mebudget.app.data.sync

import android.content.Context
import com.mebudget.app.data.AppDatabase
import com.mebudget.app.data.auth.AuthManager
import com.mebudget.app.data.auth.UserPreferences

/** Holds the sync dependency graph so the same PocketBaseClient is shared. */
data class SyncDependencies(
    val client: PocketBaseClient,
    val authManager: AuthManager,
    val syncEngine: SyncEngine,
    val realtimeListener: RealtimeListener
)

internal fun Context.syncDependencies(): SyncDependencies {
    val client = PocketBaseClient(PocketBaseConfig.DEFAULT_DEV_URL)
    val authManager = AuthManager(client, UserPreferences(this))
    val database = AppDatabase.getInstance(this)
    val realtimeListener = RealtimeListener(client)
    return SyncDependencies(
        client = client,
        authManager = authManager,
        syncEngine = SyncEngine(client, authManager, database, realtimeListener),
        realtimeListener = realtimeListener
    )
}