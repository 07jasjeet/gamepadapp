package com.example.gamepadapp

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.gamepadapp.ui.theme.GamepadappTheme


class MainActivity : ComponentActivity() {
    private val touchCommunicator = Deps.touchCommunicator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GamepadappTheme {
                val touchEvent by touchCommunicator.touchFlow.collectAsState(null)
                val keyEvent by touchCommunicator.keyEventFlow.collectAsState(null)
                var pressedButtons by remember { mutableStateOf(setOf<String>()) }

                LaunchedEffect(key1 = keyEvent) {
                    keyEvent?.run {
                        when (action) {
                            KeyEvent.ACTION_DOWN -> {
                                pressedButtons = pressedButtons + KeyEvent.keyCodeToString(keyCode)
                            }
                            KeyEvent.ACTION_UP -> {
                                pressedButtons = pressedButtons - KeyEvent.keyCodeToString(keyCode)
                            }
                            else -> Unit
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithContent {
                                touchEvent?.let {
                                    drawMotionEventToCanvas(it)
                                }
                                drawContent()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column {
                            Text(
                                text = pressedButtons.toString()
                            )
                            Button(onClick = {
                                startService()
                            }) {
                                Text(text = "Start Service")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun startService() {
        if (!Settings.canDrawOverlays(this)) {
            val myIntent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            myIntent.setData(Uri.parse("package:$packageName"))
            startActivityForResult(myIntent, 1234)
        //} else if (hasForegroundServicePermission(this)) {
            //requestForegroundServicePermission(this)
        } else {
            val overlayServiceIntent = Intent(this, OverlayService::class.java)
            val gamepadInputService = Intent(this, GamepadInputService::class.java)
            val globalTouchServiceIntent = Intent(this, GlobalTouchService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                //startForegroundService(svc)
                startService(overlayServiceIntent)
            } else {
                startService(overlayServiceIntent)
            }

            startService(gamepadInputService)
            //startService(globalTouchServiceIntent)
            //startService()
            //finish()
        }
    }

    private fun showInputMethodSelector() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0)
        val imeManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager?
        if (imeManager != null) {
            imeManager.showInputMethodPicker()
        } else {
            Toast.makeText(this, "Error", Toast.LENGTH_LONG)
                .show()
        }
    }

    fun hasForegroundServicePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.FOREGROUND_SERVICE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Foreground service permission is not required before Android P
        }
    }

    private fun requestForegroundServicePermission(activity: ComponentActivity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(android.Manifest.permission.FOREGROUND_SERVICE),
                69
            )
        }
    }
}

fun DrawScope.drawMotionEventToCanvas(event: MotionEvent) {
    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            drawCircle(color = Color.Red, center = Offset(event.x, event.y), radius = 10f)
        }
        MotionEvent.ACTION_MOVE -> {
            val historySize = event.historySize
            val path = Path().apply {
                moveTo(size.center.x, size.center.y)
            }
            for (i in 0 until historySize) {
                path.lineTo(event.x * 10, event.y * 10)
            }

            drawPath(path, color = Color.Blue)
        }
    }
}
