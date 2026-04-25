package com.overlayapp

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.core.app.NotificationCompat
import com.overlayapp.databinding.OverlayControlPanelBinding
import com.overlayapp.databinding.OverlayFloatingButtonBinding
import com.overlayapp.databinding.OverlayTargetPointBinding

class OverlayService : Service() {

    companion object {
        var isRunning = false
        const val CHANNEL_ID = "overlay_service_channel"
        const val NOTIFICATION_ID = 101
        const val ACTION_STOP = "ACTION_STOP"
    }

    private lateinit var windowManager: WindowManager

    // Views
    private var floatingBtnView: View? = null
    private var targetView: View? = null
    private var panelView: View? = null
    private var tapCatcherView: View? = null

    // Layout params
    private var floatingParams: WindowManager.LayoutParams? = null
    private var targetParams: WindowManager.LayoutParams? = null
    private var panelParams: WindowManager.LayoutParams? = null

    // Bindings
    private var floatingBinding: OverlayFloatingButtonBinding? = null
    private var targetBinding: OverlayTargetPointBinding? = null
    private var panelBinding: OverlayControlPanelBinding? = null

    // State
    private var targetX = 400
    private var targetY = 600
    private var buttonSize = 120
    private var buttonAlpha = 0.85f
    private var clickIntervalMs = 1000L
    private var isAutoClickRunning = false
    private var isPanelVisible = false

    private val handler = Handler(Looper.getMainLooper())
    private val autoClickRunnable = object : Runnable {
        override fun run() {
            if (isAutoClickRunning) {
                performTap()
                handler.postDelayed(this, clickIntervalMs)
            }
        }
    }

    // ─── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        addFloatingButton()
        addTargetPoint()
        addControlPanel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        isRunning = false
        isAutoClickRunning = false
        handler.removeCallbacksAndMessages(null)
        safeRemove(floatingBtnView)
        safeRemove(targetView)
        safeRemove(panelView)
        safeRemove(tapCatcherView)
    }

    // ─── Overlay type helper ───────────────────────────────────────────────────

    private fun overlayType() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE

    // ─── Floating Button ───────────────────────────────────────────────────────

    private fun addFloatingButton() {
        floatingBinding = OverlayFloatingButtonBinding.inflate(LayoutInflater.from(this))
        floatingBtnView = floatingBinding!!.root

        floatingParams = WindowManager.LayoutParams(
            buttonSize, buttonSize,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 60
            y = 300
        }

        floatingBtnView!!.alpha = buttonAlpha
        windowManager.addView(floatingBtnView, floatingParams)
        setupFloatingButtonTouch()
    }

    private fun setupFloatingButtonTouch() {
        var downRawX = 0f; var downRawY = 0f
        var downParamX = 0; var downParamY = 0
        var moved = false

        floatingBtnView!!.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = e.rawX; downRawY = e.rawY
                    downParamX = floatingParams!!.x; downParamY = floatingParams!!.y
                    moved = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - downRawX).toInt()
                    val dy = (e.rawY - downRawY).toInt()
                    if (moved || dx * dx + dy * dy > 25) {
                        moved = true
                        floatingParams!!.x = downParamX + dx
                        floatingParams!!.y = downParamY + dy
                        windowManager.updateViewLayout(floatingBtnView, floatingParams)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!moved) handleFloatingButtonTap()
                    true
                }
                else -> false
            }
        }
    }

    private fun handleFloatingButtonTap() {
        // If running, tapping the button pauses; otherwise opens panel
        if (isAutoClickRunning) {
            stopAutoClick()
        } else {
            togglePanel()
        }
    }

    // ─── Target Point ──────────────────────────────────────────────────────────

    private fun addTargetPoint() {
        targetBinding = OverlayTargetPointBinding.inflate(LayoutInflater.from(this))
        targetView = targetBinding!!.root

        targetParams = WindowManager.LayoutParams(
            80, 80,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = targetX
            y = targetY
        }

        windowManager.addView(targetView, targetParams)

        var downRawX = 0f; var downRawY = 0f
        var downParamX = 0; var downParamY = 0

        targetView!!.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downRawX = e.rawX; downRawY = e.rawY
                    downParamX = targetParams!!.x; downParamY = targetParams!!.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    targetParams!!.x = downParamX + (e.rawX - downRawX).toInt()
                    targetParams!!.y = downParamY + (e.rawY - downRawY).toInt()
                    targetX = targetParams!!.x
                    targetY = targetParams!!.y
                    windowManager.updateViewLayout(targetView, targetParams)
                    refreshCoords()
                    true
                }
                else -> false
            }
        }
    }

    // ─── Control Panel ─────────────────────────────────────────────────────────

    private fun addControlPanel() {
        panelBinding = OverlayControlPanelBinding.inflate(LayoutInflater.from(this))
        panelView = panelBinding!!.root

        panelParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType(),
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 180
        }

        panelView!!.visibility = View.GONE
        windowManager.addView(panelView, panelParams)

        wirePanel()
        refreshCoords()
        refreshSpeedLabel()
        refreshSizeLabel()
        refreshAlphaLabel()
    }

    private fun wirePanel() {
        val pb = panelBinding ?: return

        pb.btnClosePanel.setOnClickListener { hidePanel() }

        // ── Start / Stop ──
        pb.btnStartStop.setOnClickListener {
            if (isAutoClickRunning) stopAutoClick() else startAutoClick()
            hidePanel()
        }

        // ── Click once ──
        pb.btnClickOnce.setOnClickListener { performTap() }

        // ── Tap-to-set target ──
        pb.btnSetTarget.setOnClickListener {
            hidePanel()
            showTapCatcher()
        }

        // ── Size ──
        pb.btnSizeMinus.setOnClickListener {
            buttonSize = (buttonSize - 10).coerceAtLeast(50)
            applyButtonSize()
            refreshSizeLabel()
        }
        pb.btnSizePlus.setOnClickListener {
            buttonSize = (buttonSize + 10).coerceAtMost(250)
            applyButtonSize()
            refreshSizeLabel()
        }

        // ── Alpha ──
        pb.btnAlphaMinus.setOnClickListener {
            buttonAlpha = (buttonAlpha - 0.1f).coerceAtLeast(0.1f)
            floatingBtnView?.alpha = buttonAlpha
            refreshAlphaLabel()
        }
        pb.btnAlphaPlus.setOnClickListener {
            buttonAlpha = (buttonAlpha + 0.1f).coerceAtMost(1.0f)
            floatingBtnView?.alpha = buttonAlpha
            refreshAlphaLabel()
        }

        // ── Speed ──
        pb.btnSpeedMinus.setOnClickListener {
            clickIntervalMs = (clickIntervalMs + 200).coerceAtMost(5000)
            refreshSpeedLabel()
        }
        pb.btnSpeedPlus.setOnClickListener {
            clickIntervalMs = (clickIntervalMs - 200).coerceAtLeast(100)
            refreshSpeedLabel()
        }
    }

    // ─── Tap-to-Set Target fullscreen catcher ──────────────────────────────────

    private fun showTapCatcher() {
        val view = View(this).apply {
            setBackgroundColor(Color.argb(80, 0, 0, 0))
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            overlayType(),
            0, // focusable so it catches all touches
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.TOP or Gravity.START }

        view.setOnTouchListener { _, e ->
            if (e.action == MotionEvent.ACTION_DOWN) {
                // Place target center at finger position
                targetX = e.rawX.toInt() - 40
                targetY = e.rawY.toInt() - 40
                targetParams!!.x = targetX
                targetParams!!.y = targetY
                windowManager.updateViewLayout(targetView, targetParams)
                refreshCoords()
                safeRemove(tapCatcherView)
                tapCatcherView = null
            }
            true
        }

        tapCatcherView = view
        windowManager.addView(view, params)
    }

    // ─── Auto Click ────────────────────────────────────────────────────────────

    private fun startAutoClick() {
        isAutoClickRunning = true
        floatingBinding?.root?.setBackgroundResource(R.drawable.fab_bg_active)
        floatingBinding?.btnPlayPause?.text = "⏸"
        panelBinding?.btnStartStop?.text = "⏹ Stop Auto-Click"
        handler.post(autoClickRunnable)
    }

    private fun stopAutoClick() {
        isAutoClickRunning = false
        handler.removeCallbacks(autoClickRunnable)
        floatingBinding?.root?.setBackgroundResource(R.drawable.fab_bg_idle)
        floatingBinding?.btnPlayPause?.text = "▶"
        panelBinding?.btnStartStop?.text = "▶ Start Auto-Click"
    }

    private fun performTap() {
        // Animate target as visual feedback
        targetBinding?.ivTarget?.animate()
            ?.scaleX(1.4f)?.scaleY(1.4f)?.setDuration(80)
            ?.withEndAction {
                targetBinding?.ivTarget?.animate()
                    ?.scaleX(1f)?.scaleY(1f)?.setDuration(80)?.start()
            }?.start()

        // Real touch injection via AccessibilityService
        val cx = targetX + 40
        val cy = targetY + 40
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            AccessibilityClickService.isEnabled()) {
            AccessibilityClickService.performTap(cx, cy)
        }
    }

    // ─── Panel helpers ─────────────────────────────────────────────────────────

    private fun togglePanel() {
        if (isPanelVisible) hidePanel() else showPanel()
    }

    private fun showPanel() {
        isPanelVisible = true
        // Position the panel to the right of the floating button, or below if near right edge
        val btnX = floatingParams?.x ?: 60
        val btnY = floatingParams?.y ?: 300
        panelParams!!.x = btnX + buttonSize + 12
        panelParams!!.y = btnY
        windowManager.updateViewLayout(panelView, panelParams)
        panelView?.visibility = View.VISIBLE
        panelBinding?.btnStartStop?.text =
            if (isAutoClickRunning) "⏹ Stop Auto-Click" else "▶ Start Auto-Click"
    }

    private fun hidePanel() {
        isPanelVisible = false
        panelView?.visibility = View.GONE
    }

    // ─── Label refreshers ──────────────────────────────────────────────────────

    private fun refreshCoords() {
        panelBinding?.tvTargetCoords?.text = "🎯 Target: ($targetX, $targetY)"
    }

    private fun refreshSizeLabel() {
        panelBinding?.tvSizeValue?.text = "${buttonSize}px"
    }

    private fun refreshAlphaLabel() {
        panelBinding?.tvAlphaValue?.text = "${(buttonAlpha * 100).toInt()}%"
    }

    private fun refreshSpeedLabel() {
        panelBinding?.tvSpeedValue?.text =
            if (clickIntervalMs < 1000) "${clickIntervalMs}ms"
            else "${"%.1f".format(clickIntervalMs / 1000.0)}s"
    }

    private fun applyButtonSize() {
        floatingParams?.width = buttonSize
        floatingParams?.height = buttonSize
        floatingBtnView?.let { windowManager.updateViewLayout(it, floatingParams) }
    }

    // ─── Utility ───────────────────────────────────────────────────────────────

    private fun safeRemove(view: View?) {
        try { view?.let { windowManager.removeView(it) } } catch (_: Exception) {}
    }

    // ─── Notification ──────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Overlay Clicker",
                NotificationManager.IMPORTANCE_LOW).apply {
                description = "Running overlay clicker service"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = PendingIntent.getService(
            this, 1,
            Intent(this, OverlayService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⚡ Overlay Clicker Active")
            .setContentText("Tap the green circle to configure or start clicking")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_delete, "Stop Service", stopIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}
