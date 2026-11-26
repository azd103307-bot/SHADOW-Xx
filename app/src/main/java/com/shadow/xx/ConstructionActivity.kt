package com.shadow.xx

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.net.Uri
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
    private var measureMode=false
    private var measureStart:Pair<Float,Float>?=null
    private var measureCaptureUri:Uri?=null
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
        btn(buildRow,"جدار"){wallBuilderDialog();}
        btn(buildRow,"قالب صب"){selected="قالب";info.text="المس المشهد لإضافة قالب الصب"}
        btn(buildRow,"قطعة فوقها"){scene.renderer.addOnTop(selected);info.text="أضيفت قطعة فوق القطعة المحددة"}
        btn(buildRow,"مكتبة القطع"){openPartLibrary()}
        controls.addView(buildRow)
        val cameraRow=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(cameraRow,"منظور"){scene.renderer.setView("perspective")}
        btn(cameraRow,"أعلى"){scene.renderer.setView("top")}
        btn(cameraRow,"أمام"){scene.renderer.setView("front")}
        btn(cameraRow,"جانب"){scene.renderer.setView("side")}
        btn(cameraRow,"الشبكة"){scene.renderer.gridVisible=!scene.renderer.gridVisible;info.text=if(scene.renderer.gridVisible)"شبكة القياس ظاهرة" else "شبكة القياس مخفية"}
        controls.addView(cameraRow)
        val snapRow=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(snapRow,"محاذاة الشبكة"){scene.renderer.snapEnabled=!scene.renderer.snapEnabled;info.text=if(scene.renderer.snapEnabled)"المحاذاة إلى 10 سم مفعّلة" else "المحاذاة متوقفة"}
        btn(snapRow,"قياس مسافة"){measureMode=!measureMode;measureStart=null;info.text=if(measureMode)"المس نقطة البداية على أرضية المشهد" else "تم إيقاف القياس"}
        controls.addView(snapRow)
        info=TextView(this).apply{text="اختر بلوك أو خشب، ثم المس المشهد لإضافته. اسحب بإصبعك للتدوير وبإصبعين للتقريب.";textSize=12f;setTextColor(0xFFE2E8F0.toInt());gravity=Gravity.CENTER;setPadding(2,dp(5),2,dp(5))}
        controls.addView(info)
        val third=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(third,"تراجع"){scene.renderer.undo()}
        btn(third,"إعادة"){scene.renderer.redo()}
        btn(third,"مسح الكل"){confirmClearScene()}
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
        btn(fifth,"فتح المشروع"){getSharedPreferences("shadow_build",MODE_PRIVATE).getString("project",null)?.let{if(scene.renderer.importProject(it))info.text="تم فتح المشروع" else info.text="ملف المشروع غير صالح"}?:run{info.text="لا يوجد مشروع محفوظ"}}
        btn(fifth,"التمتير"){measurementDialog()}
        btn(fifth,"تقرير ونسخ"){val t=scene.renderer.report();(getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("تقرير البناء",t));info.text="تم نسخ تقرير الكميات"}
        btn(fifth,"تكلفة المشروع"){projectCostDialog()}
        controls.addView(fifth)
        val sixth=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(sixth,"حفظ باسم"){saveNamedProject()}
        btn(sixth,"مشاريعي المحفوظة"){openProjectList()}
        btn(sixth,"مشاركة التقرير"){shareReport()}
        btn(sixth,"حفظ صورة"){saveSceneImage()}
        controls.addView(sixth)
        val colorRow=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(colorRow,"طبيعي"){scene.renderer.activeColor=0}
        btn(colorRow,"رمادي"){scene.renderer.activeColor=1}
        btn(colorRow,"أحمر"){scene.renderer.activeColor=2}
        btn(colorRow,"أخضر"){scene.renderer.activeColor=3}
        btn(colorRow,"تلوين المحددة"){scene.renderer.applyColor();info.text="تم تغيير لون القطعة المحددة"}
        controls.addView(colorRow)
        val seventh=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(seventh,"تصدير ملف المشروع"){exportProjectFile()}
        btn(seventh,"استيراد ملف مشروع"){importProjectFile()}
        btn(seventh,"اختيار صورة"){chooseMeasurePhoto()}
        btn(seventh,"التقاط بالكاميرا"){captureMeasurePhoto()}
        btn(seventh,"حذف مشروع"){deleteProject()}
        controls.addView(seventh)
        val eighth=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(eighth,"شرح وخطوات المشروع"){editProjectNotes()}
        btn(eighth,"عرض الشرح"){showProjectNotes()}
        controls.addView(eighth)
        val ninth=LinearLayout(this).apply{gravity=Gravity.CENTER}
        btn(ninth,"تصدير مخطط DXF"){exportDxfFile()}
        btn(ninth,"تصدير OBJ ثلاثي الأبعاد"){exportObjFile()}
        controls.addView(ninth)
        val controlsScroll=ScrollView(this).apply{isFillViewport=false;addView(controls)}
        val panelHeight=if(resources.configuration.orientation==Configuration.ORIENTATION_LANDSCAPE)dp(170) else dp(285)
        root.addView(controlsScroll,LinearLayout.LayoutParams(-1,panelHeight));setContentView(root)
        scene.onTap={x,y->
            if(measureMode){
                val point=scene.renderer.groundPointAt(x,y,scene.width,scene.height)
                if(point==null){info.text="تعذر تحديد النقطة؛ جرّب مكاناً آخر"}
                else{
                    val start=measureStart
                    if(start==null){measureStart=point;info.text="تم تحديد البداية؛ المس نقطة النهاية"}
                    else{
                        val distance=hypot((point.first-start.first).toDouble(),(point.second-start.second).toDouble())
                        info.text="المسافة الأفقية: %.3f متر (%.1f سم)".format(distance,distance*100)
                        measureStart=null;measureMode=false
                    }
                }
            }else scene.renderer.addAt(selected,x,y,scene.width,scene.height)
        }
        scene.onSelect={val d=scene.renderer.selectedDimensions();if(d!=null)info.text="المحدد: طول %.2f م × ارتفاع %.2f م × عرض %.2f م".format(d[0],d[1],d[2])}
        restoreAutosave()
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
    private fun wallBuilderDialog(){
        val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(22,8,22,0)}
        fun field(label:String,value:String)=EditText(this).apply{hint=label;setText(value);inputType=8194}
        val length=field("طول الجدار بالمتر","4")
        val height=field("ارتفاع الجدار بالمتر","2.8")
        val thickness=field("سماكة الجدار بالمتر","0.20")
        val blockLength=field("طول البلوك بالمتر","0.40")
        val blockHeight=field("ارتفاع البلوك بالمتر","0.20")
        val joint=field("فاصل المونة بالمتر","0.01")
        val openingWidth=field("عرض فتحة باب أو نافذة (0 بدون فتحة)","0")
        val openingHeight=field("ارتفاع الفتحة بالمتر","0")
        val openingBottom=field("ارتفاع بداية الفتحة عن الأرض بالمتر","0")
        listOf(length,height,thickness,blockLength,blockHeight,joint,openingWidth,openingHeight,openingBottom).forEach{panel.addView(it)}
        AlertDialog.Builder(this).setTitle("إنشاء جدار بمقاساتك").setView(ScrollView(this).apply{addView(panel)})
            .setNegativeButton("إلغاء",null).setPositiveButton("إنشاء"){_,_->
                val l=length.text.toString().toFloatOrNull()?:0f
                val h=height.text.toString().toFloatOrNull()?:0f
                val t=thickness.text.toString().toFloatOrNull()?:0f
                val bl=blockLength.text.toString().toFloatOrNull()?:0f
                val bh=blockHeight.text.toString().toFloatOrNull()?:0f
                val j=joint.text.toString().toFloatOrNull()?:0f
                val ow=openingWidth.text.toString().toFloatOrNull()?:0f
                val oh=openingHeight.text.toString().toFloatOrNull()?:0f
                val ob=openingBottom.text.toString().toFloatOrNull()?:0f
                if(!l.isFinite()||!h.isFinite()||!t.isFinite()||!bl.isFinite()||!bh.isFinite()||!j.isFinite()||!ow.isFinite()||!oh.isFinite()||!ob.isFinite()||
                    l !in .1f..30f||h !in .1f..20f||t !in .05f..2f||bl !in .05f..2f||bh !in .05f..1f||j !in 0f..0.05f||
                    ow !in 0f..l||oh<0f||ob<0f||ob+oh>h){
                    Toast.makeText(this,"تحقق من الأبعاد المدخلة",Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                val added=scene.renderer.addCustomWall(l,h,t,bl,bh,j,ow,oh,ob)
                if(added>0)info.text="تم إنشاء الجدار: $added قطعة، بطول $l م وارتفاع $h م"+if(ow>0f&&oh>0f)" مع فتحة" else ""
                else info.text="الجدار كبير جداً أو تعذر إنشاؤه"
            }.show()
    }
    private fun openPartLibrary(){
        val items=arrayOf("بلوك","طوب","لوح خشب","مربع خشب","دعامة","قالب","عمود خرسانة","كمرة خرسانة","بلاطة خرسانة","قاعدة خرسانية","سلم خرسانة")
        AlertDialog.Builder(this).setTitle("مكتبة مواد البناء").setItems(items){_,which->
            selected=when(which){2->"لوح";3->"مربع";4->"دعامة";5->"قالب";6->"عمود";7->"كمرة";8->"بلاطة";9->"قاعدة";10->"سلم";else->items[which]}
            info.text="القطعة المختارة: ${items[which]} — المس المشهد لوضعها"
        }.show()
    }
    private fun confirmClearScene(){
        AlertDialog.Builder(this).setTitle("مسح مساحة العمل")
            .setMessage("هل تريد حذف جميع القطع من المشهد؟ سيتم حفظ الحالة السابقة في التراجع.")
            .setNegativeButton("إلغاء",null).setPositiveButton("مسح"){_,_->scene.renderer.clear();info.text="تم مسح مساحة العمل"}.show()
    }
    private fun showProjectNotes(){
        val steps=scene.renderer.getNotes().lineSequence().map{it.trim()}.filter{it.isNotEmpty()}.toList()
        if(steps.isEmpty()){Toast.makeText(this,"لم تكتب شرحاً للمشروع بعد",Toast.LENGTH_SHORT).show();return}
        var index=0
        val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(22,12,22,8)}
        val counter=TextView(this).apply{textSize=13f;gravity=Gravity.CENTER;setTextColor(0xFF64748B.toInt())}
        val step=TextView(this).apply{textSize=19f;setTextColor(0xFF172033.toInt());setPadding(8,24,8,24);minHeight=dp(150);gravity=Gravity.CENTER_VERTICAL;textDirection=android.view.View.TEXT_DIRECTION_LOCALE}
        val nav=LinearLayout(this).apply{gravity=Gravity.CENTER}
        val previous=Button(this).apply{text="السابق";isAllCaps=false}
        val next=Button(this).apply{text="التالي";isAllCaps=false}
        fun render(){
            counter.text="الخطوة ${index+1} من ${steps.size}"
            step.text=steps[index]
            previous.isEnabled=index>0
            next.isEnabled=index<steps.lastIndex
        }
        previous.setOnClickListener{index=(index-1).coerceAtLeast(0);render()}
        next.setOnClickListener{index=(index+1).coerceAtMost(steps.lastIndex);render()}
        nav.addView(previous,LinearLayout.LayoutParams(0,dp(48),1f))
        nav.addView(next,LinearLayout.LayoutParams(0,dp(48),1f))
        panel.addView(counter);panel.addView(step);panel.addView(nav)
        render()
        AlertDialog.Builder(this).setTitle("عرض شرح البناء خطوة بخطوة").setView(panel)
            .setNegativeButton("إنهاء",null).show()
    }
    private fun editProjectNotes(){
        val editor=EditText(this).apply{
            hint="اكتب كل خطوة في سطر مستقل، وسيتم عرضها للآخرين خطوة بخطوة..."
            setText(scene.renderer.getNotes())
            minLines=5;gravity=Gravity.TOP;inputType=android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
        }
        AlertDialog.Builder(this).setTitle("شرح المشروع وخطواته").setView(editor)
            .setNegativeButton("إلغاء",null).setPositiveButton("حفظ"){_,_->
                scene.renderer.setNotes(editor.text.toString())
                info.text="تم حفظ شرح المشروع ضمن ملفه"
            }.show()
    }
    private fun captureMeasurePhoto(){
        try{
            val values=ContentValues().apply{
                put(MediaStore.Images.Media.DISPLAY_NAME,"SHADOW_MEASURE_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg")
                if(Build.VERSION.SDK_INT>=29)put(MediaStore.Images.Media.RELATIVE_PATH,Environment.DIRECTORY_PICTURES+"/SHADOW BUILD")
            }
            val uri=contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,values)
                ?:throw IllegalStateException("تعذر تجهيز مكان للصورة")
            measureCaptureUri=uri
            val intent=Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply{
                putExtra(MediaStore.EXTRA_OUTPUT,uri)
                clipData=android.content.ClipData.newUri(contentResolver,"measurement",uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            }
            try{startActivityForResult(intent,4104)}
            catch(e:Exception){contentResolver.delete(uri,null,null);measureCaptureUri=null;throw e}
        }catch(e:Exception){Toast.makeText(this,"تعذر تشغيل الكاميرا: ${e.message}",Toast.LENGTH_LONG).show()}
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
    private fun exportObjFile(){
        val intent=Intent(Intent.ACTION_CREATE_DOCUMENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE);type="application/octet-stream"
            putExtra(Intent.EXTRA_TITLE,"SHADOW_BUILD_model.obj")
        }
        startActivityForResult(intent,4106)
    }
    private fun exportDxfFile(){
        val intent=Intent(Intent.ACTION_CREATE_DOCUMENT).apply{
            addCategory(Intent.CATEGORY_OPENABLE);type="application/dxf"
            putExtra(Intent.EXTRA_TITLE,"SHADOW_BUILD_plan.dxf")
        }
        startActivityForResult(intent,4105)
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
        if(resultCode!=RESULT_OK){
            if(requestCode==4104){measureCaptureUri?.let{contentResolver.delete(it,null,null)};measureCaptureUri=null}
            return
        }
        try{
            if(requestCode==4104){
                val cameraUri=measureCaptureUri?:throw IllegalStateException("لم يتم حفظ الصورة")
                showPhotoMeasure(decodeMeasurePhoto(cameraUri))
                measureCaptureUri=null
                return
            }
            val uri=data?.data?:return
            if(requestCode==4106){
                val obj=scene.renderer.exportObj()
                val output=contentResolver.openOutputStream(uri)?:throw IllegalStateException("تعذر إنشاء ملف OBJ")
                output.use{it.write(obj.toByteArray(Charsets.UTF_8))}
                Toast.makeText(this,"تم تصدير النموذج ثلاثي الأبعاد OBJ",Toast.LENGTH_LONG).show()
            }else if(requestCode==4105){
                val dxf=scene.renderer.exportDxf()
                val output=contentResolver.openOutputStream(uri)?:throw IllegalStateException("تعذر إنشاء ملف DXF")
                output.use{it.write(dxf.toByteArray(Charsets.UTF_8))}
                Toast.makeText(this,"تم تصدير المخطط DXF",Toast.LENGTH_LONG).show()
            }else if(requestCode==4101){
                val json=scene.renderer.exportProject()
                val output=contentResolver.openOutputStream(uri)?:throw IllegalStateException("تعذر إنشاء الملف")
                output.use{it.write(json.toByteArray(Charsets.UTF_8))}
                Toast.makeText(this,"تم تصدير ملف المشروع",Toast.LENGTH_LONG).show()
            }else if(requestCode==4102){
                val json=contentResolver.openInputStream(uri)?.bufferedReader()?.use{it.readText()}
                    ?:throw IllegalStateException("تعذر قراءة الملف")
                if(!scene.renderer.importProject(json))throw IllegalArgumentException("ملف المشروع غير صالح")
                Toast.makeText(this,"تم استيراد ملف المشروع",Toast.LENGTH_LONG).show()
            }else if(requestCode==4103){
                showPhotoMeasure(decodeMeasurePhoto(uri))
            }
        }catch(e:Exception){Toast.makeText(this,"تعذر تنفيذ العملية: ${e.message}",Toast.LENGTH_LONG).show()}
    }
    private fun restoreAutosave(){
        val saved=getSharedPreferences("shadow_build",MODE_PRIVATE).getString("autosave",null)?:return
        try{
            val count=org.json.JSONObject(saved).getJSONArray("parts").length()
            if(count>0&&scene.renderer.importProject(saved))info.text="تمت استعادة آخر مشروع تلقائياً"
        }catch(_:Exception){}
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
            prefs.getString("project_"+name,null)?.let{if(scene.renderer.importProject(it))info.text="تم فتح المشروع: $name" else info.text="ملف المشروع غير صالح"}
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
    private fun projectCostDialog(){
        val panel=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,8,20,0)}
        fun priceField(label:String)=EditText(this).apply{hint=label;inputType=8194}
        val block=priceField("سعر البلوكة")
        val brick=priceField("سعر الطوبة")
        val timber=priceField("سعر متر الخشب")
        val concrete=priceField("سعر متر الخرسانة المكعب")
        listOf(block,brick,timber,concrete).forEach{panel.addView(it)}
        AlertDialog.Builder(this).setTitle("تقدير تكلفة مواد النموذج").setView(panel)
            .setNegativeButton("إلغاء",null).setPositiveButton("احسب"){_,_->
                val prices=doubleArrayOf(block.text.toString().toDoubleOrNull()?:0.0,brick.text.toString().toDoubleOrNull()?:0.0,timber.text.toString().toDoubleOrNull()?:0.0,concrete.text.toString().toDoubleOrNull()?:0.0)
                val result=scene.renderer.projectCost(prices)
                AlertDialog.Builder(this).setTitle("تقدير التكلفة").setMessage(result).setPositiveButton("إغلاق",null)
                    .setNeutralButton("نسخ"){_,_->(getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("تكلفة المشروع",result))}.show()
            }.show()
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
            if(x!=null&&y!=null&&z!=null&&x.isFinite()&&y.isFinite()&&z.isFinite()&&x in .001f..100f&&y in .001f..100f&&z in .001f..100f)scene.renderer.setSelectedDimensions(x,y,z) else info.text="أدخل أبعاداً صحيحة بين 1 ملم و100 متر"
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
        val waste=field("نسبة هدر البلوك %","5")
        val price=field("سعر البلوكة الواحدة (اختياري)","0")
        val cementPrice=field("سعر كيس الإسمنت 50 كجم (اختياري)","0")
        val sandPrice=field("سعر متر الرمل المكعب (اختياري)","0")
        val fields=listOf(length,height,thickness,opening,blockL,blockH,joint,waste,price,cementPrice,sandPrice)
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
                val wastePct=waste.text.toString().toDoubleOrNull()?:5.0
                val unit=(price.text.toString().toDoubleOrNull()?:0.0).coerceAtLeast(0.0)
                val cementUnit=(cementPrice.text.toString().toDoubleOrNull()?:0.0).coerceAtLeast(0.0)
                val sandUnit=(sandPrice.text.toString().toDoubleOrNull()?:0.0).coerceAtLeast(0.0)
                val gross=l*h
                if(l<=0||h<=0||t<=0||bl<=0||bh<=0||j<0||wastePct !in 0.0..50.0||op<0||op>gross){
                    Toast.makeText(this,"تحقق من الأبعاد والفتحات؛ يجب أن تكون القيم صحيحة وموجبة.",Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                val net=(gross-op).coerceAtLeast(0.0)
                val module=(bl+j)*(bh+j)
                val baseCount=if(module>0)kotlin.math.ceil(net/module).toInt() else 0
                val count=kotlin.math.ceil(baseCount*(1.0+wastePct/100.0)).toInt()
                val volume=net*t
                val mortar=(volume-baseCount*bl*bh*t).coerceAtLeast(0.0)
                val dryMortar=mortar*1.33
                val cementBags=kotlin.math.ceil((dryMortar/5.0*1440.0)/50.0).toInt()
                val sandVolume=dryMortar*4.0/5.0
                val costBlocks=count*unit;val costCement=cementBags*cementUnit;val costSand=sandVolume*sandUnit
                val totalCost=costBlocks+costCement+costSand
                val result="مساحة الجدار الإجمالية: %.2f م²\\nمساحة الفتحات: %.2f م²\\nالمساحة الصافية: %.2f م²\\nحجم الجدار التقريبي: %.3f م³\\nعدد البلوك الأساسي: %d قطعة\\nنسبة الهدر: %.1f%%\\nعدد البلوك بعد الهدر: %d قطعة\\nحجم المونة التقريبي: %.3f م³\\nأكياس الإسمنت (50 كجم) تقديرياً: %d\\nالرمل التقديري: %.3f م³\\nتكلفة البلوك: %.2f\\nتكلفة الإسمنت: %.2f\\nتكلفة الرمل: %.2f\\nإجمالي المواد المقدّر: %.2f\\n\\nملاحظة: تقدير الإسمنت والرمل على خلطة 1:4 ومعامل جاف 1.33، والهدر مطبق على عدد البلوك فقط. الأرقام تقديرية.".format(gross,op,net,volume,baseCount,wastePct,count,mortar,cementBags,sandVolume,costBlocks,costCement,costSand,totalCost)
                AlertDialog.Builder(this).setTitle("نتيجة التمتير").setMessage(result)
                    .setNeutralButton("نسخ التقرير"){_,_->(getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("التمتير",result))}
                    .setPositiveButton("إغلاق",null).show()
            }.show()
    }
    override fun onResume(){super.onResume();if(::scene.isInitialized)scene.onResume()}
    override fun onPause(){if(::scene.isInitialized){getSharedPreferences("shadow_build",MODE_PRIVATE).edit().putString("autosave",scene.renderer.exportProject()).apply();scene.onPause()};super.onPause()}
}

private class BuildSurface(activity:Activity):GLSurfaceView(activity){
    val renderer=BuildRenderer()
    var onTap:((Float,Float)->Unit)?=null
    var onSelect:(()->Unit)?=null
    private var draggingPart=false
    private var moveHistoryStarted=false
    private var lastX=0f;private var lastY=0f;private var lastDistance=0f;private var moved=false
    init{setEGLContextClientVersion(2);setRenderer(renderer);renderMode=RENDERMODE_CONTINUOUSLY}
    override fun onTouchEvent(e:MotionEvent):Boolean{
        when(e.actionMasked){
            MotionEvent.ACTION_DOWN->{lastX=e.x;lastY=e.y;moved=false;moveHistoryStarted=false;draggingPart=renderer.pick(e.x,e.y,width,height);if(draggingPart)onSelect?.invoke();return true}
            MotionEvent.ACTION_POINTER_DOWN->{lastDistance=distance(e);return true}
            MotionEvent.ACTION_MOVE->{
                if(e.pointerCount>=2){
                    val d=distance(e);if(lastDistance>0f)renderer.zoom((lastDistance-d)*.018f);lastDistance=d;moved=true
                }else{
                    val dx=e.x-lastX;val dy=e.y-lastY
                    if(abs(dx)+abs(dy)>3)moved=true
                    if(draggingPart){
                        if(moved){if(!moveHistoryStarted){renderer.beginMove();moveHistoryStarted=true};renderer.moveSelected(dx,dy,width,height)}
                    }else renderer.rotate(dx*.45f,dy*.35f)
                    lastX=e.x;lastY=e.y
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
    data class Part(val kind:String,val x:Float,val y:Float,val z:Float,val sx:Float,val sy:Float,val sz:Float,val nailed:Boolean,val color:Int=0)
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
    @Volatile var snapEnabled=true
    @Volatile var activeColor=0
    @Volatile private var selectedIndex=-1
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
        val grid=FloatArray(484*6);var gi=0
        for(i in 0..120){val v=-30f+i*.5f
            val lines=arrayOf(floatArrayOf(-30f,.004f,v),floatArrayOf(30f,.004f,v),floatArrayOf(v,.004f,-30f),floatArrayOf(v,.004f,30f))
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
        drawCube(0f,-.12f,0f,60f,.12f,60f,floatArrayOf(.22f,.27f,.32f))
        drawGrid()
        val copy=synchronized(parts){parts.toList()}
        copy.forEachIndexed{index,p->
            val color=when(p.color){1->floatArrayOf(.48f,.52f,.58f);2->floatArrayOf(.78f,.22f,.18f);3->floatArrayOf(.20f,.58f,.34f);else->when(p.kind){"بلوك"->floatArrayOf(.68f,.53f,.36f);"طوب"->floatArrayOf(.62f,.30f,.20f);"لوح","مربع","دعامة","قالب"->floatArrayOf(.57f,.32f,.14f);"عمود","كمرة","بلاطة","قاعدة","درجة"->floatArrayOf(.62f,.64f,.67f);else->floatArrayOf(.75f,.52f,.25f)}}
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
        GLES20.glLineWidth(1f);GLES20.glDrawArrays(GLES20.GL_LINES,0,484)
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
    private fun dimensionsFor(kind:String):Triple<Float,Float,Float> = when(kind){
        "بلوك"->Triple(.68f,.32f,.32f)
        "طوب"->Triple(.20f,.06f,.10f)
        "لوح"->Triple(woodLength,.035f,.10f)
        "مربع"->Triple(woodLength,.07f,.07f)
        "دعامة"->Triple(.07f,woodLength,.07f)
        "قالب"->Triple(woodLength,.08f,.18f)
        "عمود"->Triple(.30f,2.5f,.30f)
        "كمرة"->Triple(2f,.25f,.25f)
        "بلاطة"->Triple(2f,.15f,2f)
        "قاعدة"->Triple(1f,.40f,1f)
        "سلم"->Triple(1.2f,1.26f,1.96f)
        "درجة"->Triple(1.2f,.18f,.28f)
        else->Triple(woodLength,.07f,.07f)
    }
    fun setView(view:String){when(view){"top"->{angleX=82f;angleY=0f;cameraDistance=10f};"front"->{angleX=0f;angleY=0f;cameraDistance=9f};"side"->{angleX=0f;angleY=90f;cameraDistance=9f};else->{angleX=22f;angleY=35f;cameraDistance=9f}}}
    fun rotate(dx:Float,dy:Float){angleY+=dx;angleX=(angleX+dy).coerceIn(-55f,85f)}
    fun zoom(delta:Float){cameraDistance=(cameraDistance+delta).coerceIn(4.5f,18f)}
    fun add(kind:String){synchronized(parts){
        val n=parts.size
        val x=((n%8)-3.5f)*.68f
        val z=((n/8)%8-3.5f)*.68f
        val dims=dimensionsFor(kind)
        parts.add(Part(kind,x,dims.second/2,z,dims.first,dims.second,dims.third,nailing))
    }}
    fun groundPointAt(sx:Float,sy:Float,w:Int,h:Int):Pair<Float,Float>?{
        val width=w.coerceAtLeast(1);val height=h.coerceAtLeast(1)
        val viewM=FloatArray(16);val projM=FloatArray(16);val vp=FloatArray(16);val inverse=FloatArray(16)
        Matrix.setLookAtM(viewM,0,0f,4.2f,cameraDistance,0f,1f,0f,0f,1f,0f)
        Matrix.rotateM(viewM,0,angleX,1f,0f,0f);Matrix.rotateM(viewM,0,angleY,0f,1f,0f)
        Matrix.perspectiveM(projM,0,45f,width.toFloat()/height,.1f,100f)
        Matrix.multiplyMM(vp,0,projM,0,viewM,0)
        if(!Matrix.invertM(inverse,0,vp,0))return null
        val nx=2f*sx/width-1f;val ny=1f-2f*sy/height
        val near=FloatArray(4);val far=FloatArray(4)
        Matrix.multiplyMV(near,0,inverse,0,floatArrayOf(nx,ny,-1f,1f),0)
        Matrix.multiplyMV(far,0,inverse,0,floatArrayOf(nx,ny,1f,1f),0)
        if(abs(near[3])<1e-6f||abs(far[3])<1e-6f)return null
        for(i in 0..2){near[i]/=near[3];far[i]/=far[3]}
        val dy=far[1]-near[1]
        if(abs(dy)<1e-6f)return null
        val t=-near[1]/dy
        if(t<0f||!t.isFinite())return null
        val x=(near[0]+(far[0]-near[0])*t).coerceIn(-30f,30f)
        val z=(near[2]+(far[2]-near[2])*t).coerceIn(-30f,30f)
        return x to z
    }
    fun addAt(kind:String,sx:Float,sy:Float,w:Int,h:Int){
        val point=groundPointAt(sx,sy,w,h)?:return
        val x=point.first;val z=point.second
        val placeX=if(snapEnabled)round(x*10f)/10f else x
        val placeZ=if(snapEnabled)round(z*10f)/10f else z
        synchronized(parts){
            remember()
            if(kind=="سلم"){
                for(i in 0 until 7){
                    val stepHeight=.18f
                    val tread=.28f
                    parts.add(Part("درجة",placeX,stepHeight*i+stepHeight/2f,placeZ+tread*i+tread/2f,1.2f,stepHeight,tread,nailing,activeColor))
                }
            }else{
                val dims=dimensionsFor(kind)
                parts.add(Part(kind,placeX,dims.second/2,placeZ,dims.first,dims.second,dims.third,nailing,activeColor))
            }
            selectedIndex=parts.lastIndex
        }
    }
    fun addOnTop(kind:String){synchronized(parts){
        if(selectedIndex !in parts.indices)return
        remember()
        val base=parts[selectedIndex]
        val dims=dimensionsFor(kind)
        parts.add(Part(kind,base.x,base.y+base.sy/2+dims.second/2,base.z,dims.first,dims.second,dims.third,nailing,activeColor))
        selectedIndex=parts.lastIndex
    }}
    fun raiseSelected(amount:Float){synchronized(parts){if(selectedIndex in parts.indices){remember();val p=parts[selectedIndex];parts[selectedIndex]=p.copy(y=(p.y+amount).coerceIn(p.sy/2,100f))}}}
    fun addCustomWall(length:Float,height:Float,thickness:Float,blockLength:Float,blockHeight:Float,joint:Float,openingWidth:Float=0f,openingHeight:Float=0f,openingBottom:Float=0f):Int{
        val moduleX=blockLength+joint
        val moduleY=blockHeight+joint
        val columns=kotlin.math.ceil(length/moduleX).toInt()+2
        val rows=kotlin.math.ceil(height/moduleY).toInt()
        if(columns<=0||rows<=0||columns.toLong()*rows.toLong()>3000L)return 0
        val openingLeft=-openingWidth/2f
        val openingRight=openingWidth/2f
        val openingTop=openingBottom+openingHeight
        synchronized(parts){
            remember()
            val before=parts.size
            for(row in 0 until rows){
                val offset=if(row%2==1)-moduleX/2f else 0f
                for(column in 0 until columns){
                    val center=-length/2f+blockLength/2f+offset+column*moduleX
                    val left=maxOf(center-blockLength/2f,-length/2f)
                    val right=minOf(center+blockLength/2f,length/2f)
                    val bottom=row*moduleY
                    val top=minOf(bottom+blockHeight,height)
                    val actualWidth=right-left
                    val actualHeight=top-bottom
                    val overlapsOpening=openingWidth>0f&&openingHeight>0f&&right>openingLeft&&left<openingRight&&top>openingBottom&&bottom<openingTop
                    if(actualWidth>.02f&&actualHeight>.02f&&!overlapsOpening){
                        parts.add(Part("بلوك",(left+right)/2f,bottom+actualHeight/2f,0f,actualWidth,actualHeight,thickness,nailing,activeColor))
                    }
                }
            }
            if(openingWidth>0f&&openingHeight>0f&&openingTop+.20f<=height){
                val lintelWidth=minOf(openingWidth+.40f,length)
                parts.add(Part("كمرة",0f,openingTop+.10f,0f,lintelWidth,.20f,thickness,nailing,activeColor))
            }
            selectedIndex=parts.lastIndex
            return parts.size-before
        }
    }
    fun addRow(){synchronized(parts){remember();for(i in 0 until 8)parts.add(Part("بلوك",(i-3.5f)*.68f,.16f,0f,.68f,.32f,.32f,nailing,activeColor));selectedIndex=parts.lastIndex}}
    fun addWall(){synchronized(parts){remember();for(row in 0 until 4)for(i in 0 until 8){val shift=if(row%2==0)0f else .34f;parts.add(Part("بلوك",(i-3.5f)*.68f+shift,.16f+row*.32f,0f,.68f,.32f,.32f,nailing,activeColor))};selectedIndex=parts.lastIndex}}
    fun pick(px:Float,py:Float,w:Int,h:Int):Boolean{
        synchronized(parts){
            var best=-1;var bestDepth=Float.MAX_VALUE
            val width=w.coerceAtLeast(1);val height=h.coerceAtLeast(1)
            val viewM=FloatArray(16);val projM=FloatArray(16);val vp=FloatArray(16)
            val modelM=FloatArray(16);val clip=FloatArray(4);val world=FloatArray(4)
            Matrix.setLookAtM(viewM,0,0f,4.2f,cameraDistance,0f,1f,0f,0f,1f,0f)
            Matrix.rotateM(viewM,0,angleX,1f,0f,0f);Matrix.rotateM(viewM,0,angleY,0f,1f,0f)
            Matrix.perspectiveM(projM,0,45f,width.toFloat()/height,.1f,100f)
            Matrix.multiplyMM(vp,0,projM,0,viewM,0)
            parts.forEachIndexed{i,p->
                Matrix.setIdentityM(modelM,0);Matrix.translateM(modelM,0,p.x,p.y,p.z)
                val m=FloatArray(16);Matrix.multiplyMM(m,0,vp,0,modelM,0)
                var minX=Float.MAX_VALUE;var maxX=-Float.MAX_VALUE
                var minY=Float.MAX_VALUE;var maxY=-Float.MAX_VALUE
                var visible=0
                for(ix in 0..1)for(iy in 0..1)for(iz in 0..1){
                    world[0]=if(ix==0)-p.sx/2 else p.sx/2
                    world[1]=if(iy==0)-p.sy/2 else p.sy/2
                    world[2]=if(iz==0)-p.sz/2 else p.sz/2
                    world[3]=1f
                    Matrix.multiplyMV(clip,0,m,0,world,0)
                    if(clip[3]>0f){
                        val x=(clip[0]/clip[3]+1f)*width/2f
                        val y=(1f-clip[1]/clip[3])*height/2f
                        minX=min(minX,x);maxX=max(maxX,x);minY=min(minY,y);maxY=max(maxY,y);visible++
                    }
                }
                if(visible==8&&px>=minX-10f&&px<=maxX+10f&&py>=minY-10f&&py<=maxY+10f){
                    Matrix.multiplyMV(clip,0,m,0,floatArrayOf(0f,0f,0f,1f),0)
                    val depth=if(clip[3]>0f)clip[2]/clip[3] else Float.MAX_VALUE
                    if(depth<bestDepth){bestDepth=depth;best=i}
                }
            }
            selectedIndex=best
            return best>=0
        }
    }
    fun beginMove(){synchronized(parts){if(selectedIndex in parts.indices)remember()}}
    fun moveSelected(dx:Float,dy:Float,w:Int,h:Int){synchronized(parts){if(selectedIndex in parts.indices){val p=parts[selectedIndex];val wx=dx/w.coerceAtLeast(1)*6f;val wz=dy/h.coerceAtLeast(1)*-6f;val a=Math.toRadians(angleY.toDouble());val rx=(wx*cos(a)-wz*sin(a)).toFloat();val rz=(wx*sin(a)+wz*cos(a)).toFloat();val nx=(p.x+rx).coerceIn(-30f,30f);val nz=(p.z+rz).coerceIn(-30f,30f);parts[selectedIndex]=p.copy(x=if(snapEnabled)round(nx*10f)/10f else nx,z=if(snapEnabled)round(nz*10f)/10f else nz)}}}
    fun applyColor(){synchronized(parts){if(selectedIndex in parts.indices){remember();parts[selectedIndex]=parts[selectedIndex].copy(color=activeColor)}}}
    fun deleteSelected(){synchronized(parts){if(selectedIndex in parts.indices){remember();parts.removeAt(selectedIndex)};selectedIndex=(selectedIndex.coerceAtMost(parts.lastIndex)).coerceAtLeast(-1)}}
    fun selectedDimensions():FloatArray?=synchronized(parts){if(selectedIndex in parts.indices){val p=parts[selectedIndex];floatArrayOf(p.sx,p.sy,p.sz)}else null}
    fun setSelectedDimensions(x:Float,y:Float,z:Float){synchronized(parts){if(selectedIndex in parts.indices){remember();val p=parts[selectedIndex];parts[selectedIndex]=p.copy(sx=x,sy=y,sz=z)}}}
    fun selectLast(){synchronized(parts){selected=parts.lastIndex;selectedIndex=parts.lastIndex}}
    private var selected=-1
    fun duplicateLast(){synchronized(parts){if(selectedIndex in parts.indices){remember();val p=parts[selectedIndex];parts.add(p.copy(x=p.x+.45f,z=p.z+.35f));selected=parts.lastIndex;selectedIndex=parts.lastIndex}else if(parts.isNotEmpty()){remember();val p=parts.last();parts.add(p.copy(x=p.x+.45f,z=p.z+.35f));selectedIndex=parts.lastIndex}}}
    fun rotateLast(){synchronized(parts){if(selectedIndex in parts.indices){remember();val p=parts[selectedIndex];parts[selectedIndex]=p.copy(sx=p.sz,sz=p.sx)}else if(parts.isNotEmpty()){remember();val p=parts.last();parts[parts.lastIndex]=p.copy(sx=p.sz,sz=p.sx);selectedIndex=parts.lastIndex}}}
    fun deleteLast(){synchronized(parts){if(parts.isNotEmpty())parts.removeAt(parts.lastIndex);selected=(parts.size-1).coerceAtLeast(-1)}}
    fun exportProject():String{synchronized(parts){val a=org.json.JSONArray();parts.forEach{p->a.put(org.json.JSONObject().put("kind",p.kind).put("x",p.x).put("y",p.y).put("z",p.z).put("sx",p.sx).put("sy",p.sy).put("sz",p.sz).put("nailed",p.nailed).put("color",p.color))};return org.json.JSONObject().put("parts",a).put("notes",projectNotes).toString()}}
    fun importProject(data:String):Boolean{
        return try{
            val root=org.json.JSONObject(data)
            val array=root.getJSONArray("parts")
            if(array.length()>10000)return false
            val loaded=mutableListOf<Part>()
            val allowed=setOf("بلوك","طوب","لوح","مربع","دعامة","قالب","عمود","كمرة","بلاطة","قاعدة","درجة")
            for(i in 0 until array.length()){
                val o=array.getJSONObject(i)
                val x=o.optDouble("x",Double.NaN).toFloat()
                val y=o.optDouble("y",Double.NaN).toFloat()
                val z=o.optDouble("z",Double.NaN).toFloat()
                val sx=o.optDouble("sx",Double.NaN).toFloat()
                val sy=o.optDouble("sy",Double.NaN).toFloat()
                val sz=o.optDouble("sz",Double.NaN).toFloat()
                if(!x.isFinite()||!y.isFinite()||!z.isFinite()||x !in -1000f..1000f||y !in -1000f..1000f||z !in -1000f..1000f)throw IllegalArgumentException("invalid position")
                if(!sx.isFinite()||!sy.isFinite()||!sz.isFinite()||sx !in .001f..100f||sy !in .001f..100f||sz !in .001f..100f)throw IllegalArgumentException("invalid dimensions")
                val kind=o.optString("kind","بلوك")
                loaded.add(Part(if(kind in allowed)kind else "بلوك",x,y,z,sx,sy,sz,o.optBoolean("nailed"),o.optInt("color",0).coerceIn(0,3)))
            }
            val notes=root.optString("notes","").take(20000)
            synchronized(parts){remember();parts.clear();parts.addAll(loaded);selectedIndex=parts.lastIndex}
            projectNotes=notes
            true
        }catch(_:Exception){false}
    }
    fun projectCost(prices:DoubleArray):String=synchronized(parts){
        val blocks=parts.count{it.kind=="بلوك"};val bricks=parts.count{it.kind=="طوب"}
        val timber=parts.filter{it.kind=="لوح"||it.kind=="مربع"||it.kind=="دعامة"||it.kind=="قالب"}.sumOf{if(it.kind=="دعامة")it.sy.toDouble() else maxOf(it.sx,it.sz).toDouble()}
        val concrete=parts.filter{it.kind=="عمود"||it.kind=="كمرة"||it.kind=="بلاطة"||it.kind=="قاعدة"||it.kind=="درجة"}.sumOf{(it.sx*it.sy*it.sz).toDouble()}
        val p=DoubleArray(4){prices.getOrElse(it){0.0}.coerceAtLeast(0.0)}
        val blockCost=blocks*p[0];val brickCost=bricks*p[1];val timberCost=timber*p[2];val concreteCost=concrete*p[3]
        val total=blockCost+brickCost+timberCost+concreteCost
        "البلوك: $blocks × %.2f = %.2f\\nالطوب: $bricks × %.2f = %.2f\\nالخشب: %.2f م × %.2f = %.2f\\nالخرسانة: %.3f م³ × %.2f = %.2f\\n\\nإجمالي التكلفة التقديري: %.2f".format(p[0],blockCost,p[1],brickCost,timber,p[2],timberCost,concrete,p[3],concreteCost,total)
    }
    fun exportDxf():String=synchronized(parts){
        val out=StringBuilder()
        fun pair(code:Int,value:String){out.append(code).append("\r\n").append(value).append("\r\n")}
        pair(0,"SECTION");pair(2,"HEADER")
        pair(9,"\$ACADVER");pair(1,"AC1015")
        pair(9,"\$INSUNITS");pair(70,"6")
        pair(0,"ENDSEC")
        pair(0,"SECTION");pair(2,"TABLES")
        pair(0,"TABLE");pair(2,"LAYER");pair(70,"3")
        pair(0,"LAYER");pair(2,"0");pair(70,"0");pair(62,"7");pair(6,"CONTINUOUS")
        pair(0,"LAYER");pair(2,"PARTS");pair(70,"0");pair(62,"7");pair(6,"CONTINUOUS")
        pair(0,"LAYER");pair(2,"LABELS");pair(70,"0");pair(62,"3");pair(6,"CONTINUOUS")
        pair(0,"ENDTAB");pair(0,"ENDSEC")
        pair(0,"SECTION");pair(2,"ENTITIES")
        parts.forEachIndexed{index,p->
            val x1=p.x-p.sx/2;val x2=p.x+p.sx/2
            val y1=p.z-p.sz/2;val y2=p.z+p.sz/2
            val corners=listOf(x1 to y1,x2 to y1,x2 to y2,x1 to y2)
            for(i in 0..3){
                val a=corners[i];val b=corners[(i+1)%4]
                pair(0,"LINE");pair(8,"PARTS")
                pair(10,a.first.toString());pair(20,a.second.toString());pair(30,"0")
                pair(11,b.first.toString());pair(21,b.second.toString());pair(31,"0")
            }
            pair(0,"TEXT");pair(8,"LABELS");pair(10,p.x.toString());pair(20,p.z.toString());pair(30,"0")
            val label=when(p.kind){"بلوك"->"BLOCK";"طوب"->"BRICK";"لوح"->"WOOD_BOARD";"مربع"->"WOOD_SQUARE";"دعامة"->"SUPPORT";"قالب"->"FORMWORK";"عمود"->"COLUMN";"كمرة"->"BEAM";"بلاطة"->"SLAB";"قاعدة"->"FOOTING";"درجة"->"STAIR_STEP";else->"PART"}
            pair(40,"0.12");pair(1,"${label}_${index+1}");pair(50,"0")
        }
        pair(0,"ENDSEC");pair(0,"EOF")
        out.toString()
    }
    fun exportObj():String=synchronized(parts){
        val out=StringBuilder()
        out.append("# SHADOW BUILD 3D model - units: meters\\n")
        var base=1
        parts.forEachIndexed{index,p->
            val label=when(p.kind){"بلوك"->"BLOCK";"طوب"->"BRICK";"لوح"->"WOOD_BOARD";"مربع"->"WOOD_SQUARE";"دعامة"->"SUPPORT";"قالب"->"FORMWORK";"عمود"->"COLUMN";"كمرة"->"BEAM";"بلاطة"->"SLAB";else->"PART"}
            out.append("o ${label}_${index+1}\\n")
            val hx=p.sx/2f;val hy=p.sy/2f;val hz=p.sz/2f
            val vertices=arrayOf(
                floatArrayOf(p.x-hx,p.y-hy,p.z-hz),floatArrayOf(p.x+hx,p.y-hy,p.z-hz),
                floatArrayOf(p.x+hx,p.y+hy,p.z-hz),floatArrayOf(p.x-hx,p.y+hy,p.z-hz),
                floatArrayOf(p.x-hx,p.y-hy,p.z+hz),floatArrayOf(p.x+hx,p.y-hy,p.z+hz),
                floatArrayOf(p.x+hx,p.y+hy,p.z+hz),floatArrayOf(p.x-hx,p.y+hy,p.z+hz)
            )
            vertices.forEach{v->out.append("v ${v[0]} ${v[1]} ${v[2]}\\n")}
            val faces=arrayOf("5 6 7 8","2 1 4 3","1 5 8 4","6 2 3 7","4 8 7 3","1 2 6 5")
            faces.forEach{face->out.append("f ").append(face.split(" ").joinToString(" "){(it.toInt()+base-1).toString()}).append("\\n")}
            base+=8
        }
        out.toString()
    }
    fun report():String{synchronized(parts){
        val blocks=parts.count{it.kind=="بلوك"};val bricks=parts.count{it.kind=="طوب"}
        val boards=parts.count{it.kind=="لوح"};val squares=parts.count{it.kind=="مربع"}
        val supports=parts.count{it.kind=="دعامة"};val molds=parts.count{it.kind=="قالب"}
        val columns=parts.count{it.kind=="عمود"};val beams=parts.count{it.kind=="كمرة"};val slabs=parts.count{it.kind=="بلاطة"}
        val footings=parts.count{it.kind=="قاعدة"};val stairSteps=parts.count{it.kind=="درجة"}
        val woodLength=parts.filter{it.kind=="لوح"||it.kind=="مربع"||it.kind=="دعامة"||it.kind=="قالب"}.sumOf{if(it.kind=="دعامة")it.sy.toDouble() else maxOf(it.sx,it.sz).toDouble()}
        val volume=parts.sumOf{(it.sx*it.sy*it.sz).toDouble()}
        return "تقرير ورشة البناء\\nإجمالي القطع: ${parts.size}\\nالبلوك: $blocks\\nالطوب: $bricks\\nألواح الخشب: $boards\\nمربعات الخشب: $squares\\nالدعامات: $supports\\nقوالب الصب: $molds\\nالأعمدة الخرسانية: $columns\\nالكمرات: $beams\\nالبلاطات: $slabs\\nالقواعد الخرسانية: $footings\\nدرجات السلم: $stairSteps\\nإجمالي أطوال قطع الخشب: %.2f م\\nالحجم التقريبي: %.3f م³\\n\\nشرح المشروع:\\n$projectNotes".format(woodLength,volume)
    }}
    fun removeLast(){synchronized(parts){if(parts.isNotEmpty())parts.removeAt(parts.lastIndex);selectedIndex=parts.lastIndex}}
    fun clear(){synchronized(parts){if(parts.isNotEmpty())remember();parts.clear();selectedIndex=-1}}
    private fun shader(type:Int,src:String):Int{val s=GLES20.glCreateShader(type);GLES20.glShaderSource(s,src);GLES20.glCompileShader(s);return s}
}
