package com.example.gamepadapp

import android.inputmethodservice.InputMethodService
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import com.example.gamepadapp.Util.isGamepadKeyEvent
import com.example.gamepadapp.Util.isGamepadMotionEvent
import com.example.gamepadapp.Util.isJoystickEvent
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch


class GamepadInputService : InputMethodService() {
    private val scope = MainScope()
    private val touchCommunicator = Deps.touchCommunicator

    override fun onCreate() {
        super.onCreate()
        Log.d("test", "GamepadInputService.onCreate")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        Log.d("test", "onKeyDown: " + event.toString())
        if (event.isJoystickEvent || event.isGamepadKeyEvent) {
            // send Key event
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        Log.d("test", "onKeyUp: " + event.toString())
        if (event.isJoystickEvent || event.isGamepadKeyEvent) {
            // send key event
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        Log.d("test", event.toString())
        if (event.isJoystickEvent || event.isGamepadMotionEvent) {
            scope.launch {
                touchCommunicator.touchFlow.emit(event)
            }
            return true
        }

        return super.onGenericMotionEvent(event)
    }
}