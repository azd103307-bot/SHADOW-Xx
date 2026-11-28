package com.lasaqti.app;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.*;
import android.view.*;
import android.widget.*;
public class FloatingService extends Service {
 WindowManager wm;View panel;WindowManager.LayoutParams lp;int size=1;
 public IBinder onBind(Intent i){return null;}
 TextView action(String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setTextSize(13);t.setGravity(Gravity.CENTER);t.setPadding(9,10,9,10);t.setBackgroundColor(0xff5146a5);return t;}
 void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
 void showSelectionPanel(){
  int len=AccessibilityPasteService.focusedTextLength();if(len<0){toast("فعّل خدمة إمكانية الوصول أولاً");return;}if(len==0){toast("لا يوجد نص في العنصر المحدد");return;}
  LinearLayout form=new LinearLayout(this);form.setOrientation(1);form.setPadding(24,12,24,4);
  TextView info=new TextView(this);info.setText("طول النص: "+len+" حرفاً. أدخل بداية ونهاية الجزء المطلوب (العد يبدأ من صفر).");form.addView(info);
  EditText start=new EditText(this);start.setInputType(2);start.setSingleLine(true);start.setHint("بداية التحديد");start.setText("0");form.addView(start);
  EditText end=new EditText(this);end.setInputType(2);end.setSingleLine(true);end.setHint("نهاية التحديد");end.setText(String.valueOf(len));form.addView(end);
  new AlertDialog.Builder(this).setTitle("لوحة تحديد النص").setView(form)
   .setPositiveButton("تحديد النص",(d,w)->{try{int s=Integer.parseInt(start.getText().toString()),e=Integer.parseInt(end.getText().toString());toast(AccessibilityPasteService.selectRangeFocused(s,e)?"تم تحديد النص":"تعذر التحديد؛ تحقق من النطاق ودعم التطبيق");}catch(Exception ex){toast("أدخل أرقاماً صحيحة");}})
   .setNeutralButton("تحديد الكل",(d,w)->toast(AccessibilityPasteService.selectAllFocused()?"تم تحديد الكل":"هذا العنصر لا يدعم التحديد")).setNegativeButton("إغلاق",null).show();
 }
 @Override public void onCreate(){
  super.onCreate();startForeground(77,buildNotification());wm=(WindowManager)getSystemService(WINDOW_SERVICE);
  LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(6,6,6,6);box.setBackgroundColor(0xee29235c);
  TextView drag=action("لصقتي ⋮ اسحب للتحريك");box.addView(drag);
  LinearLayout r1=new LinearLayout(this);r1.setOrientation(0);
  TextView c=action("نسخ");c.setOnClickListener(v->toast(AccessibilityPasteService.copyFocused()?"تم نسخ النص المحدد":"حدد نصاً وفعّل خدمة إمكانية الوصول"));
  TextView p=action("لصق");p.setOnClickListener(v->toast(AccessibilityPasteService.pasteFocused()?"تم تنفيذ اللصق":"ضع المؤشر داخل حقل كتابة"));
  TextView all=action("تحديد الكل");all.setOnClickListener(v->toast(AccessibilityPasteService.selectAllFocused()?"تم تحديد الكل":"تعذر التحديد في هذا التطبيق"));
  r1.addView(c,new LinearLayout.LayoutParams(0,-2,1));r1.addView(p,new LinearLayout.LayoutParams(0,-2,1));r1.addView(all,new LinearLayout.LayoutParams(0,-2,1));box.addView(r1);
  LinearLayout r2=new LinearLayout(this);r2.setOrientation(0);
  TextView range=action("لوحة تحديد النص");range.setOnClickListener(v->showSelectionPanel());
  TextView z=action("الحجم");z.setOnClickListener(v->{size=size==1?0:size==0?2:1;float scale=size==0?.85f:size==2?1.2f:1f;box.setScaleX(scale);box.setScaleY(scale);});
  TextView close=action("إغلاق ×");close.setOnClickListener(v->stopSelf());
  r2.addView(range,new LinearLayout.LayoutParams(0,-2,2));r2.addView(z,new LinearLayout.LayoutParams(0,-2,1));r2.addView(close,new LinearLayout.LayoutParams(0,-2,1));box.addView(r2);panel=box;
  int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
  lp=new WindowManager.LayoutParams(-2,-2,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,PixelFormat.TRANSLUCENT);lp.gravity=Gravity.RIGHT|Gravity.CENTER_VERTICAL;wm.addView(panel,lp);
  drag.setOnTouchListener(new View.OnTouchListener(){int sx,sy,ox,oy;public boolean onTouch(View v,MotionEvent e){if(e.getAction()==0){sx=(int)e.getRawX();sy=(int)e.getRawY();ox=lp.x;oy=lp.y;return true;}if(e.getAction()==2){lp.x=ox+sx-(int)e.getRawX();lp.y=oy+(int)e.getRawY()-sy;wm.updateViewLayout(panel,lp);return true;}return true;}});
 }
 private Notification buildNotification(){String ch="lasaqti_overlay";if(Build.VERSION.SDK_INT>=26){NotificationChannel c=new NotificationChannel(ch,"أزرار لصقتي العائمة",NotificationManager.IMPORTANCE_LOW);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c);}Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,ch):new Notification.Builder(this);return b.setSmallIcon(android.R.drawable.ic_menu_edit).setContentTitle("لصقتي يعمل").setContentText("أزرار النسخ واللصق وتحديد النص مفعّلة").setOngoing(true).build();}
 @Override public void onDestroy(){if(panel!=null&&wm!=null)try{wm.removeView(panel);}catch(Exception ignored){}stopForeground(true);super.onDestroy();}
}