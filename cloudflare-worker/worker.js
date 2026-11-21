const cors = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type",
  "Access-Control-Max-Age": "86400"
};
const json = (data, status = 200) => new Response(JSON.stringify(data), {
  status, headers: { ...cors, "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store" }
});
export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (request.method === "OPTIONS") return new Response(null, { status: 204, headers: cors });
    if (request.method === "GET" && url.pathname === "/health")
      return json({ ok: true, service: "SHADOW AI", keyConfigured: Boolean(env.GROQ_API_KEY) });
    if (request.method !== "POST" || url.pathname !== "/chat") return json({ error: "Not found" }, 404);
    if (!env.GROQ_API_KEY) return json({ error: "لم يتم إعداد GROQ_API_KEY في Cloudflare Worker." }, 503);
    let body;
    try { body = await request.json(); } catch { return json({ error: "طلب JSON غير صالح." }, 400); }
    const message = typeof body.message === "string" ? body.message.trim() : "";
    if (!message) return json({ error: "اكتب رسالة أولاً." }, 400);
    if (message.length > 4000) return json({ error: "الرسالة طويلة جداً؛ الحد 4000 حرف." }, 413);
    const history = Array.isArray(body.history) ? body.history.slice(-10).filter(x =>
      x && ["user", "assistant"].includes(x.role) && typeof x.content === "string"
    ).map(x => ({ role: x.role, content: x.content.slice(0, 4000) })) : [];
    try {
      const upstream = await fetch("https://api.groq.com/openai/v1/chat/completions", {
        method: "POST",
        headers: { "Authorization": `Bearer ${env.GROQ_API_KEY}`, "Content-Type": "application/json" },
        body: JSON.stringify({
          model: "llama-3.3-70b-versatile",
          messages: [
            { role: "system", content: "أنت SHADOW AI، مساعد عربي عملي. ساعد في التعلم والبرمجة والأمن السيبراني الدفاعي المصرح به فقط. لا تساعد على اختراق حسابات أو أجهزة أو شبكات دون إذن، ولا تطلب أسرار المستخدم أو مفاتيح API. كن واضحاً ومختصراً." },
            ...history, { role: "user", content: message }
          ],
          temperature: 0.5, max_tokens: 1200
        })
      });
      const result = await upstream.json().catch(() => ({}));
      if (!upstream.ok) return json({ error: result?.error?.message || `Groq API error ${upstream.status}` }, 502);
      const reply = result?.choices?.[0]?.message?.content;
      if (typeof reply !== "string" || !reply.trim()) return json({ error: "لم يرجع النموذج إجابة صالحة." }, 502);
      return json({ reply });
    } catch (_) { return json({ error: "تعذر الوصول إلى خدمة الذكاء الاصطناعي حالياً." }, 502); }
  }
};
