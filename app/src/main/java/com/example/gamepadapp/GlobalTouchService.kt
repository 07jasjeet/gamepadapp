package com.example.gamepadapp

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Build
import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.accessibility.AccessibilityEvent
import androidx.compose.ui.geometry.Offset
import com.example.gamepadapp.Util.JoystickTouchEvent.Companion.toJoystickTouchEvent
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


class GlobalTouchService(
    private val touchCommunicator: TouchCommunicator = Deps.touchCommunicator
) : AccessibilityService() {

    private val scope = MainScope()

    init {
        scope.launch {
            touchCommunicator.leftTouchFlow.collect { event ->
                if (event != null) {

                }
            }
        }

        scope.launch {
            touchCommunicator.rightTouchFlow.collect { event ->
                if (event != null) {

                }
            }
        }

        scope.launch {
            touchCommunicator.touchFlow.collect { event ->
                if (event != null) {
                    Log.d("test", event.toString())
                    //simulateTouch(event)
                }
            }
        }
    }

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo()
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK
        info.notificationTimeout = 100
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        info.flags = run {
            var flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS or
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                    AccessibilityServiceInfo.DEFAULT or
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                flags = flags or AccessibilityServiceInfo.FLAG_ENABLE_ACCESSIBILITY_VOLUME
            }

            return@run flags
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            info.motionEventSources = InputDevice.SOURCE_JOYSTICK
        }
        this.serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Not used, but required to implement
        // Log.d("test", "Accessibility event: " + event.toString())
    }

    override fun onInterrupt() {}

    override fun onKeyEvent(event: KeyEvent?): Boolean {
        event ?: super.onKeyEvent(event)

        Log.d("test", "Key event: " + event.toString())
        scope.launch { touchCommunicator.keyEventFlow.emit(event) }

        return false
    }

    private var previousLeftEvent: Util.JoystickTouchEvent? = null
    private var previousRightEvent: Util.JoystickTouchEvent? = null

    override fun onMotionEvent(event: MotionEvent) {
        val density = baseContext.resources.displayMetrics.density
        val leftJoystickEvent = event.toJoystickTouchEvent(
            joystickCenter = Offset(200 * density, 200 * density),
            isRightJoystick = false,
            joystickRadius = 150 * density
        )
        val leftJoystickGesture = leftJoystickEvent.toGestureDescription(previousLeftEvent)

        val rightJoystickEvent = event.toJoystickTouchEvent(
            joystickCenter = Offset(400 * density,  200 * density),
            isRightJoystick = true,
            joystickRadius = 150 * density
        )
        val rightJoystickGesture = rightJoystickEvent.toGestureDescription(previousRightEvent)

        dispatchGesture(leftJoystickGesture, null, null)
        dispatchGesture(rightJoystickGesture, null, null)

        previousLeftEvent = leftJoystickEvent
        previousRightEvent = rightJoystickEvent

        //touchCommunicator.touchFlow.value = event
    }
}