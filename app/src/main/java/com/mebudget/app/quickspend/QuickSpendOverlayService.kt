package com.mebudget.app.quickspend

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.mebudget.app.data.AppDatabase
import com.mebudget.app.data.BudgetRepository
import com.mebudget.app.data.WalletEntity
import java.time.LocalDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class QuickSpendOverlayService : Service() {
    private lateinit var windowManager: WindowManager
    private lateinit var settingsStore: QuickSpendSettingsStore
    private lateinit var foregroundAppDetector: ForegroundAppDetector
    private lateinit var database: AppDatabase
    private lateinit var repository: BudgetRepository
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var overlayView: View? = null
    private var overlayParams: WindowManager.LayoutParams? = null
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
        database = AppDatabase.getInstance(this)
        repository = database.run {
            BudgetRepository(budgetDao(), walletDao(), transactionDao())
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Quick Spend",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows when Quick Spend overlay is active"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Quick Spend")
                .setContentText("Overlay is running")
                .setSmallIcon(android.R.drawable.ic_menu_share)
                .setOngoing(true)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Quick Spend")
                .setContentText("Overlay is running")
                .setSmallIcon(android.R.drawable.ic_menu_share)
                .setOngoing(true)
                .build()
        }

        startForeground(NOTIFICATION_ID, notification)

        handler.post(pollRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        hideOverlay()
        serviceScope.cancel()
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
        if (!QuickSpendPermissions.canDrawOverlays(this)) return
        removeOverlayView()
        val params = baseLayoutParams(focusable = false).apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP or Gravity.START
            x = DEFAULT_X
            y = DEFAULT_Y
        }
        val button = TextView(this).apply {
            text = "₦ BUDGET"
            textSize = 16f
            setTextColor(Color.WHITE)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(28, 16, 28, 16)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 0f
                setColor(Color.rgb(0, 85, 255))
                setStroke(6, Color.BLACK)
            }
            elevation = 10f
            setOnClickListener { showMiniForm() }
        }
        attachDragHandler(button, params)
        val container = FrameLayout(this).apply {
            addView(button)
        }
        windowManager.addView(container, params)
        overlayView = container
        overlayParams = params
        overlayVisible = true
    }

    private fun hideOverlay() {
        removeOverlayView()
        overlayVisible = false
    }

    private fun showMiniForm() {
        val settings = settingsStore.load()
        val selectedBudgetId = settings.selectedBudgetId
        if (selectedBudgetId == null) {
            Toast.makeText(this, "Choose a quick-spend budget.", Toast.LENGTH_SHORT).show()
            showOverlay()
            return
        }

        removeOverlayView()
        val params = baseLayoutParams(focusable = true).apply {
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
        }
        val amountInput = EditText(this).apply {
            hint = "AMOUNT"
            inputType = InputType.TYPE_CLASS_NUMBER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = 16f
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 0f
                setColor(Color.WHITE)
                setStroke(4, Color.BLACK)
            }
            setPadding(20, 16, 20, 16)
        }
        val noteInput = EditText(this).apply {
            hint = "NOTE"
            inputType = InputType.TYPE_CLASS_TEXT
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textSize = 16f
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 0f
                setColor(Color.WHITE)
                setStroke(4, Color.BLACK)
            }
            setPadding(20, 16, 20, 16)
        }
        val walletSpinner = Spinner(this)
        val saveButton = Button(this).apply {
            text = "SAVE"
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 0f
                setColor(Color.rgb(0, 85, 255))
                setStroke(6, Color.BLACK)
            }
            setPadding(20, 16, 20, 16)
        }
        val cancelButton = Button(this).apply {
            text = "CANCEL"
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.BLACK)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 0f
                setColor(Color.WHITE)
                setStroke(6, Color.BLACK)
            }
            setPadding(20, 16, 20, 16)
        }
        val statusText = TextView(this).apply { text = "Loading wallets..." }
        var activeWallets: List<WalletEntity> = emptyList()

        val root = FrameLayout(this).apply {
            setBackgroundColor(0x33000000)
        }
        val panel = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 0f
                setColor(Color.WHITE)
                setStroke(6, Color.BLACK)
            }
            elevation = 12f
        }
        val titleText = TextView(this).apply {
            text = "RECORD SPEND"
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(Color.rgb(22, 28, 36))
        }
        panel.apply {
            addView(titleText)
            addView(amountInput)
            addView(noteInput)
            addView(walletSpinner)
            addView(statusText)
            addView(saveButton)
            addView(cancelButton)
        }
        root.addView(
            panel,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            ).apply {
                leftMargin = 24
                rightMargin = 24
            }
        )

        cancelButton.setOnClickListener { showOverlay() }
        saveButton.setOnClickListener {
            val amount = amountInput.text.toString().trim().toLongOrNull()
            if (amount == null || amount <= 0L) {
                amountInput.error = "Enter a valid amount."
                return@setOnClickListener
            }
            val wallet = activeWallets.getOrNull(walletSpinner.selectedItemPosition)
            if (wallet == null) {
                Toast.makeText(this, "Choose a wallet.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            serviceScope.launch {
                val result = repository.addExpense(
                    budgetId = selectedBudgetId,
                    walletId = wallet.id,
                    amount = amount,
                    dateEpochDay = LocalDate.now().toEpochDay(),
                    note = noteInput.text.toString()
                )
                result.onSuccess {
                    Toast.makeText(
                        this@QuickSpendOverlayService,
                        "Expense recorded.",
                        Toast.LENGTH_SHORT
                    ).show()
                    showOverlay()
                }.onFailure {
                    Toast.makeText(
                        this@QuickSpendOverlayService,
                        it.message ?: "Something went wrong.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        windowManager.addView(root, params)
        overlayView = root
        overlayParams = params
        overlayVisible = true
        amountInput.requestFocus()

        serviceScope.launch {
            activeWallets = withContext(Dispatchers.IO) {
                database.walletDao()
                    .getWalletsForBudget(selectedBudgetId)
                    .filterNot { it.archived }
            }
            val walletNames = activeWallets.map { it.name }
            statusText.text = if (walletNames.isEmpty()) "No wallets available." else ""
            walletSpinner.adapter = ArrayAdapter(
                this@QuickSpendOverlayService,
                android.R.layout.simple_spinner_dropdown_item,
                walletNames
            )
        }
    }

    private fun attachDragHandler(view: View, params: WindowManager.LayoutParams) {
        val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var moved = false

        view.setOnTouchListener { touchedView, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    moved = false
                    true
                }

                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    if (!moved && deltaX * deltaX + deltaY * deltaY < touchSlop * touchSlop) {
                        return@setOnTouchListener true
                    }
                    params.x = initialX + deltaX.toInt()
                    params.y = initialY + deltaY.toInt()
                    overlayView?.let { windowManager.updateViewLayout(it, params) }
                    moved = true
                    true
                }

                MotionEvent.ACTION_UP -> {
                    if (!moved) touchedView.performClick()
                    true
                }

                else -> false
            }
        }
    }

    private fun removeOverlayView() {
        overlayView?.let { view ->
            runCatching { windowManager.removeView(view) }
        }
        overlayView = null
    }

    private fun baseLayoutParams(focusable: Boolean): WindowManager.LayoutParams {
        val flags = if (focusable) {
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        } else {
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        }
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayWindowType(),
            flags,
            PixelFormat.TRANSLUCENT
        )
    }

    private fun overlayWindowType(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
    }

    private companion object {
        const val POLL_INTERVAL_MILLIS = 1_000L
        const val DEFAULT_X = 24
        const val DEFAULT_Y = 240
        const val NOTIFICATION_CHANNEL_ID = "quick_spend_overlay"
        const val NOTIFICATION_ID = 1001
    }
}
