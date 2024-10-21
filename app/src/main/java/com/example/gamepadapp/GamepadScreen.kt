package com.example.gamepadapp

import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import com.example.gamepadapp.Util.JoystickTouchEvent.Companion.toJoystickTouchEvent
import com.example.gamepadapp.Util.isGamepadKeyEvent
import com.example.gamepadapp.Util.isJoystickEvent
import com.example.gamepadapp.Util.leftStickX
import com.example.gamepadapp.Util.leftStickY
import com.example.gamepadapp.Util.rightStickX
import com.example.gamepadapp.Util.rightStickY
import kotlin.math.pow
import kotlin.math.sqrt

interface GamepadScreen {
    var rightTouchSimulator: TouchEventSimulator
    var leftTouchSimulator: TouchEventSimulator
    var canvasPosition: IntArray //= IntArray(2)
    var canvasSize: Size //= Size(0f, 0f)

    val joystickRadius: Float //= 150f
    var leftJoystickCenter: Offset //by mutableStateOf(Offset(250f, 250f))
    var rightJoystickCenter: Offset // by mutableStateOf(Offset(750f, 750f))
    var isConfigMode: Boolean //by mutableStateOf(false)

    fun openApp()

    @Composable
    fun GamepadButtonDisplay() {
        var pressedButtons by remember { mutableStateOf(setOf<Int>()) }
        var leftStickX by remember { mutableFloatStateOf(0f) }
        var leftStickY by remember { mutableFloatStateOf(0f) }
        var rightStickX by remember { mutableFloatStateOf(0f) }
        var rightStickY by remember { mutableFloatStateOf(0f) }
        val view = LocalView.current

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DrawingCanvas(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onGloballyPositioned { layoutCoordinates ->
                            val position = layoutCoordinates.positionInWindow()
                            val size = layoutCoordinates.size.toSize()
                            canvasPosition[0] = position.x.toInt()
                            canvasPosition[1] = position.y.toInt()
                            canvasSize = size
                        }
                )

                Text(
                    text = "Pressed Buttons:",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (pressedButtons.isEmpty()) {
                    Text(
                        text = "None",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    pressedButtons.forEach { button ->
                        Text(
                            text = "Button ${KeyEvent.keyCodeToString(button)}",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Analog Sticks:",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text("Left Stick: X = %.2f, Y = %.2f".format(leftStickX, leftStickY))
                Text("Right Stick: X = %.2f, Y = %.2f".format(rightStickX, rightStickY))
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { openApp() },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text("Open drawing")
                }
                Button(
                    onClick = { isConfigMode = !isConfigMode },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(if (isConfigMode) "Exit Config Mode" else "Configure Joysticks")
                }
            }

            if (isConfigMode) {
                ConfigOverlay(
                    onCloseRequest = { isConfigMode = false }
                )
            }
        }

        DisposableEffect(view) {
            view.isFocusableInTouchMode = true
            view.requestFocus()

            val keyListener = View.OnKeyListener { _, keyCode, event ->
                if (event.isGamepadKeyEvent) {
                    when (event.action) {
                        KeyEvent.ACTION_DOWN -> {
                            pressedButtons = pressedButtons + keyCode
                            true
                        }
                        KeyEvent.ACTION_UP -> {
                            pressedButtons = pressedButtons - keyCode
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            }

            val motionListener = View.OnGenericMotionListener { _, event ->
                if (event.isJoystickEvent &&
                    event.action == MotionEvent.ACTION_MOVE
                ) {
                    rightStickX = event.rightStickX
                    rightStickY = event.rightStickY

                    leftStickX = event.leftStickX
                    leftStickY = event.leftStickY

                    event.toJoystickTouchEvent(
                        joystickRadius = joystickRadius,
                        joystickCenter = leftJoystickCenter,
                        isRightJoystick = false
                    ).run {
                        leftTouchSimulator.simulateTouch(
                            touchX = x,
                            touchY = y,
                            downTime = downTime,
                            eventTime = eventTime,
                            isActive = true
                        )
                    }

                    event.toJoystickTouchEvent(
                        joystickRadius = joystickRadius,
                        joystickCenter = rightJoystickCenter,
                        isRightJoystick = true
                    ).run {
                        rightTouchSimulator.simulateTouch(
                            touchX = x,
                            touchY = y,
                            downTime = downTime,
                            eventTime = eventTime,
                            isActive = true
                        )
                    }

                    true
                } else {
                    false
                }
            }

            view.setOnKeyListener(keyListener)
            view.setOnGenericMotionListener(motionListener)

            onDispose {
                view.setOnKeyListener(null)
                view.setOnGenericMotionListener(null)
            }
        }
    }

    @Composable
    fun ConfigOverlay(onCloseRequest: () -> Unit) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val position = change.position
                        when {
                            isInsideCircle(position, leftJoystickCenter) -> {
                                leftJoystickCenter =
                                    position.coerceInBounds(canvasSize, joystickRadius)
                            }

                            isInsideCircle(position, rightJoystickCenter) -> {
                                rightJoystickCenter =
                                    position.coerceInBounds(canvasSize, joystickRadius)
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White,
                    center = leftJoystickCenter,
                    radius = joystickRadius,
                    style = Stroke(width = 4f)
                )
                drawCircle(
                    color = Color.White,
                    center = rightJoystickCenter,
                    radius = joystickRadius,
                    style = Stroke(width = 4f)
                )
            }

            Button(
                onClick = onCloseRequest,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Text("Close")
            }
        }
    }

    private fun isInsideCircle(point: Offset, center: Offset): Boolean {
        val distance = sqrt((point.x - center.x).pow(2) + (point.y - center.y).pow(2))
        return distance <= joystickRadius
    }

    // Helper function to ensure joystick center remains within bounds
    private fun Offset.coerceInBounds(bounds: Size, radius: Float): Offset {
        return Offset(
            x = x.coerceIn(radius, bounds.width - radius),
            y = y.coerceIn(radius, bounds.height - radius)
        )
    }

    @Composable
    fun DrawingCanvas(
        modifier: Modifier
    ) {
        val paths = remember { mutableStateListOf<Path>() }
        val currentPath = remember { mutableStateOf<Path?>(null) }

        Canvas(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            currentPath.value = Path().apply {
                                moveTo(offset.x, offset.y)
                            }
                        },
                        onDrag = { change, _ ->
                            currentPath.value?.lineTo(change.position.x, change.position.y)
                        },
                        onDragEnd = {
                            currentPath.value?.let {
                                paths.add(it)
                            }
                            currentPath.value = null
                        }
                    )
                }
        ) {
            paths.forEach { path ->
                drawPath(
                    path = path,
                    color = Color.Black,
                    style = Stroke(width = 5f)
                )
            }
            currentPath.value?.let { path ->
                drawPath(
                    path = path,
                    color = Color.Black,
                    style = Stroke(width = 5f)
                )
            }

            drawCircle(
                color = Color.Gray,
                center = leftJoystickCenter,
                radius = joystickRadius,
                style = Stroke(width = 2f)
            )
            drawCircle(
                color = Color.Gray,
                center = rightJoystickCenter,
                radius = joystickRadius,
                style = Stroke(width = 2f)
            )
        }

        Button(
            onClick = paths::clear,
            modifier = Modifier.padding(8.dp)
        ) {
            Text("Clear")
        }
    }
}
