# تشغيل MANSER AI على GitHub Codespaces

هذه بيئة تجربة مؤقتة وليست استضافة دائمة. حساب GitHub Free لديه حصة شهرية محدودة لساعات Codespaces والتخزين؛ راجع صفحة الفوترة في حسابك قبل التشغيل. لا تختَر آلة مدفوعة أو ترفع حد الإنفاق.

## التشغيل من الهاتف

1. افتح مستودع MANSER: https://github.com/azd103307-bot/SHADOW-Xx
2. اضغط Code ثم Codespaces ثم Create codespace on main.
3. انتظر إعداد البيئة. ملف devcontainer يثبت Python 3.11 وFFmpeg وImageMagick ويجهز MoneyPrinterTurbo.
4. افتح Terminal داخل Codespace ونفّذ:

    cd ~/MoneyPrinterTurbo
    nano config.toml

أدخل مفاتيح مزودي الخدمات التي تملكها في أماكنها داخل الملف، ثم احفظ. لا تضع المفاتيح في GitHub أو ترسلها في المحادثة. بعض مزودي الصوت والنماذج والوسائط يفرضون رسوماً مستقلة.

5. شغّل واجهة API:

    cd ~/MoneyPrinterTurbo
    uv run python main.py

6. افتح تبويب Ports في Codespaces. يجب أن يظهر المنفذ 8080. افتح عنوانه HTTPS، ثم أضف /docs لاختبار الواجهة.
7. انسخ عنوان HTTPS الأساسي (من دون /docs) وضعه في إعدادات الخادم داخل تطبيق MANSER.

## تنبيهات

- اترك Visibility على Private ولا تشارك رابط المنفذ مع الآخرين.
- لا تشارك config.toml أو مفاتيح API.
- أوقف Codespace عندما تنتهي. التوقف عند الخمول والحصة الشهرية يحددان مدة التشغيل.
- لا يوجد ضمان بأن التوليد مجاني بالكامل؛ تكاليف مزودي الذكاء الاصطناعي والمواد والصوت منفصلة.
