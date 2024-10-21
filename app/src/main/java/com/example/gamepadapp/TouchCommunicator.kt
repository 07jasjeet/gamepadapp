package com.example.gamepadapp

import android.view.MotionEvent
import kotlinx.coroutines.flow.MutableStateFlow

class TouchCommunicator {
    val leftTouchFlow = MutableStateFlow<MotionEvent?>(null)
    val rightTouchFlow = MutableStateFlow<MotionEvent?>(null)

    val touchFlow = MutableStateFlow<MotionEvent?>(null)
}