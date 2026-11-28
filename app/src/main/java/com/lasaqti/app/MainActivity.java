package com.lasaqti.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.view.*;
import android.view.accessibility.AccessibilityManager;
import android.widget.*;

public class MainActivity extends Activity {
    int purple=Color.rgb(42,35,92);
    TextView text(String s,int size,int color){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.CENTER); t.setPadding(16,14,16,14); return t; }
    Button btn(String s){ Button b=new Button(this); b.setText(s); b.setTextSize(16); return b; }
    public void onCreate(Bundle b){ super.onCreate(b); getWindow().setStatusBarColor(purple); getWindow().setNavigationBarColor(purple);
        LinearLayout root=new LinearLayout(this); root.setOrientation(1); root.setPadding(20,24,20,20); root.setBackgroundColor(Color.rgb(246,245,252));
        ScrollView sc=new ScrollView(this); sc.addView(root); setContentView(sc);
        root.addView(text("لصقتي  |  Lasaqti",28,purple));
        root.addView(text("أدوات سريعة للنسخ واللصق في تطبيقات Android",15,Color.DKGRAY));
        EditText input=new EditText(this); input.setHint("اكتب نصاً هنا لنسخه"); input.setMinLines(3); input.setGravity(Gravity.TOP|Gravity.RIGHT); root.addView(input);
        Button copy=btn("نسخ النص المكتوب"); copy.setOnClickListener(v->{ ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE); cm.setPrimaryClip(ClipData.newPlainText("Lasaqti",input.getText())); Toast.makeText(this,"تم نسخ النص",Toast.LENGTH_SHORT).show(); }); root.addView(copy);
        Button overlay=btn("تشغيل الأزرار العائمة"); overlay.setOnClickListener(v->{
            if(!Settings.canDrawOverlays(this)){ getSharedPreferences("lasaqti",MODE_PRIVATE).edit().putBoolean("start_overlay_when_return",true).apply(); startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName()))); }
            else { startService(new Intent(this,FloatingService.class)); Toast.makeText(this,"الأزرار العائمة تعمل",Toast.LENGTH_SHORT).show(); }
        }); root.addView(overlay);
        Button stop=btn("إيقاف الأزرار العائمة"); stop.setOnClickListener(v->stopService(new Intent(this,FloatingService.class))); root.addView(stop);
        Button access=btn("تفعيل خدمة النسخ واللصق"); access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))); root.addView(access);
        Button history=btn("عرض محتوى الحافظة"); history.setOnClickListener(v->{ ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE); if(cm.hasPrimaryClip()&&cm.getPrimaryClip().getItemCount()>0){ CharSequence s=cm.getPrimaryClip().getItemAt(0).coerceToText(this); new AlertDialog.Builder(this).setTitle("آخر نص منسوخ").setMessage(s).setPositiveButton("نسخ مجدداً",(d,w)->cm.setPrimaryClip(ClipData.newPlainText("Lasaqti",s))).setNegativeButton("إغلاق",null).show(); } else Toast.makeText(this,"الحافظة فارغة",Toast.LENGTH_SHORT).show(); }); root.addView(history);
        root.addView(text("طريقة الاستخدام: اسمح بالظهور فوق التطبيقات، ثم فعّل خدمة إمكانية الوصول من إعدادات Android. من داخل أي تطبيق حدّد النص واضغط «نسخ» في الزر العائم، أو ضع المؤشر داخل حقل كتابة واضغط «لصق». قد تمنع بعض التطبيقات أو الحقول المحمية هذه العمليات.",14,Color.DKGRAY));
        root.addView(text("ملاحظة الخصوصية: خدمة إمكانية الوصول تستخدم فقط لتنفيذ أمر النسخ أو اللصق عند ضغطك الزر، ولا ترسل محتوى الشاشة إلى الإنترنت.",13,Color.GRAY));
    }
    @Override protected void onResume(){ super.onResume(); android.content.SharedPreferences p=getSharedPreferences("lasaqti",MODE_PRIVATE); if(p.getBoolean("start_overlay_when_return",false) && Settings.canDrawOverlays(this)){ p.edit().putBoolean("start_overlay_when_return",false).apply(); startService(new Intent(this,FloatingService.class)); } }
}