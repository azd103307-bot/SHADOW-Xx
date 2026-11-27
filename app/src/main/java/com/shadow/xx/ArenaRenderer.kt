package com.shadow.xx

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

class ArenaRenderer(private val mode: Int) : GLSurfaceView.Renderer {
    @Volatile var moveX = 0f
    @Volatile var moveY = 0f
    @Volatile var action = false
    @Volatile var score = 0
    @Volatile var lives = 3
    var onHud: ((Int, Int) -> Unit)? = null

    private var program = 0
    private var pos = 0
    private var colorLoc = 0
    private var matrixLoc = 0
    private lateinit var cube: FloatBuffer
    private lateinit var sphere: FloatBuffer
    private var sphereCount = 0
    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val mv = FloatArray(16)
    private val mvp = FloatArray(16)
    private var lastTime = 0L
    private var worldTime = 0f
    private var playerX = 0f
    private var playerY = 0f
    private var ballZ = -4f
    private var ballV = 0f
    private var shotZ = -100f
    private var boostTime = 0f
    private var damageCooldown = 0f
    private var wasAction = false
    private var lastScore = -1
    private var lastLives = -1
    private val raceCoins = IntArray(9) { -1 }
    private val raceHits = IntArray(10) { -1 }
    private val adventureCoins = IntArray(13) { -1 }
    private val flightRings = IntArray(6) { -1 }
    private val spaceTargets = IntArray(8) { -1 }

    private val white = floatArrayOf(.94f,.97f,1f,1f)
    private val dark = floatArrayOf(.055f,.075f,.11f,1f)
    private val gold = floatArrayOf(1f,.72f,.12f,1f)
    private val green = floatArrayOf(.08f,.48f,.2f,1f)
    private val blue = floatArrayOf(.12f,.55f,.95f,1f)
    private val red = floatArrayOf(.9f,.13f,.12f,1f)
    private val wood = floatArrayOf(.35f,.19f,.08f,1f)

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(.04f,.07f,.12f,1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        program = buildProgram()
        pos = GLES20.glGetAttribLocation(program, "aPosition")
        colorLoc = GLES20.glGetUniformLocation(program, "uColor")
        matrixLoc = GLES20.glGetUniformLocation(program, "uMVP")
        cube = buffer(cubeVertices())
        val s = sphereVertices(12,16)
        sphere = buffer(s.first)
        sphereCount = s.second
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        GLES20.glViewport(0,0,max(1,w),max(1,h))
        val ratio = max(1,w).toFloat()/max(1,h)
        Matrix.frustumM(projection,0,-ratio,ratio,-1f,1f,1f,110f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = if(lastTime==0L) .016f else ((now-lastTime)/1_000_000_000f).coerceIn(0f,.05f)
        lastTime = now
        worldTime += dt
        val tap = action && !wasAction
        wasAction = action
        update(dt,tap)

        val clear = when(mode) {
            1 -> floatArrayOf(.035f,.29f,.52f,1f)
            2 -> floatArrayOf(.025f,.13f,.07f,1f)
            3 -> floatArrayOf(.22f,.34f,.19f,1f)
            4 -> floatArrayOf(.2f,.53f,.78f,1f)
            else -> floatArrayOf(.008f,.012f,.055f,1f)
        }
        GLES20.glClearColor(clear[0],clear[1],clear[2],1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        GLES20.glUseProgram(program)
        val eyeY = if(mode==4) playerY+6.5f else 6f
        val targetY = if(mode==4) playerY else 0f
        Matrix.setLookAtM(view,0,playerX,eyeY,11f,playerX,targetY,-5f,0f,1f,0f)
        when(mode) {
            1 -> drawRace()
            2 -> drawFootball()
            3 -> drawAdventure()
            4 -> drawFlight()
            else -> drawSpace()
        }
        drawPlayer()
        if(score!=lastScore || lives!=lastLives) {
            lastScore=score; lastLives=lives
            onHud?.invoke(score,lives)
        }
    }

    private fun update(dt:Float,tap:Boolean) {
        val speed = if(mode==1 && boostTime>0f) 10f else 7f
        playerX=(playerX+moveX*dt*speed).coerceIn(-4.2f,4.2f)
        if(mode==4 || mode==5) playerY=(playerY+moveY*dt*5f).coerceIn(-2.2f,5f)
        boostTime=max(0f,boostTime-dt)
        damageCooldown=max(0f,damageCooldown-dt)

        when(mode) {
            1 -> {
                if(tap) boostTime=1.25f
                for(i in 0..8) {
                    val phase=i*12f+worldTime*10f
                    val z=-(phase%62f)-2f
                    val cycle=floor(phase/62f).toInt()
                    val lane=((i%3)-1)*2.5f
                    if(z in -1.35f..-.15f && abs(playerX-lane)<1.35f && raceCoins[i]!=cycle) {
                        raceCoins[i]=cycle; score+=10
                    }
                }
                for(i in 0..9) {
                    val phase=i*9f+worldTime*12f
                    val z=-(phase%55f)-3f
                    val cycle=floor(phase/55f).toInt()
                    val lane=if(i%2==0) -2.5f else 2.5f
                    if(z in -1.2f..-.15f && abs(playerX-lane)<1.45f && raceHits[i]!=cycle) {
                        raceHits[i]=cycle; hitPlayer()
                    }
                }
            }
            2 -> {
                ballZ+=ballV*dt
                ballV*=.992f
                if(ballZ < -28f) {
                    if(abs(playerX)<3.5f) score+=1
                    ballZ=-4f; ballV=0f
                }
                if(ballZ > -2f) { ballZ=-4f; ballV=0f }
                if(tap && abs(ballZ+4f)<2f) { ballV=-24f; ballZ=-3.8f }
            }
            3 -> {
                for(i in 0..12) {
                    val phase=i*7f+worldTime*3f
                    val z=-(phase%58f)-2f
                    val cycle=floor(phase/58f).toInt()
                    val x=if(i%3==0) 2.8f else -2.8f
                    if(z in -1.25f..-.15f && abs(playerX-x)<1.5f && adventureCoins[i]!=cycle) {
                        adventureCoins[i]=cycle; score+=5
                    }
                }
                if(tap) boostTime=.35f
            }
            4 -> {
                for(i in 0..5) {
                    val phase=i*10f+worldTime*8f
                    val z=-(phase%55f)-3f
                    val cycle=floor(phase/55f).toInt()
                    val x=((i%3)-1)*4f
                    if(z in -1.5f..-.1f && abs(playerX-x)<1.8f && abs(playerY)<2.4f && flightRings[i]!=cycle) {
                        flightRings[i]=cycle; score+=10
                    }
                }
            }
            5 -> {
                if(tap) shotZ=-1f
                if(shotZ>-90f) {
                    shotZ-=dt*32f
                    for(i in 0..7) {
                        val phase=i*8f+worldTime*9f
                        val z=-(phase%46f)-3f
                        val cycle=floor(phase/46f).toInt()
                        val x=sin(i*2.1f)*6f
                        val y=cos(i*1.5f)*3f
                        if(abs(z-shotZ)<1.25f && abs(playerX-x)<1.7f && abs(playerY-y)<1.7f && spaceTargets[i]!=cycle) {
                            spaceTargets[i]=cycle; score+=15; shotZ=-100f
                        }
                    }
                    if(shotZ < -55f) shotZ=-100f
                }
            }
        }
    }

    private fun hitPlayer() {
        if(damageCooldown<=0f) {
            lives=(lives-1).coerceAtLeast(0)
            damageCooldown=1.1f
        }
    }

    private fun drawRace() {
        box(0f,-.25f,-22f,30f,.4f,65f,green)
        box(0f,-.01f,-22f,11f,.08f,65f,floatArrayOf(.13f,.15f,.18f,1f))
        box(-5.55f,.04f,-22f,.12f,.08f,65f,white)
        box(5.55f,.04f,-22f,.12f,.08f,65f,white)
        for(i in 0..8) {
            val z=-i*8f+(worldTime*18f%8f)
            box(0f,.04f,z,.16f,.04f,3.2f,white)
        }
        for(i in 0..9) {
            val z=-((i*9f+worldTime*12f)%55f)-3f
            val lane=if(i%2==0) -2.5f else 2.5f
            box(lane,.58f,z,1.45f,1.05f,1.9f,red)
            box(lane,.98f,z+.12f,.95f,.32f,1.1f,floatArrayOf(.12f,.2f,.28f,1f))
        }
        for(i in 0..8) {
            val z=-((i*12f+worldTime*10f)%62f)-2f
            val lane=((i%3)-1)*2.5f
            sphereAt(lane,.9f,z,.42f,gold)
        }
        for(side in listOf(-1f,1f)) for(i in 0..7) {
            val z=-i*8f-4f
            box(side*8f,1f,z,2f,2.5f,2f,floatArrayOf(.38f,.45f,.55f,1f))
            box(side*8f,2.35f,z,2.05f,.12f,2.05f,gold)
        }
    }

    private fun drawFootball() {
        box(0f,-.3f,-18f,34f,.5f,60f,floatArrayOf(.04f,.42f,.16f,1f))
        box(0f,-.02f,-18f,24f,.04f,45f,white)
        box(0f,.02f,-18f,.12f,.04f,45f,white)
        box(-12f,.02f,-18f,.12f,.04f,45f,white)
        box(12f,.02f,-18f,.12f,.04f,45f,white)
        box(0f,.02f,-7f,24f,.04f,.08f,white)
        box(0f,1.6f,-29f,8f,3.2f,.25f,white)
        box(-4f,.8f,-29f,.22f,1.6f,.22f,white)
        box(4f,.8f,-29f,.22f,1.6f,.22f,white)
        sphereAt(playerX,.45f,ballZ,.52f,white)
        for(i in 0..5) {
            val x=-9f+i*3.6f
            sphereAt(x,1.1f,-25f,.2f,blue)
            box(x,.65f,-25f,.28f,.7f,.28f,blue)
        }
        box(0f,.01f,-7f,1.5f,.04f,.15f,gold)
    }

    private fun drawAdventure() {
        box(0f,-.4f,-22f,32f,.8f,65f,floatArrayOf(.42f,.3f,.16f,1f))
        for(i in 0..12) {
            val z=-((i*7f+worldTime*3f)%58f)-2f
            val x=if(i%2==0) -7f else 7f
            box(x,.7f,z,.35f,1.4f,.35f,wood)
            sphereAt(x,1.8f,z,1.25f,floatArrayOf(.08f,.38f,.14f,1f))
            val treasureX=if(i%3==0) 2.8f else -2.8f
            sphereAt(treasureX,.5f,z-3f,.32f,gold)
        }
        for(i in 0..5) box(0f,.05f,-i*8f-5f,2f,.12f,1f,floatArrayOf(.62f,.47f,.28f,1f))
        sphereAt(0f,.6f,-31f,1.7f,floatArrayOf(.55f,.32f,.12f,1f))
        box(0f,.2f,-31f,2.5f,.4f,2.5f,wood)
    }

    private fun drawFlight() {
        for(i in 0..10) {
            val z=-((i*7f+worldTime*5f)%55f)-5f
            sphereAt(((i%3)-1)*5f,4f,z,1.4f,floatArrayOf(.92f,.96f,1f,1f))
        }
        for(i in 0..5) {
            val z=-((i*10f+worldTime*8f)%55f)-3f
            val x=((i%3)-1)*4f
            box(x,-1.4f,z,5f,.65f,4f,floatArrayOf(.18f,.36f,.28f,1f))
            sphereAt(x,0f,z,.72f,gold)
        }
        box(0f,-2.2f,-25f,24f,.2f,1f,green)
    }

    private fun drawSpace() {
        for(i in 0..24) {
            val x=sin(i*13.7f)*13f
            val y=cos(i*7.1f)*8f
            val z=-((i*5f+worldTime*3f)%58f)-2f
            sphereAt(x,y,z,.08f,white)
        }
        sphereAt(-7f,2f,-35f,4f,floatArrayOf(.35f,.2f,.72f,1f))
        sphereAt(8f,-1f,-48f,5f,floatArrayOf(.08f,.42f,.72f,1f))
        for(i in 0..7) {
            val x=sin(i*2.1f)*6f
            val y=cos(i*1.5f)*3f
            val z=-((i*8f+worldTime*9f)%46f)-3f
            box(x,y,z,.9f,.9f,.9f,red,i*17f)
        }
        if(shotZ>-90f) sphereAt(playerX,playerY,shotZ,.18f,gold)
    }

    private fun drawPlayer() {
        when(mode) {
            1 -> {
                box(playerX,.55f,0f,1.65f,.55f,2.6f,blue)
                box(playerX,.95f,.15f,1.1f,.55f,1.25f,floatArrayOf(.12f,.2f,.3f,1f))
                for(s in listOf(-1f,1f)) for(z in listOf(-.8f,.8f)) sphereAt(playerX+s*.9f,.27f,z,.34f,dark)
            }
            2,3 -> {
                box(playerX,.85f,0f,.8f,.95f,.55f,blue)
                sphereAt(playerX,1.55f,0f,.36f,floatArrayOf(.9f,.7f,.52f,1f))
                box(playerX,.25f,0f,.85f,.25f,.5f,red)
            }
            4 -> {
                box(playerX,playerY,0f,1.2f,.35f,2f,white)
                box(playerX,playerY,-.2f,3f,.12f,.45f,red)
                box(playerX,playerY+.12f,.65f,.35f,.15f,.7f,gold)
            }
            else -> {
                box(playerX,playerY,0f,1.4f,.35f,1.8f,blue)
                box(playerX,playerY,-.1f,2.8f,.12f,.45f,red)
                sphereAt(playerX,playerY,0f,.35f,gold)
            }
        }
    }

    private fun box(x:Float,y:Float,z:Float,sx:Float,sy:Float,sz:Float,c:FloatArray,rot:Float=0f) {
        Matrix.setIdentityM(model,0)
        Matrix.translateM(model,0,x,y,z)
        if(rot!=0f) Matrix.rotateM(model,0,rot,0f,1f,0f)
        Matrix.scaleM(model,0,sx,sy,sz)
        draw(cube,36,c)
    }
    private fun sphereAt(x:Float,y:Float,z:Float,r:Float,c:FloatArray) {
        Matrix.setIdentityM(model,0)
        Matrix.translateM(model,0,x,y,z)
        Matrix.scaleM(model,0,r,r,r)
        draw(sphere,sphereCount,c)
    }
    private fun draw(vertices:FloatBuffer,count:Int,c:FloatArray) {
        Matrix.multiplyMM(mv,0,view,0,model,0)
        Matrix.multiplyMM(mvp,0,projection,0,mv,0)
        GLES20.glUniformMatrix4fv(matrixLoc,1,false,mvp,0)
        GLES20.glUniform4fv(colorLoc,1,c,0)
        vertices.position(0)
        GLES20.glEnableVertexAttribArray(pos)
        GLES20.glVertexAttribPointer(pos,3,GLES20.GL_FLOAT,false,0,vertices)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,count)
        GLES20.glDisableVertexAttribArray(pos)
    }

    private fun buildProgram():Int {
        val vs="attribute vec4 aPosition; uniform mat4 uMVP; void main(){ gl_Position=uMVP*aPosition; }"
        val fs="precision mediump float; uniform vec4 uColor; void main(){ gl_FragColor=uColor; }"
        fun shader(type:Int,src:String):Int {
            val sh=GLES20.glCreateShader(type)
            GLES20.glShaderSource(sh,src)
            GLES20.glCompileShader(sh)
            val ok=IntArray(1)
            GLES20.glGetShaderiv(sh,GLES20.GL_COMPILE_STATUS,ok,0)
            if(ok[0]==0) throw RuntimeException(GLES20.glGetShaderInfoLog(sh))
            return sh
        }
        val p=GLES20.glCreateProgram()
        GLES20.glAttachShader(p,shader(GLES20.GL_VERTEX_SHADER,vs))
        GLES20.glAttachShader(p,shader(GLES20.GL_FRAGMENT_SHADER,fs))
        GLES20.glLinkProgram(p)
        val ok=IntArray(1)
        GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0)
        if(ok[0]==0) throw RuntimeException(GLES20.glGetProgramInfoLog(p))
        return p
    }

    private fun buffer(a:FloatArray):FloatBuffer =
        ByteBuffer.allocateDirect(a.size*4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(a); position(0) }

    private fun cubeVertices():FloatArray {
        val v=floatArrayOf(
            -.5f,-.5f,.5f,.5f,-.5f,.5f,.5f,.5f,.5f,-.5f,.5f,.5f,
            -.5f,-.5f,-.5f,-.5f,.5f,-.5f,.5f,.5f,-.5f,.5f,-.5f,-.5f
        )
        val idx=intArrayOf(0,1,2,0,2,3,4,5,6,4,6,7,0,4,7,0,7,3,1,5,6,1,6,2,3,2,6,3,6,7,0,1,5,0,5,4)
        val out=FloatArray(idx.size*3)
        for(i in idx.indices) {
            val k=idx[i]*3
            out[i*3]=v[k]; out[i*3+1]=v[k+1]; out[i*3+2]=v[k+2]
        }
        return out
    }

    private fun sphereVertices(lat:Int,lon:Int):Pair<FloatArray,Int> {
        val out=ArrayList<Float>()
        for(i in 0 until lat) {
            val a=PI.toFloat()*i/lat
            val b=PI.toFloat()*(i+1)/lat
            for(j in 0 until lon) {
                val c=2f*PI.toFloat()*j/lon
                val d=2f*PI.toFloat()*(j+1)/lon
                val p0=floatArrayOf(sin(a)*cos(c),cos(a),sin(a)*sin(c))
                val p1=floatArrayOf(sin(b)*cos(c),cos(b),sin(b)*sin(c))
                val p2=floatArrayOf(sin(b)*cos(d),cos(b),sin(b)*sin(d))
                val p3=floatArrayOf(sin(a)*cos(d),cos(a),sin(a)*sin(d))
                for(p in listOf(p0,p1,p2,p0,p2,p3)) for(k in 0..2) out.add(p[k])
            }
        }
        return Pair(out.toFloatArray(),out.size/3)
    }
}
