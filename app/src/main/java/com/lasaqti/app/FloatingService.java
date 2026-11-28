package com.lasaqti.app;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.*;
import android.view.*;
import android.widget.*;

public class FloatingService extends Service {
    WindowManager wm; View panel; WindowManager.LayoutParams lp; int size=1;
    public IBinder onBind(Intent i){ return null; }
    TextView action(String label){ TextView t=new TextView(this); t.setText(label); t.setTextColor(Color.WHITE); t.setTextSize(14); t.setGravity(Gravity.CENTER); t.setPadding(12,9,12,9); t.setBackgroundColor(0xff5146a5); return t; }
    @Override public void onCreate(){
        super.onCreate();
        startForeground(77, buildNotification());
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(6,6,6,6); box.setBackgroundColor(0xee29235c);
        TextView drag=action("لصقتي  ⋮  اسحب للتحريك"); box.addView(drag);
        LinearLayout row=new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        TextView c=action("نسخ"); c.setOnClickListener(v->{ boolean ok=AccessibilityPasteService.copyFocused(); Toast.makeText(this,ok?"تم نسخ النص المحدد":"حدد نصاً أولاً وفعّل خدمة إمكانية الوصول",Toast.LENGTH_SHORT).show(); });
        TextView p=action("لصق"); p.setOnClickListener(v->{ boolean ok=AccessibilityPasteService.pasteFocused(); Toast.makeText(this,ok?"تم تنفيذ اللصق":"ضع المؤشر داخل حقل كتابة وفعّل خدمة إمكانية الوصول",Toast.LENGTH_SHORT).show(); });
        TextView z=action("الحجم"); z.setOnClickListener(v->{ size=size==1?0:size==0?2:1; float scale=size==0?.85f:size==2?1.2f:1f; box.setScaleX(scale); box.setScaleY(scale); });
        TextView close=action("×"); close.setOnClickListener(v->stopSelf());
        row.addView(c,new LinearLayout.LayoutParams(0,-2,1)); row.addView(p,new LinearLayout.LayoutParams(0,-2,1)); row.addView(z,new LinearLayout.LayoutParams(0,-2,1)); row.addView(close,new LinearLayout.LayoutParams(0,-2,1)); box.addView(row); panel=box;
        int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
        lp=new WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);
        lp.gravity=Gravity.RIGHT|Gravity.CENTER_VERTICAL; wm.addView(panel,lp);
        drag.setOnTouchListener(new View.OnTouchListener(){ int sx,sy,ox,oy; public boolean onTouch(View v,MotionEvent e){ if(e.getAction()==MotionEvent.ACTION_DOWN){sx=(int)e.getRawX();sy=(int)e.getRawY();ox=lp.x;oy=lp.y;return true;} if(e.getAction()==MotionEvent.ACTION_MOVE){lp.x=ox+sx-(int)e.getRawX();lp.y=oy+(int)e.getRawY()-sy;wm.updateViewLayout(panel,lp);return true;} return true; }});
    }
    private Notification buildNotification(){
        String channel="lasaqti_overlay";
        if(Build.VERSION.SDK_INT>=26){ NotificationChannel c=new NotificationChannel(channel,"أزرار لصقتي العائمة",NotificationManager.IMPORTANCE_LOW); ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c); }
        Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,channel):new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_menu_edit).setContentTitle("لصقتي يعمل").setContentText("أزرار النسخ واللصق العائمة مفعّلة").setOngoing(true).build();
    }
    @Override public void onDestroy(){ if(panel!=null&&wm!=null) try{wm.removeView(panel);}catch(Exception ignored){} stopForeground(true); super.onDestroy(); }
}