// Azooz AI Academy Cloudflare Worker.
// Keep API credentials in Cloudflare Secrets, never in source control.
// Before public release, add authentication, rate limiting, and spending limits.
const supported = new Set(["HTML","CSS","JavaScript","Python","Kotlin","Swift","Luau (Roblox)","Java","C++","C#","Dart","PHP","SQL","Rust","Go","TypeScript","لغة أخرى"]);

function respond(data, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {"content-type":"application/json; charset=utf-8","cache-control":"no-store"}
  });
}

// Only expose predefined diagnostics; never forward provider error bodies or tokens.
function providerFailure(status) {
  const known = {
    400: "AI request rejected (400)",
    401: "OpenAI authentication failed (401). Check the API key.",
    403: "OpenAI access denied (403). Check project permissions.",
    404: "AI model or endpoint unavailable (404).",
    429: "OpenAI rate limit or quota reached (429). Check API billing and limits."
  };
  return known[status] || (status >= 500 ? "OpenAI service temporarily unavailable (" + status + ")" : "AI service rejected request (" + status + ")");
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    if (url.pathname !== "/chat") return respond({error:"Not found"},404);
    if (request.method !== "POST") return respond({error:"POST required"},405);
    if (!env.OPENAI_API_KEY) return respond({error:"AI key not configured"},503);
    if (Number(request.headers.get("content-length") || 0) > 12000) return respond({error:"Payload too large"},413);
    let data;
    try { data = await request.json(); } catch { return respond({error:"Invalid JSON"},400); }
    if (!data || typeof data !== "object" || Array.isArray(data)) return respond({error:"Invalid request"},400);
    const language = String(data.language || "").slice(0,80);
    const level = String(data.level || "").slice(0,40);
    const message = String(data.message || "").trim();
    if (!supported.has(language) || !["مبتدئ","متوسط","متقدم"].includes(level) || !message || message.length > 6000)
      return respond({error:"Invalid request"},400);

    const instructions = `أنت مدرس برمجة عربي في Azooz AI Academy. اللغة: ${language}. المستوى: ${level}. اشرح بالتدرج مع مثال صغير. عند طلب اختبار اطرح سؤالًا واحدًا ولا تكشف الإجابة قبل محاولة المتعلم. إذا أخطأ المتعلم، وضح الخطأ وأعد تدريبه قبل الانتقال. لا تدعِ تنفيذ الكود. صحح الكود بحذر ووضوح.`;
    try {
      const api = await fetch("https://api.openai.com/v1/chat/completions", {
        method:"POST",
        headers:{"Authorization":`Bearer ${env.OPENAI_API_KEY}`,"Content-Type":"application/json"},
        body:JSON.stringify({
          model:env.OPENAI_MODEL || "gpt-4o-mini",
          max_tokens:900,
          messages:[{role:"system",content:instructions},{role:"user",content:message}]
        })
      });
      if (!api.ok) {
        console.error("OpenAI request failed", {upstreamStatus:api.status});
        return respond({error:providerFailure(api.status)},502);
      }
      const body = await api.json();
      return respond({reply:body.choices?.[0]?.message?.content || "لم يصل رد"});
    } catch (error) {
      console.error("AI request transport failure", {kind:error instanceof TypeError ? "network" : "processing"});
      return respond({error:"Network or response processing error"},502);
    }
  }
};
