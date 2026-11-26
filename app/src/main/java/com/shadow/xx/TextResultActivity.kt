package com.shadow.xx

import android.app.Activity
import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.Gravity
import android.widget.*

class TextResultActivity : Activity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val screenText = intent.getStringExtra("screen_text").orEmpty()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(24), dp(22), dp(20))
            setBackgroundColor(0xFF101827.toInt())
        }
        val titleView = TextView(this).apply {
            text = "النص المستخرج — حدّد وانسخ"; textSize = 21f
            setTextColor(0xFFFFFFFF.toInt()); gravity = Gravity.CENTER; setPadding(0, 0, 0, dp(16))
        }
        val edit = EditText(this).apply {
            setText(screenText); textSize = 17f; minLines = 8; setTextIsSelectable(true); setSelectAllOnFocus(false)
            setTextColor(0xFFFFFFFF.toInt()); setHintTextColor(0xFFCBD5E1.toInt())
            setHint("النص المستخرج يظهر هنا"); setBackgroundColor(0xFF243247.toInt())
            setPadding(dp(14), dp(14), dp(14), dp(14))
        }
        val selectButton = Button(this).apply {
            text = "تحديد الكل"; isAllCaps = false; textSize = 17f; minHeight = dp(58)
            setOnClickListener { edit.requestFocus(); edit.selectAll() }
        }
        val copyButton = Button(this).apply {
            text = "نسخ النص كاملاً"; isAllCaps = false; textSize = 17f; minHeight = dp(58)
            setOnClickListener {
                val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("نص الشاشة", edit.text))
                Toast.makeText(this@TextResultActivity, "تم نسخ النص", Toast.LENGTH_SHORT).show()
            }
        }
        root.addView(titleView); root.addView(edit, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(selectButton); root.addView(copyButton); setContentView(root)
    }
}