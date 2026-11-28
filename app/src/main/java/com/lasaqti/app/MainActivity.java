package com.lasaqti.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    int purple=Color.rgb(42,35,92);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String activeRoom = "";
    TextView text(String s,int size,int color){ TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.CENTER); t.setPadding(16,14,16,14); return t; }
    Button btn(String s){ Button b=new Button(this); b.setText(s); b.setTextSize(16); return b; }

    public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(purple);
        getWindow().setNavigationBarColor(purple);
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(20,24,20,20);
        root.setBackgroundColor(Color.rgb(246,245,252));
        ScrollView sc=new ScrollView(this);
        sc.addView(root);
        setContentView(sc);

        root.addView(text("لصقتي  |  Lasaqti",28,purple));
        root.addView(text("أدوات سريعة للنسخ واللصق في تطبيقات Android",15,Color.DKGRAY));
        EditText input=new EditText(this);
        input.setHint("اكتب نصاً هنا لنسخه أو إرساله للكمبيوتر");
        input.setMinLines(3);
        input.setGravity(Gravity.TOP|Gravity.RIGHT);
        root.addView(input);

        Button copy=btn("نسخ النص المكتوب");
        copy.setOnClickListener(v->{
            ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("Lasaqti",input.getText()));
            Toast.makeText(this,"تم نسخ النص",Toast.LENGTH_SHORT).show();
        });
        root.addView(copy);

        Button bridge=btn("ربط كمبيوتر Manus وإرسال النص");
        bridge.setOnClickListener(v->createBridgeRoom(input));
        root.addView(bridge);

        Button overlay=btn("تشغيل الأزرار العائمة");
        overlay.setOnClickListener(v->{
            if(!Settings.canDrawOverlays(this)){
                getSharedPreferences("lasaqti",MODE_PRIVATE).edit().putBoolean("start_overlay_when_return",true).apply();
                startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));
            } else {
                startService(new Intent(this,FloatingService.class));
                Toast.makeText(this,"الأزرار العائمة تعمل",Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(overlay);

        Button stop=btn("إيقاف الأزرار العائمة");
        stop.setOnClickListener(v->stopService(new Intent(this,FloatingService.class)));
        root.addView(stop);

        Button access=btn("تفعيل خدمة النسخ واللصق");
        access.setOnClickListener(v->startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(access);

        Button history=btn("عرض محتوى الحافظة");
        history.setOnClickListener(v->{
            ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            if(cm.hasPrimaryClip()&&cm.getPrimaryClip().getItemCount()>0){
                CharSequence s=cm.getPrimaryClip().getItemAt(0).coerceToText(this);
                new AlertDialog.Builder(this).setTitle("آخر نص منسوخ").setMessage(s)
                    .setPositiveButton("نسخ مجدداً",(d,w)->cm.setPrimaryClip(ClipData.newPlainText("لصقتي",s)))
                    .setNegativeButton("إغلاق",null).show();
            } else Toast.makeText(this,"الحافظة فارغة",Toast.LENGTH_SHORT).show();
        });
        root.addView(history);

        root.addView(text("طريقة النقل إلى كمبيوتر Manus: أنشئ غرفة من هنا، ثم افتح رابط لصقتي Bridge في متصفح كمبيوتر Manus وأدخل رمز الغرفة. أرسل النص من الهاتف، واضغط «نسخ إلى حافظة الكمبيوتر» في صفحة الاستقبال، ثم الصقه في محرر الأكواد.",14,Color.DKGRAY));
        root.addView(text("الغرف مؤقتة لمدة 10 دقائق. لا ترسل كلمات مرور أو مفاتيح سرية. لا تُرسل خدمة إمكانية الوصول محتوى الشاشة إلى الإنترنت؛ لا يتم نقل النص إلا عندما تضغط زر الإرسال.",13,Color.GRAY));
    }

    private void createBridgeRoom(EditText source) {
        Toast.makeText(this,"جارٍ إنشاء غرفة النقل...",Toast.LENGTH_SHORT).show();
        executor.execute(() -> {
            try {
                String code = BridgeClient.createRoom();
                mainHandler.post(() -> showBridgeDialog(code, source.getText().toString()));
            } catch (Exception e) {
                mainHandler.post(() -> new AlertDialog.Builder(this)
                    .setTitle("تعذر إنشاء الغرفة")
                    .setMessage("تحقق من اتصال الإنترنت ثم حاول مرة أخرى.\n" + e.getMessage())
                    .setPositiveButton("حسناً", null).show());
            }
        });
    }

    private void showBridgeDialog(String code, String initialText) {
        activeRoom = code;
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(1);
        layout.setPadding(24, 12, 24, 4);

        TextView codeView = text("رمز الغرفة: " + code, 22, purple);
        codeView.setTextIsSelectable(true);
        layout.addView(codeView);

        TextView instructions = text("افتح هذا الموقع في متصفح كمبيوتر Manus، واختر «استقبال في الكمبيوتر»، ثم أدخل الرمز أعلاه.", 14, Color.DKGRAY);
        layout.addView(instructions);

        EditText message = new EditText(this);
        message.setGravity(Gravity.TOP | Gravity.RIGHT);
        message.setMinLines(5);
        message.setText(initialText);
        message.setHint("النص أو الكود الذي تريد إرساله");
        layout.addView(message);

        TextView expiry = text("تنتهي الغرفة تلقائياً بعد 10 دقائق.", 12, Color.GRAY);
        layout.addView(expiry);

        AlertDialog dialog = new AlertDialog.Builder(this)
            .setTitle("جسر الهاتف والكمبيوتر")
            .setView(layout)
            .setNegativeButton("إغلاق", null)
            .setNeutralButton("نسخ الرمز والرابط", null)
            .setPositiveButton("إرسال إلى الكمبيوتر", null)
            .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                String share = "https://lasaqti-bridge-mgk27z.v2.appdeploy.ai/\nرمز الغرفة: " + code;
                ClipboardManager cm = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                cm.setPrimaryClip(ClipData.newPlainText("Lasaqti Bridge", share));
                Toast.makeText(this, "تم نسخ الرابط والرمز", Toast.LENGTH_SHORT).show();
            });
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String payload = message.getText().toString();
                if (payload.trim().isEmpty()) {
                    message.setError("اكتب النص أولاً");
                    return;
                }
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                executor.execute(() -> {
                    try {
                        BridgeClient.sendText(code, payload);
                        mainHandler.post(() -> {
                            Toast.makeText(this, "وصل النص إلى غرفة الكمبيوتر", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        });
                    } catch (Exception e) {
                        mainHandler.post(() -> {
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            new AlertDialog.Builder(this).setTitle("فشل الإرسال")
                                .setMessage("لم يتم إرسال النص. قد تكون الغرفة انتهت؛ أنشئ غرفة جديدة.\n" + e.getMessage())
                                .setPositiveButton("حسناً", null).show();
                        });
                    }
                });
            });
        });
        dialog.show();
    }

    @Override protected void onResume(){
        super.onResume();
        android.content.SharedPreferences p=getSharedPreferences("lasaqti",MODE_PRIVATE);
        if(p.getBoolean("start_overlay_when_return",false) && Settings.canDrawOverlays(this)){
            p.edit().putBoolean("start_overlay_when_return",false).apply();
            startService(new Intent(this,FloatingService.class));
        }
    }

    @Override protected void onDestroy() {
        executor.shutdown();
        super.onDestroy();
    }
}