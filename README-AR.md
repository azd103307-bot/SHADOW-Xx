# SHADOW X Wi-Fi RECON — مشروع Android

هذا المشروع يغلّف واجهة SHADOW X الحالية داخل Android WebView.

## بنية المشروع
- `app/src/main/assets/index.html`: واجهة HTML المضمّنة.
- `app/src/main/java/com/shadowx/recon/MainActivity.java`: شاشة التطبيق.
- `app/src/main/AndroidManifest.xml`: تعريف التطبيق.
- `app/src/main/res/values/styles.xml`: المظهر.
- `app/build.gradle`: إعداد وحدة Android.
- `.github/workflows/build-apk.yml`: بناء APK عبر GitHub Actions.

## بناء APK عبر GitHub
1. ارفع **محتويات هذا المجلد** إلى جذر مستودع GitHub مع الحفاظ على المجلدات.
2. افتح تبويب Actions.
3. اختر Build SHADOW X APK ثم Run workflow، أو ادفع commit جديداً إلى main.
4. بعد نجاح التشغيل افتح Artifacts ونزّل `SHADOW-X-RECON-APK`.
5. فك ضغط Artifact وثبّت `app-debug.apk` على هاتف Android.

## حدود النسخة
هذه نسخة WebView من الواجهة، وليست تطبيقاً يلتقط حركة Wi-Fi مباشرة أو يتحكم بالراوتر. تحليل الملفات والسجلات محلي ودفاعي. ملف APK الناتج Debug للتجربة، وليس إصداراً موقعاً للنشر العام.
