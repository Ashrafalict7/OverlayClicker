package com.overlayapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.overlayapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val REQUEST_OVERLAY_PERMISSION = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        updateServiceStatus()
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
    }

    private fun setupUI() {
        // Start/Stop overlay service button
        binding.btnToggleService.setOnClickListener {
            if (OverlayService.isRunning) {
                stopOverlayService()
            } else {
                checkAndStartOverlay()
            }
        }

        // Grant permission button
        binding.btnGrantPermission.setOnClickListener {
            requestOverlayPermission()
        }
    }

    private fun updateServiceStatus() {
        val hasPermission = Settings.canDrawOverlays(this)

        if (hasPermission) {
            binding.tvPermissionStatus.text = "✅ Overlay Permission: Granted"
            binding.btnGrantPermission.isEnabled = false
            binding.btnGrantPermission.alpha = 0.5f
            binding.btnToggleService.isEnabled = true
        } else {
            binding.tvPermissionStatus.text = "❌ Overlay Permission: Not Granted"
            binding.btnGrantPermission.isEnabled = true
            binding.btnGrantPermission.alpha = 1f
            binding.btnToggleService.isEnabled = false
        }

        if (OverlayService.isRunning) {
            binding.btnToggleService.text = "⏹ Stop Overlay"
            binding.btnToggleService.setBackgroundColor(getColor(R.color.stop_red))
            binding.tvServiceStatus.text = "🟢 Service: Running"
        } else {
            binding.btnToggleService.text = "▶ Start Overlay"
            binding.btnToggleService.setBackgroundColor(getColor(R.color.start_green))
            binding.tvServiceStatus.text = "🔴 Service: Stopped"
        }
    }

    private fun checkAndStartOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please grant overlay permission first", Toast.LENGTH_SHORT).show()
            requestOverlayPermission()
            return
        }
        startOverlayService()
    }

    private fun startOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Overlay started! You can now minimize the app.", Toast.LENGTH_LONG).show()
        updateServiceStatus()
    }

    private fun stopOverlayService() {
        val intent = Intent(this, OverlayService::class.java)
        stopService(intent)
        Toast.makeText(this, "Overlay stopped.", Toast.LENGTH_SHORT).show()
        updateServiceStatus()
    }

    private fun requestOverlayPermission() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:$packageName")
        )
        startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            updateServiceStatus()
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
