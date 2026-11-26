package com.example.keyblocker

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {

    private val bg = Color.parseColor("#0F1013")
    private val textMain = Color.parseColor("#ECECEF")
    private val onTrack = Color.parseColor("#6EE7B7")
    private val offTrack = Color.parseColor("#22252C")
    private val onThumb = Color.parseColor("#0F1013")
    private val offThumb = Color.parseColor("#6B6F7B")

    private val component =
        "com.example.keyblocker/com.example.keyblocker.KeyBlockerService"

    private lateinit var track: FrameLayout
    private lateinit var thumb: View
    private val trackShape = GradientDrawable()
    private val thumbShape = GradientDrawable()
    private lateinit var mainText: TextView

    private var travel = 0
    private var shownOn: Boolean? = null
    private var curTrack = offTrack
    private var curThumb = offThumb

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg

        val trackW = dp(248)
        val trackH = dp(100)
        val d = dp(76)
        val m = dp(12)
        travel = trackW - d - 2 * m

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.gravity = Gravity.CENTER
        root.setBackgroundColor(bg)
        root.setPadding(dp(28), dp(28), dp(28), dp(28))

        trackShape.cornerRadius = trackH / 2f
        trackShape.setColor(offTrack)
        thumbShape.shape = GradientDrawable.OVAL
        thumbShape.setColor(offThumb)

        track = FrameLayout(this)
        track.background = trackShape
        track.contentDescription = "Blocker switch"
        track.setOnClickListener { onToggle() }
        thumb = View(this)
        thumb.background = thumbShape
        val tlp = FrameLayout.LayoutParams(d, d, Gravity.CENTER_VERTICAL or Gravity.START)
        tlp.leftMargin = m
        track.addView(thumb, tlp)
        root.addView(track, LinearLayout.LayoutParams(trackW, trackH))

        mainText = TextView(this)
        mainText.setTextColor(textMain)
        mainText.textSize = 22f
        mainText.letterSpacing = 0.25f
        mainText.typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        mainText.gravity = Gravity.CENTER
        val mlp = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        mlp.topMargin = dp(44)
        root.addView(mainText, mlp)

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
        val animate = shownOn != null && shownOn != on
        applyState(on, animate)
        shownOn = on
    }

    private fun applyState(on: Boolean, animate: Boolean) {
        mainText.text = if (on) "BLOCKED" else "ACTIVE"

        val x = if (on) travel.toFloat() else 0f
        val toTrack = if (on) onTrack else offTrack
        val toThumb = if (on) onThumb else offThumb

        if (animate) {
            thumb.animate().translationX(x).setDuration(240).start()
            val fromT = curTrack
            val fromH = curThumb
            val a = ValueAnimator.ofFloat(0f, 1f)
            a.duration = 240
            a.addUpdateListener {
                val f = it.animatedValue as Float
                curTrack = ArgbEvaluator().evaluate(f, fromT, toTrack) as Int
                curThumb = ArgbEvaluator().evaluate(f, fromH, toThumb) as Int
                trackShape.setColor(curTrack)
                thumbShape.setColor(curThumb)
            }
            a.start()
        } else {
            thumb.translationX = x
            curTrack = toTrack
            curThumb = toThumb
            trackShape.setColor(curTrack)
            thumbShape.setColor(curThumb)
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
