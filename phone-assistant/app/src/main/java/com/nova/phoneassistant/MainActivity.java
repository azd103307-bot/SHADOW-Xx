package com.nova.phoneassistant;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;

public class MainActivity extends Activity {
    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(12,16,31));
        getWindow().setNavigationBarColor(Color.rgb(12,16,31));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22),dp(28),dp(22),dp(20));
        root.setBackgroundColor(Color.rgb(12,16,31));

        TextView title = new TextView(this);
        title.setText("NOVA 0.4 🤖  مساعد الهاتف");
        title.setTextColor(Color.WHITE);
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0,dp(18),0,dp(12));
        root.addView(title);

        TextView intro = new TextView(this);
        intro.setText("اكتب مهمة بالعربية. يستطيع تقسيمها إلى خطوات، فتح التطبيقات والبحث، قراءة نص الشاشة بالعربية والإنجليزية من الصورة أو عناصر الوصول، الكتابة والضغط والتمرير والتحكم بألواح النظام. يحتاج إلى صلاحية إمكانية الوصول والنافذة العائمة.");
        intro.setTextColor(Color.LTGRAY);
        intro.setTextSize(15);
        intro.setGravity(Gravity.CENTER);
        intro.setPadding(0,dp(8),0,dp(28));
        root.addView(intro);

        Button accessibility = button("1) تفعيل خدمة التحكم بالشاشة");
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(accessibility);

        Button overlay = button("2) السماح بالظهور فوق التطبيقات");
        overlay.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } else startOverlay();
        });
        root.addView(overlay);

        Button start = button("3) تشغيل المساعد العائم");
        start.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
            } else startOverlay();
        });
        root.addView(start);

        TextView note = new TextView(this);
        note.setText("أمثلة: افتح يوتيوب ثم ابحث عن القطط ثم افتح أول نتيجة. أو: اقرأ الشاشة، افتح الإشعارات، انتظر 3 ثوانٍ، ارفع الصوت. القراءة البصرية تحتاج Android 11 أو أحدث. الإرسال والحذف والدفع والنشر والاتصال تتطلب تأكيداً.");
        note.setTextColor(Color.rgb(255,205,125));
        note.setTextSize(13);
        note.setPadding(0,dp(25),0,0);
        root.addView(note);
        setContentView(root);
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setTextSize(14);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(58));
        p.bottomMargin = dp(12);
        b.setLayoutParams(p);
        return b;
    }

    private void startOverlay() {
        startService(new Intent(this, AssistantOverlayService.class));
        moveTaskToBack(true);
    }
}
