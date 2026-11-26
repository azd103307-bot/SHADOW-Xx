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

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        showPanel()
    }
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() { hidePanel() }
    override fun onDestroy() { hidePanel(); super.onDestroy() }

    private fun showPanel() {
        if (panel != null) return
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(7, 7, 7, 7)
            setBackgroundColor(0xEE172033.toInt())
        }
        val grip = TextView(this).apply {
            text = "SHADOW  ⋮"; textSize = 12f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); setPadding(4, 6, 4, 6)
            setOnTouchListener(DragTouch())
        }
        bar.addView(grip)
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER }
        fun add(label: String, click: () -> Unit) {
            val b = TextView(this).apply {
                text = label; textSize = 12f; gravity = Gravity.CENTER
                setTextColor(Color.WHITE); setPadding(9, 12, 9, 12)
                setBackgroundColor(0xFF334155.toInt())
                setOnClickListener { click() }
            }
            val p = LinearLayout.LayoutParams(-2, -2); p.setMargins(3, 2, 3, 2); row.addView(b, p)
        }
        add("⌂") { performGlobalAction(GLOBAL_ACTION_HOME) }
        add("رجوع") { performGlobalAction(GLOBAL_ACTION_BACK) }
        add("قفل") {
            if (Build.VERSION.SDK_INT >= 28) performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            else Toast.makeText(this, "قفل الشاشة يحتاج Android 9 أو أحدث", Toast.LENGTH_SHORT).show()
        }
        add("نص") { extractVisibleText() }
        add("١") { launchSaved(1) }
        add("٢") { launchSaved(2) }
        add("×") { hidePanel() }
        bar.addView(row)
        panel = bar
        val type = if (Build.VERSION.SDK_INT >= 22) WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE
        params = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 12; y = 180 }
        try { wm?.addView(bar, params) } catch (_: Exception) { panel = null }
    }
    private fun hidePanel() {
        try { panel?.let { wm?.removeView(it) } } catch (_: Exception) {}
        panel = null
    }
    private fun launchSaved(slot: Int) {
        val pkg = getSharedPreferences("shadow", MODE_PRIVATE).getString("app$slot", null)
        if (pkg == null) { Toast.makeText(this, "اختر التطبيق $slot من واجهة SHADOW أولاً", Toast.LENGTH_SHORT).show(); return }
        val i = packageManager.getLaunchIntentForPackage(pkg)
        if (i != null) { i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); startActivity(i) }
        else Toast.makeText(this, "التطبيق غير متاح", Toast.LENGTH_SHORT).show()
    }
    private fun extractVisibleText() {
        val root = rootInActiveWindow
        if (root == null) { Toast.makeText(this, "تعذر قراءة نص هذه الشاشة", Toast.LENGTH_SHORT).show(); return }
        val out = StringBuilder()
        collect(root, out)
        root.recycle()
        val value = out.toString().trim()
        if (value.isBlank()) {
            Toast.makeText(this, "لم أجد نصاً قابلاً للقراءة في هذه الشاشة", Toast.LENGTH_LONG).show()
            return
        }
        val input = EditText(this).apply {
            setText(value); setTextIsSelectable(true); textSize = 16f; minLines = 5
        }
        AlertDialog.Builder(this).setTitle("النص المستخرج — حدّد وانسخ")
            .setView(input)
            .setPositiveButton("نسخ الكل") { _, _ ->
                val clip = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                clip.setPrimaryClip(ClipData.newPlainText("نص الشاشة", input.text))
                Toast.makeText(this, "تم نسخ النص", Toast.LENGTH_SHORT).show()
            }.setNeutralButton("تحديد الكل") { _, _ ->
                input.requestFocus(); input.selectAll()
            }.setNegativeButton("إغلاق", null).show()
    }
    private fun collect(node: AccessibilityNodeInfo, out: StringBuilder) {
        val text = node.text?.toString()?.trim()
        if (!text.isNullOrEmpty() && out.indexOf(text) < 0) out.append(text).append('\n')
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            collect(child, out); child.recycle()
        }
    }
    private inner class DragTouch : View.OnTouchListener {
        private var sx = 0f; private var sy = 0f; private var ox = 0; private var oy = 0
        override fun onTouch(v: View, e: MotionEvent): Boolean {
            val p = params ?: return false
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = p.x; oy = p.y; return true }
                MotionEvent.ACTION_MOVE -> { p.x = ox + (e.rawX - sx).toInt(); p.y = oy + (e.rawY - sy).toInt(); try { wm?.updateViewLayout(panel, p) } catch (_: Exception) {}; return true }
            }
            return true
        }
    }
}
