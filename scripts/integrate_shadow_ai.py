from pathlib import Path

path = Path("app/src/main/assets/index.html")
if not path.is_file():
    raise SystemExit("Missing app web asset: " + str(path))
html = path.read_text(encoding="utf-8")
if "shadow-ai-dock" in html:
    print("SHADOW AI integration already present")
    raise SystemExit(0)

integration = r"""
<style id="shadow-ai-style">
#shadow-ai-launch{position:fixed;z-index:99990;left:16px;bottom:18px;border:1px solid #39dce8;border-radius:50px;background:#092b40;color:#8ffaff;padding:13px 18px;font-weight:700;box-shadow:0 5px 22px #0008}
#shadow-ai-dock{display:none;position:fixed;z-index:99999;inset:0;background:#06111fee;padding:12px;align-items:flex-end;justify-content:center;font-family:Arial,sans-serif;color:#eaf4ff}
#shadow-ai-dock.open{display:flex}
#shadow-ai-card{width:min(100%,680px);height:min(78vh,720px);background:#0b1b2e;border:1px solid #24536b;border-radius:18px;display:flex;flex-direction:column;overflow:hidden;box-shadow:0 10px 40px #000a}
#shadow-ai-head{padding:13px 15px;background:#102b42;display:flex;align-items:center;justify-content:space-between}
#shadow-ai-head strong{color:#75edf2;font-size:17px}
#shadow-ai-close{border:0;border-radius:9px;padding:8px 12px;background:#24465d;color:white}
#shadow-ai-log{padding:12px;overflow:auto;flex:1;line-height:1.7}
.shadow-ai-msg{white-space:pre-wrap;overflow-wrap:anywhere;padding:10px 12px;margin:8px 0;border-radius:12px;background:#142c42}
.shadow-ai-msg.me{border-right:3px solid #55dce5}
.shadow-ai-msg.bot{border-right:3px solid #39b987}
#shadow-ai-compose{padding:10px;border-top:1px solid #24445d;display:flex;gap:8px}
#shadow-ai-input{flex:1;min-width:0;min-height:52px;max-height:120px;resize:vertical;background:#071625;color:#fff;border:1px solid #35566e;border-radius:10px;padding:10px;font-size:16px}
#shadow-ai-send{border:0;border-radius:10px;padding:0 17px;background:#126078;color:white;font-weight:bold}
#shadow-ai-status{padding:0 12px 8px;color:#ffd27a;font-size:12px}
</style>
<button id="shadow-ai-launch" type="button" aria-label="فتح مساعد SHADOW AI">🧠 SHADOW AI</button>
<div id="shadow-ai-dock" role="dialog" aria-modal="true" aria-label="مساعد SHADOW AI">
 <section id="shadow-ai-card">
  <header id="shadow-ai-head"><strong>🧠 SHADOW AI</strong><button id="shadow-ai-close" type="button">إغلاق ✕</button></header>
  <div id="shadow-ai-log"><div class="shadow-ai-msg bot">مرحباً. أنا مساعد SHADOW AI. يمكنني مساعدتك في تحليل النتائج والبرمجة والأمن الدفاعي المصرح به.</div></div>
  <div id="shadow-ai-status" aria-live="polite"></div>
  <div id="shadow-ai-compose"><textarea id="shadow-ai-input" rows="2" maxlength="4000" placeholder="اكتب سؤالك..."></textarea><button id="shadow-ai-send" type="button">إرسال</button></div>
 </section>
</div>
<script id="shadow-ai-script">
(function(){
 const ENDPOINT='https://shadow-ai.azd103307.workers.dev/chat';
 const dock=document.getElementById('shadow-ai-dock');
 const log=document.getElementById('shadow-ai-log');
 const input=document.getElementById('shadow-ai-input');
 const status=document.getElementById('shadow-ai-status');
 const send=document.getElementById('shadow-ai-send');
 const history=[];
 document.getElementById('shadow-ai-launch').addEventListener('click',()=>{dock.classList.add('open');input.focus();});
 document.getElementById('shadow-ai-close').addEventListener('click',()=>dock.classList.remove('open'));
 dock.addEventListener('click',e=>{if(e.target===dock)dock.classList.remove('open');});
 function add(text,who){const el=document.createElement('div');el.className='shadow-ai-msg '+who;el.textContent=text;log.appendChild(el);log.scrollTop=log.scrollHeight;}
 async function submit(){
  const message=input.value.trim(); if(!message||send.disabled)return;
  add(message,'me'); input.value=''; send.disabled=true; status.textContent='جارٍ الاتصال بالمساعد...';
  try{
   const response=await fetch(ENDPOINT,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({message,history})});
   const data=await response.json().catch(()=>({}));
   if(!response.ok)throw new Error(data.error||('تعذر الاتصال ('+response.status+')'));
   if(typeof data.reply!=='string'||!data.reply.trim())throw new Error('لم تصل إجابة صالحة من الخادم.');
   add(data.reply,'bot');history.push({role:'user',content:message},{role:'assistant',content:data.reply});
   while(history.length>10)history.shift();
   status.textContent='';
  }catch(error){status.textContent='خطأ: '+(error.message||'تعذر الاتصال. تحقق من نشر Cloudflare وإعداد GROQ_API_KEY.');}
  finally{send.disabled=false;input.focus();}
 }
 send.addEventListener('click',submit);
 input.addEventListener('keydown',e=>{if(e.key==='Enter'&&!e.shiftKey){e.preventDefault();submit();}});
})();
</script>
"""
if "</body>" in html.lower():
    pos = html.lower().rfind("</body>")
    html = html[:pos] + integration + "\n" + html[pos:]
else:
    html += integration
path.write_text(html, encoding="utf-8")
print("Integrated SHADOW AI panel into existing Wi-Fi RECON asset.")
