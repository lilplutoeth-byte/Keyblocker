package com.example.keyblocker

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class KeyBlockerService : AccessibilityService() {

    // Set to true to ALSO block the Back key (keycode 4).
    private val blockBack = false

    override fun onKeyEvent(event: KeyEvent): Boolean {
        Log.d("KeyBlocker", "key=${event.keyCode} action=${event.action}")
        return when (event.keyCode) {
            KeyEvent.KEYCODE_APP_SWITCH -> true   // swallow Recent key
            KeyEvent.KEYCODE_BACK -> blockBack
            else -> false
        }
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
