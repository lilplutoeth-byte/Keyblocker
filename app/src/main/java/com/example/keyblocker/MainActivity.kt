package com.example.keyblocker

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val bg = Color.parseColor("#0E0E10")
    private val card = Color.parseColor("#1A1A1F")
    private val accent = Color.parseColor("#00E5A0")
    private val off = Color.parseColor("#FF5C5C")
    private val dim = Color.parseColor("#8A8A93")

    private val component =
        "com.example.keyblocker/com.example.keyblocker.KeyBlockerService"

    private lateinit var statusDot: TextView
    private lateinit var statusText: TextView
    private lateinit var toggleBtn: Button
    private lateinit var hint: TextView

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun round(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER_HORIZONTAL
        root.setBackgroundColor(bg)
        root.setPadding(dp(24), dp(72), dp(24), dp(24))

        val title = TextView(this)
        title.text = "bar"
        title.setTextColor(Color.WHITE)
        title.textSize = 28f
        title.setTypeface(null, Typeface.BOLD)

        val sub = TextView(this)
        sub.text = "Ignores the Recent key"
        sub.setTextColor(dim)
        sub.textSize = 14f
        sub.setPadding(0, dp(4), 0, dp(32))

        val cardView = LinearLayout(this)
        cardView.orientation = LinearLayout.VERTICAL
        cardView.gravity = Gravity.CENTER_HORIZONTAL
        cardView.background = round(card, 20)
        cardView.setPadding(dp(24), dp(32), dp(24), dp(28))

        statusDot = TextView(this)
        statusDot.text = "●"
        statusDot.textSize = 40f
        statusDot.gravity = Gravity.CENTER

        statusText = TextView(this)
        statusText.textSize = 22f
        statusText.setTypeface(null, Typeface.BOLD)
        statusText.gravity = Gravity.CENTER
        statusText.setPadding(0, dp(8), 0, dp(24))

        toggleBtn = Button(this)
        toggleBtn.setTextColor(bg)
        toggleBtn.textSize = 16f
        toggleBtn.setTypeface(null, Typeface.BOLD)
        toggleBtn.isAllCaps = false
        toggleBtn.setOnClickListener { onToggle() }

        cardView.addView(statusDot)
        cardView.addView(statusText)
        cardView.addView(
            toggleBtn,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(56))
        )

        hint = TextView(this)
        hint.setTextColor(dim)
        hint.textSize = 13f
        hint.setPadding(dp(4), dp(24), dp(4), 0)

        root.addView(title)
        root.addView(sub)
        root.addView(cardView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(hint)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun isServiceOn(): Boolean {
        val s = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return s.split(":").any { it.equals(component, ignoreCase = true) }
    }

    private fun canToggleDirectly(): Boolean =
        checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") ==
            PackageManager.PERMISSION_GRANTED

    private fun refresh() {
        val on = isServiceOn()
        statusDot.setTextColor(if (on) accent else off)
        statusText.text = if (on) "Blocker ON" else "Blocker OFF"
        statusText.setTextColor(Color.WHITE)
        toggleBtn.text = if (on) "Turn OFF" else "Turn ON"
        toggleBtn.background = round(if (on) off else accent, 14)
        hint.text = if (canToggleDirectly()) {
            "Tip: turn it OFF before opening your bank app, then ON again."
        } else {
            "The button opens Accessibility settings. For a true one-tap toggle, " +
                "grant the extra permission from a PC:\n\n" +
                "adb shell pm grant com.example.keyblocker android.permission.WRITE_SECURE_SETTINGS"
        }
    }

    private fun onToggle() {
        val turnOn = !isServiceOn()
        if (canToggleDirectly()) {
            if (setServiceEnabled(turnOn)) {
                refresh()
                return
            }
            Toast.makeText(this, "Could not change it, opening settings", Toast.LENGTH_SHORT).show()
        }
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun setServiceEnabled(on: Boolean): Boolean {
        return try {
            val current = Settings.Secure.getString(
                contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            val list = current.split(":")
                .filter { it.isNotBlank() && !it.equals(component, ignoreCase = true) }
                .toMutableList()
            if (on) list.add(component)
            Settings.Secure.putString(
                contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                list.joinToString(":")
            )
            Settings.Secure.putString(
                contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED,
                if (list.isEmpty()) "0" else "1"
            )
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
