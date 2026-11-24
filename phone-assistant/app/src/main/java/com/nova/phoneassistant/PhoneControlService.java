package com.nova.phoneassistant;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.accessibility.AccessibilityEvent;
import java.util.Locale;

public class PhoneControlService extends AccessibilityService {
    public static volatile PhoneControlService instance;

    @Override public void onServiceConnected() { instance = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() { }
    @Override public void onDestroy() { if (instance == this) instance = null; super.onDestroy(); }

    public String execute(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "اكتب أمراً أولاً.";
        String normalized = raw.trim();
        String lower = normalized.toLowerCase(Locale.ROOT);

        if (containsAny(lower, "افتح", "شغل", "اذهب الى", "انتقل الى", "open ")) {
            String app = extractAfterVerb(normalized);
            return launchApp(app);
        }
        if (containsAny(lower, "ارجع", "عودة", "رجوع", "back")) {
            performGlobalAction(GLOBAL_ACTION_BACK);
            return "تم تنفيذ الرجوع.";
        }
        if (containsAny(lower, "الشاشة الرئيسية", "الرئيسية", "اذهب للرئيسية", "home")) {
            performGlobalAction(GLOBAL_ACTION_HOME);
            return "تم الانتقال إلى الشاشة الرئيسية.";
        }
        if (containsAny(lower, "اكتب", "اكتب النص", "type ")) {
            String text = extractAfterVerb(normalized);
            return typeText(text);
        }
        if (containsAny(lower, "اضغط", "انقر", "اختار", "اختر", "tap ")) {
            String target = extractAfterVerb(normalized);
            return clickText(target);
        }
        if (containsAny(lower, "مرر للاسفل", "انزل", "اسفل", "scroll down")) {
            return scroll(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        }
        if (containsAny(lower, "مرر للاعلى", "اصعد", "اعلى", "scroll up")) {
            return scroll(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        }
        return "لم أفهم الأمر بعد. جرّب: افتح يوتيوب، اكتب مرحباً، اضغط بحث، مرر للأسفل، أو ارجع.";
    }

    private boolean containsAny(String s, String... terms) {
        for (String term : terms) if (s.contains(term)) return true;
        return false;
    }

    private String extractAfterVerb(String s) {
        String[] verbs = {"اذهب الى","انتقل الى","اذهب إلى","انتقل إلى","افتح","شغل","اكتب النص","اكتب","اضغط على","اضغط","انقر على","انقر","اختار","اختر","tap","open","type"};
        String lower = s.toLowerCase(Locale.ROOT);
        for (String verb : verbs) {
            int i = lower.indexOf(verb.toLowerCase(Locale.ROOT));
            if (i >= 0) return s.substring(i + verb.length()).trim().replaceFirst("^(على|الى|إلى)\\s*", "").trim();
        }
        return s.trim();
    }

    private String launchApp(String name) {
        String n = name.replaceAll("[.!؟?]","").trim().toLowerCase(Locale.ROOT);
        String pkg = null;
        if (n.contains("واتساب") || n.contains("whatsapp")) pkg = "com.whatsapp";
        else if (n.contains("يوتيوب") || n.contains("youtube")) pkg = "com.google.android.youtube";
        else if (n.contains("متصفح") || n.contains("كروم") || n.contains("chrome")) pkg = "com.android.chrome";
        else if (n.contains("تيليجرام") || n.contains("telegram")) pkg = "org.telegram.messenger";
        else if (n.contains("فيسبوك") || n.contains("facebook")) pkg = "com.facebook.katana";
        else if (n.contains("انستغرام") || n.contains("instagram")) pkg = "com.instagram.android";
        else if (n.contains("الإعدادات") || n.contains("الاعدادات") || n.contains("settings")) pkg = "com.android.settings";

        PackageManager pm = getPackageManager();
        Intent intent = pkg == null ? pm.getLaunchIntentForPackage(name) : pm.getLaunchIntentForPackage(pkg);
        if (intent == null) return "لم أجد التطبيق: " + name + ". جرّب اسمه المعروف مثل واتساب أو يوتيوب أو كروم.";
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        return "جارٍ فتح " + name;
    }

    private String typeText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "لا أستطيع الوصول إلى الشاشة الحالية.";
        AccessibilityNodeInfo field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (field == null || !field.isEditable()) {
            field = findEditable(root);
        }
        if (field == null) return "لم أجد خانة كتابة. اضغط أولاً داخل خانة النص ثم أعد الأمر.";
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        boolean ok = field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        return ok ? "تمت كتابة النص." : "التطبيق الحالي لم يسمح بالكتابة في هذه الخانة.";
    }

    private AccessibilityNodeInfo findEditable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isEditable() && node.isEnabled()) return node;
        for (int i=0;i<node.getChildCount();i++) {
            AccessibilityNodeInfo found = findEditable(node.getChild(i));
            if (found != null) return found;
        }
        return null;
    }

    private String clickText(String target) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "لا أستطيع قراءة عناصر الشاشة الحالية.";
        AccessibilityNodeInfo node = findText(root, target);
        if (node == null) return "لم أجد العنصر: " + target;
        AccessibilityNodeInfo clickable = node;
        while (clickable != null && !clickable.isClickable()) clickable = clickable.getParent();
        boolean ok = clickable != null && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        return ok ? "تم الضغط على: " + target : "وجدت العنصر لكن تعذّر الضغط عليه.";
    }

    private AccessibilityNodeInfo findText(AccessibilityNodeInfo node, String target) {
        if (node == null) return null;
        CharSequence t = node.getText();
        CharSequence d = node.getContentDescription();
        String q = target.trim().toLowerCase(Locale.ROOT);
        if ((t != null && t.toString().toLowerCase(Locale.ROOT).contains(q)) ||
            (d != null && d.toString().toLowerCase(Locale.ROOT).contains(q))) return node;
        for (int i=0;i<node.getChildCount();i++) {
            AccessibilityNodeInfo found = findText(node.getChild(i), target);
            if (found != null) return found;
        }
        return null;
    }

    private String scroll(int action) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "لا أستطيع الوصول إلى الشاشة الحالية.";
        AccessibilityNodeInfo node = findScrollable(root);
        if (node == null) node = root;
        boolean ok = node.performAction(action);
        return ok ? "تم التمرير." : "تعذّر التمرير في هذه الشاشة.";
    }

    private AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isScrollable()) return node;
        for (int i=0;i<node.getChildCount();i++) {
            AccessibilityNodeInfo found = findScrollable(node.getChild(i));
            if (found != null) return found;
        }
        return null;
    }
}
