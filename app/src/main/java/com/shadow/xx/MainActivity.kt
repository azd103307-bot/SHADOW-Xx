package com.shadow.xx

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun cardBg():GradientDrawable=GradientDrawable().apply {
        colors=intArrayOf(0xFF17283D.toInt(),0xFF102033.toInt())
        orientation=GradientDrawable.Orientation.TL_BR
        cornerRadius=dp(20).toFloat()
        setStroke(dp(1),0xFF304A67.toInt())
    }
    private data class Game(val id:Int,val icon:String,val title:String,val desc:String,val color:Int)
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=Color.rgb(7,15,27)
        window.navigationBarColor=Color.rgb(7,15,27)
        val root=LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(18),dp(20),dp(18),dp(12))
            setBackgroundColor(0xFF07101D.toInt())
        }
        root.addView(TextView(this).apply {
            text="FIVE WORLDS 3D"
            textSize=28f; setTextColor(Color.WHITE); typeface=Typeface.DEFAULT_BOLD
            gravity=Gravity.CENTER; letterSpacing=.08f
        })
        root.addView(TextView(this).apply {
            text="خمسة عوالم لعب ثلاثية الأبعاد"
            textSize=19f; setTextColor(0xFFFFD166.toInt()); gravity=Gravity.CENTER
            setPadding(0,dp(7),0,dp(5))
        })
        root.addView(TextView(this).apply {
            text="اختر عالمك وابدأ اللعب"
            textSize=14f; setTextColor(0xFFB5C4D8.toInt()); gravity=Gravity.CENTER
            setPadding(0,0,0,dp(14))
        })
        val scroll=ScrollView(this)
        val list=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL }
        val games=listOf(
            Game(1,"🏎️","سباق المدينة","قد سيارتك بسرعة وتفادى الحواجز واجمع النقاط",0xFF1E88E5.toInt()),
            Game(2,"⚽","بطولة كرة القدم","تحرك باللاعب وسدد الكرة نحو المرمى",0xFF159957.toInt()),
            Game(3,"🌴","مغامرة البقاء","استكشف الجزيرة واجمع الكنوز وتقدم للأمام",0xFFB7791F.toInt()),
            Game(4,"✈️","الطيران الحر","حلّق بين الجزر والسحب واجتز الحلقات",0xFF3986C8.toInt()),
            Game(5,"🚀","حراس الفضاء","تحرك في الفضاء وأسقط الأهداف المتحركة",0xFF7755CC.toInt())
        )
        for(g in games) {
            val item=LinearLayout(this).apply {
                orientation=LinearLayout.HORIZONTAL
                gravity=Gravity.CENTER_VERTICAL
                background=cardBg()
                setPadding(dp(14),dp(12),dp(14),dp(12))
                isClickable=true; isFocusable=true
                setOnClickListener {
                    startActivity(Intent(this@MainActivity,GameActivity::class.java).putExtra("mode",g.id))
                }
            }
            val icon=TextView(this).apply {
                text=g.icon; textSize=35f; gravity=Gravity.CENTER
                setBackgroundColor(g.color); setPadding(dp(8),dp(10),dp(8),dp(10))
            }
            item.addView(icon,LinearLayout.LayoutParams(dp(70),dp(74)))
            val texts=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(14),0,0,0) }
            texts.addView(TextView(this).apply {
                text=g.title; textSize=19f; typeface=Typeface.DEFAULT_BOLD
                setTextColor(Color.WHITE)
            })
            texts.addView(TextView(this).apply {
                text=g.desc; textSize=13f; setTextColor(0xFFB9C7D8.toInt())
                setPadding(0,dp(4),0,0)
            })
            item.addView(texts,LinearLayout.LayoutParams(0,-2,1f))
            item.addView(TextView(this).apply {
                text="ابدأ  ›"; textSize=15f; typeface=Typeface.DEFAULT_BOLD
                setTextColor(0xFFFFD166.toInt())
            })
            list.addView(item,LinearLayout.LayoutParams(-1,dp(102)).apply { bottomMargin=dp(11) })
        }
        scroll.addView(list)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        root.addView(TextView(this).apply {
            text="نسخة تجريبية أصلية • تحكم بالأسهم وزر التنفيذ • لا تحتاج اتصالاً بالإنترنت"
            textSize=11f; setTextColor(0xFF8193A9.toInt()); gravity=Gravity.CENTER
            setPadding(0,dp(10),0,0)
        })
        setContentView(root)
    }
}
