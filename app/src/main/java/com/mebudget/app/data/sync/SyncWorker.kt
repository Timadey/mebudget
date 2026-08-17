package com.mebudget.app.data.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * Builds the sync dependency graph from the app context (manual DI, no Koin)
 * and runs a full push + pull cycle.
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val deps = applicationContext.syncDependencies()
        deps.authManager.restoreSession()
        return try {
            deps.syncEngine.syncNow().fold(
                onSuccess = { Result.success() },
                onFailure = {
                    if (runAttemptCount < 3) Result.retry() else Result.failure()
                }
            )
        } catch (e: Exception) {
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }
}