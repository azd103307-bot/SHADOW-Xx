package com.yemenmobile.yemen4g;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.provider.Settings;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(dp(8), dp(8), dp(8), dp(8));
        return t;
    }

    private void addButton(LinearLayout parent, String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(15);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackground(shape(Color.rgb(15, 112, 91), 14));
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(54));
        p.setMargins(0, dp(7), 0, dp(7));
        parent.addView(b, p);
        b.setOnClickListener(v -> action.run());
    }

    private void openSettings(String action, String fallback) {
        try {
            startActivity(new Intent(action));
        } catch (Exception e) {
            try {
                startActivity(new Intent(fallback));
            } catch (Exception ignored) {
                Toast.makeText(this, "افتح إعدادات الهاتف ثم الشبكة يدوياً", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.rgb(244, 248, 247));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(20), dp(24), dp(20), dp(24));
        scroll.addView(page);

        page.addView(text("Yemen 4G", 30, Color.rgb(12, 91, 76), true));
        page.addView(text("مساعد إعداد شبكة يمن موبايل", 17, Color.DKGRAY, true));
        page.addView(text("لهاتف Huawei Y6 • ATU-L31", 14, Color.GRAY, false));

        TextView warning = text("هذا التطبيق يفتح إعدادات الهاتف ويساعدك على ضبط الشبكة. لا يستطيع تفعيل خدمة 4G من الشركة أو تجاوز ضعف التغطية.", 15, Color.rgb(75, 65, 35), false);
        warning.setBackground(shape(Color.rgb(255, 244, 210), 14));
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-1, -2);
        wp.setMargins(0, dp(18), 0, dp(14));
        page.addView(warning, wp);

        addButton(page, "فتح إعدادات الشبكة", () ->
            openSettings(Settings.ACTION_WIRELESS_SETTINGS, Settings.ACTION_SETTINGS));
        addButton(page, "فتح إعدادات APN", () ->
            openSettings("android.settings.APN_SETTINGS", Settings.ACTION_SETTINGS));
        addButton(page, "نسخ APN: ymdata", () -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Yemen Mobile APN", "ymdata"));
            Toast.makeText(this, "تم نسخ ymdata", Toast.LENGTH_SHORT).show();
        });

        TextView steps = text(
            "إعداد يمن موبايل\n\n" +
            "1. افتح إعدادات APN وأنشئ نقطة وصول جديدة.\n" +
            "2. اكتب ymdata في خانة APN.\n" +
            "3. اترك بقية الخانات كما هي ما لم تزودك الشركة بإعدادات أخرى.\n" +
            "4. احفظ نقطة الوصول واخترها، ثم أعد تشغيل الهاتف.\n\n" +
            "إذا لم يظهر 4G، تواصل مع يمن موبايل للتأكد من تفعيل الخدمة على الشريحة وتوافق إعدادات الجهاز والتغطية.",
            15, Color.rgb(35, 48, 45), false);
        steps.setGravity(Gravity.START);
        steps.setBackground(shape(Color.WHITE, 14));
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, -2);
        sp.setMargins(0, dp(12), 0, dp(12));
        page.addView(steps, sp);

        page.addView(text("Yemen 4G • إصدار 1.0", 12, Color.GRAY, false));
        setContentView(scroll);
    }
}
