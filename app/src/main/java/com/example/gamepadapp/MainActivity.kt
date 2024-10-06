package com.example.gamepadapp

import android.os.Bundle
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.gamepadapp.ui.theme.GamepadappTheme
import kotlin.math.pow
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {

    private lateinit var touchEventSimulator: TouchEventSimulator
    private lateinit var leftTouchSimulator: TouchEventSimulator
    private var canvasPosition = IntArray(2)
    private var canvasSize = Size(0f, 0f)

    private val joystickRadius = 150f
    private var leftJoystickCenter by mutableStateOf(Offset(250f, 250f))
    private var rightJoystickCenter by mutableStateOf(Offset(750f, 750f))
    private var isConfigMode by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GamepadappTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GamepadButtonDisplay()
                }
            }
        }

        val view = window.decorView
        touchEventSimulator = TouchEventSimulator(view)
        leftTouchSimulator = TouchEventSimulator(view)
    }

    private fun openApp() {
        val launchIntent = packageManager.getLaunchIntentForPackage("com.raed.drawing")
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "Drawing app not installed", Toast.LENGTH_SHORT).show()
        }
    }

    @Composable
    fun GamepadButtonDisplay() {
        var pressedButtons by remember { mutableStateOf(setOf<Int>()) }
        var leftStickX by remember { mutableFloatStateOf(0f) }
        var leftStickY by remember { mutableFloatStateOf(0f) }
        var rightStickX by remember { mutableFloatStateOf(0f) }
        var rightStickY by remember { mutableFloatStateOf(0f) }
        val view = LocalView.current

        Box(modifier = Modifier.fillMaxSize()) {
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
                        .background(Color.White)
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
                if (event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) {
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
                if (event.source and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK &&
                    event.action == MotionEvent.ACTION_MOVE
                ) {
                    rightStickX = event.getAxisValue(MotionEvent.AXIS_Z)
                    rightStickY = event.getAxisValue(MotionEvent.AXIS_RZ)

                    leftStickX = event.getAxisValue(MotionEvent.AXIS_X)
                    leftStickY = event.getAxisValue(MotionEvent.AXIS_Y)

                    val deadzone = 0.1f
                    if (kotlin.math.abs(rightStickX) > deadzone || kotlin.math.abs(rightStickY) > deadzone) {
                        val touchX = canvasPosition[0] + rightJoystickCenter.x + rightStickX * joystickRadius
                        val touchY = canvasPosition[1] + rightJoystickCenter.y - rightStickY * joystickRadius
                        touchEventSimulator.simulateTouch(touchX, touchY, true)
                    } else {
                        touchEventSimulator.simulateTouch(touchX = 0f, touchY = 0f, isActive = false)
                    }

                    if (kotlin.math.abs(leftStickX) > deadzone || kotlin.math.abs(leftStickY) > deadzone) {
                        val touchX = canvasPosition[0] + leftJoystickCenter.x + leftStickX * joystickRadius
                        val touchY = canvasPosition[1] + leftJoystickCenter.y - leftStickY * joystickRadius
                        leftTouchSimulator.simulateTouch(touchX, touchY, true)
                    } else {
                        leftTouchSimulator.simulateTouch(touchX = 0f, touchY = 0f, isActive = false)
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
                                leftJoystickCenter = position.coerceInBounds(canvasSize, joystickRadius)
                            }
                            isInsideCircle(position, rightJoystickCenter) -> {
                                rightJoystickCenter = position.coerceInBounds(canvasSize, joystickRadius)
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
    fun DrawingCanvas(modifier: Modifier = Modifier) {
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
    }
}
