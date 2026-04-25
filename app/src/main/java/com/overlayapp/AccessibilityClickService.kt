package com.overlayapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi

class AccessibilityClickService : AccessibilityService() {

    companion object {
        var instance: AccessibilityClickService? = null

        fun isEnabled(): Boolean = instance != null

        fun performTap(x: Int, y: Int): Boolean {
            val service = instance ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                service.doTap(x, y)
            } else {
                false
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    @RequiresApi(Build.VERSION_CODES.N)
    fun doTap(x: Int, y: Int): Boolean {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val stroke = GestureDescription.StrokeDescription(path, 0, 50)
        val gesture = GestureDescription.Builder().addStroke(stroke).build()
        return dispatchGesture(gesture, null, null)
    }
}
