package com.example.gamepadapp

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class OverlayService(
    private val touchCommunicator: TouchCommunicator = Deps.touchCommunicator
) : Service(), GamepadScreen {

    private val scope = MainScope()

    override lateinit var rightTouchSimulator: TouchEventSimulator
    override lateinit var leftTouchSimulator: TouchEventSimulator
    override var canvasPosition: IntArray = IntArray(2)
    override var canvasSize: Size = Size(0f, 0f)

    override val joystickRadius: Float = 150f
    override var leftJoystickCenter: Offset by mutableStateOf(Offset(250f, 250f))
    override var rightJoystickCenter: Offset by mutableStateOf(Offset(750f, 750f))
    override var isConfigMode: Boolean by mutableStateOf(false)
    private var expanded by mutableStateOf(false)

    private val lifecycleOwner: MockLifecycleOwner = MockLifecycleOwner()
    private val viewModelStoreOwner: MockViewModelStoreOwner = MockViewModelStoreOwner()

    override fun openApp() {
        val launchIntent = packageManager.getLaunchIntentForPackage("com.raed.drawing")
        if (launchIntent != null) {
            startActivity(launchIntent)
        } else {
            Toast.makeText(this, "Drawing app not installed", Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var windowManager: WindowManager
    private lateinit var overlayView: View

    override fun onBind(intent: Intent?): IBinder? = null

    @SuppressLint("InflateParams")
    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Set the layout parameters
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        lifecycleOwner.apply {
            performRestore(null)
            handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            handleLifecycleEvent(Lifecycle.Event.ON_START)
            handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        // Inflate your overlay layout
        overlayView = ComposeView(this).apply {
            setViewTreeSavedStateRegistryOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(viewModelStoreOwner)
            setViewTreeLifecycleOwner(lifecycleOwner)
            setContent {
                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedVisibility(visible = expanded) {
                        GamepadButtonDisplay()
                    }

                    Row {
                        Button(
                            modifier = Modifier.padding(16.dp),
                            onClick = { expanded = !expanded }) {
                            Text(text = "Toggle")
                        }

                        Button(
                            modifier = Modifier
                                .padding(16.dp),
                            onClick = { stopSelf() }) {
                            Text(text = "Stop")
                        }
                    }
                }
            }

            setOnTouchListener { v, event ->

                //Log.d("test", "onTouchListener called: $event")
                runBlocking {
                    touchCommunicator.touchFlow.value = event
                }

                v.performClick()
            }
        }

        rightTouchSimulator = TouchEventSimulator(overlayView) {
            scope.launch {
                touchCommunicator.rightTouchFlow.emit(it)
            }
        }

        leftTouchSimulator = TouchEventSimulator(overlayView) {
            scope.launch {
                touchCommunicator.leftTouchFlow.emit(it)
            }
        }

        // Add the view to the window
        windowManager.addView(overlayView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::windowManager.isInitialized && ::overlayView.isInitialized) {
            lifecycleOwner.run {
                handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
                handleLifecycleEvent(Lifecycle.Event.ON_STOP)
                handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                performSave(Bundle())
            }
            viewModelStoreOwner.viewModelStore.clear()

            windowManager.removeView(overlayView)
        }
    }
}
