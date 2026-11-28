package com.lasaqti.app;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.graphics.Color;
import android.view.*;
import android.widget.*;

public class MainActivity extends Activity {
    LinearLayout root;
    int purple=Color.rgb(42,35,92);
    TextView text(String s,int size,int color){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setGravity(17); t.setPadding(16,14,16,14); return t; }
    Button btn(String s){ Button b=new Button(this); b.setText(s); b.setTextSize(17); return b; }
    public void onCreate(Bundle b){ super.onCreate(b); getWindow().setStatusBarColor(purple); getWindow().setNavigationBarColor(purple);
        root=new LinearLayout(this); root.setOrientation(1); root.setPadding(22,28,22,22); root.setBackgroundColor(Color.rgb(246,245,252));
        ScrollView sc=new ScrollView(this); sc.addView(root); setContentView(sc);
        root.addView(text("لصقتي  |  Lasaqti",28,purple));
        root.addView(text("انسخ بسرعة، والصق أينما يسمح Android",16,Color.DKGRAY));
        EditText input=new EditText(this); input.setHint("اكتب النص هنا لنسخه"); input.setMinLines(3); input.setGravity( top()); root.addView(input);
        Button copy=btn("نسخ النص"); copy.setOnClickListener(v->{ ((android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Lasaqti",input.getText())); Toast.makeText(this,"تم النسخ",0).show(); }); root.addView(copy);
        Button overlay=btn("تفعيل الأزرار العائمة"); overlay.setOnClickListener(v->{
            if(!Settings.canDrawOverlays(this)){ Intent i=new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:"+getPackageName())); startActivity(i); }
            else { startService(new Intent(this,FloatingService.class)); Toast.makeText(this,"ظهرت أزرار النسخ واللصق",0).show(); }
        }); root.addView(overlay);
        Button history=btn("إظهار آخر نص منسوخ"); history.setOnClickListener(v->{ android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE); if(cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount()>0){ CharSequence s=cm.getPrimaryClip().getItemAt(0).coerceToText(this); new AlertDialog.Builder(this).setTitle("الحافظة").setMessage(s).setPositiveButton("نسخ مجدداً",(d,w)->cm.setPrimaryClip(ClipData.newPlainText("Lasaqti",s))).setNegativeButton("إغلاق",null).show(); } else Toast.makeText(this,"الحافظة فارغة",0).show(); }); root.addView(history);
        root.addView(text("ملاحظة: زر اللصق ينسخ النص إلى الحافظة. افتح الحقل المطلوب واضغط مطولاً ثم اختر لصق. بعض التطبيقات والحقول المحمية تمنع الإدخال التلقائي.",14,Color.DKGRAY));
    }
    int top(){ return Gravity.TOP|Gravity.RIGHT; }
}
