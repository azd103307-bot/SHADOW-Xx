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
        val screenText = intent.getStringExtra("screen_text").orEmpty()
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 24, 22, 20)
            setBackgroundColor(0xFF101827.toInt())
        }
        val titleView = TextView(this).apply {
            this.text = "النص المستخرج — حدّد وانسخ"
            textSize = 20f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 16)
        }
        val edit = EditText(this).apply {
            setText(screenText)
            textSize = 16f
            minLines = 8
            setTextIsSelectable(true)
            setSelectAllOnFocus(false)
            setPadding(12, 12, 12, 12)
        }
        val copyButton = Button(this).apply {
            this.text = "نسخ النص كاملاً"
            isAllCaps = false
            setOnClickListener {
                val cb = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cb.setPrimaryClip(ClipData.newPlainText("نص الشاشة", edit.text))
                Toast.makeText(this@TextResultActivity, "تم نسخ النص", Toast.LENGTH_SHORT).show()
            }
        }
        val selectButton = Button(this).apply {
            this.text = "تحديد الكل"
            isAllCaps = false
            setOnClickListener { edit.requestFocus(); edit.selectAll() }
        }
        root.addView(titleView)
        root.addView(edit, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(selectButton)
        root.addView(copyButton)
        setContentView(root)
    }
}
