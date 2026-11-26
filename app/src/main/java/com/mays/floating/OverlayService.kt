package com.mays.floating

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var panel: LinearLayout? = null

    override fun onCreate() {
        super.onCreate()
        val channelId = "mays_overlay"
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, "Mays floating bar", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = if (Build.VERSION.SDK_INT >= 26)
            Notification.Builder(this, channelId).setContentTitle("ميس يعمل").setContentText("الشريط العائم نشط")
                .setSmallIcon(android.R.drawable.ic_menu_view).build()
        else Notification.Builder(this).setContentTitle("ميس يعمل").setSmallIcon(android.R.drawable.ic_menu_view).build()
        startForeground(7, notification)
        if (android.provider.Settings.canDrawOverlays(this)) showPanel()
    }

    private fun showPanel() {
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 8)
            setBackgroundColor(0xDD202040.toInt())
        }
        fun add(label: String, action: () -> Unit) {
            box.addView(Button(this).apply { text = label; setOnClickListener { action() } })
        }
        add("رجوع") { MaysAccessibilityService.instance?.back() }
        add("الرئيسية") {
            val service = MaysAccessibilityService.instance
            if (service != null) service.home() else {
                val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(i)
            }
        }
        add("المفتوحة") { MaysAccessibilityService.instance?.recents() }
        add("قفل") { MaysAccessibilityService.instance?.lock() }
        add("١") { launchSaved(1) }
        add("٢") { launchSaved(2) }
        add("×") { stopSelf() }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply { gravity = Gravity.END or Gravity.CENTER_VERTICAL; x = 8 }
        panel = box
        wm.addView(box, params)
    }

    private fun launchSaved(slot: Int) {
        val pkg = getSharedPreferences("mays", MODE_PRIVATE).getString("app$slot", null) ?: return
        val intent = packageManager.getLaunchIntentForPackage(pkg)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent != null) startActivity(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY
    override fun onDestroy() {
        panel?.let { if (::wm.isInitialized) wm.removeView(it) }
        panel = null
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null
}
