package com.jarvisai

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 * Hardware-safe JARVIS orb renderer.
 *
 * The glow uses BlurMaskFilter, so this view deliberately renders through a
 * software layer. That keeps the animation deterministic across Samsung tablet
 * GPU/driver combinations while leaving the rest of the application hardware
 * accelerated.
 */
class VoiceOrbView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private var phase = 0f
    private var level = 0.2f
    private var listening = false

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 2200L
        repeatCount = ValueAnimator.INFINITE
        addUpdateListener {
            phase = (it.animatedValue as? Float) ?: 0f
            if (isAttachedToWindow) invalidate()
        }
    }

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!animator.isStarted) animator.start()
    }

    fun setListening(value: Boolean) {
        listening = value
        invalidate()
    }

    fun setAudioLevel(value: Float) {
        level = value.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        if (width <= 0 || height <= 0) return

        val cx = width / 2f
        val cy = height / 2f
        val size = minOf(width, height).toFloat()
        val r = size * 0.205f
        val wave = ((sin(phase * Math.PI * 2.0) + 1.0) / 2.0).toFloat()
        val pulseAmount = if (listening) 0.12f else 0.045f
        val rr = r * (1f + pulseAmount * wave)
        val spin = phase * Math.PI * 2.0

        // Wide atmospheric halo.
        glow.maskFilter = BlurMaskFilter(size * 0.105f, BlurMaskFilter.Blur.NORMAL)
        glow.color = Color.argb(if (listening) 125 else 78, 25, 155, 255)
        c.drawCircle(cx, cy, rr * 1.55f, glow)
        glow.maskFilter = BlurMaskFilter(size * 0.045f, BlurMaskFilter.Blur.NORMAL)
        glow.color = Color.argb(if (listening) 110 else 65, 0, 235, 255)
        c.drawCircle(cx, cy, rr * 1.18f, glow)
        glow.maskFilter = null

        // Concentric holographic rings.
        paint.shader = null
        paint.style = Paint.Style.STROKE
        for (i in 0 until 4) {
            paint.strokeWidth = if (i == 0) 2.2f else 1.1f
            paint.color = Color.argb(105 - i * 17, 66, 205, 255)
            c.drawCircle(cx, cy, rr * (1.27f + i * 0.22f), paint)
        }

        // Rotating segmented orbit, with a counter-rotating inner arc.
        val outer = RectF(cx - rr * 1.66f, cy - rr * 1.66f, cx + rr * 1.66f, cy + rr * 1.66f)
        paint.strokeWidth = 2.4f
        paint.color = Color.argb(205, 75, 220, 255)
        c.drawArc(outer, (spin * 57.2958).toFloat(), 74f, false, paint)
        c.drawArc(outer, (180.0 - spin * 35.0).toFloat(), 48f, false, paint)
        val innerOrbit = RectF(cx - rr * 1.42f, cy - rr * 1.42f, cx + rr * 1.42f, cy + rr * 1.42f)
        paint.strokeWidth = 1.5f
        paint.color = Color.argb(145, 145, 120, 255)
        c.drawArc(innerOrbit, (-spin * 45.0).toFloat(), 96f, false, paint)

        // Orbital light nodes.
        for (i in 0 until 4) {
            val angle = spin * (if (i % 2 == 0) 1.0 else -0.72) + i * Math.PI / 2.0
            val orbitR = rr * (if (i % 2 == 0) 1.66f else 1.42f)
            val x = cx + cos(angle).toFloat() * orbitR
            val y = cy + sin(angle).toFloat() * orbitR
            paint.style = Paint.Style.FILL
            paint.color = if (i % 2 == 0) Color.rgb(130, 245, 255) else Color.rgb(170, 150, 255)
            c.drawCircle(x, y, if (listening) 3.7f else 2.6f, paint)
        }

        // Glass core with a cool white-blue specular highlight.
        paint.shader = RadialGradient(
            cx - rr * 0.30f,
            cy - rr * 0.34f,
            rr * 1.38f,
            intArrayOf(
                Color.WHITE,
                Color.rgb(166, 245, 255),
                Color.rgb(45, 160, 235),
                Color.rgb(13, 45, 100),
                Color.rgb(2, 8, 24)
            ),
            floatArrayOf(0f, 0.16f, 0.43f, 0.76f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        c.drawCircle(cx, cy, rr, paint)
        paint.shader = null

        // Fine luminous rim and a soft inner reflection.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.3f
        paint.color = Color.argb(235, 195, 250, 255)
        c.drawCircle(cx, cy, rr, paint)
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(60, 255, 255, 255)
        c.drawOval(
            RectF(cx - rr * 0.58f, cy - rr * 0.72f, cx + rr * 0.12f, cy - rr * 0.48f),
            paint
        )

        // A subtle audio corona remains visible at rest; listening makes it react.
        for (i in 0 until 36) {
            val a = i * Math.PI * 2.0 / 36.0 + spin * 0.08
            val audio = (sin(phase * Math.PI * 4.0 + i.toDouble()) + 1.0).toFloat()
            val amp = if (listening) 7f + level * 18f + audio * 4f else 3f + audio * 2f
            val inner = rr * 1.78f
            val outerR = inner + amp
            paint.strokeWidth = if (listening) 2.1f else 1.2f
            paint.color = Color.argb(if (listening) 175 else 85, 92, 218, 255)
            c.drawLine(
                cx + cos(a).toFloat() * inner,
                cy + sin(a).toFloat() * inner,
                cx + cos(a).toFloat() * outerR,
                cy + sin(a).toFloat() * outerR,
                paint
            )
        }
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }
}
