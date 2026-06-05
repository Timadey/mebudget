package com.mebudget.app.quickspend

import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.concurrent.TimeUnit

class ForegroundAppDetector(
    private val context: Context
) {
    fun currentForegroundPackage(): String? {
        if (!QuickSpendPermissions.hasUsageAccess(context)) return null
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            now - TimeUnit.MINUTES.toMillis(2),
            now
        )
        return stats.maxByOrNull { it.lastTimeUsed }?.packageName
    }
}
