package com.shadow.xx

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.Intent
import android.view.Gravity
import android.widget.*
import android.graphics.Typeface

class MainActivity : Activity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24), dp(28), dp(24), dp(24))
            setBackgroundColor(0xFF101827.toInt())
        }
        root.addView(TextView(this).apply {
            text = "SHADOW BUILD"
            textSize = 32f
            setTextColor(0xFFFFFFFF.toInt())
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(10))
        })
        root.addView(TextView(this).apply {
            text = "ورشة عربية حرة للبناء والتشييد والتصميم ثلاثي الأبعاد"
            textSize = 17f
            setTextColor(0xFFCBD5E1.toInt())
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(28))
        })
        root.addView(Button(this).apply {
            text = "ابدأ البناء والتصميم"
            isAllCaps = false
            textSize = 19f
            minHeight = dp(64)
            setOnClickListener { startActivity(Intent(this@MainActivity, ConstructionActivity::class.java)) }
        }, LinearLayout.LayoutParams(-1, dp(68)))
        root.addView(Button(this).apply {
            text = "عن ورشة البناء"
            isAllCaps = false
            textSize = 16f
            setOnClickListener {
                AlertDialog.Builder(this@MainActivity)
                    .setTitle("SHADOW BUILD")
                    .setMessage("أنت المصمم والمسؤول عن العمل. أضف قطع البناء والخشب والخرسانة بنفسك، قِس الكميات، واكتب شرح مشروعك واحفظه أو صدّره.")
                    .setPositiveButton("حسناً", null)
                    .show()
            }
        }, LinearLayout.LayoutParams(-1, dp(58)).apply { topMargin = dp(12) })
        setContentView(root)
    }
}
