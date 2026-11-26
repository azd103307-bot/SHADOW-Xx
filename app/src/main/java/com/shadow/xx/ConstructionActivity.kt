package com.shadow.xx

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.content.Intent
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
        btn(first,"دعامة"){selected="دعامة";info.text="دعامة خشبية رأسية"}
        btn(first,"تثبيت / تسمير"){fixed=!fixed;scene.renderer.nailing=fixed;info.text=if(fixed)"التسمير مفعّل للقطع الجديدة" else "التسمير متوقف"}
        controls.addView(first)
        val second=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(second,"3 م"){scene.renderer.woodLength=3f;info.text="طول الخشب: 3 أمتار"}
        btn(second,"1.5 م"){scene.renderer.woodLength=1.5f;info.text="طول الخشب: متر ونصف"}
        btn(second,"1 م"){scene.renderer.woodLength=1f;info.text="طول الخشب: متر واحد"}
        btn(second,"75 سم"){scene.renderer.woodLength=.75f;info.text="طول الخشب: 75 سم"}
        controls.addView(second)
        val buildRow=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(buildRow,"صف بلوك"){scene.renderer.addRow();info.text="تمت إضافة صف بلوكات"}
        btn(buildRow,"جدار"){scene.renderer.addWall();info.text="تمت إضافة جدار من عدة طبقات"}
        btn(buildRow,"قالب صب"){selected="قالب";info.text="المس المشهد لإضافة قالب الصب"}
        btn(buildRow,"قطعة فوقها"){scene.renderer.addOnTop(selected);info.text="أضيفت قطعة فوق القطعة المحددة"}
        controls.addView(buildRow)
        val cameraRow=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(cameraRow,"منظور"){scene.renderer.setView("perspective")}
        btn(cameraRow,"أعلى"){scene.renderer.setView("top")}
        btn(cameraRow,"أمام"){scene.renderer.setView("front")}
        btn(cameraRow,"جانب"){scene.renderer.setView("side")}
        btn(cameraRow,"الشبكة"){scene.renderer.gridVisible=!scene.renderer.gridVisible;info.text=if(scene.renderer.gridVisible)"شبكة القياس ظاهرة" else "شبكة القياس مخفية"}
        controls.addView(cameraRow)
        info=TextView(this).apply{text="اختر بلوك أو خشب، ثم المس المشهد لإضافته. اسحب بإصبعك للتدوير وبإصبعين للتقريب.";textSize=12f;setTextColor(0xFFE2E8F0.toInt());gravity=Gravity.CENTER;setPadding(2,dp(5),2,dp(5))}
        controls.addView(info)
        val third=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(third,"تراجع"){scene.renderer.undo()}
        btn(third,"إعادة"){scene.renderer.redo()}
        btn(third,"مسح الكل"){scene.renderer.clear()}
        btn(third,"رجوع"){finish()}
        controls.addView(third)
        val fourth=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(fourth,"تحديد آخر قطعة"){scene.renderer.selectLast();info.text="تم تحديد آخر قطعة"}
        btn(fourth,"نسخ قطعة"){scene.renderer.duplicateLast();info.text="تم نسخ القطعة"}
        btn(fourth,"تدوير 90°"){scene.renderer.rotateLast();info.text="تم تدوير القطعة"}
        btn(fourth,"حذف المحددة"){scene.renderer.deleteSelected();info.text="تم حذف القطعة المحددة"}
        controls.addView(fourth)
        val dimensionRow=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(dimensionRow,"الأبعاد"){editSelectedDimensions()}
        btn(dimensionRow,"رفع"){scene.renderer.raiseSelected(.10f)}
        btn(dimensionRow,"خفض"){scene.renderer.raiseSelected(-.10f)}
        controls.addView(dimensionRow)
        val fifth=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(fifth,"حفظ المشروع"){getSharedPreferences("shadow_build",MODE_PRIVATE).edit().putString("project",scene.renderer.exportProject()).apply();info.text="تم حفظ المشروع على الجهاز"}
        btn(fifth,"فتح المشروع"){getSharedPreferences("shadow_build",MODE_PRIVATE).getString("project",null)?.let{scene.renderer.importProject(it);info.text="تم فتح المشروع"}?:run{info.text="لا يوجد مشروع محفوظ"}}
        btn(fifth,"التمتير"){measurementDialog()}
        btn(fifth,"تقرير ونسخ"){val t=scene.renderer.report();(getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("تقرير البناء",t));info.text="تم نسخ تقرير الكميات"}
        controls.addView(fifth)
        val sixth=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(sixth,"حفظ باسم"){saveNamedProject()}
        btn(sixth,"مشاريعي المحفوظة"){openProjectList()}
        btn(sixth,"مشاركة التقرير"){shareReport()}
        btn(sixth,"حفظ صورة"){saveSceneImage()}
        controls.addView(sixth)
        val seventh=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(seventh,"تصدير ملف المشروع"){exportProjectFile()}
        btn(seventh,"استيراد ملف مشروع"){importProjectFile()}
        btn(seventh,"قياس من صورة"){chooseMeasurePhoto()}
        btn(seventh,"حذف مشروع"){deleteProject()}
        controls.addView(seventh)
        val eighth=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(eighth,"شرح وخطوات المشروع"){editProjectNotes()}
        controls.addView(eighth)
        val controlsScroll=ScrollView(this).apply{isFillViewport=false;addView(controls)}
        root.addView(controlsScroll,LinearLayout.LayoutParams(-1,dp(285)));setContentView(root)
        scene.onTap={x,y->scene.renderer.addAt(selected,x,y,scene.width,scene.height)}
    }
    private fun deleteProject(){
        val prefs=getSharedPreferences("shadow_build_projects",MODE_PRIVATE)
        val names=prefs.getStringSet("names",emptySet())?.toList()?.sorted()?:emptyList()
        if(names.isEmpty()){Toast.makeText(this,"لا توجد مشاريع لحذفها",Toast.LENGTH_SHORT).show();return}
        AlertDialog.Builder(this).setTitle("اختر المشروع المراد حذفه").setItems(names.toTypedArray()){_,which->
            val name=names[which]
            AlertDialog.Builder(this).setMessage("حذف المشروع «$name» نهائياً من هذا الجهاز؟")
                .setNegativeButton("إلغاء",null).setPositiveButton("حذف"){_,_->
                    val updated=names.toMutableSet().apply{remove(name)}
                    prefs.edit().remove("project_"+name).putStringSet("names",updated).apply()
                    info.text="تم حذف المشروع: $name"
                }.show()
        }.setNegativeButton("إغلاق",null).show()
    }
    private fun editProjectNotes(){
        val editor=EditText(this).apply{
            hint="اكتب شرح البناء وخطوات التنفيذ بالترتيب..."
            setText(scene.renderer.getNotes())
            minLines=5;gravity=Gravity.TOP;inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        AlertDialog.Builder(this).setTitle("شرح المشروع وخطواته").setView(editor)
            .setNegativeButton("إلغاء",null).setPositiveButton("حفظ"){_,_->
                scene.renderer.setNotes(editor.text.toString())
                info.text="تم حفظ شرح المشروع ضمن ملفه"
            }.show()
    }
    private fun chooseMeasurePhoto(){
        val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{addCategory(Intent.CATEGORY_OPENABLE);type="image/*"}
        startActivityForResult(intent,4103)
    }
    private fun decodeMeasurePhoto(uri:android.net.Uri):Bitmap{
        val bounds=BitmapFactory.Options().apply{inJustDecodeBounds=true}
        contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,bounds)}
        var sample=1
        while(bounds.outWidth/sample>1800||bounds.outHeight/sample>1800)sample*=2
        val opts=BitmapFactory.Options().apply{inSampleSize=sample}
        return contentResolver.openInputStream(uri)?.use{BitmapFactory.decodeStream(it,null,opts)}
            ?:throw IllegalStateException("تعذر فتح الصورة")
    }
    private fun showPhotoMeasure(bitmap:Bitmap){
        val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,8,12,4)}
        val reference=EditText(this).apply{hint="طول مرجعي معروف بالمتر، مثل 1.00";inputType=8194}
        val message=TextView(this).apply{text="أدخل طولاً تعرفه في الصورة، ثم المس نقطتي بدايته ونهايته لمعايرة القياس.";textSize=13f}
        val image=ImageView(this).apply{setImageBitmap(bitmap);scaleType=ImageView.ScaleType.FIT_CENTER;adjustViewBounds=true;contentDescription="صورة القياس"}
        panel.addView(reference);panel.addView(message);panel.addView(image,LinearLayout.LayoutParams(-1,dp(300)))
        var first:PointF?=null
        var metersPerPixel:Double?=null
        image.setOnTouchListener{_,event->
            if(event.action==MotionEvent.ACTION_DOWN){
                val mapped=floatArrayOf(event.x,event.y)
                val inverse=android.graphics.Matrix()
                if(!image.imageMatrix.invert(inverse))return@setOnTouchListener true
                inverse.mapPoints(mapped)
                if(mapped[0]<0||mapped[1]<0||mapped[0]>bitmap.width||mapped[1]>bitmap.height)return@setOnTouchListener true
                val a=first
                if(a==null){first=PointF(mapped[0],mapped[1]);message.text="تم تحديد النقطة الأولى؛ المس النقطة الثانية."}
                else{
                    val pixels=hypot((mapped[0]-a.x).toDouble(),(mapped[1]-a.y).toDouble()).coerceAtLeast(1.0)
                    if(metersPerPixel==null){
                        val known=reference.text.toString().toDoubleOrNull()
                        if(known==null||known<=0){message.text="أدخل طولاً مرجعياً صحيحاً أولاً."}
                        else{metersPerPixel=known/pixels;message.text="تمت المعايرة. المس نقطتين لقياس أي مسافة أخرى."}
                    }else{
                        val measured=pixels*metersPerPixel!!
                        message.text="المسافة التقريبية: %.3f متر (%.1f سم)".format(measured,measured*100)
                    }
                    first=null
                }
            }
            true
        }
        AlertDialog.Builder(this).setTitle("القياس اليدوي من صورة").setView(ScrollView(this).apply{addView(panel)})
            .setPositiveButton("إغلاق",null).show()
    }
    private fun exportProjectFile(){
        val intent=Intent(Intent.ACTION_CREATE_DOCUMENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE);type="application/json"
            putExtra(Intent.EXTRA_TITLE,"shadow_build_project.json")
        }
        startActivityForResult(intent,4101)
    }
    private fun importProjectFile(){
        val intent=Intent(Intent.ACTION_OPEN_DOCUMENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE);type="application/json"
        }
        startActivityForResult(intent,4102)
    }
    @Deprecated("Deprecated in Android API; retained for broad device compatibility")
    override fun onActivityResult(requestCode:Int,resultCode:Int,data:Intent?){
        super.onActivityResult(requestCode,resultCode,data)
        if(resultCode!=RESULT_OK||data?.data==null)return
        try{
            if(requestCode==4101){
                val json=scene.renderer.exportProject()
                contentResolver.openOutputStream(data.data!!)?.use{it.write(json.toByteArray(Charsets.UTF_8))}
                Toast.makeText(this,"تم تصدير ملف المشروع",Toast.LENGTH_LONG).show()
            }else if(requestCode==4102){
                val json=contentResolver.openInputStream(data.data!!)?.bufferedReader()?.use{it.readText()}
                    ?:throw IllegalStateException("تعذر قراءة الملف")
                scene.renderer.importProject(json)
                Toast.makeText(this,"تم استيراد ملف المشروع",Toast.LENGTH_LONG).show()
            }else if(requestCode==4103){
                val photo=decodeMeasurePhoto(data.data!!)
                showPhotoMeasure(photo)
            }
        }catch(e:Exception){Toast.makeText(this,"تعذر تنفيذ العملية: ${e.message}",Toast.LENGTH_LONG).show()}
    }
    private fun saveNamedProject(){
        val input=EditText(this).apply{hint="اسم المشروع"}
        AlertDialog.Builder(this).setTitle("حفظ مشروع جديد").setView(input).setNegativeButton("إلغاء",null).setPositiveButton("حفظ"){_,_->
            val name=input.text.toString().trim()
            if(name.isBlank()){Toast.makeText(this,"اكتب اسم المشروع أولاً",Toast.LENGTH_SHORT).show();return@setPositiveButton}
            val prefs=getSharedPreferences("shadow_build_projects",MODE_PRIVATE)
            val names=prefs.getStringSet("names",emptySet())?.toMutableSet()?:mutableSetOf()
            prefs.edit().putString("project_"+name,scene.renderer.exportProject()).putStringSet("names",names.apply{add(name)}).apply()
            info.text="تم حفظ المشروع: $name"
        }.show()
    }
    private fun openProjectList(){
        val prefs=getSharedPreferences("shadow_build_projects",MODE_PRIVATE)
        val names=prefs.getStringSet("names",emptySet())?.toList()?.sorted()?:emptyList()
        if(names.isEmpty()){Toast.makeText(this,"لا توجد مشاريع محفوظة بعد",Toast.LENGTH_SHORT).show();return}
        AlertDialog.Builder(this).setTitle("اختر مشروعاً لفتحه").setItems(names.toTypedArray()){_,which->
            val name=names[which]
            prefs.getString("project_"+name,null)?.let{scene.renderer.importProject(it);info.text="تم فتح المشروع: $name"}
        }.setNegativeButton("إغلاق",null).show()
    }
    private fun saveSceneImage(){
        scene.renderer.captureWhenReady { bitmap ->
            runOnUiThread {
                try {
                    val values=ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME,"SHADOW_BUILD_${System.currentTimeMillis()}.png")
                        put(MediaStore.Images.Media.MIME_TYPE,"image/png")
                        if(Build.VERSION.SDK_INT>=29)put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/SHADOW BUILD")
                    }
                    val uri=contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)
                    if(uri==null)throw IllegalStateException("تعذر إنشاء ملف الصورة")
                    val stream=contentResolver.openOutputStream(uri)?:throw IllegalStateException("تعذر فتح ملف الصورة")
                    stream.use{bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
                    Toast.makeText(this,"تم حفظ صورة المشروع في الصور",Toast.LENGTH_LONG).show()
                }catch(e:Exception){Toast.makeText(this,"تعذر حفظ الصورة: ${e.message}",Toast.LENGTH_LONG).show()}
            }
        }
    }
    private fun shareReport(){
        val report=scene.renderer.report()
        val send=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_SUBJECT,"تقرير مشروع البناء");putExtra(Intent.EXTRA_TEXT,report)}
        startActivity(Intent.createChooser(send,"مشاركة تقرير البناء"))
    }
    private fun editSelectedDimensions(){
        val current=scene.renderer.selectedDimensions()?:run{info.text="حدد قطعة أولاً";return}
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(24,8,24,0)}
        val a=EditText(this).apply{hint="الطول بالمتر";setText(current[0].toString());inputType=8194}
        val b=EditText(this).apply{hint="الارتفاع بالمتر";setText(current[1].toString());inputType=8194}
        val d=EditText(this).apply{hint="العرض بالمتر";setText(current[2].toString());inputType=8194}
        listOf(a,b,d).forEach{box.addView(it)}
        AlertDialog.Builder(this).setTitle("تعديل أبعاد القطعة").setView(box).setNegativeButton("إلغاء",null).setPositiveButton("تطبيق"){_,_->
            val x=a.text.toString().toFloatOrNull();val y=b.text.toString().toFloatOrNull();val z=d.text.toString().toFloatOrNull()
            if(x!=null&&y!=null&&z!=null&&x>0&&y>0&&z>0)scene.renderer.setSelectedDimensions(x,y,z) else info.text="أدخل أبعاداً صحيحة أكبر من صفر"
        }.show()
    }
    private fun measurementDialog(){
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(22,8,22,0)}
        fun field(hintText:String,value:String="",decimal:Boolean=true)=EditText(this).apply{
            hint=hintText;if(value.isNotEmpty())setText(value);inputType=if(decimal)8194 else 2
        }
        val length=field("طول الجدار بالمتر")
        val height=field("ارتفاع الجدار بالمتر")
        val thickness=field("سماكة الجدار بالمتر","0.20")
        val opening=field("مساحة الأبواب والنوافذ بالمتر المربع","0")
        val blockL=field("طول البلوك بالمتر","0.40")
        val blockH=field("ارتفاع البلوك بالمتر","0.20")
        val joint=field("سماكة المونة بين البلوكات بالمتر","0.01")
        val price=field("سعر البلوكة الواحدة (اختياري)","0")
        val fields=listOf(length,height,thickness,opening,blockL,blockH,joint,price)
        fields.forEach{box.addView(it)}
        AlertDialog.Builder(this).setTitle("التمتير وحساب البلوك").setView(ScrollView(this).apply{addView(box)})
            .setNegativeButton("إلغاء",null).setPositiveButton("احسب"){_,_->
                val l=length.text.toString().toDoubleOrNull()?:0.0
                val h=height.text.toString().toDoubleOrNull()?:0.0
                val t=thickness.text.toString().toDoubleOrNull()?:0.0
                val op=opening.text.toString().toDoubleOrNull()?:0.0
                val bl=blockL.text.toString().toDoubleOrNull()?:.4
                val bh=blockH.text.toString().toDoubleOrNull()?:.2
                val j=joint.text.toString().toDoubleOrNull()?:.01
                val unit=price.text.toString().toDoubleOrNull()?:0.0
                val gross=l*h
                val net=(gross-op).coerceAtLeast(0.0)
                val module=(bl+j)*(bh+j)
                val count=if(module>0)kotlin.math.ceil(net/module).toInt() else 0
                val volume=net*t
                val cost=count*unit
                val mortar=(volume-count*bl*bh*t).coerceAtLeast(0.0)
                val result="مساحة الجدار الإجمالية: %.2f م²\\nمساحة الفتحات: %.2f م²\\nالمساحة الصافية: %.2f م²\\nحجم الجدار التقريبي: %.3f م³\\nعدد البلوك التقديري: %d قطعة\\nحجم المونة التقريبي: %.3f م³\\nتكلفة البلوك حسب السعر المدخل: %.2f".format(gross,op,net,volume,count,mortar,cost)
                AlertDialog.Builder(this).setTitle("نتيجة التمتير").setMessage(result)
                    .setNeutralButton("نسخ التقرير"){_,_->(getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("التمتير",result))}
                    .setPositiveButton("إغلاق",null).show()
            }.show()
    }
    override fun onResume(){super.onResume();if(::scene.isInitialized)scene.onResume()}
    override fun onPause(){if(::scene.isInitialized)scene.onPause();super.onPause()}
}

private class BuildSurface(activity:Activity):GLSurfaceView(activity){
    val renderer=BuildRenderer()
    var onTap:((Float,Float)->Unit)?=null
    private var draggingPart=false
    private var lastX=0f;private var lastY=0f;private var lastDistance=0f;private var moved=false
    init{setEGLContextClientVersion(2);setRenderer(renderer);renderMode=RENDERMODE_CONTINUOUSLY}
    override fun onTouchEvent(e:MotionEvent):Boolean{
        when(e.actionMasked){
            MotionEvent.ACTION_DOWN->{lastX=e.x;lastY=e.y;moved=false;draggingPart=renderer.pick(e.x,e.y,width,height);if(draggingPart)renderer.beginMove();return true}
            MotionEvent.ACTION_POINTER_DOWN->{lastDistance=distance(e);return true}
            MotionEvent.ACTION_MOVE->{
                if(e.pointerCount>=2){
                    val d=distance(e);if(lastDistance>0f)renderer.zoom((lastDistance-d)*.018f);lastDistance=d;moved=true
                }else{
                    val dx=e.x-lastX;val dy=e.y-lastY
                    if(abs(dx)+abs(dy)>3)moved=true
                    if(draggingPart)renderer.moveSelected(dx,dy,width,height) else renderer.rotate(dx*.45f,dy*.35f);lastX=e.x;lastY=e.y
                };return true
            }
            MotionEvent.ACTION_POINTER_UP->{lastDistance=0f;return true}
            MotionEvent.ACTION_UP->{if(!moved&&!draggingPart)onTap?.invoke(e.x,e.y);draggingPart=false;performClick();return true}
        };return true
    }
    private fun distance(e:MotionEvent):Float{if(e.pointerCount<2)return 0f;val dx=e.getX(0)-e.getX(1);val dy=e.getY(0)-e.getY(1);return hypot(dx,dy)}
    override fun performClick():Boolean{super.performClick();return true}
}

private class BuildRenderer:GLSurfaceView.Renderer{
    @Volatile private var captureCallback:((Bitmap)->Unit)?=null
    fun captureWhenReady(callback:(Bitmap)->Unit){captureCallback=callback}
    data class Part(val kind:String,val x:Float,val y:Float,val z:Float,val sx:Float,val sy:Float,val sz:Float,val nailed:Boolean)
    private val parts=mutableListOf<Part>()
    @Volatile private var projectNotes=""
    fun setNotes(value:String){projectNotes=value}
    fun getNotes():String=projectNotes
    private val undoStack=java.util.ArrayDeque<List<Part>>()
    private val redoStack=java.util.ArrayDeque<List<Part>>()
    private fun remember(){if(undoStack.size>=40)undoStack.removeFirst();undoStack.addLast(parts.toList());redoStack.clear()}
    fun undo(){synchronized(parts){if(undoStack.isNotEmpty()){redoStack.addLast(parts.toList());parts.clear();parts.addAll(undoStack.removeLast());selectedIndex=(selectedIndex.coerceAtMost(parts.lastIndex)).coerceAtLeast(-1)}}}
    fun redo(){synchronized(parts){if(redoStack.isNotEmpty()){undoStack.addLast(parts.toList());parts.clear();parts.addAll(redoStack.removeLast());selectedIndex=(selectedIndex.coerceAtMost(parts.lastIndex)).coerceAtLeast(-1)}}}
    @Volatile var angleY=35f
    @Volatile var angleX=22f
    @Volatile var cameraDistance=9f
    @Volatile var woodLength=3f
    @Volatile var nailing=false
    @Volatile var gridVisible=true
    private var selectedIndex=-1
    private var viewportW=1
    private var viewportH=1
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
    private lateinit var gridBuffer:FloatBuffer
    private val vertexShader="attribute vec4 aPosition;attribute vec4 aColor;uniform mat4 uMVP;varying vec4 vColor;void main(){gl_Position=uMVP*aPosition;vColor=aColor;}"
    private val fragmentShader="precision mediump float;varying vec4 vColor;void main(){gl_FragColor=vColor;}"
    override fun onSurfaceCreated(gl:GL10?,config:EGLConfig?){
        GLES20.glClearColor(.055f,.085f,.13f,1f);GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        val vs=shader(GLES20.GL_VERTEX_SHADER,vertexShader);val fs=shader(GLES20.GL_FRAGMENT_SHADER,fragmentShader)
        program=GLES20.glCreateProgram();GLES20.glAttachShader(program,vs);GLES20.glAttachShader(program,fs);GLES20.glLinkProgram(program)
        posHandle=GLES20.glGetAttribLocation(program,"aPosition");colorHandle=GLES20.glGetAttribLocation(program,"aColor");mvpHandle=GLES20.glGetUniformLocation(program,"uMVP")
        buffer=ByteBuffer.allocateDirect(verts.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer()
        val grid=FloatArray(84*6);var gi=0
        for(i in 0..20){val v=-5f+i*.5f
            val lines=arrayOf(floatArrayOf(-5f,.004f,v),floatArrayOf(5f,.004f,v),floatArrayOf(v,.004f,-5f),floatArrayOf(v,.004f,5f))
            lines.forEach{p->grid[gi++]=p[0];grid[gi++]=p[1];grid[gi++]=p[2];grid[gi++]=.30f;grid[gi++]=.40f;grid[gi++]=.50f}
        }
        gridBuffer=ByteBuffer.allocateDirect(grid.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer();gridBuffer.put(grid);gridBuffer.position(0)
    }
    override fun onSurfaceChanged(gl:GL10?,w:Int,h:Int){viewportW=w;viewportH=h;GLES20.glViewport(0,0,w,h);Matrix.perspectiveM(projection,0,45f,w.toFloat()/h.coerceAtLeast(1),.1f,100f)}
    override fun onDrawFrame(gl:GL10?){
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        Matrix.setLookAtM(view,0,0f,4.2f,cameraDistance,0f,1f,0f,0f,1f,0f)
        Matrix.rotateM(view,0,angleX,1f,0f,0f);Matrix.rotateM(view,0,angleY,0f,1f,0f)
        GLES20.glUseProgram(program)
        drawCube(0f,-.12f,0f,7f,.12f,7f,floatArrayOf(.22f,.27f,.32f))
        val copy=synchronized(parts){parts.toList()}
        copy.forEachIndexed{index,p->
            val color=when(p.kind){"بلوك"->floatArrayOf(.68f,.53f,.36f);"لوح"->floatArrayOf(.57f,.32f,.14f);else->floatArrayOf(.75f,.52f,.25f)}
            if(index==selectedIndex){color[0]=(color[0]+.25f).coerceAtMost(1f);color[1]=(color[1]+.25f).coerceAtMost(1f)}
            drawCube(p.x,p.y,p.z,p.sx,p.sy,p.sz,color)
            if(p.nailed)drawCube(p.x,p.y+p.sy/2+.018f,p.z,.045f,.035f,.045f,floatArrayOf(.78f,.82f,.88f))
        }
        deliverCaptureIfRequested()
    }
    private fun deliverCaptureIfRequested(){
        val callback=captureCallback?:return
        captureCallback=null
        val w=viewportW.coerceAtLeast(1);val h=viewportH.coerceAtLeast(1)
        val bytes=ByteBuffer.allocateDirect(w*h*4).order(ByteOrder.nativeOrder())
        GLES20.glReadPixels(0,0,w,h,GLES20.GL_RGBA,GLES20.GL_UNSIGNED_BYTE,bytes)
        bytes.position(0);val raw=IntArray(w*h);bytes.asIntBuffer().get(raw);val pixels=IntArray(w*h)
        for(y in 0 until h)for(x in 0 until w){val v=raw[y*w+x];val r=v and 255;val g=(v shr 8) and 255;val b=(v shr 16) and 255;val a=(v ushr 24) and 255;pixels[(h-1-y)*w+x]=(a shl 24) or (r shl 16) or (g shl 8) or b}
        val bitmap=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);bitmap.setPixels(pixels,0,w,0,0,w,h);callback(bitmap)
    }
    private fun drawGrid(){
        if(!gridVisible)return
        Matrix.setIdentityM(model,0);Matrix.multiplyMM(mvp,0,view,0,model,0);Matrix.multiplyMM(finalM,0,projection,0,mvp,0)
        GLES20.glUniformMatrix4fv(mvpHandle,1,false,finalM,0)
        gridBuffer.position(0);GLES20.glEnableVertexAttribArray(posHandle);GLES20.glVertexAttribPointer(posHandle,3,GLES20.GL_FLOAT,false,24,gridBuffer)
        gridBuffer.position(3);GLES20.glEnableVertexAttribArray(colorHandle);GLES20.glVertexAttribPointer(colorHandle,3,GLES20.GL_FLOAT,false,24,gridBuffer)
        GLES20.glLineWidth(1f);GLES20.glDrawArrays(GLES20.GL_LINES,0,84)
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
    fun setView(view:String){when(view){"top"->{angleX=82f;angleY=0f;cameraDistance=10f};"front"->{angleX=0f;angleY=0f;cameraDistance=9f};"side"->{angleX=0f;angleY=90f;cameraDistance=9f};else->{angleX=22f;angleY=35f;cameraDistance=9f}}}
    fun rotate(dx:Float,dy:Float){angleY+=dx;angleX=(angleX+dy).coerceIn(-55f,85f)}
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
    fun addAt(kind:String,sx:Float,sy:Float,w:Int,h:Int){
        val x=((sx/w.coerceAtLeast(1).toFloat())-.5f)*6f
        val z=((.5f-(sy/h.coerceAtLeast(1).toFloat()))*6f)
        synchronized(parts){
            remember()
            val dims=when(kind){"بلوك"->Triple(.68f,.32f,.32f);"لوح"->Triple(woodLength,.035f,.10f);"دعامة"->Triple(.07f,woodLength,.07f);"قالب"->Triple(woodLength,.08f,.18f);else->Triple(woodLength,.07f,.07f)}
            parts.add(Part(kind,x,dims.second/2,z,dims.first,dims.second,dims.third,nailing));selectedIndex=parts.lastIndex
        }
    }
    fun addOnTop(kind:String){synchronized(parts){
        if(selectedIndex !in parts.indices)return
        remember()
        val base=parts[selectedIndex]
        val dims=when(kind){"بلوك"->Triple(.68f,.32f,.32f);"لوح"->Triple(woodLength,.035f,.10f);"دعامة"->Triple(.07f,woodLength,.07f);"قالب"->Triple(woodLength,.08f,.18f);else->Triple(woodLength,.07f,.07f)}
        parts.add(Part(kind,base.x,base.y+base.sy/2+dims.second/2,base.z,dims.first,dims.second,dims.third,nailing))
        selectedIndex=parts.lastIndex
    }}
    fun raiseSelected(amount:Float){synchronized(parts){if(selectedIndex in parts.indices){remember();val p=parts[selectedIndex];parts[selectedIndex]=p.copy(y=(p.y+amount).coerceAtLeast(p.sy/2))}}}
    fun addRow(){synchronized(parts){remember();for(i in 0 until 8)parts.add(Part("بلوك",(i-3.5f)*.68f,.16f,0f,.68f,.32f,.32f,nailing));selectedIndex=parts.lastIndex}}
    fun addWall(){synchronized(parts){remember();for(row in 0 until 4)for(i in 0 until 8){val shift=if(row%2==0)0f else .34f;parts.add(Part("بلوك",(i-3.5f)*.68f+shift,.16f+row*.32f,0f,.68f,.32f,.32f,nailing))};selectedIndex=parts.lastIndex}}
    fun pick(px:Float,py:Float,w:Int,h:Int):Boolean{
        synchronized(parts){
            var best=-1;var bestDist=Float.MAX_VALUE
            val viewM=FloatArray(16);val projM=FloatArray(16);val modelM=FloatArray(16);val vp=FloatArray(16);val clip=FloatArray(4)
            Matrix.setLookAtM(viewM,0,0f,4.2f,cameraDistance,0f,1f,0f,0f,1f,0f)
            Matrix.rotateM(viewM,0,angleX,1f,0f,0f);Matrix.rotateM(viewM,0,angleY,0f,1f,0f)
            Matrix.perspectiveM(projM,0,45f,w.toFloat()/h.coerceAtLeast(1),.1f,100f);Matrix.multiplyMM(vp,0,projM,0,viewM,0)
            parts.forEachIndexed{i,p->
                Matrix.setIdentityM(modelM,0);Matrix.translateM(modelM,0,p.x,p.y,p.z);val m=FloatArray(16);Matrix.multiplyMM(m,0,vp,0,modelM,0)
                Matrix.multiplyMV(clip,0,m,0,floatArrayOf(0f,0f,0f,1f),0)
                if(clip[3]>0){val x=(clip[0]/clip[3]+1f)*w/2f;val y=(1f-clip[1]/clip[3])*h/2f;val dist=hypot(px-x,py-y);if(dist<bestDist&&dist<65f){bestDist=dist;best=i}}
            }
            selectedIndex=best;return best>=0
        }
    }
    fun beginMove(){synchronized(parts){if(selectedIndex in parts.indices)remember()}}
    fun moveSelected(dx:Float,dy:Float,w:Int,h:Int){synchronized(parts){if(selectedIndex in parts.indices){val p=parts[selectedIndex];val wx=dx/w.coerceAtLeast(1)*6f;val wz=dy/h.coerceAtLeast(1)*-6f;val a=Math.toRadians(angleY.toDouble());val rx=(wx*cos(a)-wz*sin(a)).toFloat();val rz=(wx*sin(a)+wz*cos(a)).toFloat();parts[selectedIndex]=p.copy(x=p.x+rx,z=p.z+rz)}}}
    fun deleteSelected(){synchronized(parts){if(selectedIndex in parts.indices){remember();parts.removeAt(selectedIndex)};selectedIndex=(selectedIndex.coerceAtMost(parts.lastIndex)).coerceAtLeast(-1)}}
    fun selectedDimensions():FloatArray?=synchronized(parts){if(selectedIndex in parts.indices){val p=parts[selectedIndex];floatArrayOf(p.sx,p.sy,p.sz)}else null}
    fun setSelectedDimensions(x:Float,y:Float,z:Float){synchronized(parts){if(selectedIndex in parts.indices){remember();val p=parts[selectedIndex];parts[selectedIndex]=p.copy(sx=x,sy=y,sz=z)}}}
    fun selectLast(){synchronized(parts){selected=parts.lastIndex;selectedIndex=parts.lastIndex}}
    private var selected=-1
    fun duplicateLast(){synchronized(parts){if(selectedIndex in parts.indices){remember();val p=parts[selectedIndex];parts.add(p.copy(x=p.x+.45f,z=p.z+.35f));selected=parts.lastIndex;selectedIndex=parts.lastIndex}else if(parts.isNotEmpty()){remember();val p=parts.last();parts.add(p.copy(x=p.x+.45f,z=p.z+.35f));selectedIndex=parts.lastIndex}}}
    fun rotateLast(){synchronized(parts){if(selectedIndex in parts.indices){remember();val p=parts[selectedIndex];parts[selectedIndex]=p.copy(sx=p.sz,sz=p.sx)}else if(parts.isNotEmpty()){remember();val p=parts.last();parts[parts.lastIndex]=p.copy(sx=p.sz,sz=p.sx);selectedIndex=parts.lastIndex}}}
    fun deleteLast(){synchronized(parts){if(parts.isNotEmpty())parts.removeAt(parts.lastIndex);selected=(parts.size-1).coerceAtLeast(-1)}}
    fun exportProject():String{synchronized(parts){val a=org.json.JSONArray();parts.forEach{p->a.put(org.json.JSONObject().put("kind",p.kind).put("x",p.x).put("y",p.y).put("z",p.z).put("sx",p.sx).put("sy",p.sy).put("sz",p.sz).put("nailed",p.nailed))};return org.json.JSONObject().put("parts",a).put("notes",projectNotes).toString()}}
    fun importProject(data:String){try{val root=org.json.JSONObject(data);projectNotes=root.optString("notes","");val a=root.getJSONArray("parts");synchronized(parts){parts.clear();for(i in 0 until a.length()){val o=a.getJSONObject(i);parts.add(Part(o.optString("kind","بلوك"),o.optDouble("x").toFloat(),o.optDouble("y").toFloat(),o.optDouble("z").toFloat(),o.optDouble("sx",.5).toFloat(),o.optDouble("sy",.2).toFloat(),o.optDouble("sz",.2).toFloat(),o.optBoolean("nailed")))}}}catch(_:Exception){}}
    fun report():String{synchronized(parts){val blocks=parts.count{it.kind=="بلوك"};val wood=parts.size-blocks;val volume=parts.sumOf{(it.sx*it.sy*it.sz).toDouble()};return "تقرير ورشة البناء\\nإجمالي القطع: ${parts.size}\\nالبلوكات: $blocks\\nالخشب والقطع الأخرى: $wood\\nالحجم التقريبي: %.3f م³\\n\\nشرح المشروع:\\n$projectNotes".format(volume)}}
    fun removeLast(){synchronized(parts){if(parts.isNotEmpty())parts.removeAt(parts.lastIndex);selectedIndex=parts.lastIndex}}
    fun clear(){synchronized(parts){if(parts.isNotEmpty())remember();parts.clear();selectedIndex=-1}}
    private fun shader(type:Int,src:String):Int{val s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);return s}
}
