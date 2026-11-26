package com.shadow.xx

import android.app.Activity
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.widget.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

class ConstructionActivity : Activity() {
    private lateinit var scene: BuildSurface
    private lateinit var info: TextView
    private var selected = "بلوك"
    private var fixed = false
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        scene=BuildSurface(this)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(0xFF101827.toInt())}
        root.addView(TextView(this).apply{text="ورشة البناء ثلاثية الأبعاد";textSize=21f;gravity=Gravity.CENTER;setTextColor(-1);setPadding(8,dp(10),8,dp(10))})
        root.addView(scene,LinearLayout.LayoutParams(-1,0,1f))
        val controls=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(5),dp(4),dp(5),dp(5))}
        val first=LinearLayout(this).apply{gravity=Gravity.CENTER}
        fun btn(row:LinearLayout,label:String,fn:()->Unit){row.addView(Button(this).apply{text=label;isAllCaps=false;textSize=12f;setOnClickListener{fn()}},LinearLayout.LayoutParams(0,dp(45),1f).apply{setMargins(dp(2),0,dp(2),0)})}
        btn(first,"بلوك"){selected="بلوك";info.text="بلوك: المس أرضية المشهد لإضافته"}
        btn(first,"لوح خشب"){selected="لوح";info.text="لوح: عرض 10 سم، طول 3 أمتار"}
        btn(first,"مربع خشب"){selected="مربع";info.text="مربع: 7 × 7 سم"}
        btn(first,"تثبيت / تسمير"){fixed=!fixed;scene.renderer.nailing=fixed;info.text=if(fixed)"التسمير مفعّل للقطع الجديدة" else "التسمير متوقف"}
        controls.addView(first)
        val second=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(second,"3 م"){scene.renderer.woodLength=3f;info.text="طول الخشب: 3 أمتار"}
        btn(second,"1.5 م"){scene.renderer.woodLength=1.5f;info.text="طول الخشب: متر ونصف"}
        btn(second,"1 م"){scene.renderer.woodLength=1f;info.text="طول الخشب: متر واحد"}
        btn(second,"75 سم"){scene.renderer.woodLength=.75f;info.text="طول الخشب: 75 سم"}
        controls.addView(second)
        info=TextView(this).apply{text="اختر بلوك أو خشب، ثم المس المشهد لإضافته. اسحب بإصبعك للتدوير وبإصبعين للتقريب.";textSize=12f;setTextColor(0xFFE2E8F0.toInt());gravity=Gravity.CENTER;setPadding(2,dp(5),2,dp(5))}
        controls.addView(info)
        val third=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(third,"تراجع"){scene.renderer.removeLast()}
        btn(third,"مسح الكل"){scene.renderer.clear()}
        btn(third,"رجوع"){finish()}
        controls.addView(third)
        val fourth=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(fourth,"تحديد آخر قطعة"){scene.renderer.selectLast();info.text="تم تحديد آخر قطعة"}
        btn(fourth,"نسخ قطعة"){scene.renderer.duplicateLast();info.text="تم نسخ القطعة"}
        btn(fourth,"تدوير 90°"){scene.renderer.rotateLast();info.text="تم تدوير القطعة"}
        btn(fourth,"حذف المحددة"){scene.renderer.deleteLast();info.text="تم حذف القطعة"}
        controls.addView(fourth)
        val fifth=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(fifth,"حفظ المشروع"){getSharedPreferences("shadow_build",MODE_PRIVATE).edit().putString("project",scene.renderer.exportProject()).apply();info.text="تم حفظ المشروع على الجهاز"}
        btn(fifth,"فتح المشروع"){getSharedPreferences("shadow_build",MODE_PRIVATE).getString("project",null)?.let{scene.renderer.importProject(it);info.text="تم فتح المشروع"}?:run{info.text="لا يوجد مشروع محفوظ"}}
        btn(fifth,"التمتير"){measurementDialog()}
        btn(fifth,"تقرير ونسخ"){val t=scene.renderer.report();(getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("تقرير البناء",t));info.text="تم نسخ تقرير الكميات"}
        controls.addView(fifth)
        root.addView(controls);setContentView(root)
        scene.onTap={scene.renderer.add(selected)}
    }
    private fun measurementDialog(){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,8,24,0)}
        val length=EditText(this).apply{hint="الطول بالمتر";inputType=8194}
        val width=EditText(this).apply{hint="العرض بالمتر";inputType=8194}
        val height=EditText(this).apply{hint="الارتفاع بالمتر";inputType=8194}
        listOf(length,width,height).forEach{box.addView(it)}
        AlertDialog.Builder(this).setTitle("حاسبة التمتير").setView(box).setNegativeButton("إلغاء",null).setPositiveButton("احسب"){_,_->
            val l=length.text.toString().toDoubleOrNull()?:0.0
            val w=width.text.toString().toDoubleOrNull()?:0.0
            val h=height.text.toString().toDoubleOrNull()?:0.0
            val area=l*h;val volume=l*w*h;val blocks=if(area>0)kotlin.math.ceil(area/(.4*.2)).toInt() else 0
            AlertDialog.Builder(this).setTitle("نتيجة الحساب").setMessage("مساحة الجدار: %.2f م²\\nالحجم: %.3f م³\\nالبلوك التقديري (40×20 سم): %d".format(area,volume,blocks)).setPositiveButton("حسناً",null).show()
        }.show()
    }
    override fun onResume(){super.onResume();if(::scene.isInitialized)scene.onResume()}
    override fun onPause(){if(::scene.isInitialized)scene.onPause();super.onPause()}
}

private class BuildSurface(activity:Activity):GLSurfaceView(activity){
    val renderer=BuildRenderer()
    var onTap:(()->Unit)?=null
    private var lastX=0f;private var lastY=0f;private var lastDistance=0f;private var moved=false
    init{setEGLContextClientVersion(2);setRenderer(renderer);renderMode=RENDERMODE_CONTINUOUSLY}
    override fun onTouchEvent(e:MotionEvent):Boolean{
        when(e.actionMasked){
            MotionEvent.ACTION_DOWN->{lastX=e.x;lastY=e.y;moved=false;return true}
            MotionEvent.ACTION_POINTER_DOWN->{lastDistance=distance(e);return true}
            MotionEvent.ACTION_MOVE->{
                if(e.pointerCount>=2){
                    val d=distance(e);if(lastDistance>0f)renderer.zoom((lastDistance-d)*.018f);lastDistance=d;moved=true
                }else{
                    val dx=e.x-lastX;val dy=e.y-lastY
                    if(abs(dx)+abs(dy)>3)moved=true
                    renderer.rotate(dx*.45f,dy*.35f);lastX=e.x;lastY=e.y
                };return true
            }
            MotionEvent.ACTION_POINTER_UP->{lastDistance=0f;return true}
            MotionEvent.ACTION_UP->{if(!moved)onTap?.invoke();performClick();return true}
        };return true
    }
    private fun distance(e:MotionEvent):Float{if(e.pointerCount<2)return 0f;val dx=e.getX(0)-e.getX(1);val dy=e.getY(0)-e.getY(1);return hypot(dx,dy)}
    override fun performClick():Boolean{super.performClick();return true}
}

private class BuildRenderer:GLSurfaceView.Renderer{
    data class Part(val kind:String,val x:Float,val y:Float,val z:Float,val sx:Float,val sy:Float,val sz:Float,val nailed:Boolean)
    private val parts=mutableListOf<Part>()
    @Volatile var angleY=35f
    @Volatile var angleX=22f
    @Volatile var cameraDistance=9f
    @Volatile var woodLength=3f
    @Volatile var nailing=false
    private var program=0
    private var posHandle=0
    private var colorHandle=0
    private var mvpHandle=0
    private val projection=FloatArray(16)
    private val view=FloatArray(16)
    private val model=FloatArray(16)
    private val mvp=FloatArray(16)
    private val finalM=FloatArray(16)
    private val verts=FloatArray(216)
    private lateinit var buffer:FloatBuffer
    private val vertexShader="attribute vec4 aPosition;attribute vec4 aColor;uniform mat4 uMVP;varying vec4 vColor;void main(){gl_Position=uMVP*aPosition;vColor=aColor;}"
    private val fragmentShader="precision mediump float;varying vec4 vColor;void main(){gl_FragColor=vColor;}"
    override fun onSurfaceCreated(gl:GL10?,config:EGLConfig?){
        GLES20.glClearColor(.055f,.085f,.13f,1f);GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        val vs=shader(GLES20.GL_VERTEX_SHADER,vertexShader);val fs=shader(GLES20.GL_FRAGMENT_SHADER,fragmentShader)
        program=GLES20.glCreateProgram();GLES20.glAttachShader(program,vs);GLES20.glAttachShader(program,fs);GLES20.glLinkProgram(program)
        posHandle=GLES20.glGetAttribLocation(program,"aPosition");colorHandle=GLES20.glGetAttribLocation(program,"aColor");mvpHandle=GLES20.glGetUniformLocation(program,"uMVP")
        buffer=ByteBuffer.allocateDirect(verts.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer()
    }
    override fun onSurfaceChanged(gl:GL10?,w:Int,h:Int){GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(projection,0,45f,w.toFloat()/h.coerceAtLeast(1),.1f,100f)}
    override fun onDrawFrame(gl:GL10?){
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        Matrix.setLookAtM(view,0,0f,4.2f,cameraDistance,0f,1f,0f,0f,1f,0f)
        Matrix.rotateM(view,0,angleX,1f,0f,0f);Matrix.rotateM(view,0,angleY,0f,1f,0f)
        GLES20.glUseProgram(program)
        drawCube(0f,-.12f,0f,7f,.12f,7f,floatArrayOf(.22f,.27f,.32f))
        val copy=synchronized(parts){parts.toList()}
        copy.forEach{p->
            val color=when(p.kind){"بلوك"->floatArrayOf(.68f,.53f,.36f);"لوح"->floatArrayOf(.57f,.32f,.14f);else->floatArrayOf(.75f,.52f,.25f)}
            drawCube(p.x,p.y,p.z,p.sx,p.sy,p.sz,color)
            if(p.nailed)drawCube(p.x,p.y+p.sy/2+.018f,p.z,.045f,.035f,.045f,floatArrayOf(.78f,.82f,.88f))
        }
    }
    private fun drawCube(x:Float,y:Float,z:Float,sx:Float,sy:Float,sz:Float,c:FloatArray){
        val hx=sx/2;val hy=sy/2;val hz=sz/2
        val f=arrayOf(
            floatArrayOf(-hx,-hy,hz,hx,-hy,hz,hx,hy,hz,-hx,hy,hz),
            floatArrayOf(hx,-hy,-hz,-hx,-hy,-hz,-hx,hy,-hz,hx,hy,-hz),
            floatArrayOf(-hx,-hy,-hz,-hx,-hy,hz,-hx,hy,hz,-hx,hy,-hz),
            floatArrayOf(hx,-hy,hz,hx,-hy,-hz,hx,hy,-hz,hx,hy,hz),
            floatArrayOf(-hx,hy,hz,hx,hy,hz,hx,hy,-hz,-hx,hy,-hz),
            floatArrayOf(-hx,-hy,-hz,hx,-hy,-hz,hx,-hy,hz,-hx,-hy,hz))
        val shade=floatArrayOf(.82f,.58f,.72f,.64f,1f,.48f);var k=0
        f.forEachIndexed{face,arr->intArrayOf(0,1,2,0,2,3).forEach{idx->val j=idx*3
            verts[k++]=arr[j]+x;verts[k++]=arr[j+1]+y;verts[k++]=arr[j+2]+z
            verts[k++]=c[0]*shade[face];verts[k++]=c[1]*shade[face];verts[k++]=c[2]*shade[face]
        }}
        buffer.clear();buffer.put(verts);buffer.position(0)
        Matrix.setIdentityM(model,0);Matrix.multiplyMM(mvp,0,view,0,model,0);Matrix.multiplyMM(finalM,0,projection,0,mvp,0)
        GLES20.glUniformMatrix4fv(mvpHandle,1,false,finalM,0)
        buffer.position(0);GLES20.glEnableVertexAttribArray(posHandle);GLES20.glVertexAttribPointer(posHandle,3,GLES20.GL_FLOAT,false,24,buffer)
        buffer.position(3);GLES20.glEnableVertexAttribArray(colorHandle);GLES20.glVertexAttribPointer(colorHandle,3,GLES20.GL_FLOAT,false,24,buffer)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,36)
    }
    fun rotate(dx:Float,dy:Float){angleY+=dx;angleX=(angleX+dy).coerceIn(-55f,65f)}
    fun zoom(delta:Float){cameraDistance=(cameraDistance+delta).coerceIn(4.5f,18f)}
    fun add(kind:String){synchronized(parts){
        val n=parts.size
        val x=((n%8)-3.5f)*.68f
        val z=((n/8)%8-3.5f)*.68f
        val dims=when(kind){
            "بلوك"->Triple(.68f,.32f,.32f)
            "لوح"->Triple(woodLength,.035f,.10f)
            else->Triple(woodLength,.07f,.07f)
        }
        parts.add(Part(kind,x,dims.second/2,z,dims.first,dims.second,dims.third,nailing))
    }}
    fun selectLast(){synchronized(parts){selected=parts.lastIndex}}
    private var selected=-1
    fun duplicateLast(){synchronized(parts){if(parts.isNotEmpty()){val p=parts.last();parts.add(p.copy(x=p.x+.45f,z=p.z+.35f));selected=parts.lastIndex}}}
    fun rotateLast(){synchronized(parts){if(parts.isNotEmpty()){val p=parts.last();parts[parts.lastIndex]=p.copy(sx=p.sz,sz=p.sx);selected=parts.lastIndex}}}
    fun deleteLast(){synchronized(parts){if(parts.isNotEmpty())parts.removeAt(parts.lastIndex);selected=(parts.size-1).coerceAtLeast(-1)}}
    fun exportProject():String{synchronized(parts){val a=org.json.JSONArray();parts.forEach{p->a.put(org.json.JSONObject().put("kind",p.kind).put("x",p.x).put("y",p.y).put("z",p.z).put("sx",p.sx).put("sy",p.sy).put("sz",p.sz).put("nailed",p.nailed))};return org.json.JSONObject().put("parts",a).toString()}}
    fun importProject(data:String){try{val a=org.json.JSONObject(data).getJSONArray("parts");synchronized(parts){parts.clear();for(i in 0 until a.length()){val o=a.getJSONObject(i);parts.add(Part(o.optString("kind","بلوك"),o.optDouble("x").toFloat(),o.optDouble("y").toFloat(),o.optDouble("z").toFloat(),o.optDouble("sx",.5).toFloat(),o.optDouble("sy",.2).toFloat(),o.optDouble("sz",.2).toFloat(),o.optBoolean("nailed")))}}}catch(_:Exception){}}
    fun report():String{synchronized(parts){val blocks=parts.count{it.kind=="بلوك"};val wood=parts.size-blocks;val volume=parts.sumOf{(it.sx*it.sy*it.sz).toDouble()};return "تقرير ورشة البناء\\nإجمالي القطع: ${parts.size}\\nالبلوكات: $blocks\\nالخشب: $wood\\nالحجم التقريبي: %.3f م³".format(volume)}}
    fun removeLast(){synchronized(parts){if(parts.isNotEmpty())parts.removeAt(parts.lastIndex)}}
    fun clear(){synchronized(parts){parts.clear()}}
    private fun shader(type:Int,src:String):Int{val s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);return s}
}
