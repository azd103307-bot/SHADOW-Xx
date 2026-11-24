export default async function handler(req, res) {
  if (req.method !== "POST") return res.status(405).json({ error: "طريقة الطلب غير مدعومة." });
  const key = process.env.POLLINATIONS_API_KEY;
  if (!key) return res.status(503).json({ error: "لم يتم ربط مفتاح خدمة توليد الصور بعد. أضف POLLINATIONS_API_KEY في إعدادات Vercel." });
  try {
    const { prompt, style = "", width = 1024, height = 1024, quality = "standard" } = req.body || {};
    if (typeof prompt !== "string" || !prompt.trim()) return res.status(400).json({ error: "اكتب وصف الصورة أولاً." });
    if (prompt.length > 1200) return res.status(400).json({ error: "الوصف طويل جداً." });
    const allowed = new Set(["1024x1024", "1024x1536", "1536x1024"]);
    const size = `${Number(width)}x${Number(height)}`;
    if (!allowed.has(size)) return res.status(400).json({ error: "أبعاد الصورة غير مدعومة." });
    const fullPrompt = [prompt.trim(), style ? `Art style: ${style}` : ""].filter(Boolean).join(", ");
    const model = quality === "high" ? "black-forest-labs/flux.2-pro" : "black-forest-labs/flux.1-schnell";
    const url = `https://gen.pollinations.ai/image/${encodeURIComponent(fullPrompt)}?model=${encodeURIComponent(model)}&width=${width}&height=${height}`;
    const upstream = await fetch(url, { headers: { Authorization: `Bearer ${key}` } });
    if (!upstream.ok) {
      const detail = await upstream.text().catch(() => "");
      return res.status(upstream.status).json({ error: upstream.status === 402 ? "رصيد خدمة الصور غير كافٍ." : upstream.status === 401 ? "مفتاح الخدمة غير صحيح." : "فشلت خدمة توليد الصور. تحقق من المفتاح أو حاول لاحقاً.", detail: detail.slice(0, 300) });
    }
    const contentType = upstream.headers.get("content-type") || "image/png";
    if (!contentType.startsWith("image/")) return res.status(502).json({ error: "لم تُرجع الخدمة ملف صورة." });
    res.setHeader("Content-Type", contentType);
    res.setHeader("Cache-Control", "no-store");
    const buffer = Buffer.from(await upstream.arrayBuffer());
    return res.status(200).send(buffer);
  } catch (error) {
    return res.status(500).json({ error: "حدث خطأ داخلي أثناء إنشاء الصورة." });
  }
}