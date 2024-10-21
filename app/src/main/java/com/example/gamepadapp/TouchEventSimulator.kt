
package com.example.gamepadapp

import android.view.MotionEvent
import android.view.View

class TouchEventSimulator(
    private val view: View,
    private val onEvent: (MotionEvent) -> Unit
) {
    private var isTouchActive = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    fun simulateTouch(touchX: Float, touchY: Float, downTime: Long, eventTime: Long, isActive: Boolean) {

        if (!isTouchActive && isActive) {
            val downEvent = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_DOWN,
                touchX,
                touchY,
                0
            )
            onEvent(downEvent)
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
            onEvent(moveEvent)
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
            onEvent(upEvent)
            view.dispatchTouchEvent(upEvent)
            upEvent.recycle()
            isTouchActive = false
        }

        lastTouchX = touchX
        lastTouchY = touchY
    }
}
