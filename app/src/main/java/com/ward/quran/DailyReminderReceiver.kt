package com.ward.quran
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.os.Build
import androidx.core.app.NotificationCompat
class DailyReminderReceiver : BroadcastReceiver() {
 override fun onReceive(context: Context,intent: Intent?) {
  val manager=context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
  if(Build.VERSION.SDK_INT>=26) manager.createNotificationChannel(NotificationChannel("ward_daily","تذكير وِرد اليومي",NotificationManager.IMPORTANCE_DEFAULT))
  val open=PendingIntent.getActivity(context,7103,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  val notification=NotificationCompat.Builder(context,"ward_daily").setSmallIcon(android.R.drawable.ic_menu_my_calendar).setContentTitle("وِرد — تذكير يومي").setContentText("اقرأ ولو صفحة من القرآن الكريم 🌿").setStyle(NotificationCompat.BigTextStyle().bigText("اقرأ ولو صفحة من القرآن الكريم. اجعل لك وردًا يوميًا ولو قليلًا.")).setContentIntent(open).setAutoCancel(true).build()
  if(Build.VERSION.SDK_INT<33 || context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)==android.content.pm.PackageManager.PERMISSION_GRANTED) manager.notify(7102,notification)
 }
}