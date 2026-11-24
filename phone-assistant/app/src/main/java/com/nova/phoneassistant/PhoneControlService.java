package com.nova.phoneassistant;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.Intent;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhoneControlService extends AccessibilityService {
    public static volatile PhoneControlService instance;
    private final Handler sequenceHandler = new Handler(Looper.getMainLooper());
    private Runnable repeatRunnable;
    private volatile boolean repeatCancelled = false;

    public interface SequenceCallback {
        void onUpdate(String message);
    }

    @Override public void onServiceConnected() { instance = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {}
    @Override public void onInterrupt() { }
    @Override public void onDestroy() {
        sequenceHandler.removeCallbacksAndMessages(null);
        if (instance == this) instance = null;
        super.onDestroy();
    }

    // ينفذ الأوامر واحداً بعد الآخر مع مهلة قصيرة لانتقال التطبيق أو تحديث الشاشة.
    public void executeSequence(String raw, SequenceCallback callback) {
        if (raw == null || raw.trim().isEmpty()) {
            callback.onUpdate("اكتب أمراً أولاً.");
            return;
        }
        if (startBoundedRepeat(raw, callback)) return;
        String[] parts = raw.trim().split("\\s*(?:ثم|ثم بعدها|وبعدها|بعدها|بعد ذلك|then|;|\\n)\\s*", -1);
        List<String> steps = new ArrayList<>();
        for (String part : parts) if (!part.trim().isEmpty()) steps.add(part.trim());
        if (steps.isEmpty()) {
            callback.onUpdate("اكتب أمراً واضحاً.");
            return;
        }
        runStep(steps, 0, new StringBuilder(), callback);
    }

    private void runStep(List<String> steps, int index, StringBuilder log, SequenceCallback callback) {
        if (index >= steps.size()) {
            callback.onUpdate(log.toString().trim());
            return;
        }
        String result = execute(steps.get(index));
        if (log.length() > 0) log.append("\n");
        log.append("• ").append(steps.get(index)).append(": ").append(result);
        callback.onUpdate(log.toString());
        // Stop after a failed step to avoid acting on an unexpected screen.
        if (isStepFailure(result)) {
            log.append("\n⏹ توقفت السلسلة لحماية بقية الخطوات.");
            callback.onUpdate(log.toString());
            return;
        }
        if (index + 1 < steps.size()) {
            sequenceHandler.postDelayed(() -> runStep(steps, index + 1, log, callback), 1400);
        }
    }

    private boolean isStepFailure(String result) {
        return result.startsWith("لم أجد") ||
               result.startsWith("تعذّر") ||
               result.startsWith("لا أستطيع") ||
               result.startsWith("لم أفهم") ||
               result.startsWith("وجدت العنصر لكن تعذّر") ||
               result.startsWith("التطبيق الحالي لم يسمح");
    }

    private boolean startBoundedRepeat(String raw, SequenceCallback callback) {
        String normalized = normalizeCommand(raw).trim();
        Pattern repeatPattern = Pattern.compile("^كرر(?:\\s+عملية)?\\s+اكتب\\s+(.+?)\\s+(?:ثم\\s*)?و?ارسل\\s+(\\d+)\\s*(?:مرة|مرات|times)$", Pattern.CASE_INSENSITIVE);
        Matcher m = repeatPattern.matcher(normalized);
        if (!m.matches()) return false;
        String value = m.group(1).trim();
        int count;
        try { count = Integer.parseInt(m.group(2)); } catch (Exception e) { return false; }
        if (value.isEmpty() || count < 1 || count > 5) {
            callback.onUpdate("حدد نصاً وعدداً من 1 إلى 5 مرات فقط.");
            return true;
        }
        if (!isSupportedMessagingScreen()) {
            callback.onUpdate("افتح محادثة في واتساب أو تيليجرام أو تطبيق الرسائل أولاً، ثم أعد أمر التكرار.");
            return true;
        }
        repeatCancelled = false;
        final int total = count;
        final String text = value;
        final int[] done = {0};
        callback.onUpdate("بدأ التكرار: " + total + " مرات. افتح المحادثة المطلوبة أولاً.");
        Runnable[] task = new Runnable[1];
        task[0] = () -> {
            if (repeatCancelled) {
                callback.onUpdate("⏹ تم إيقاف التكرار. اكتمل " + done[0] + " من " + total + ".");
                return;
            }
            String typed = typeText(text);
            if (!typed.startsWith("تمت كتابة")) {
                callback.onUpdate("توقف التكرار: " + typed);
                return;
            }
            String sent = clickText("إرسال");
            if (!sent.startsWith("تم الضغط")) sent = clickText("Send");
            if (!sent.startsWith("تم الضغط")) {
                callback.onUpdate("توقف التكرار لأن زر الإرسال لم يُعثر عليه.");
                return;
            }
            done[0]++;
            callback.onUpdate("تم الإرسال " + done[0] + " من " + total + ".");
            if (done[0] < total && !repeatCancelled) {
                repeatRunnable = task[0];
                sequenceHandler.postDelayed(repeatRunnable, 3000);
            } else callback.onUpdate("اكتمل التكرار: " + done[0] + " من " + total + ".");
        };
        repeatRunnable = task[0];
        sequenceHandler.post(task[0]);
        return true;
    }

    private boolean isSupportedMessagingScreen() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null || root.getPackageName() == null) return false;
        String pkg = root.getPackageName().toString();
        return pkg.equals("com.whatsapp") ||
               pkg.equals("org.telegram.messenger") ||
               pkg.equals("com.google.android.apps.messaging") ||
               pkg.equals("com.android.mms");
    }

    public void stopSequence() {
        repeatCancelled = true;
        if (repeatRunnable != null) sequenceHandler.removeCallbacks(repeatRunnable);
        sequenceHandler.removeCallbacksAndMessages(null);
    }

    public String execute(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "اكتب أمراً أولاً.";
        String normalized = raw.trim();
        String lower = normalizeCommand(normalized);

        if (containsAny(lower, "افتح", "شغل", "اذهب الى", "اذهب إلى", "انتقل الى", "انتقل إلى", "روح الى", "روح على", "قم بفتح", "open ", "launch ")) {
            String app = extractAfterVerb(normalized);
            return launchApp(app);
        }
        if (containsAny(lower, "ارجع", "عودة", "رجوع", "عد للخلف", "تراجع", "back")) {
            performGlobalAction(GLOBAL_ACTION_BACK);
            return "تم تنفيذ الرجوع.";
        }
        if (containsAny(lower, "الشاشة الرئيسية", "الرئيسية", "اذهب للرئيسية", "انتقل للرئيسية", "home")) {
            performGlobalAction(GLOBAL_ACTION_HOME);
            return "تم الانتقال إلى الشاشة الرئيسية.";
        }
        if (containsAny(lower, "امسح النص", "امسح الكتابة", "clear text")) {
            return typeText("");
        }
        if (containsAny(lower, "أضف", "اضف", "append ")) {
            return appendText(extractAfterVerb(normalized));
        }
        if (containsAny(lower, "اكتب", "دوّن", "ادخل النص", "املأ", "عبئ", "type ")) {
            String text = extractAfterVerb(normalized);
            return typeText(text);
        }
        if (containsAny(lower, "اضغط", "انقر", "اختار", "اختر", "حدد", "المس", "tap ", "click ")) {
            String target = extractAfterVerb(normalized);
            return clickText(target);
        }
        if (containsAny(lower, "مرر للاسفل", "مرر للأسفل", "انزل", "اسفل", "scroll down")) {
            return scroll(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        }
        if (containsAny(lower, "مرر للاعلى", "مرر للأعلى", "اصعد", "اعلى", "scroll up")) {
            return scroll(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
        }
        return "لم أفهم هذا الجزء. جرّب: افتح يوتيوب، اكتب مرحباً، اضغط بحث، مرر للأسفل، أو ارجع.";
    }

    private String normalizeCommand(String value) {
        return value.toLowerCase(Locale.ROOT)
            .replaceAll("[\\u064B-\\u065F\\u0670]", "")
            .replace("ـ", "")
            .replace("أ", "ا").replace("إ", "ا").replace("آ", "ا")
            .replace("ى", "ي");
    }

    private boolean containsAny(String s, String... terms) {
        for (String term : terms) if (s.contains(term)) return true;
        return false;
    }

    private String extractAfterVerb(String s) {
        String[] verbs = {"اذهب الى","انتقل الى","اذهب إلى","انتقل إلى","اكتب النص","اكتب في","دوّن","أضف","اضف","append","اضغط على","انقر على","افتح","شغل","اكتب","اضغط","انقر","اختار","اختر","tap","open","type"};
        String lower = normalizeCommand(s);
        for (String verb : verbs) {
            int i = lower.indexOf(verb.toLowerCase(Locale.ROOT));
            if (i >= 0) return s.substring(i + verb.length()).trim().replaceFirst("^(على|الى|إلى|تطبيق|في خانة الكتابة|في مربع النص)\\s*", "").replaceFirst("^[：:،,؛;\\-]+\\s*", "").trim();
        }
        return s.trim();
    }

    private String launchApp(String name) {
        String n = normalizeCommand(name.replaceAll("[.!؟?]","").trim());
        String pkg = null;
        if (n.contains("واتساب") || n.contains("whatsapp")) pkg = "com.whatsapp";
        else if (n.contains("يوتيوب") || n.contains("youtube")) pkg = "com.google.android.youtube";
        else if (n.contains("متصفح") || n.contains("كروم") || n.contains("chrome")) pkg = "com.android.chrome";
        else if (n.contains("تيليجرام") || n.contains("telegram")) pkg = "org.telegram.messenger";
        else if (n.contains("فيسبوك") || n.contains("facebook")) pkg = "com.facebook.katana";
        else if (n.contains("انستغرام") || n.contains("instagram")) pkg = "com.instagram.android";
        else if (n.contains("الإعدادات") || n.contains("الاعدادات") || n.contains("settings")) pkg = "com.android.settings";
        else if (n.contains("خرائط") || n.contains("maps") || n.contains("الخريطة")) pkg = "com.google.android.apps.maps";
        else if (n.contains("جيميل") || n.contains("gmail") || n.contains("البريد")) pkg = "com.google.android.gm";
        else if (n.contains("كاميرا") || n.contains("camera")) pkg = "com.android.camera";
        else if (n.contains("رسائل") || n.contains("messages") || n.contains("الرسائل")) pkg = "com.google.android.apps.messaging";
        else if (n.contains("الهاتف") || n.contains("phone")) pkg = "com.google.android.dialer";
        else if (n.contains("الملفات") || n.contains("مدير الملفات") || n.contains("ملفاتي") || n.contains("files")) pkg = "com.google.android.documentsui";
        else if (n.contains("متجر") || n.contains("بلاي") || n.contains("play store")) pkg = "com.android.vending";
        else if (n.contains("سناب") || n.contains("snapchat")) pkg = "com.snapchat.android";
        else if (n.contains("تيك توك") || n.contains("tiktok")) pkg = "com.zhiliaoapp.musically";

        PackageManager pm = getPackageManager();
        Intent intent = pkg == null ? pm.getLaunchIntentForPackage(name) : pm.getLaunchIntentForPackage(pkg);
        if (intent == null) return "لم أجد التطبيق: " + name + ". تأكد من تثبيته.";
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        return "جارٍ فتح " + name;
    }

    private String typeText(String text) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "لا أستطيع الوصول إلى الشاشة الحالية.";
        AccessibilityNodeInfo field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (field == null || !field.isEditable()) field = findEditable(root);
        if (field == null) return "لم أجد خانة كتابة. اضغط داخل خانة النص ثم أعد الأمر.";
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
        boolean ok = field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        if (!ok && !text.isEmpty()) {
            try {
                ClipboardManager clipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("NOVA", text));
                ok = field.performAction(AccessibilityNodeInfo.ACTION_FOCUS) &&
                     field.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            } catch (Exception ignored) { }
        }
        return ok ? (text.isEmpty() ? "تم مسح النص من الخانة." : "تمت كتابة النص.") :
            "التطبيق الحالي لم يسمح بالكتابة في هذه الخانة.";
    }

    private String appendText(String extra) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "لا أستطيع الوصول إلى الشاشة الحالية.";
        AccessibilityNodeInfo field = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (field == null || !field.isEditable()) field = findEditable(root);
        if (field == null) return "لم أجد خانة كتابة. اضغط داخل خانة النص ثم أعد الأمر.";
        CharSequence current = field.getText();
        String combined = (current == null ? "" : current.toString()) + extra;
        return typeText(combined);
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
        String q = normalizeCommand(target.trim());
        if ((t != null && normalizeCommand(t.toString()).contains(q)) ||
            (d != null && normalizeCommand(d.toString()).contains(q))) return node;
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
