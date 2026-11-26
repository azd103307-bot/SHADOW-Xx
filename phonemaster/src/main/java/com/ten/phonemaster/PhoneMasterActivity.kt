package com.ten.phonemaster

import android.app.Activity
import android.os.Bundle
import android.os.Build
import android.content.Intent
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import android.graphics.Typeface

class PhoneMasterActivity : Activity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = 0xFF101827.toInt()
        window.navigationBarColor = 0xFF101827.toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(24), dp(18), dp(20))
            setBackgroundColor(0xFFF3F6FB.toInt())
        }
        val scroll = ScrollView(this)
        val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        content.addView(TextView(this).apply {
            text = "TEN PHONE MASTER"
            textSize = 27f
            setTextColor(0xFF101827.toInt())
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        })
        content.addView(TextView(this).apply {
            text = "مركز أدوات وفحص لهاتفك"
            textSize = 16f
            setTextColor(0xFF475569.toInt())
            gravity = Gravity.CENTER
            setPadding(0, dp(6), 0, dp(18))
        })

        val info = """
            الشركة: ${Build.MANUFACTURER}
            الجهاز: ${Build.MODEL}
            الطراز: ${Build.DEVICE}
            إصدار Android: ${Build.VERSION.RELEASE}
            مستوى النظام: API ${Build.VERSION.SDK_INT}
        """.trimIndent()
        content.addView(TextView(this).apply {
            text = info
            textSize = 15f
            setTextColor(0xFF1E293B.toInt())
            setPadding(dp(14), dp(14), dp(14), dp(14))
            setBackgroundColor(0xFFFFFFFF.toInt())
        })

        addSection(content, "الشبكات والاتصال")
        addAction(content, "إعدادات شبكة الهاتف و4G", Settings.ACTION_NETWORK_OPERATOR_SETTINGS)
        addAction(content, "إعدادات نقاط الوصول APN", Settings.ACTION_APN_SETTINGS)
        addAction(content, "إعدادات Wi‑Fi", Settings.ACTION_WIFI_SETTINGS)
        addAction(content, "إعدادات Bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS)

        addSection(content, "الأداء والصيانة")
        addAction(content, "إدارة التطبيقات", Settings.ACTION_APPLICATION_SETTINGS)
        addAction(content, "معلومات التخزين", Settings.ACTION_INTERNAL_STORAGE_SETTINGS)
        addAction(content, "إعدادات البطارية", Settings.ACTION_BATTERY_SAVER_SETTINGS)
        addAction(content, "إعدادات الشاشة", Settings.ACTION_DISPLAY_SETTINGS)
        addAction(content, "إعدادات النظام", Settings.ACTION_SETTINGS)

        content.addView(TextView(this).apply {
            text = "ملاحظة: التطبيق لا يغيّر ترددات المودم أو ملفات النظام ولا يرفع قدرة المعالج فوق حدود الشركة. بعض صفحات الإعدادات قد تختلف حسب إصدار EMUI."
            textSize = 13f
            setTextColor(0xFF64748B.toInt())
            setPadding(dp(8), dp(16), dp(8), dp(8))
        })

        scroll.addView(content)
        root.addView(scroll)
        setContentView(root)
    }

    private fun addSection(parent: LinearLayout, title: String) {
        parent.addView(TextView(this).apply {
            text = title
            textSize = 19f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(0xFF0F766E.toInt())
            setPadding(dp(2), dp(22), dp(2), dp(8))
        })
    }

    private fun addAction(parent: LinearLayout, title: String, action: String) {
        val button = Button(this).apply {
            text = title
            isAllCaps = false
            textSize = 15f
            setOnClickListener {
                try {
                    startActivity(Intent(action))
                } catch (_: Exception) {
                    Toast.makeText(this@PhoneMasterActivity, "هذا الخيار غير متاح في إصدار النظام الحالي", Toast.LENGTH_LONG).show()
                }
            }
        }
        parent.addView(button, LinearLayout.LayoutParams(-1, dp(54)).apply {
            bottomMargin = dp(7)
        })
    }
}
