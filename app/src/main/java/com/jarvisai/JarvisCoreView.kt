package com.jarvisai

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class JarvisCoreView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {
    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f
    init { setLayerType(View.LAYER_TYPE_SOFTWARE, null) }
    override fun onDraw(c: Canvas) {
        val x = width / 2f; val y = height / 2f; val r = minOf(width, height) * .30f
        p.shader = RadialGradient(x,y,r*1.9f,intArrayOf(0xCC00E5FF.toInt(),0x55007CFF,0),floatArrayOf(0f,.45f,1f),Shader.TileMode.CLAMP)
        c.drawCircle(x,y,r*1.9f,p); p.shader=null
        for (i in 0..3) { p.style=Paint.Style.STROKE; p.strokeWidth=if(i==0)4f else 1.5f; p.color=if(i==0)0xFF00E5FF.toInt() else 0x6688DFFF; p.setShadowLayer(if(i==0)22f else 8f,0f,0f,0xFF00CFFF.toInt()); c.save(); c.rotate(phase*(if(i%2==0)1f else -1f),x,y); c.drawCircle(x,y,r*(.70f+i*.20f),p); c.restore() }
        p.style=Paint.Style.FILL; p.setShadowLayer(30f,0f,0f,0xFF00E5FF.toInt()); p.shader=RadialGradient(x,y,r*.62f,intArrayOf(0xFFFFFFFF.toInt(),0xFF49F4FF.toInt(),0xFF006CFF.toInt(),0xFF07101B.toInt()),floatArrayOf(0f,.20f,.55f,1f),Shader.TileMode.CLAMP); c.drawCircle(x,y,r*.55f,p); p.shader=null
        p.color=0xFFFFFFFF.toInt(); p.setShadowLayer(12f,0f,0f,0xFF00E5FF.toInt()); c.drawCircle(x,y,r*.07f,p)
        for(i in 0 until 18){val a=phase*.035f+i*(Math.PI*2/18); c.drawCircle(x+cos(a).toFloat()*r*1.12f,y+sin(a).toFloat()*r*1.12f,if(i%3==0)3.5f else 1.5f,p)}
        phase=(phase+.8f)%360f; postInvalidateOnAnimation()
    }
}
