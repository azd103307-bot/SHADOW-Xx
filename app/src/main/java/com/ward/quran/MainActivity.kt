package com.ward.quran
import android.app.Activity
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.webkit.ValueCallback
import android.net.Uri
import android.graphics.Color
import android.webkit.JavascriptInterface
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.Calendar
class MainActivity : Activity() {
 private lateinit var webView: WebView
 private var uploadCallback: ValueCallback<Array<Uri>>? = null
 private val fileRequestCode = 7105
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState)
  window.statusBarColor=Color.rgb(16,57,47); window.navigationBarColor=Color.rgb(16,57,47)
  webView=WebView(this); webView.setBackgroundColor(Color.rgb(245,242,233))
  webView.settings.javaScriptEnabled=true; webView.settings.domStorageEnabled=true
  webView.settings.cacheMode=WebSettings.LOAD_DEFAULT; webView.settings.allowFileAccess=true
  webView.addJavascriptInterface(ReminderBridge(this),"WardNative")
  webView.webViewClient=WebViewClient(); webView.webChromeClient=object:WebChromeClient(){
   override fun onShowFileChooser(view:WebView,callback:ValueCallback<Array<Uri>>,params:FileChooserParams):Boolean {
    uploadCallback?.onReceiveValue(null); uploadCallback=callback
    return try { startActivityForResult(params.createIntent(),fileRequestCode); true }
    catch(e:Exception) { uploadCallback?.onReceiveValue(null); uploadCallback=null; false }
   }
  }; setContentView(webView)
  if(savedInstanceState==null) webView.loadUrl("file:///android_asset/index.html") else webView.restoreState(savedInstanceState)
 }
 override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
  super.onActivityResult(requestCode,resultCode,data)
  if(requestCode==fileRequestCode){val callback=uploadCallback?:return;uploadCallback=null;callback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode,data))}
 }
 override fun onSaveInstanceState(outState:Bundle){webView.saveState(outState);super.onSaveInstanceState(outState)}
 @Deprecated("Deprecated in Android") override fun onBackPressed(){if(this::webView.isInitialized&&webView.canGoBack())webView.goBack() else super.onBackPressed()}
}
class ReminderBridge(private val context: Context) {
 @JavascriptInterface fun setDailyReminder(enabled:Boolean,hour:Int,minute:Int) {
  val alarm=context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
  val intent=Intent(context,DailyReminderReceiver::class.java)
  val pending=PendingIntent.getBroadcast(context,7102,intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
  if(!enabled){alarm.cancel(pending);return}
  if(android.os.Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED){(context as? Activity)?.runOnUiThread { (context as? Activity)?.requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),7104) }}
  val cal=Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,hour.coerceIn(0,23));set(Calendar.MINUTE,minute.coerceIn(0,59));set(Calendar.SECOND,0);set(Calendar.MILLISECOND,0);if(timeInMillis<=System.currentTimeMillis())add(Calendar.DAY_OF_YEAR,1) }
  alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP,cal.timeInMillis,AlarmManager.INTERVAL_DAY,pending)
 }
}