package com.anzd.nova;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.*;
import android.view.*;
import android.content.ContentValues;
import android.provider.MediaStore;
import android.net.Uri;
import android.widget.Toast;
import android.util.Base64;
import java.io.OutputStream;

public class MainActivity extends Activity {
  private WebView web;
  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow().setFlags(1024,1024);
    web = new WebView(this);
    web.setBackgroundColor(0xff05030f);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setMediaPlaybackRequiresUserGesture(false);
    s.setAllowFileAccess(true);
    s.setAllowContentAccess(true);
    web.setWebChromeClient(new WebChromeClient());
    web.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");
    setContentView(web);
    web.loadUrl("file:///android_asset/index.html");
  }
  public class AndroidBridge {
    @JavascriptInterface public void saveVideo(String dataUrl) {
      try {
        String b64 = dataUrl.substring(dataUrl.indexOf(",")+1);
        byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
        ContentValues v = new ContentValues();
        v.put(MediaStore.MediaColumns.DISPLAY_NAME, "nova-rebellion-" + System.currentTimeMillis() + ".webm");
        v.put(MediaStore.MediaColumns.MIME_TYPE, "video/webm");
        v.put(MediaStore.MediaColumns.RELATIVE_PATH, "Movies/Nova");
        Uri uri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, v);
        if (uri == null) throw new Exception("تعذر إنشاء ملف الفيديو");
        try (OutputStream out = getContentResolver().openOutputStream(uri)) { out.write(bytes); }
        runOnUiThread(() -> Toast.makeText(MainActivity.this, "تم حفظ الفيديو في Movies/Nova", Toast.LENGTH_LONG).show());
      } catch (Exception e) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, "تعذر حفظ الفيديو: " + e.getMessage(), Toast.LENGTH_LONG).show());
      }
    }
  }
  @Override public void onBackPressed() {
    if (web != null && web.canGoBack()) web.goBack(); else super.onBackPressed();
  }
  @Override protected void onDestroy() {
    if (web != null) { web.loadUrl("about:blank"); web.destroy(); }
    super.onDestroy();
  }
}
