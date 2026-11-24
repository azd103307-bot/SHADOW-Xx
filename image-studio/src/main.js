import { Txt2ImgWorkerClient } from "web-txt2img";

const $ = (id) => document.getElementById(id);
const promptEl = $("prompt"), styleEl = $("style");
const loadBtn = $("load"), goBtn = $("go");
const loadStatus = $("loadStatus"), statusEl = $("status");
const loadProgress = $("loadProgress"), genProgress = $("genProgress");
const deviceEl = $("device"), img = $("result"), placeholder = $("placeholder");
const download = $("download"), again = $("again");
let client = null, loaded = false, currentUrl = "", gpuAdapterInfo = "";

function progressText(p = {}) {
  const parts = [];
  if (p.message) parts.push(p.message);
  if (p.phase) parts.push(p.phase);
  if (typeof p.pct === "number") parts.push(Math.round(p.pct) + "%");
  if (typeof p.bytesDownloaded === "number" && typeof p.totalBytesExpected === "number") {
    parts.push((p.bytesDownloaded / 1048576).toFixed(0) + " / " + (p.totalBytesExpected / 1048576).toFixed(0) + " MB");
  }
  return parts.join(" · ") || "جارٍ تجهيز النموذج...";
}

// وجود navigator.gpu وحده لا يعني أن الهاتف يستطيع إنشاء جهاز GPU.
// نختبر إنشاء Adapter وDevice فعلياً قبل تنزيل ملفات النموذج الكبيرة.
async function getWorkingGpu() {
  if (!window.isSecureContext) {
    throw new Error("يلزم فتح NOVA عبر HTTPS آمن.");
  }
  if (!navigator.gpu) {
    throw new Error("هذا المتصفح لا يوفّر WebGPU. حدّث Chrome أو جرّب متصفحاً وجهازاً يدعمان WebGPU.");
  }

  let adapter = null;
  try {
    adapter = await navigator.gpu.requestAdapter({ powerPreference: "high-performance" });
  } catch {}
  if (!adapter) {
    try { adapter = await navigator.gpu.requestAdapter(); } catch {}
  }
  if (!adapter) {
    throw new Error("المتصفح لا يستطيع الوصول إلى معالج رسوميات متوافق. فعّل تسريع الرسوميات إن كان متاحاً، ثم أعد تشغيل المتصفح. لن يبدأ تنزيل النموذج قبل حل المشكلة.");
  }

  let device;
  try {
    device = await adapter.requestDevice();
  } catch (error) {
    throw new Error("تم العثور على GPU لكن تعذّر إنشاء جهاز تشغيل: " + (error?.message || "رفض المتصفح الوصول إلى GPU") + ". أغلق التطبيقات الثقيلة وأعد تشغيل Chrome.");
  }

  const info = adapter.info || {};
  gpuAdapterInfo = [info.vendor, info.architecture, info.device].filter(Boolean).join(" ") || "متصل";
  const lostPromise = device.lost;
  device.destroy();

  // إذا فُقد الجهاز فور إنشائه، نعرض تشخيصاً واضحاً بدلاً من بدء تنزيل غير مفيد.
  const lost = await Promise.race([
    lostPromise.then((reason) => reason),
    new Promise((resolve) => setTimeout(() => resolve(null), 150))
  ]);
  if (lost) {
    throw new Error("اتصال GPU انقطع أثناء الفحص. أعد تشغيل المتصفح وتأكد من عدم تفعيل وضع توفير الطاقة.");
  }
  return adapter;
}

async function inspectDevice() {
  const secure = window.isSecureContext;
  deviceEl.textContent = "الاتصال الآمن: " + (secure ? "متاح" : "غير متاح") +
    " · فحص WebGPU الحقيقي...";
  try {
    await getWorkingGpu();
    deviceEl.textContent = "الاتصال الآمن: متاح · WebGPU: يعمل · GPU: " + gpuAdapterInfo;
  } catch (error) {
    deviceEl.textContent = "WebGPU: غير جاهز · " + error.message;
  }
}
inspectDevice();

loadBtn.addEventListener("click", async () => {
  if (loaded) return;
  loadBtn.disabled = true;
  loadProgress.hidden = false;
  loadProgress.value = 0;
  loadStatus.textContent = "يتم اختبار معالج الرسوميات قبل تنزيل النموذج...";
  try {
    await getWorkingGpu();

    client = Txt2ImgWorkerClient.createDefault();
    const caps = await client.detect();
    if (!caps?.webgpu) {
      throw new Error("فحص مكتبة التوليد لم يجد WebGPU صالحاً، رغم نجاح اختبار المتصفح. حدّث Chrome وأعد المحاولة.");
    }

    loadStatus.textContent = "نجح فحص GPU. يبدأ الآن تنزيل نموذج SD-Turbo (حوالي 2.3 جيجابايت). استخدم Wi‑Fi ولا تغلق الصفحة.";
    const result = await client.load("sd-turbo", { backendPreference: ["webgpu"] }, (p) => {
      if (typeof p.pct === "number") loadProgress.value = p.pct;
      loadStatus.textContent = progressText(p);
    });
    if (!result?.ok) throw new Error(result?.message || "تعذر تحميل النموذج.");

    loaded = true;
    loadProgress.value = 100;
    loadStatus.textContent = "✓ النموذج جاهز. يمكنك الآن إنشاء الصور.";
    loadBtn.textContent = "✓ النموذج جاهز على هذا الجهاز";
    goBtn.disabled = false;
  } catch (e) {
    loadStatus.textContent = e?.message || "حدث خطأ أثناء تحميل النموذج.";
    loadBtn.disabled = false;
    loadProgress.value = 0;
    if (client) {
      try { await client.unload(); } catch {}
      client = null;
    }
  }
});

async function generate() {
  const prompt = promptEl.value.trim();
  if (!prompt) { statusEl.textContent = "اكتب وصف الصورة أولاً."; promptEl.focus(); return; }
  if (!loaded || !client) { statusEl.textContent = "حمّل النموذج أولاً."; return; }

  goBtn.disabled = true;
  again.disabled = true;
  genProgress.hidden = false;
  genProgress.removeAttribute("value");
  statusEl.textContent = "يتم إنشاء الصورة على معالج جهازك...";
  try {
    const fullPrompt = [prompt, styleEl.value].filter(Boolean).join(", ");
    const seed = Math.floor(Math.random() * 2147483647);
    const task = client.generate({ prompt: fullPrompt, seed }, (p) => {
      statusEl.textContent = progressText(p);
      if (typeof p.pct === "number") genProgress.value = p.pct;
      else genProgress.removeAttribute("value");
    });
    const result = await task.promise;
    if (!result?.ok || !result.blob) {
      throw new Error(result?.message || result?.reason || "فشل إنشاء الصورة.");
    }
    if (currentUrl) URL.revokeObjectURL(currentUrl);
    currentUrl = URL.createObjectURL(result.blob);
    img.src = currentUrl;
    img.style.display = "block";
    placeholder.style.display = "none";
    download.href = currentUrl;
    download.hidden = false;
    again.hidden = false;
    statusEl.textContent = "✓ اكتمل إنشاء الصورة محلياً خلال " + Math.round((result.timeMs || 0) / 1000) + " ثانية.";
    genProgress.value = 100;
  } catch (e) {
    statusEl.textContent = e?.message || "حدث خطأ أثناء التوليد. حاول مرة أخرى.";
  } finally {
    goBtn.disabled = false;
    again.disabled = false;
  }
}
goBtn.addEventListener("click", generate);
again.addEventListener("click", generate);
download.addEventListener("click", () => { download.download = "nova-ai-" + Date.now() + ".png"; });

if ("serviceWorker" in navigator && location.protocol.startsWith("http")) {
  window.addEventListener("load", () => navigator.serviceWorker.register("./sw.js").catch(() => {}));
}
