package com.lasaqti.app;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.os.*;
import android.view.*;
import android.widget.*;

public class FloatingService extends Service {
    WindowManager wm; View panel;
    public IBinder onBind(Intent i){ return null; }
    public void onCreate(){ super.onCreate(); wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(8,8,8,8); box.setBackgroundColor(0xee29235c);
        Button c=new Button(this); c.setText("نسخ"); c.setOnClickListener(v->Toast.makeText(this,"انسخ النص من الحقل أو استخدم زر النسخ داخل التطبيق",0).show());
        Button p=new Button(this); p.setText("لصق"); p.setOnClickListener(v->Toast.makeText(this,"تم تجهيز النص في الحافظة؛ اضغط مطولاً داخل الحقل ثم لصق",0).show());
        Button close=new Button(this); close.setText("×"); close.setOnClickListener(v->stopSelf());
        box.addView(c); box.addView(p); box.addView(close); panel=box;
        int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        lp.gravity=Gravity.RIGHT|Gravity.CENTER_VERTICAL; wm.addView(panel,lp);
        box.setOnTouchListener(new View.OnTouchListener(){ int x,y; public boolean onTouch(View v,android.view.MotionEvent e){ if(e.getAction()==0){x=(int)e.getRawX();y=(int)e.getRawY();return true;} if(e.getAction()==2){lp.x=Math.max(0,(int)e.getRawX()-x);lp.y+=(int)e.getRawY()-y;y=(int)e.getRawY();wm.updateViewLayout(panel,lp);return true;}return true;}});
    }
    public void onDestroy(){ if(panel!=null && wm!=null) wm.removeView(panel); super.onDestroy(); }
}
