package com.example.gamepadapp

import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class MockViewModelStoreOwner(
    override val viewModelStore: ViewModelStore = ViewModelStore()
) : ViewModelStoreOwner