package com.shadow.xx

import android.content.res.AssetManager
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*

class ArenaRenderer(private val mode: Int, private val assets: AssetManager) : GLSurfaceView.Renderer {
    @Volatile var moveX = 0f
    @Volatile var moveY = 0f
    @Volatile var action = false
    @Volatile var jumpAction = false
    @Volatile var score = 0
    @Volatile var lives = 3
    var onHud: ((Int, Int) -> Unit)? = null

    private var program = 0
    private var textureProgram = 0
    private var texturePosition = 0
    private var textureUv = 0
    private var textureMatrix = 0
    private var textureSampler = 0
    private var textureDetailSampler = 0
    private var textureDetailStrength = 0
    private lateinit var groundMesh: FloatBuffer
    private lateinit var skyMesh: FloatBuffer
    private val worldTextures = HashMap<String, Int>()
    private val worldTag = when(mode) { 1 -> "city"; 2 -> "football"; 3 -> "forest"; 4 -> "flight"; else -> "space" }
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
    private var wasJump = false
    private var playerLift = 0f
    private var jumpVelocity = 0f
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
        val s = sphereVertices(16,24)
        sphere = buffer(s.first)
        sphereCount = s.second

        textureProgram = buildTextureProgram()
        texturePosition = GLES20.glGetAttribLocation(textureProgram, "aPosition")
        textureUv = GLES20.glGetAttribLocation(textureProgram, "aUV")
        textureMatrix = GLES20.glGetUniformLocation(textureProgram, "uMVP")
        textureSampler = GLES20.glGetUniformLocation(textureProgram, "uTexture")
        textureDetailSampler = GLES20.glGetUniformLocation(textureProgram, "uDetail")
        textureDetailStrength = GLES20.glGetUniformLocation(textureProgram, "uDetailStrength")
        val groundY=when(mode) { 3 -> .015f; 4 -> -2.05f; 5 -> -.72f; else -> -.045f }
        groundMesh = buffer(floatArrayOf(
            -32f,groundY,12f, 0f,0f,  32f,groundY,12f, 14f,0f,  32f,groundY,-90f, 14f,28f,
            -32f,groundY,12f, 0f,0f,  32f,groundY,-90f, 14f,28f,  -32f,groundY,-90f, 0f,28f
        ))
        skyMesh = buffer(floatArrayOf(
            -100f,-8f,-92f, 0f,1f,  100f,-8f,-92f, 1f,1f,  100f,65f,-92f, 1f,0f,
            -100f,-8f,-92f, 0f,1f,  100f,65f,-92f, 1f,0f,  -100f,65f,-92f, 0f,0f
        ))
        loadWorldTextures()
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
        val jumpTap = jumpAction && !wasJump
        wasJump = jumpAction
        update(dt,tap,jumpTap)

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
        drawWorldBackdrop()
        GLES20.glUseProgram(program)
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

    private fun update(dt:Float,tap:Boolean,jumpTap:Boolean) {
        val speed = if(mode==1 && boostTime>0f) 17f else 10f
        playerX=(playerX+moveX*dt*speed).coerceIn(-4.2f,4.2f)
        if(mode==4 || mode==5) playerY=(playerY+moveY*dt*6f).coerceIn(-2.2f,5f)
        if(jumpTap && mode in 1..3 && playerLift <= .02f) jumpVelocity=9.2f
        if(playerLift>0f || jumpVelocity>0f) {
            playerLift=(playerLift+jumpVelocity*dt).coerceAtLeast(0f)
            jumpVelocity-=22f*dt
            if(playerLift<=0f) { playerLift=0f; jumpVelocity=0f }
        }
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
                    if(z in -1.2f..-.15f && playerLift<.9f && abs(playerX-lane)<1.45f && raceHits[i]!=cycle) {
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
            val bx=side*9.2f
            box(bx,2.35f,z,4.2f,5.3f,3.8f,floatArrayOf(.22f,.29f,.39f,1f))
            box(bx,5.08f,z,4.35f,.18f,3.95f,gold)
            for(level in 0..2) for(window in -1..1) {
                box(bx+window*1.12f,1.15f+level*1.35f,z+1.94f,.55f,.72f,.08f,floatArrayOf(.35f,.68f,.86f,1f))
            }
            box(side*6.2f,.55f,z, .16f,1.1f,.16f,dark)
            sphereAt(side*6.2f,1.18f,z,.22f,gold)
        }
        for(i in 0..5) {
            val z=-i*10f-7f
            box(-5.2f,.12f,z,.18f,.2f,1.4f,gold)
            box(5.2f,.12f,z,.18f,.2f,1.4f,gold)
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
        for(side in listOf(-1f,1f)) {
            box(side*14f,1.2f,-20f,3.2f,2.4f,24f,floatArrayOf(.12f,.18f,.24f,1f))
            for(row in 0..3) for(seat in 0..8) {
                box(side*(12.8f+seat*.28f),.45f+row*.48f,-30f+row*2.2f+seat*.32f,.25f,.25f,.42f,
                    if((row+seat)%2==0) red else blue)
            }
        }
        box(0f,5.2f,-28.4f,5.6f,1.8f,.22f,dark)
        box(0f,5.2f,-28.25f,4.9f,1.12f,.08f,floatArrayOf(.08f,.58f,.32f,1f))
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
        for(i in 0..8) {
            val z=-i*6.4f-4f
            val side=if(i%2==0) -1f else 1f
            val x=side*(5.2f+(i%3)*1.1f)
            box(x,.36f,z,1.1f,.7f,1.15f,floatArrayOf(.31f,.27f,.2f,1f))
            sphereAt(x,.78f,z,.43f,floatArrayOf(.10f,.31f,.13f,1f))
        }
        for(side in listOf(-1f,1f)) for(i in 0..4) {
            val z=-i*11f-8f
            box(side*13f,1.8f,z,1.2f,3.6f,1.2f,floatArrayOf(.45f,.42f,.36f,1f))
            box(side*13f,3.75f,z,1.65f,.22f,1.65f,gold)
        }
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
        for(i in 0..5) {
            val x=if(i%2==0) -12f else 12f
            val z=-i*13f-12f
            sphereAt(x,2.8f,z,2.8f,floatArrayOf(.87f,.94f,1f,1f))
            box(x,0f,z,2.2f,1.1f,2.2f,floatArrayOf(.20f,.42f,.31f,1f))
        }
        box(-8f,-.2f,-42f,1.1f,2.4f,1.1f,floatArrayOf(.35f,.38f,.42f,1f))
        box(8f,-.2f,-42f,1.1f,2.4f,1.1f,floatArrayOf(.35f,.38f,.42f,1f))
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
        for(i in 0..3) {
            val z=-18f-i*12f
            box(-13f+i*2.4f,5f,z,1.5f,.3f,.3f,blue,i*18f)
            box(13f-i*2.4f,-5f,z,1.2f,.25f,.25f,gold,-i*18f)
        }
        if(shotZ>-90f) {
            sphereAt(playerX,playerY,shotZ,.2f,gold)
            sphereAt(playerX,playerY,shotZ+.3f,.1f,red)
        }
    }

    private fun drawPlayer() {
        when(mode) {
            1 -> {
                val lift=playerLift
                box(playerX,.55f+lift,0f,1.75f,.62f,2.8f,blue)
                box(playerX,1.0f+lift,.18f,1.18f,.55f,1.25f,floatArrayOf(.12f,.2f,.3f,1f))
                box(playerX-.48f,1.02f+lift,.78f,.28f,.10f,.16f,gold)
                box(playerX+.48f,1.02f+lift,.78f,.28f,.10f,.16f,gold)
                for(side in listOf(-1f,1f)) for(z in listOf(-.9f,.9f)) sphereAt(playerX+side*.94f,.25f+lift,z,.35f,dark)
            }
            2,3 -> {
                val lift=playerLift
                box(playerX,.90f+lift,0f,.82f,1.0f,.52f,blue)
                sphereAt(playerX,1.62f+lift,0f,.37f,floatArrayOf(.9f,.7f,.52f,1f))
                box(playerX,.27f+lift,0f,.86f,.28f,.52f,red)
                box(playerX-.55f,1.0f+lift,0f,.23f,.78f,.24f,blue,-12f)
                box(playerX+.55f,1.0f+lift,0f,.23f,.78f,.24f,blue,12f)
                box(playerX-.22f,.02f+lift,0f,.24f,.52f,.28f,dark)
                box(playerX+.22f,.02f+lift,0f,.24f,.52f,.28f,dark)
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

    private fun loadWorldTextures() {
        loadWorldTexture("ground", "worlds/" + worldTag + "_ground.png")
        loadWorldTexture("sky", "worlds/" + worldTag + "_sky.png")
        loadWorldTexture("detail", "worlds/" + worldTag + "_detail.png")
    }

    private fun loadWorldTexture(key:String, path:String) {
        try {
            val options=BitmapFactory.Options().apply {
                inSampleSize=2
                inPreferredConfig=android.graphics.Bitmap.Config.RGB_565
            }
            val bitmap=assets.open(path).use { BitmapFactory.decodeStream(it,null,options) } ?: return
            val ids=IntArray(1)
            GLES20.glGenTextures(1,ids,0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,ids[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MIN_FILTER,GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_MAG_FILTER,GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_S,GLES20.GL_REPEAT)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D,GLES20.GL_TEXTURE_WRAP_T,GLES20.GL_REPEAT)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D,0,bitmap,0)
            bitmap.recycle()
            worldTextures[key]=ids[0]
        } catch (_:Exception) {
            // The geometric scene remains playable if a texture cannot be decoded.
        }
    }

    private fun drawWorldBackdrop() {
        val sky=worldTextures["sky"]
        if(sky!=null) drawTextured(skyMesh,sky,false)
        val ground=worldTextures["ground"]
        if(ground!=null) drawTextured(groundMesh,ground,true)
    }

    private fun drawTextured(vertices:FloatBuffer,texture:Int,detailed:Boolean) {
        GLES20.glUseProgram(textureProgram)
        Matrix.setIdentityM(model,0)
        Matrix.multiplyMM(mv,0,view,0,model,0)
        Matrix.multiplyMM(mvp,0,projection,0,mv,0)
        GLES20.glUniformMatrix4fv(textureMatrix,1,false,mvp,0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,texture)
        GLES20.glUniform1i(textureSampler,0)
        val detail=worldTextures["detail"]
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        if(detail!=null) GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,detail)
        GLES20.glUniform1i(textureDetailSampler,1)
        GLES20.glUniform1f(textureDetailStrength,if(detailed) .085f else 0f)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        vertices.position(0)
        GLES20.glEnableVertexAttribArray(texturePosition)
        GLES20.glVertexAttribPointer(texturePosition,3,GLES20.GL_FLOAT,false,20,vertices)
        vertices.position(3)
        GLES20.glEnableVertexAttribArray(textureUv)
        GLES20.glVertexAttribPointer(textureUv,2,GLES20.GL_FLOAT,false,20,vertices)
        vertices.position(0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES,0,6)
        GLES20.glDisableVertexAttribArray(texturePosition)
        GLES20.glDisableVertexAttribArray(textureUv)
    }

    private fun buildTextureProgram():Int {
        val vs = "attribute vec4 aPosition; attribute vec2 aUV; uniform mat4 uMVP; varying vec2 vUV; void main(){ gl_Position=uMVP*aPosition; vUV=aUV; }"
        val fs = "precision mediump float; uniform sampler2D uTexture; uniform sampler2D uDetail; uniform float uDetailStrength; varying vec2 vUV; void main(){ vec3 base=texture2D(uTexture,vUV).rgb; float d=texture2D(uDetail,vUV*3.0).r; vec3 col=base*(1.0+(d-0.5)*uDetailStrength); gl_FragColor=vec4(col,1.0); }"
        fun compile(type:Int,source:String):Int {
            val shader=GLES20.glCreateShader(type)
            GLES20.glShaderSource(shader,source)
            GLES20.glCompileShader(shader)
            val ok=IntArray(1)
            GLES20.glGetShaderiv(shader,GLES20.GL_COMPILE_STATUS,ok,0)
            if(ok[0]==0) throw RuntimeException(GLES20.glGetShaderInfoLog(shader))
            return shader
        }
        val p=GLES20.glCreateProgram()
        GLES20.glAttachShader(p,compile(GLES20.GL_VERTEX_SHADER,vs))
        GLES20.glAttachShader(p,compile(GLES20.GL_FRAGMENT_SHADER,fs))
        GLES20.glLinkProgram(p)
        val ok=IntArray(1)
        GLES20.glGetProgramiv(p,GLES20.GL_LINK_STATUS,ok,0)
        if(ok[0]==0) throw RuntimeException(GLES20.glGetProgramInfoLog(p))
        return p
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
