package com.shadow.xx

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.content.pm.ActivityInfo
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import android.opengl.GLSurfaceView
import android.graphics.drawable.GradientDrawable

class GameActivity : Activity() {
    private lateinit var surface: GLSurfaceView
    private lateinit var engine: ArenaRenderer
    private lateinit var scoreView: TextView\n    private lateinit var livesView: TextView
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    private fun pill(color:Int): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius=dp(18).toFloat(); setStroke(dp(1),0x55FFFFFF)
    }
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.decorView.systemUiVisibility=(View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        val mode=intent.getIntExtra("mode",1).coerceIn(1,5)
        val names=listOf("","سباق المدينة","بطولة كرة القدم","مغامرة البقاء","الطيران الحر","حراس الفضاء")
        val root=FrameLayout(this)
        surface=GLSurfaceView(this).apply {
            setEGLContextClientVersion(2)
            preserveEGLContextOnPause=true
        }
        engine=ArenaRenderer(mode)
        surface.setRenderer(engine)
        surface.renderMode=GLSurfaceView.RENDERMODE_CONTINUOUSLY
        root.addView(surface,FrameLayout.LayoutParams(-1,-1))
        val overlay=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(dp(12),dp(8),dp(12),dp(10)) }
        val top=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
        val title=TextView(this).apply { text=names[mode]; textSize=20f; setTextColor(Color.WHITE); typeface=Typeface.DEFAULT_BOLD; setPadding(dp(14),dp(7),dp(14),dp(7)); background=pill(0xAA101B2B.toInt()) }
        scoreView=TextView(this).apply { text="النقاط: 0"; textSize=17f; setTextColor(Color.WHITE); setPadding(dp(14),dp(7),dp(14),dp(7)); background=pill(0xAA101B2B.toInt()) }
        val lives=TextView(this).apply { text="❤️ 3"; textSize=16f; setTextColor(Color.WHITE); setPadding(dp(10),dp(7),dp(10),dp(7)); background=pill(0xAA101B2B.toInt()) }\n        livesView=lives\n        val back=Button(this).apply { text="رجوع"; isAllCaps=false; textSize=14f; setOnClickListener { finish() } }
        top.addView(title,LinearLayout.LayoutParams(0,dp(44),1f))
        top.addView(scoreView,LinearLayout.LayoutParams(-2,dp(44)))
        top.addView(livesView,LinearLayout.LayoutParams(-2,dp(44)))
        top.addView(back,LinearLayout.LayoutParams(dp(82),dp(44)))
        overlay.addView(top)
        val spacer=Space(this)
        overlay.addView(spacer,LinearLayout.LayoutParams(1,0,1f))
        val bottom=LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL }
        val dpad=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER }
        val row1=LinearLayout(this).apply { gravity=Gravity.CENTER }
        val row2=LinearLayout(this).apply { gravity=Gravity.CENTER }
        val row3=LinearLayout(this).apply { gravity=Gravity.CENTER }
        fun control(label:String,down:()->Unit,up:()->Unit):Button {
            return Button(this).apply {
                text=label; textSize=20f; isAllCaps=false; setTextColor(Color.WHITE); background=pill(0xCC15253A.toInt())
                setOnTouchListener { _,event ->
                    when(event.action) { MotionEvent.ACTION_DOWN -> down(); MotionEvent.ACTION_UP,MotionEvent.ACTION_CANCEL -> up() }
                    true
                }
            }
        }
        row1.addView(control("▲",{engine.moveY=1f},{engine.moveY=0f}),LinearLayout.LayoutParams(dp(62),dp(48)).apply { bottomMargin=dp(3) })
        row2.addView(control("◀",{engine.moveX=-1f},{engine.moveX=0f}),LinearLayout.LayoutParams(dp(62),dp(48)).apply { marginEnd=dp(4) })
        row2.addView(control("▼",{engine.moveY=-1f},{engine.moveY=0f}),LinearLayout.LayoutParams(dp(62),dp(48)))
        row2.addView(control("▶",{engine.moveX=1f},{engine.moveX=0f}),LinearLayout.LayoutParams(dp(62),dp(48)).apply { marginStart=dp(4) })
        dpad.addView(row1); dpad.addView(row2)
        bottom.addView(dpad)
        bottom.addView(Space(this),LinearLayout.LayoutParams(0,dp(1),1f))
        val actionLabel=when(mode) { 1 -> "⚡ تسريع"; 2 -> "⚽ تسديد"; 5 -> "🚀 إطلاق"; else -> "✦ تفاعل" }\n        bottom.addView(control(actionLabel,{engine.action=true},{engine.action=false}),LinearLayout.LayoutParams(dp(124),dp(58)))
        overlay.addView(bottom)
        root.addView(overlay,FrameLayout.LayoutParams(-1,-1))
        engine.onHud={points,health->runOnUiThread { scoreView.text="النقاط: $points"; livesView.text="❤️ $health" }}
        setContentView(root)
    }
    override fun onPause(){ super.onPause(); if(::surface.isInitialized) surface.onPause() }
    override fun onResume(){ super.onResume(); if(::surface.isInitialized) surface.onResume() }
}
