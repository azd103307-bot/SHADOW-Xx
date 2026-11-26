package com.shadow.xx

import android.accessibilityservice.AccessibilityService
import android.content.*
import android.graphics.PixelFormat
import android.os.Build
import android.view.*
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.*
import android.graphics.Color
import android.graphics.Typeface

class ShadowAccessibilityService : AccessibilityService() {
    private var wm: WindowManager? = null
    private var panel: View? = null
    private var params: WindowManager.LayoutParams? = null
    private var expanded = true
    private var controlsRow: View? = null
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    override fun onServiceConnected() { super.onServiceConnected(); wm = getSystemService(WINDOW_SERVICE) as WindowManager; showPanel() }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { hidePanel() }
    override fun onDestroy() { hidePanel(); super.onDestroy() }
    private fun showPanel() {
        if (panel != null) return
        val bar = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(7), dp(7), dp(7), dp(7)); setBackgroundColor(0xF0182438.toInt()) }
        val grip = TextView(this).apply {
            text = "SHADOW  ⋮"; textSize = 14f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD; setPadding(dp(8), dp(9), dp(8), dp(9))
            setOnTouchListener(DragTouch())
            setOnClickListener { expanded = !expanded; controlsRow?.visibility = if (expanded) View.VISIBLE else View.GONE; text = if (expanded) "SHADOW  ⋮" else "SHADOW  +" }
        }
        bar.addView(grip)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }; controlsRow = row
        fun add(label: String, click: () -> Unit) {
            val b = TextView(this).apply {
                text = label; textSize = 14f; gravity = Gravity.CENTER; setTextColor(Color.WHITE)
                typeface = Typeface.DEFAULT_BOLD; setPadding(dp(11), dp(15), dp(11), dp(15))
                setBackgroundColor(0xFF334F70.toInt()); minWidth = dp(42); minHeight = dp(50)
                setOnClickListener { click() }
            }
            row.addView(b, LinearLayout.LayoutParams(-2, -2).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) })
        }
        add("⌂") { performGlobalAction(GLOBAL_ACTION_HOME) }
        add("رجوع") { performGlobalAction(GLOBAL_ACTION_BACK) }
        add("قفل") { if (Build.VERSION.SDK_INT >= 28) performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN) else Toast.makeText(this, "قفل الشاشة يحتاج Android 9 أو أحدث", Toast.LENGTH_SHORT).show() }
        add("نص") { extractVisibleText() }
        add("١") { launchSaved(1) }; add("٢") { launchSaved(2) }
        add("−") { expanded = false; row.visibility = View.GONE; grip.text = "SHADOW  +" }
        bar.addView(row); panel = bar
        params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = dp(12); y = dp(180) }
        try { wm?.addView(bar, params) } catch (_: Exception) { panel = null }
    }
    private fun hidePanel() { try { panel?.let { wm?.removeView(it) } } catch (_: Exception) {}; panel = null }
    private fun launchSaved(slot: Int) {
        val pkg = getSharedPreferences("shadow", MODE_PRIVATE).getString("app$slot", null)
        if (pkg == null) { Toast.makeText(this, "اختر التطبيق $slot من واجهة SHADOW أولاً", Toast.LENGTH_SHORT).show(); return }
        val i = packageManager.getLaunchIntentForPackage(pkg)
        if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i) } else Toast.makeText(this, "التطبيق غير متاح", Toast.LENGTH_SHORT).show()
    }
    private fun extractVisibleText() {
        val root = rootInActiveWindow
        if (root == null) { Toast.makeText(this, "تعذر قراءة نص هذه الشاشة", Toast.LENGTH_SHORT).show(); return }
        val out = StringBuilder(); collect(root, out); root.recycle()
        val value = out.toString().trim()
        if (value.isBlank()) { Toast.makeText(this, "لم أجد نصاً قابلاً للقراءة. النص داخل الصور يحتاج OCR غير مضاف بعد.", Toast.LENGTH_LONG).show(); return }
        startActivity(Intent(this, TextResultActivity::class.java).putExtra("screen_text", value).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    private fun collect(node: AccessibilityNodeInfo, out: StringBuilder) {
        val value = node.text?.toString()?.trim()
        if (!value.isNullOrEmpty() && out.indexOf(value) < 0) out.append(value).append('\n')
        for (i in 0 until node.childCount) { val child = node.getChild(i) ?: continue; collect(child, out); child.recycle() }
    }
    private inner class DragTouch : View.OnTouchListener {
        private var sx = 0f; private var sy = 0f; private var ox = 0; private var oy = 0
        override fun onTouch(v: View, e: MotionEvent): Boolean {
            val p = params ?: return false
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = p.x; oy = p.y; return true }
                MotionEvent.ACTION_MOVE -> { p.x = ox + (e.rawX - sx).toInt(); p.y = oy + (e.rawY - sy).toInt(); try { wm?.updateViewLayout(panel, p) } catch (_: Exception) {}; return true }
                MotionEvent.ACTION_UP -> { v.performClick(); return true }
            }; return true
        }
    }
}