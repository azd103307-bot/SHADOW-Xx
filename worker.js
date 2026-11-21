const MODEL = "@cf/ibm-granite/granite-4.0-h-micro";
const SYSTEM_PROMPT = [
  "You are SHADOW AI, a helpful, accurate, and practical AI assistant.",
  "Answer in Arabic when the user writes Arabic. Otherwise answer in the user's language.",
  "Be clear and structured. Admit uncertainty and never invent sources, facts, or actions.",
  "For current facts that you cannot verify, say that they need a fresh source check.",
  "Do not ask for passwords, API keys, or sensitive account credentials.",
  "For cybersecurity requests, help with authorized defensive testing, learning, and remediation only.",
  "Keep answers concise unless the user asks for detail."
].join(" ");

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
  "Access-Control-Allow-Headers": "Content-Type, Accept",
  "Access-Control-Max-Age": "86400"
};

function json(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      ...corsHeaders,
      "Content-Type": "application/json; charset=utf-8",
      "Cache-Control": "no-store",
      "X-Content-Type-Options": "nosniff"
    }
  });
}

function cleanMessages(input) {
  if (!Array.isArray(input)) return [];
  return input.slice(-10).map((item) => {
    const role = item && (item.role === "assistant" ? "assistant" : item.role === "user" ? "user" : "");
    const content = typeof item?.content === "string" ? item.content.trim().slice(0, 2500) : "";
    return role && content ? { role, content } : null;
  }).filter(Boolean);
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders });
    }

    if (url.pathname === "/" || url.pathname === "/api/health") {
      return json({
        name: "SHADOW AI",
        status: "ok",
        aiBinding: Boolean(env.AI && typeof env.AI.run === "function"),
        model: MODEL,
        endpoint: "/api/chat"
      });
    }

    if (url.pathname !== "/api/chat") {
      return json({ error: "المسار غير موجود." }, 404);
    }

    if (request.method !== "POST") {
      return json({ error: "استخدم POST لإرسال رسالة." }, 405);
    }

    if (!env.AI || typeof env.AI.run !== "function") {
      return json({
        error: "Workers AI غير مربوط بالخادم. أضف AI Binding باسم AI في إعدادات Cloudflare Worker ثم انشره."
      }, 503);
    }

    let body;
    try {
      const raw = await request.text();
      if (raw.length > 18000) return json({ error: "حجم الطلب أكبر من المسموح." }, 413);
      body = JSON.parse(raw);
    } catch (error) {
      return json({ error: "صيغة الطلب غير صحيحة. أرسل JSON صالحاً." }, 400);
    }

    let messages = cleanMessages(body.messages);
    if (!messages.length && typeof body.message === "string" && body.message.trim()) {
      messages = [{ role: "user", content: body.message.trim().slice(0, 2500) }];
    }
    if (!messages.length || messages[messages.length - 1].role !== "user") {
      return json({ error: "اكتب رسالة قبل الإرسال." }, 400);
    }

    const totalChars = messages.reduce((sum, item) => sum + item.content.length, 0);
    if (totalChars > 10000) {
      return json({ error: "المحادثة طويلة جداً. ابدأ محادثة جديدة أو اختصر الرسائل." }, 413);
    }

    try {
      const result = await env.AI.run(MODEL, {
        messages: [{ role: "system", content: SYSTEM_PROMPT }, ...messages],
        max_tokens: 350,
        temperature: 0.55,
        top_p: 0.9
      });

      const reply = typeof result?.response === "string"
        ? result.response
        : typeof result?.result?.response === "string"
          ? result.result.response
          : typeof result?.text === "string"
            ? result.text
            : "";

      if (!reply.trim()) {
        return json({ error: "لم يُرجع النموذج نصاً. حاول مرة أخرى بسؤال أقصر." }, 502);
      }

      return json({
        reply: reply.trim(),
        model: MODEL
      });
    } catch (error) {
      const message = String(error?.message || "");
      const quota = /quota|limit|neuron|billing|exceed/i.test(message);
      return json({
        error: quota
          ? "تم استهلاك الحصة اليومية المتاحة لخدمة الذكاء الاصطناعي. أعد المحاولة بعد تجدد الحصة."
          : "تعذر تشغيل النموذج حالياً. تحقق من حالة Workers AI ثم أعد المحاولة."
      }, 502);
    }
  }
};