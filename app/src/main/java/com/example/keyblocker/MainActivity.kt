package com.example.keyblocker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(48, 48, 48, 48)

        val info = TextView(this)
        info.text = "Tap the button, find \"Key Blocker\" in the list, and switch it ON.\nThe Recent key will then be ignored."
        info.textSize = 18f

        val openBtn = Button(this)
        openBtn.text = "Open Accessibility settings"
        openBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        layout.addView(info)
        layout.addView(openBtn)
        setContentView(layout)
    }
}
