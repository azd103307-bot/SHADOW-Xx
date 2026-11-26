package com.shadow.xx

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.content.Intent
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import android.graphics.Typeface

class MainActivity : Activity() {
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(22), dp(28), dp(22), dp(22))
            setBackgroundColor(0xFF101827.toInt())
        }
        val title = TextView(this).apply {
            text = "SHADOW Xx"; textSize = 30f
            setTextColor(0xFFFFFFFF.toInt()); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setPadding(0, dp(12), 0, dp(8))
        }
        val sub = TextView(this).apply {
            text = "شريط أدوات عائم للتحكم السريع ونسخ نص الشاشة"
            textSize = 16f; setTextColor(0xFFCBD5E1.toInt()); gravity = Gravity.CENTER
            setPadding(0, 0, 0, dp(24))
        }
        root.addView(title); root.addView(sub)
        root.addView(button("تفعيل الشريط العائم") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            Toast.makeText(this, "فعّل خدمة SHADOW Xx من خدمات إمكانية الوصول", Toast.LENGTH_LONG).show()
        }, lp())
        root.addView(button("افتح ورشة البناء ثلاثية الأبعاد") { startActivity(Intent(this, ConstructionActivity::class.java)) }, lp())
        root.addView(button("اختيار التطبيق الأول") { chooseApp(1) }, lp())
        root.addView(button("اختيار التطبيق الثاني") { chooseApp(2) }, lp())
        val hint = TextView(this).apply {
            text = "بعد تفعيل الخدمة سيظهر الشريط فوق التطبيقات. زر «نص» يجمع النصوص التي تسمح واجهة التطبيق بقراءتها، ثم يتيح تحديدها ونسخها. قد لا تعمل القراءة داخل الشاشات المحمية أو النصوص المرسومة داخل الصور."
            textSize = 15f; setTextColor(0xFFE2E8F0.toInt()); setPadding(0, dp(20), 0, 0)
        }
        root.addView(hint)
        setContentView(root)
    }
    private fun lp() = LinearLayout.LayoutParams(-1, dp(62)).apply { bottomMargin = dp(14) }
    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 17f; minHeight = dp(58)
        setOnClickListener { action() }
    }
    private fun chooseApp(slot: Int) {
        val pm = packageManager
        val apps = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .sortedBy { it.loadLabel(pm).toString().lowercase() }
        if (apps.isEmpty()) { Toast.makeText(this, "لم يتم العثور على تطبيقات قابلة للتشغيل", Toast.LENGTH_SHORT).show(); return }
        val labels = apps.map { it.loadLabel(pm).toString() }.toTypedArray()
        AlertDialog.Builder(this).setTitle(if (slot == 1) "اختر التطبيق الأول" else "اختر التطبيق الثاني")
            .setItems(labels) { _, which ->
                getSharedPreferences("shadow", MODE_PRIVATE).edit().putString("app$slot", apps[which].activityInfo.packageName).apply()
                Toast.makeText(this, "تم حفظ التطبيق", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("إلغاء", null).show()
    }
}