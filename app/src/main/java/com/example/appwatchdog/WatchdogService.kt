package com.example.appwatchdog

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.app.usage.UsageStatsManager
import android.app.usage.UsageStats
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi

class WatchdogService : Service() {

    private var monitoredPackage: String = ""
    private val handler = Handler(Looper.getMainLooper())
    private val pollRunnable = object : Runnable {
        override fun run() {
            checkForegroundApp()
            handler.postDelayed(this, 2000)
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        val notification = Notification.Builder(this, "watchdog_channel")
            .setContentTitle("App Watchdog Running")
            .setContentText("Monitoring app usage")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .build()
        startForeground(1, notification)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        monitoredPackage = intent?.getStringExtra("MONITORED_PACKAGE") ?: ""
        handler.post(pollRunnable)
        return START_STICKY
    }

    private fun checkForegroundApp() {
        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 3000
        val stats: List<UsageStats> = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            beginTime,
            endTime
        )
        val currentApp = stats.maxByOrNull { it.lastTimeUsed }?.packageName
        if (currentApp == monitoredPackage) {
            Log.d("WATCHDOG", "Current app: $currentApp")
            showOverlay()
        }
    }
    private var overlayView: View? = null
    private var overlayShown = false

    private fun showOverlay() {

        if (overlayShown) return
        overlayShown = true

        val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setBackgroundColor(Color.BLACK)
        layout.setPadding(80, 80, 80, 80)

        val text = TextView(this)
        text.text = "⚠ App Detected. Take a breath."
        text.textSize = 24f
        text.setTextColor(Color.WHITE)

        val button = Button(this)
        button.text = "Dismiss"

        layout.addView(text)
        layout.addView(button)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.CENTER

        overlayView = layout
        windowManager.addView(overlayView, params)

        button.setOnClickListener {
            overlayView?.let { windowManager.removeView(it) }
            overlayShown = false
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "watchdog_channel",
                "App Watchdog",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
    }
}