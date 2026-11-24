import { Txt2ImgWorkerClient } from "web-txt2img";

const $ = (id) => document.getElementById(id);
const promptEl = $("prompt"), styleEl = $("style");
const loadBtn = $("load"), goBtn = $("go");
const loadStatus = $("loadStatus"), statusEl = $("status");
const loadProgress = $("loadProgress"), genProgress = $("genProgress");
const deviceEl = $("device"), img = $("result"), placeholder = $("placeholder");
const download = $("download"), again = $("again");
let client = null, loaded = false, currentUrl = "";

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

async function inspectDevice() {
  const secure = window.isSecureContext;
  const hasGpu = !!navigator.gpu;
  deviceEl.textContent = "الاتصال الآمن: " + (secure ? "متاح" : "غير متاح") +
    " · WebGPU: " + (hasGpu ? "متاح مبدئياً" : "غير مدعوم في هذا المتصفح");
  if (!secure) deviceEl.textContent += " · افتح الموقع عبر HTTPS.";
  if (hasGpu) {
    try {
      const adapter = await navigator.gpu.requestAdapter();
      if (!adapter) throw new Error("لم يتم العثور على معالج رسوميات متوافق");
      const info = adapter.info || {};
      const label = [info.vendor, info.architecture].filter(Boolean).join(" ");
      if (label) deviceEl.textContent += " · GPU: " + label;
    } catch (e) {
      deviceEl.textContent += " · تعذّر الوصول إلى GPU.";
    }
  }
}
inspectDevice();

loadBtn.addEventListener("click", async () => {
  if (loaded) return;
  loadBtn.disabled = true;
  loadProgress.hidden = false;
  loadProgress.value = 0;
  loadStatus.textContent = "جارٍ التحقق من دعم WebGPU...";
  try {
    if (!window.isSecureContext) throw new Error("يلزم فتح الموقع عبر اتصال HTTPS آمن.");
    if (!navigator.gpu) throw new Error("هذا المتصفح لا يدعم WebGPU. جرّب Chrome محدثاً على جهاز متوافق.");
    client = Txt2ImgWorkerClient.createDefault();
    const caps = await client.detect();
    if (!caps.webgpu) throw new Error("WebGPU غير متاح على هذا الجهاز.");
    loadStatus.textContent = "يتم تنزيل نموذج SD-Turbo لأول مرة. الحجم التقريبي 2.3 جيجابايت؛ لا تغلق الصفحة أثناء التنزيل.";
    const result = await client.load("sd-turbo", { backendPreference: ["webgpu"] }, (p) => {
      if (typeof p.pct === "number") loadProgress.value = p.pct;
      loadStatus.textContent = progressText(p);
    });
    if (!result?.ok) throw new Error(result?.message || "تعذر تحميل النموذج.");
    loaded = true;
    loadProgress.value = 100;
    loadStatus.textContent = "✓ النموذج جاهز. سيحاول المتصفح الاحتفاظ بملفاته محلياً.";
    loadBtn.textContent = "✓ النموذج جاهز على هذا الجهاز";
    goBtn.disabled = false;
  } catch (e) {
    loadStatus.textContent = e?.message || "حدث خطأ أثناء تحميل النموذج.";
    loadBtn.disabled = false;
    if (client) { try { await client.unload(); } catch {} client = null; }
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
    if (!result?.ok || !result.blob) throw new Error(result?.message || result?.reason || "فشل إنشاء الصورة.");
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
