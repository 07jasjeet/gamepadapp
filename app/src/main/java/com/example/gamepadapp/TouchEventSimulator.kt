
package com.example.gamepadapp

import android.os.SystemClock
import android.view.MotionEvent
import android.view.View

class TouchEventSimulator(private val view: View) {
    private var isTouchActive = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    fun simulateTouch(touchX: Float, touchY: Float, isActive: Boolean) {
        val eventTime = SystemClock.uptimeMillis()

        if (!isTouchActive && isActive) {
            val downEvent = MotionEvent.obtain(
                eventTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                touchX,
                touchY,
                0
            )
            view.dispatchTouchEvent(downEvent)
            downEvent.recycle()
            isTouchActive = true
        } else if (isTouchActive && isActive) {
            val moveEvent = MotionEvent.obtain(
                eventTime,
                eventTime,
                MotionEvent.ACTION_MOVE,
                touchX,
                touchY,
                0
            )
            view.dispatchTouchEvent(moveEvent)
            moveEvent.recycle()
        } else if (isTouchActive && !isActive) {
            val upEvent = MotionEvent.obtain(
                eventTime,
                eventTime,
                MotionEvent.ACTION_UP,
                lastTouchX,
                lastTouchY,
                0
            )
            view.dispatchTouchEvent(upEvent)
            upEvent.recycle()
            isTouchActive = false
        }

        lastTouchX = touchX
        lastTouchY = touchY
    }
}
