package com.mebudget.app.quickspend

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.WindowManager
import com.mebudget.app.data.AppDatabase
import com.mebudget.app.data.BudgetRepository

class QuickSpendOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var settingsStore: QuickSpendSettingsStore
    private lateinit var foregroundAppDetector: ForegroundAppDetector
    private lateinit var repository: BudgetRepository
    private val handler = Handler(Looper.getMainLooper())
    private var overlayVisible = false

    private val pollRunnable = object : Runnable {
        override fun run() {
            updateOverlayVisibility()
            handler.postDelayed(this, POLL_INTERVAL_MILLIS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        settingsStore = SharedPreferencesQuickSpendSettingsStore(this)
        foregroundAppDetector = ForegroundAppDetector(this)
        repository = AppDatabase.getInstance(this).run {
            BudgetRepository(budgetDao(), walletDao(), transactionDao())
        }
        handler.post(pollRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        hideOverlay()
        super.onDestroy()
    }

    private fun updateOverlayVisibility() {
        val settings = settingsStore.load()
        val setupComplete = settings.isSetupComplete(
            overlayPermissionGranted = QuickSpendPermissions.canDrawOverlays(this),
            usageAccessGranted = QuickSpendPermissions.hasUsageAccess(this)
        )
        val shouldShow = setupComplete &&
            settings.matchesForegroundPackage(foregroundAppDetector.currentForegroundPackage())
        if (shouldShow && !overlayVisible) showOverlay()
        if (!shouldShow && overlayVisible) hideOverlay()
    }

    private fun showOverlay() {
        overlayVisible = true
    }

    private fun hideOverlay() {
        overlayVisible = false
    }

    private companion object {
        const val POLL_INTERVAL_MILLIS = 1_000L
    }
}
