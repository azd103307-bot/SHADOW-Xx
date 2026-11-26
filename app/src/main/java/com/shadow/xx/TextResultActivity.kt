package com.shadow.xx

import android.app.Activity
import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.Gravity
import android.widget.*

class TextResultActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent.getStringExtra("screen_text").orEmpty()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 24, 22, 20)
            setBackgroundColor(0xFF101827.toInt())
        }
        val title = TextView(this).apply {
            text = "النص المستخرج — حدّد وانسخ"
            textSize = 20f; setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER; setPadding(0, 0, 0, 16)
        }
        val edit = EditText(this).apply {
            setText(text); textSize = 16f; minLines = 8
            setTextIsSelectable(true); setSelectAllOnFocus(false)
            setPadding(12, 12, 12, 12)
        }
        val copy = Button(this).apply {
            text = "نسخ النص كاملاً"; isAllCaps = false
            setOnClickListener {
                val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("نص الشاشة", edit.text))
                Toast.makeText(this@TextResultActivity, "تم نسخ النص", Toast.LENGTH_SHORT).show()
            }
        }
        val select = Button(this).apply {
            text = "تحديد الكل"; isAllCaps = false
            setOnClickListener { edit.requestFocus(); edit.selectAll() }
        }
        root.addView(title)
        root.addView(edit, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(select)
        root.addView(copy)
        setContentView(root)
    }
}
