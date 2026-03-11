package com.example.appwatchdog

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.EditText
import android.widget.Switch
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var packageEdit: EditText
    private lateinit var watchdogSwitch: Switch

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        packageEdit = findViewById(R.id.packageNameEdit)
        watchdogSwitch = findViewById(R.id.watchdogSwitch)

        // Request Usage Access if not granted
        if (!hasUsageStatsPermission()) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        watchdogSwitch.setOnCheckedChangeListener { _, isChecked ->
            val packageName = packageEdit.text.toString().trim()
            val intent = Intent(this, WatchdogService::class.java)
            intent.putExtra("MONITORED_PACKAGE", packageName)
            if (isChecked) {
                startForegroundService(intent)
            } else {
                stopService(intent)
            }
        }
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }
}