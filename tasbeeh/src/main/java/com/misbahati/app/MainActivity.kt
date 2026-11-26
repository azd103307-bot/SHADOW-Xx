package com.misbahati.app

import android.app.*
import android.os.*
import android.content.*
import android.graphics.Color
import android.graphics.Typeface
import android.view.*
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val p by lazy { getSharedPreferences("misbahati", MODE_PRIVATE) }
    private var count = 0L
    private var goal = 1_000_000L
    private var dhikr = "سبحان الله"
    private var haptics = false
    private lateinit var number: TextView
    private lateinit var info: TextView
    private lateinit var today: TextView
    private lateinit var phrase: Button
    private val green = Color.rgb(13,65,52)
    private val gold = Color.rgb(218,181,105)
    private val white = Color.rgb(247,244,233)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        count = p.getLong("count",0L); goal = p.getLong("goal",1_000_000L)
        dhikr = p.getString("dhikr","سبحان الله") ?: "سبحان الله"
        haptics = p.getBoolean("haptics",false)
        val root = LinearLayout(this).apply { orientation=1; gravity=Gravity.CENTER_HORIZONTAL; setPadding(d(20),d(18),d(20),d(20)); setBackgroundColor(green); layoutDirection=View.LAYOUT_DIRECTION_RTL }
        val scroll = ScrollView(this)
        val c = LinearLayout(this).apply { orientation=1; gravity=Gravity.CENTER_HORIZONTAL; layoutDirection=View.LAYOUT_DIRECTION_RTL }
        scroll.addView(c)
        c.addView(label("مِسبحتي",30,gold,true))
        c.addView(label("رفيق ذكرك في كل وقت",14,white,false).apply { setPadding(0,0,0,d(16)) })
        phrase = Button(this).apply { text=dhikr+"  ▾"; textSize=17f; setTextColor(white); setBackgroundColor(Color.rgb(24,91,72)); setOnClickListener { chooseDhikr() } }
        c.addView(phrase,lp(-1,-2))
        val card=LinearLayout(this).apply { orientation=1; gravity=Gravity.CENTER; setPadding(d(14),d(24),d(14),d(20)); setBackgroundColor(Color.rgb(19,79,63)) }
        card.addView(label("عدد التسبيحات",16,white,false))
        number=label("0",58,gold,true).apply { setPadding(0,d(8),0,d(4)) }; card.addView(number)
        info=label("",14,white,false); card.addView(info)
        today=label("",14,white,false).apply { setPadding(0,d(8),0,0) }; card.addView(today)
        c.addView(card,lp(-1,-2).apply { topMargin=d(18); bottomMargin=d(12) })
        c.addView(label("●   ●   ●   ●   ●",24,gold,true).apply { setPadding(0,d(4),0,d(12)) })
        val tap=Button(this).apply {
            text="سَبِّح  +1"; textSize=23f; typeface=Typeface.DEFAULT_BOLD; setTextColor(green); setBackgroundColor(gold); minHeight=d(76)
            setOnClickListener {
                if(count<Long.MAX_VALUE) count++
                val key=dayKey(); p.edit().putLong("count",count).putLong(key,p.getLong(key,0L)+1L).apply()
                if(haptics) vibrate()
                refresh()
            }
        }
        c.addView(tap,lp(-1,d(76)))
        val r1=LinearLayout(this).apply { orientation=0; layoutDirection=View.LAYOUT_DIRECTION_RTL }
        r1.addView(btn("الهدف") { chooseGoal() },LinearLayout.LayoutParams(0,d(52),1f))
        r1.addView(btn("تراجع −1") { if(count>0){count--; val k=dayKey(); p.edit().putLong("count",count).putLong(k,(p.getLong(k,0L)-1).coerceAtLeast(0)).apply(); refresh()} },LinearLayout.LayoutParams(0,d(52),1f))
        c.addView(r1,lp(-1,-2).apply { topMargin=d(10) })
        val r2=LinearLayout(this).apply { orientation=0; layoutDirection=View.LAYOUT_DIRECTION_RTL }
        r2.addView(btn("تصفير") { AlertDialog.Builder(this).setTitle("تصفير العدّاد؟").setMessage("سيتم تصفير العدد الحالي فقط.").setNegativeButton("إلغاء",null).setPositiveButton("تصفير"){_,_->count=0;p.edit().putLong("count",0).apply();refresh()}.show() },LinearLayout.LayoutParams(0,d(50),1f))
        r2.addView(btn("الإعدادات") { settings() },LinearLayout.LayoutParams(0,d(50),1f))
        c.addView(r2,lp(-1,-2).apply { topMargin=d(7) })
        c.addView(label("أذكار مختصرة",18,gold,true).apply { setPadding(0,d(20),0,d(6)) })
        listOf("سبحان الله","الحمد لله","الله أكبر","لا إله إلا الله","أستغفر الله","اللهم صل وسلم على نبينا محمد").forEach { s ->
            c.addView(btn(s) { dhikr=s; p.edit().putString("dhikr",s).apply(); refresh() },lp(-1,d(46)).apply { topMargin=d(4) })
        }
        c.addView(label("يُحفظ عدّادك تلقائياً على هذا الجهاز • يعمل دون إنترنت",12,white,false).apply { setPadding(0,d(18),0,d(8)) })
        root.addView(scroll,lp(-1,-1)); setContentView(root); refresh()
    }
    private fun refresh() {
        number.text=String.format(Locale.US,"%,d",count)
        info.text="الهدف: "+String.format(Locale.US,"%,d",goal)+"  •  "+(count.toDouble()/goal*100).coerceAtMost(100.0).toInt()+"%"
        today.text="تسبيحات اليوم: "+String.format(Locale.US,"%,d",p.getLong(dayKey(),0L))
        phrase.text=dhikr+"  ▾"
    }
    private fun chooseDhikr() {
        val a=arrayOf("سبحان الله","الحمد لله","الله أكبر","لا إله إلا الله","أستغفر الله","اللهم صل وسلم على نبينا محمد")
        AlertDialog.Builder(this).setTitle("اختر الذكر").setItems(a){_,i->dhikr=a[i];p.edit().putString("dhikr",dhikr).apply();refresh()}.show()
    }
    private fun chooseGoal() {
        val v=longArrayOf(33,100,1000,10000,100000,1000000)
        val n=arrayOf("33","100","1,000","10,000","100,000","1,000,000")
        AlertDialog.Builder(this).setTitle("اختر هدفك").setItems(n){_,i->goal=v[i];p.edit().putLong("goal",goal).apply();refresh()}.show()
    }
    private fun settings() {
        val items=arrayOf(if(haptics)"إيقاف الاهتزاز" else "تفعيل الاهتزاز","سجل اليوم")
        AlertDialog.Builder(this).setTitle("الإعدادات").setItems(items){_,i->
            if(i==0){haptics=!haptics;p.edit().putBoolean("haptics",haptics).apply();settings()}
            else AlertDialog.Builder(this).setTitle("سجل اليوم").setMessage("إجمالي اليوم: "+p.getLong(dayKey(),0L)+" تسبيحة").setPositiveButton("حسناً",null).show()
        }.setNegativeButton("إغلاق",null).show()
    }
    private fun btn(t:String, f:()->Unit)=Button(this).apply{text=t;textSize=15f;setTextColor(white);setBackgroundColor(Color.rgb(34,99,78));setOnClickListener{f()}}
    private fun label(t:String,s:Int,color:Int,b:Boolean)=TextView(this).apply{text=t;textSize=s.toFloat();setTextColor(color);gravity=Gravity.CENTER;if(b)typeface=Typeface.DEFAULT_BOLD}
    private fun lp(w:Int,h:Int)=LinearLayout.LayoutParams(w,h)
    private fun d(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun dayKey()="day_"+SimpleDateFormat("yyyyMMdd",Locale.US).format(Date())
    private fun vibrate(){try{val v=getSystemService(VIBRATOR_SERVICE) as Vibrator;if(Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(25,45)) else @Suppress("DEPRECATION") v.vibrate(25)}catch(_:Exception){}}
}