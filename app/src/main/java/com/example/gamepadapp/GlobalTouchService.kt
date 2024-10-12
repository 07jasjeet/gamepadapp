package com.example.gamepadapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.accessibility.AccessibilityEvent

class GlobalTouchService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used, but required to implement
    }

    override fun onInterrupt() {
        // Not used, but required to implement
    }

    fun simulateTouch(x: Float, y: Float) {
        val gestureBuilder = GestureDescription.Builder()
        val path = Path()
        path.moveTo(x, y)
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 1))
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    fun simulateTouchDown(x: Float, y: Float) {
        val gestureBuilder = GestureDescription.Builder()
        val path = Path()
        path.moveTo(x, y)
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 1))
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    fun simulateTouchMove(x: Float, y: Float) {
        val gestureBuilder = GestureDescription.Builder()
        val path = Path()
        path.moveTo(x, y)
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 1))
        dispatchGesture(gestureBuilder.build(), null, null)
    }

    fun simulateTouchUp(x: Float, y: Float) {
        val gestureBuilder = GestureDescription.Builder()
        val path = Path()
        path.moveTo(x, y)
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 1))
        dispatchGesture(gestureBuilder.build(), null, null)
    }
}