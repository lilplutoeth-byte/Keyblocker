package com.example.keyblocker

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent

class KeyBlockerService : AccessibilityService() {

    // Set to true to ALSO block the Back key (keycode 4).
    private val blockBack = false

    // Two Volume Down presses within this time (milliseconds) open Recent apps.
    // Make it bigger if it is hard to trigger, smaller if it fires by accident.
    private val doubleClickWindowMs = 400L

    private var lastVolDownTime = 0L
    private var swallowingThisPress = false

    override fun onKeyEvent(event: KeyEvent): Boolean {
        Log.d("KeyBlocker", "key=${event.keyCode} action=${event.action}")
        return when (event.keyCode) {
            KeyEvent.KEYCODE_APP_SWITCH -> true   // swallow the (ghost touch) Recent key
            KeyEvent.KEYCODE_BACK -> blockBack
            KeyEvent.KEYCODE_VOLUME_DOWN -> handleVolumeDown(event)
            else -> false
        }
    }

    private fun handleVolumeDown(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            // Holding the key down (auto-repeat) is not a double click.
            if (event.repeatCount > 0) {
                lastVolDownTime = 0L
                return swallowingThisPress
            }

            val now = event.eventTime
            return if (lastVolDownTime != 0L && now - lastVolDownTime <= doubleClickWindowMs) {
                // Second quick press: open Recent apps and hide this press from the system
                lastVolDownTime = 0L
                swallowingThisPress = true
                performGlobalAction(GLOBAL_ACTION_RECENTS)
                true
            } else {
                // First press: let it through so normal volume control still works
                lastVolDownTime = now
                swallowingThisPress = false
                false
            }
        }

        // Key released: swallow the release too if we swallowed the press
        val swallow = swallowingThisPress
        swallowingThisPress = false
        return swallow
    }

    override fun onAccessibilityEvent(e: AccessibilityEvent?) {}
    override fun onInterrupt() {}
}
