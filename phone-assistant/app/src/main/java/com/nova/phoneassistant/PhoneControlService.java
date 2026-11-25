package com.nova.phoneassistant;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityNodeInfo;
import android.content.Intent;
import android.content.ClipboardManager;
import android.content.ClipData;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
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
    private volatile boolean sequenceCancelled = false;
    private volatile long screenRevision = 0;

    public interface SequenceCallback {
        void onUpdate(String message);
    }

    @Override public void onServiceConnected() { instance = this; }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        screenRevision++;
    }
    @Override public void onInterrupt() { }
    @Override public void onDestroy() {
        sequenceHandler.removeCallbacksAndMessages(null);
        if (instance == this) instance = null;
        super.onDestroy();
    }

    // تقسيم الطلب إلى خطة أوامر. يدعم الفواصل العربية وصيغ العطف الشائعة.
    public void executeSequence(String raw, SequenceCallback callback) {
        if (raw == null || raw.trim().isEmpty()) {
            callback.onUpdate("اكتب أمراً أولاً.");
            return;
        }
        stopSequence();
        sequenceCancelled = false;
        if (startBoundedRepeat(raw, callback)) return;

        List<String> steps = buildPlan(raw);
        if (steps.isEmpty()) {
            callback.onUpdate("لم أستطع تحويل النص إلى خطوات. اكتب مثلاً: افتح يوتيوب ثم ابحث عن القطط.");
            return;
        }
        callback.onUpdate("فهمت المهمة إلى " + steps.size() + " خطوة. بدأ التنفيذ...");
        runStep(steps, 0, new StringBuilder(), callback, 0);
    }

    private List<String> buildPlan(String raw) {
        List<String> result = new ArrayList<>();
        String[] chunks = raw.trim().split("(?i)\\s*(?:ثم\\s+بعدها|ثم|وبعدها|بعدها|بعد ذلك|;|\\n)\\s*");
        Pattern conjunction = Pattern.compile("(?i)\\s+و(?=(?:افتح|فتح|شغل|تشغيل|ابحث|بحث|اكتب|دوّن|اضف|أضف|اضغط|انقر|اختر|اختار|حدد|اقرأ|اقرا|مرر|اسحب|انتقل|اذهب|ارجع|انتظر|خذ لقطة))");
        for (String chunk : chunks) {
            Matcher matcher = conjunction.matcher(chunk.trim());
            int start = 0;
            while (matcher.find()) {
                String part = chunk.substring(start, matcher.start()).trim();
                if (!part.isEmpty()) result.add(part);
                start = matcher.end();
            }
            String tail = chunk.substring(start).trim();
            if (!tail.isEmpty()) result.add(tail);
        }
        return result;
    }

    private void runStep(List<String> steps, int index, StringBuilder log,
            SequenceCallback callback, int attempt) {
        if (sequenceCancelled) {
            callback.onUpdate(log.toString().trim() + "\\n⏹ أوقف المستخدم التنفيذ.");
            return;
        }
        if (index >= steps.size()) {
            callback.onUpdate("اكتملت المهمة.\\n" + log.toString().trim());
            return;
        }

        String command = steps.get(index);
        long waitMillis = parseWaitMillis(command);
        if (waitMillis > 0) {
            String line = "• " + command + ": انتظار " + (waitMillis / 1000) + " ثانية.";
            if (log.length() > 0) log.append("\\n");
            log.append(line);
            callback.onUpdate(log.toString());
            sequenceHandler.postDelayed(
                () -> runStep(steps, index + 1, log, callback, 0), waitMillis);
            return;
        }

        String result = execute(command);
        if (isStepFailure(result) && attempt < 2 && !isPermanentFailure(result)) {
            callback.onUpdate("الخطوة " + (index + 1) + " لم تكتمل بعد. أنتظر تحديث الشاشة وأعيد المحاولة (" + (attempt + 1) + "/2)...");
            sequenceHandler.postDelayed(
                () -> runStep(steps, index, log, callback, attempt + 1), 950);
            return;
        }

        if (log.length() > 0) log.append("\\n");
        log.append("• ").append(command).append(": ").append(result);
        callback.onUpdate(log.toString());

        if (isStepFailure(result)) {
            log.append("\\n⏹ توقفت بقية الخطوات حتى لا أنفذها على شاشة غير متوقعة.");
            callback.onUpdate(log.toString());
            return;
        }

        if (index + 1 < steps.size()) {
            long delay = result.startsWith("جارٍ فتح") ? 1900 : 850;
            sequenceHandler.postDelayed(
                () -> runStep(steps, index + 1, log, callback, 0), delay);
        } else {
            callback.onUpdate("اكتملت المهمة.\\n" + log.toString().trim());
        }
    }

    private long parseWaitMillis(String command) {
        String x = normalizeCommand(command).trim();
        Matcher m = Pattern.compile("^انتظر(?: لمدة)?\\s+(\\d+)\\s*(ثانية|ثوان|ثانيه|دقيقة|دقائق|دقيقه)$").matcher(x);
        if (!m.matches()) return 0;
        try {
            long amount = Long.parseLong(m.group(1));
            amount = Math.max(1, Math.min(amount, 30));
            if (m.group(2).startsWith("دقيق")) amount *= 60;
            return amount * 1000;
        } catch (Exception ignored) { return 0; }
    }

    private boolean isPermanentFailure(String result) {
        return result.startsWith("لم أجد تطبيقاً") ||
               result.startsWith("لا أستطيع") && result.contains("صلاحية");
    }

    private boolean isStepFailure(String result) {
        return result.startsWith("لم أجد") ||
               result.startsWith("تعذّر") ||
               result.startsWith("لا أستطيع") ||
               result.startsWith("لم أفهم") ||
               result.startsWith("وجدت العنصر لكن تعذّر") ||
               result.startsWith("التطبيق الحالي لم يسمح") ||
               result.startsWith("لا يمكن تنفيذ");
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
        sequenceCancelled = true;
        if (repeatRunnable != null) sequenceHandler.removeCallbacks(repeatRunnable);
        sequenceHandler.removeCallbacksAndMessages(null);
    }

    public String execute(String raw) {
        if (raw == null || raw.trim().isEmpty()) return "اكتب أمراً أولاً.";
        String normalized = raw.trim();
        String lower = normalizeCommand(normalized);

        if (isScreenReadCommand(lower)) return readCurrentScreen();

        if (containsAny(lower, "ابحث عن", "ابحث في", "بحث عن", "search for")) {
            return searchFor(extractSearchQuery(normalized));
        }
        if (containsAny(lower, "اول نتيجة", "أول نتيجة", "اول فيديو", "أول فيديو", "اول عنصر", "أول عنصر", "first result")) {
            return clickFirstResult();
        }
        if (containsAny(lower, "افتح الرابط", "اذهب الى الرابط", "انتقل الى الرابط", "open link")) {
            return openLink(extractAfterVerb(normalized));
        }
        if (containsAny(lower, "ارفع الصوت", "زود الصوت", "زيادة الصوت", "volume up")) {
            return adjustVolume(AudioManager.ADJUST_RAISE);
        }
        if (containsAny(lower, "اخفض الصوت", "خفض الصوت", "قلل الصوت", "volume down")) {
            return adjustVolume(AudioManager.ADJUST_LOWER);
        }
        if (containsAny(lower, "اكتم الصوت", "كتم الصوت", "صامت", "mute")) {
            return adjustVolume(AudioManager.ADJUST_MUTE);
        }
        if (containsAny(lower, "لقطة شاشة", "التقط الشاشة", "صور الشاشة", "screenshot")) {
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                boolean ok = performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
                return ok ? "تم طلب التقاط لقطة شاشة." : "تعذّر التقاط لقطة الشاشة.";
            }
            return "لقطة الشاشة تتطلب Android 9 أو أحدث.";
        }
        if (containsAny(lower, "اقفل الشاشة", "قفل الشاشة", "اقفل الهاتف", "lock screen")) {
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                boolean ok = performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
                return ok ? "تم قفل الشاشة." : "تعذّر قفل الشاشة.";
            }
            return "قفل الشاشة من هذه الخدمة يتطلب Android 9 أو أحدث.";
        }
        if (containsAny(lower, "افتح", "فتح ", "شغل", "تشغيل ", "اذهب الى", "انتقل الى", "روح الى", "روح على", "قم بفتح", "قم بتشغيل", "open ", "launch ")) {
            String app = extractAfterVerb(normalized);
            return launchApp(app);
        }
        if (containsAny(lower, "ارجع", "عودة", "رجوع", "عد للخلف", "تراجع", "اخف لوحة المفاتيح", "اغلق لوحة المفاتيح", "back")) {
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
            return typeText(extractTextPayload(extractAfterVerb(normalized)));
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
        return "لم أفهم هذا الجزء. مثال: افتح يوتيوب ثم ابحث عن القطط ثم افتح أول نتيجة.";
    }

    private String extractSearchQuery(String command) {
        String lower = normalizeCommand(command);
        String[] markers = {"ابحث عن", "بحث عن", "ابحث في", "search for"};
        for (String marker : markers) {
            int i = lower.indexOf(marker);
            if (i >= 0) return command.substring(i + marker.length()).trim()
                .replaceFirst("^(عن|في)\\s*", "").trim();
        }
        return "";
    }

    private String extractTextPayload(String text) {
        return text.replaceFirst("\\s+(?:في خانة الكتابة|في مربع النص|في خانة البحث|في مربع البحث)$", "").trim();
    }

    private String searchFor(String query) {
        if (query == null || query.trim().isEmpty()) return "اكتب ما تريد البحث عنه بعد عبارة: ابحث عن.";
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "لا أستطيع الوصول إلى الشاشة الحالية.";
        AccessibilityNodeInfo field = findSearchEditable(root);
        if (field == null) field = findEditable(root);
        if (field == null) return "لم أجد خانة بحث أو كتابة في هذه الشاشة.";
        field.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        Bundle args = new Bundle();
        args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, query);
        boolean typed = field.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        if (!typed) return "التطبيق الحالي لم يسمح بالكتابة في خانة البحث.";
        boolean submitted = false;
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            submitted = field.performAction(AccessibilityNodeInfo.ACTION_IME_ENTER);
        }
        if (!submitted) {
            String clicked = clickText("بحث");
            if (!clicked.startsWith("تم الضغط")) clicked = clickText("Search");
            if (!clicked.startsWith("تم الضغط")) clicked = clickText("انتقال");
            submitted = clicked.startsWith("تم الضغط");
        }
        return submitted ? "تم البحث عن: " + query : "كتبت عبارة البحث، لكن لم أجد زر بحث لإرسالها. اضغط بحث على لوحة المفاتيح.";
    }

    private AccessibilityNodeInfo findSearchEditable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isEditable() && node.isEnabled()) {
            String hint = "";
            CharSequence desc = node.getContentDescription();
            if (desc != null) hint = desc.toString();
            if (android.os.Build.VERSION.SDK_INT >= 26 && node.getHintText() != null) {
                hint += " " + node.getHintText().toString();
            }
            String id = "";
            try { id = node.getViewIdResourceName(); } catch (Exception ignored) { }
            String searchable = normalizeCommand(hint + " " + id);
            if (containsAny(searchable, "بحث", "search", "query", "find")) return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findSearchEditable(node.getChild(i));
            if (found != null) return found;
        }
        return null;
    }

    private String clickFirstResult() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "لا أستطيع قراءة عناصر الشاشة الحالية.";
        AccessibilityNodeInfo candidate = findFirstMeaningfulClickable(root, 0);
        if (candidate == null) return "لم أجد نتيجة قابلة للفتح في هذه الشاشة.";
        CharSequence label = candidate.getText();
        String name = label == null ? "أول عنصر قابل للضغط" : label.toString();
        boolean ok = candidate.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        return ok ? "تم فتح: " + name : "وجدت أول نتيجة لكن تعذّر فتحها.";
    }

    private AccessibilityNodeInfo findFirstMeaningfulClickable(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 10) return null;
        CharSequence label = node.getText();
        String value = label == null ? "" : normalizeCommand(label.toString());
        if (node.isClickable() && !value.isEmpty() &&
            !containsAny(value, "بحث", "search", "الرئيسية", "home", "رجوع", "back", "اعلان", "ad")) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findFirstMeaningfulClickable(node.getChild(i), depth + 1);
            if (found != null) return found;
        }
        return null;
    }

    private String openLink(String rawUrl) {
        String candidate = rawUrl == null ? "" : rawUrl.trim();
        if (candidate.isEmpty()) return "اكتب الرابط بعد عبارة افتح الرابط.";
        if (!candidate.matches("(?i)^https?://[^\\s]+$")) return "الرابط غير صالح. استخدم رابطاً يبدأ بـ https:// أو http://.";
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(candidate));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return "جارٍ فتح الرابط.";
        } catch (Exception e) { return "تعذّر فتح الرابط على هذا الهاتف."; }
    }

    private String adjustVolume(int direction) {
        try {
            AudioManager audio = (AudioManager)getSystemService(AUDIO_SERVICE);
            if (audio == null) return "تعذّر الوصول إلى إعداد الصوت.";
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI);
            return "تم تعديل مستوى صوت الوسائط.";
        } catch (Exception e) { return "تعذّر تعديل الصوت."; }
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
        String[] verbs = {"قم بتشغيل","قم بفتح","اذهب الى","انتقل الى","روح الى","روح على","ادخل الى","اكتب النص","اكتب في","دوّن","اضف","append","اضغط على","انقر على","افتح","فتح","شغل","تشغيل","اكتب","اضغط","انقر","اختار","اختر","tap","open","launch","type"};
        String lower = normalizeCommand(s);
        for (String verb : verbs) {
            int i = lower.indexOf(verb.toLowerCase(Locale.ROOT));
            if (i >= 0) return s.substring(i + verb.length()).trim().replaceFirst("^(على|الى|تطبيق|في خانة الكتابة|في مربع النص|في خانة البحث|في مربع البحث|في البحث|لي)\\s*", "").replaceFirst("^[：:،,؛;\\-]+\\s*", "").trim();
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

        else if (n.equals("x") || n.contains("تويتر") || n.contains("twitter")) pkg = "com.twitter.android";
        else if (n.contains("ديسكورد") || n.contains("discord")) pkg = "com.discord";
        else if (n.contains("سبوتيفاي") || n.contains("spotify")) pkg = "com.spotify.music";
        else if (n.contains("جوجل") || n.equals("google")) pkg = "com.google.android.googlequicksearchbox";
        else if (n.contains("متصفح سامسونج") || n.contains("samsung internet")) pkg = "com.sec.android.app.sbrowser";

        PackageManager pm = getPackageManager();
        Intent intent = pkg == null ? null : pm.getLaunchIntentForPackage(pkg);
        if (intent == null) {
            String discovered = findInstalledAppPackage(n);
            if (discovered != null) intent = pm.getLaunchIntentForPackage(discovered);
        }
        if (intent == null) return "لم أجد تطبيقاً مطابقاً للاسم: " + name + ". تأكد من تثبيته أو اكتب اسمه كما يظهر في الهاتف.";
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        return "جارٍ فتح " + name;
    }


    private boolean isScreenReadCommand(String lower) {
        return containsAny(lower, "اقرا الشاشة", "اقرا ما على الشاشة", "اقرا محتوى الشاشة",
            "ماذا يظهر على الشاشة", "ما الذي يظهر على الشاشة", "ماذا يوجد على الشاشة",
            "ما الموجود على الشاشة", "صف الشاشة", "اقرا الازرار", "اقرا النص الظاهر",
            "اقرا هذه الصفحة", "read screen", "describe screen", "what is on screen");
    }

    private String readCurrentScreen() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return "لا أستطيع قراءة الشاشة الآن. افتح التطبيق المطلوب ثم أعد الأمر.";
        String packageName = root.getPackageName() == null ? "" : root.getPackageName().toString();
        String appLabel = packageName;
        try {
            android.content.pm.ApplicationInfo info = getPackageManager().getApplicationInfo(packageName, 0);
            appLabel = getPackageManager().getApplicationLabel(info).toString();
        } catch (Exception ignored) { }
        StringBuilder out = new StringBuilder("الشاشة الحالية: ").append(appLabel).append("\n");
        int[] count = {0};
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        appendVisibleNodes(root, out, seen, count, 0);
        if (count[0] == 0) out.append("لم يعرض التطبيق نصوصاً قابلة للقراءة. قد تكون الشاشة رسومية أو محمية.");
        if (out.length() > 2600) out.setLength(2600);
        if (out.length() >= 2598) out.append("\n… تم اختصار بقية المحتوى.");
        return out.toString();
    }

    private void appendVisibleNodes(AccessibilityNodeInfo node, StringBuilder out,
            java.util.Set<String> seen, int[] count, int depth) {
        if (node == null || depth > 12 || count[0] >= 38 || out.length() >= 2450) return;
        String text = "";
        if (node.isPassword()) {
            if (node.isEditable()) text = "[حقل كلمة مرور مخفي]";
        } else {
            CharSequence label = node.getText();
            CharSequence description = node.getContentDescription();
            if (label != null) text = label.toString().trim();
            if (text.isEmpty() && description != null) text = description.toString().trim();
        }
        if (!text.isEmpty()) {
            String key = normalizeCommand(text);
            if (!key.isEmpty() && seen.add(key)) {
                String kind = node.isEditable() ? "حقل إدخال" : (node.isClickable() ? "عنصر قابل للضغط" : "نص");
                out.append("• ").append(kind).append(": ").append(text).append("\n");
                count[0]++;
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            if (count[0] >= 38 || out.length() >= 2450) break;
            appendVisibleNodes(node.getChild(i), out, seen, count, depth + 1);
        }
    }

    private String findInstalledAppPackage(String wanted) {
        if (wanted == null || wanted.trim().isEmpty()) return null;
        try {
            Intent launcher = new Intent(Intent.ACTION_MAIN, null);
            launcher.addCategory(Intent.CATEGORY_LAUNCHER);
            List<android.content.pm.ResolveInfo> apps = getPackageManager().queryIntentActivities(launcher, 0);
            String bestPackage = null;
            int bestScore = 0;
            for (android.content.pm.ResolveInfo app : apps) {
                if (app.activityInfo == null) continue;
                String candidate = normalizeCommand(app.loadLabel(getPackageManager()).toString());
                if (candidate.isEmpty()) continue;
                int score = 0;
                if (candidate.equals(wanted)) score = 100;
                else if (candidate.startsWith(wanted) || wanted.startsWith(candidate)) score = 70;
                else if (candidate.contains(wanted) || wanted.contains(candidate)) score = 50;
                if (score > bestScore) { bestScore = score; bestPackage = app.activityInfo.packageName; }
            }
            return bestScore >= 50 ? bestPackage : null;
        } catch (Exception ignored) { return null; }
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
        if (q.isEmpty()) return null;
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
