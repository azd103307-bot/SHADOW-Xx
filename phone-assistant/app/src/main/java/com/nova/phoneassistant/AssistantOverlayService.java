package com.nova.phoneassistant;

import android.app.Service;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.IBinder;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.View;
import android.view.MotionEvent;
import android.view.WindowManager.LayoutParams;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.app.AlertDialog;

public class AssistantOverlayService extends Service {
    private WindowManager wm;
    private View bubble;
    private View panel;
    private LayoutParams bubbleParams;
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density); }

    @Override public void onCreate() {
        super.onCreate();
        wm = (WindowManager)getSystemService(WINDOW_SERVICE);
        showBubble();
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }

    private LayoutParams params(int w, int h, int gravity) {
        return new LayoutParams(w,h,LayoutParams.TYPE_APPLICATION_OVERLAY,
            LayoutParams.FLAG_NOT_FOCUSABLE | LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
    }

    private void showBubble() {
        if (bubble != null) return;
        TextView b = new TextView(this);
        b.setText("N");
        b.setTextColor(Color.WHITE);
        b.setTextSize(21);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setGravity(Gravity.CENTER);
        b.setBackground(shape(Color.rgb(105,82,220), 40));
        bubble = b;
        bubbleParams = params(dp(54),dp(54),Gravity.TOP|Gravity.END);
        bubbleParams.x = dp(14); bubbleParams.y = dp(180);
        b.setOnClickListener(v -> showPanel());
        b.setOnTouchListener(new View.OnTouchListener() {
            float sx,sy; int ox,oy;
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction()==MotionEvent.ACTION_DOWN) { sx=e.getRawX(); sy=e.getRawY(); ox=bubbleParams.x; oy=bubbleParams.y; return false; }
                if (e.getAction()==MotionEvent.ACTION_MOVE) {
                    bubbleParams.x = Math.max(0, ox + (int)(sx-e.getRawX()));
                    bubbleParams.y = Math.max(0, oy + (int)(e.getRawY()-sy));
                    wm.updateViewLayout(bubble,bubbleParams); return true;
                }
                return false;
            }
        });
        wm.addView(bubble,bubbleParams);
    }

    private void showPanel() {
        if (panel != null) { wm.removeView(panel); panel=null; return; }
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(15),dp(14),dp(15),dp(12));
        box.setBackground(shape(Color.rgb(17,23,43),20));

        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("NOVA مساعد الهاتف");
        title.setTextColor(Color.WHITE); title.setTextSize(17); title.setTypeface(null,Typeface.BOLD);
        head.addView(title,new LinearLayout.LayoutParams(0,dp(42),1));
        Button close = new Button(this); close.setText("×"); close.setTextSize(18); close.setAllCaps(false);
        head.addView(close,new LinearLayout.LayoutParams(dp(48),dp(42)));
        box.addView(head);
        close.setOnClickListener(v -> { if(panel!=null){wm.removeView(panel);panel=null;} });

        EditText command = new EditText(this);
        command.setHint("مثال: افتح يوتيوب");
        command.setTextColor(Color.WHITE); command.setHintTextColor(Color.LTGRAY);
        command.setTextSize(15); command.setMinLines(2); command.setGravity(Gravity.TOP|Gravity.RIGHT);
        command.setBackground(shape(Color.rgb(35,44,69),14));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1,dp(94));
        cp.topMargin=dp(10); box.addView(command,cp);

        TextView status = new TextView(this);
        status.setText("جاهز. أمثلة: افتح واتساب، اضغط بحث، اكتب مرحباً، مرر للأسفل.");
        status.setTextColor(Color.rgb(185,198,225)); status.setTextSize(12);
        status.setPadding(dp(2),dp(9),dp(2),dp(8)); box.addView(status);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button run = new Button(this); run.setText("تنفيذ الأمر"); run.setAllCaps(false);
        Button stop = new Button(this); stop.setText("إيقاف"); stop.setAllCaps(false);
        actions.addView(run,new LinearLayout.LayoutParams(0,dp(48),1));
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(dp(92),dp(48));
        stopParams.leftMargin = dp(8);
        actions.addView(stop,stopParams);
        box.addView(actions);
        stop.setOnClickListener(v -> {
            PhoneControlService service = PhoneControlService.instance;
            if (service != null) service.stopSequence();
            status.setText("تم إيقاف تنفيذ الخطوات المتبقية.");
        });
        run.setOnClickListener(v -> {
            String text=command.getText().toString().trim();
            if (text.isEmpty()) { status.setText("اكتب أمراً أولاً."); return; }
            if (isSensitive(text)) {
                new AlertDialog.Builder(this)
                    .setTitle("تأكيد إجراء حساس")
                    .setMessage("هذا الأمر قد يرسل أو يحذف أو ينشر أو يدفع أو يجري اتصالاً. هل تريد المتابعة؟\n\n"+text)
                    .setNegativeButton("إلغاء",(d,w)->status.setText("تم إلغاء الأمر."))
                    .setPositiveButton("تأكيد التنفيذ",(d,w)->runCommand(text,status))
                    .show();
            } else runCommand(text,status);
        });

        panel=box;
        LayoutParams pp=params(-1, -2, Gravity.CENTER);
        pp.flags = LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        pp.width=getResources().getDisplayMetrics().widthPixels-dp(28);
        pp.gravity=Gravity.CENTER;
        wm.addView(panel,pp);
    }

    private boolean isSensitive(String s) {
        String x=s.toLowerCase();
        String[] words={"ارسل","أرسل","ارسال","إرسال","send","احذف","حذف","delete","شراء","اشتر","دفع","ادفع","payment","pay","انشر","نشر","post","اتصل","اتصال","call"};
        for(String w:words) if(x.contains(w)) return true;
        return false;
    }

    private void runCommand(String text, TextView status) {
        PhoneControlService service=PhoneControlService.instance;
        if(service==null) {
            status.setText("فعّل خدمة NOVA للتحكم بالشاشة من إعدادات إمكانية الوصول أولاً.");
            return;
        }
        status.setText("بدأ تنفيذ الخطوات...");
        service.executeSequence(text, message -> status.setText(message));
    }

    @Override public int onStartCommand(Intent intent,int flags,int startId) { showBubble(); return START_STICKY; }
    @Override public void onDestroy() {
        if(panel!=null) wm.removeView(panel);
        if(bubble!=null) wm.removeView(bubble);
        panel=null; bubble=null; super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
