package com.mays.floating

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 32, 28, 32)
        }
        root.addView(TextView(this).apply {
            text = "ميس Mays\nالشريط العائم لاختصاراتك"
            textSize = 24f
            gravity = Gravity.CENTER
        })
        root.addView(Button(this).apply {
            text = "اختيار التطبيق الأول"
            setOnClickListener { chooseApp(1) }
        })
        root.addView(Button(this).apply {
            text = "اختيار التطبيق الثاني"
            setOnClickListener { chooseApp(2) }
        })
        root.addView(Button(this).apply {
            text = "تفعيل الشريط العائم"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")))
                } else {
                    startForegroundService(Intent(this@MainActivity, OverlayService::class.java))
                    Toast.makeText(this, "تم تشغيل شريط ميس", Toast.LENGTH_SHORT).show()
                }
            }
        })
        setContentView(root)
    }

    private fun chooseApp(slot: Int) {
        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
            .filter { pm.getLaunchIntentForPackage(it.packageName) != null && it.packageName != packageName }
            .sortedBy { pm.getApplicationLabel(it).toString().lowercase() }
        val labels = apps.map { pm.getApplicationLabel(it).toString() }.toTypedArray()
        android.app.AlertDialog.Builder(this)
            .setTitle(if (slot == 1) "اختر التطبيق الأول" else "اختر التطبيق الثاني")
            .setItems(labels) { _, which ->
                getSharedPreferences("mays", MODE_PRIVATE).edit()
                    .putString("app$slot", apps[which].packageName).apply()
                Toast.makeText(this, "تم حفظ الاختيار", Toast.LENGTH_SHORT).show()
            }.setNegativeButton("إلغاء", null).show()
    }
}
