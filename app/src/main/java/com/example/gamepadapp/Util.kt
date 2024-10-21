package com.example.gamepadapp

import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

object Util {
    inline val Any.TAG: String? get() = this::class.simpleName

    val KeyEvent.isGamepadKeyEvent: Boolean get() = source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD
    val KeyEvent.isJoystickEvent: Boolean get() = source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK

    val MotionEvent.isGamepadMotionEvent: Boolean get() = source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD
    val MotionEvent.isJoystickEvent: Boolean get() = source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK

    val MotionEvent.rightStickX : Float get() = getAxisValue(MotionEvent.AXIS_Z)
    val MotionEvent.rightStickY : Float get() = getAxisValue(MotionEvent.AXIS_RZ)

    val MotionEvent.leftStickX  : Float get() = getAxisValue(MotionEvent.AXIS_X)
    val MotionEvent.leftStickY  : Float get() = getAxisValue(MotionEvent.AXIS_Y)

    fun Offset.isInsideCircle(joystickRadius: Float, center: Offset): Boolean {
        val distance = sqrt((x - center.x).pow(2) + (y - center.y).pow(2))
        return distance <= joystickRadius
    }

    // Helper function to ensure joystick center remains within bounds
    fun Offset.coerceInBounds(bounds: Size, radius: Float): Offset {
        return Offset(
            x = x.coerceIn(radius, bounds.width - radius),
            y = y.coerceIn(radius, bounds.height - radius)
        )
    }

    fun MotionEvent.rightStickXDisplacement(
        joystickRadius: Float,
        center: Offset
    ): Float = center.x + rightStickX * joystickRadius

    fun MotionEvent.rightStickYDisplacement(
        joystickRadius: Float,
        center: Offset
    ): Float = center.y - rightStickY * joystickRadius

    fun MotionEvent.leftStickXDisplacement(
        joystickRadius: Float,
        center: Offset
    ): Float = center.x + leftStickX * joystickRadius

    fun MotionEvent.leftStickYDisplacement(
        joystickRadius: Float,
        joystickCenter: Offset
    ): Float = joystickCenter.y - leftStickY * joystickRadius

    fun MotionEvent.isRightInDeadzone(deadzone: Float = 0.1f): Boolean =
        abs(rightStickX) < deadzone && abs(rightStickY) < deadzone

    fun MotionEvent.isLeftInDeadzone(deadzone: Float = 0.1f): Boolean =
        abs(leftStickX) < deadzone && abs(leftStickY) < deadzone

    data class JoystickTouchEvent(
        val x: Float,
        val y: Float,
        val action: Int,
        val downTime: Long,
        val eventTime: Long,
        val metaState: Int,
        val isRightJoystick: Boolean,
        val joystickRadius: Float,
        val joystickCenter: Offset
    ) {
        fun toGestureDescription(previousEvent: JoystickTouchEvent?): GestureDescription {
            val path = Path()
            val duration = 1L

            val gestureBuilder = GestureDescription.Builder()
            /*val stroke = when {
                action == MotionEvent.ACTION_DOWN && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
                    //GestureDescription.StrokeDescription(path, event.downTime, event.eventTime, true)
                    path.moveTo(x, y)
                    GestureDescription.StrokeDescription(path, 0, duration, true)
                }
                action == MotionEvent.ACTION_UP && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O  -> {
                    path.moveTo(x, y)
                    GestureDescription.StrokeDescription(path, 59_999, duration*//*59_999*//*, false)
                }
                else -> {
                    if (action == MotionEvent.ACTION_MOVE) {
                        path.moveTo(previousEvent?.x ?: joystickCenter.x, previousEvent?.y ?: joystickCenter.y)
                        path.lineTo(x, y)
                    }
                    GestureDescription.StrokeDescription(path, 0, duration)
                }
            }*/
            path.moveTo(previousEvent?.x ?: joystickCenter.x, previousEvent?.y ?: joystickCenter.y)
            path.lineTo(x, y)

            val stroke = GestureDescription.StrokeDescription(path, 0, duration)
            gestureBuilder.addStroke(stroke)

            return gestureBuilder.build()
        }

        companion object {
            fun MotionEvent.toJoystickTouchEvent(
                joystickRadius: Float,
                joystickCenter: Offset,
                isRightJoystick: Boolean
            ): JoystickTouchEvent {
                val x: Float
                val y: Float
                if (isRightJoystick) {
                    if (isRightInDeadzone()) {
                        x = 0f
                        y = 0f
                    } else {
                        x = rightStickXDisplacement(joystickRadius, joystickCenter)
                        y = rightStickYDisplacement(joystickRadius, joystickCenter)
                    }
                } else {
                    if (isLeftInDeadzone()) {
                        x = 0f
                        y = 0f
                    } else {
                        x = leftStickXDisplacement(joystickRadius, joystickCenter)
                        y = leftStickYDisplacement(joystickRadius, joystickCenter)
                    }
                }

                return JoystickTouchEvent(
                    x = x,
                    y = y,
                    action = action,
                    downTime = downTime,
                    eventTime = eventTime,
                    isRightJoystick = isRightJoystick,
                    joystickRadius = joystickRadius,
                    metaState = metaState,
                    joystickCenter = joystickCenter
                )
            }
        }
    }
}