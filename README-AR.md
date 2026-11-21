# SHADOW AI / SHADOW-X

## محتويات المشروع
- \`index.html\`: واجهة عربية للمحادثة والبحث.
- \`worker.js\`: خادم المحادثة الذي يستدعي Cloudflare Workers AI.
- \`wrangler.toml\`: إعداد نشر Worker وربط AI باسم \`AI\`.
- \`app.zip\`: مشروع Android الأصلي الذي يستخدمه مسار بناء APK.
- \`.github/workflows/main.yml\`: يبني APK ويضع آخر نسخة من \`index.html\` داخل أصول Android.
- \`.github/workflows/deploy.yml\`: ينشر واجهة الويب عبر GitHub Pages.

## تشغيل الذكاء الاصطناعي على Cloudflare
1. افتح Worker باسم \`shadow-ai\` في Cloudflare.
2. افتح Edit Code واستبدل كود Worker بمحتوى الملف \`worker.js\` ثم Deploy.
3. افتح Settings ثم Bindings وأضف Workers AI Binding.
4. اجعل اسم المتغير (Variable name) هو \`AI\`، ثم احفظ وانشر التغييرات.
5. اختبر العنوان \`https://shadow-ai.azd103307.workers.dev/api/health\`. يجب أن يظهر \`"aiBinding": true\`.
6. افتح التطبيق، ثم الإعدادات، واضغط اختبار الاتصال.

لا يحتاج هذا الربط إلى وضع API key داخل HTML أو Android. لا ترسل مفاتيح حساب Cloudflare إلى المحادثة أو المستودع.

## الحصة والتكلفة
Workers AI يقدم حصة مجانية يومية محدودة (10,000 Neurons يومياً حسب صفحة التسعير وقت إعداد هذا الإصدار). ليست الحصة غير محدودة؛ إذا استُهلكت، قد تتوقف الردود حتى إعادة تعيينها أو يتطلب الاستخدام الإضافي خطة مدفوعة. راقب الاستهلاك من لوحة Cloudflare.

## بناء تطبيق Android
1. ادفع التغييرات إلى فرع \`main\`.
2. افتح تبويب Actions في GitHub.
3. افتح سير عمل SHADOW-X Android Build وانتظر نجاحه.
4. من نتيجة التشغيل افتح Artifacts وحمّل \`SHADOW-X-APK\`.
5. فك الضغط وثبّت ملف APK على هاتف Android.

## ملاحظات
- ملف APK الناتج Debug للتجربة، وليس إصداراً موقعاً للنشر في متجر.
- البحث العام يستخدم Wikipedia وCrossref وOpenAlex، وقد يتعذر مصدر مؤقتاً.
- لا ترسل كلمات مرور أو مفاتيح API أو بيانات شخصية حساسة إلى المساعد.
