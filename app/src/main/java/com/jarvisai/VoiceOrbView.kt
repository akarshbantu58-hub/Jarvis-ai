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
        val r = minOf(width, height) * 0.24f
        val wave = ((sin(phase * Math.PI * 2.0) + 1.0) / 2.0).toFloat()
        val pulseAmount = if (listening) 0.09f else 0.035f
        val rr = r * (1f + pulseAmount * wave)

        glow.maskFilter = BlurMaskFilter(r * 0.55f, BlurMaskFilter.Blur.NORMAL)
        glow.color = Color.argb(if (listening) 110 else 65, 70, 190, 255)
        c.drawCircle(cx, cy, rr * 1.35f, glow)

        for (i in 0 until 3) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = if (listening) 3f else 2f
            paint.color = Color.argb(80 - i * 18, 80, 210, 255)
            c.drawCircle(cx, cy, rr * (1.35f + i * 0.25f), paint)
        }

        paint.shader = RadialGradient(
            cx - rr * 0.25f,
            cy - rr * 0.25f,
            rr * 1.2f,
            intArrayOf(
                Color.WHITE,
                Color.rgb(120, 225, 255),
                Color.rgb(15, 80, 140),
                Color.rgb(3, 12, 24)
            ),
            floatArrayOf(0f, 0.22f, 0.62f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.style = Paint.Style.FILL
        c.drawCircle(cx, cy, rr, paint)
        paint.shader = null

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = Color.argb(210, 190, 245, 255)
        c.drawCircle(cx, cy, rr, paint)

        if (listening) {
            for (i in 0 until 24) {
                val a = i * Math.PI * 2.0 / 24.0
                val audio = (sin(phase * Math.PI * 4.0 + i.toDouble()) + 1.0).toFloat()
                val amp = 8f + level * 25f + audio * 3f
                paint.strokeWidth = 2f
                paint.color = Color.argb(155, 100, 225, 255)
                val inner = rr + 10f
                val outer = inner + amp
                c.drawLine(
                    cx + cos(a).toFloat() * inner,
                    cy + sin(a).toFloat() * inner,
                    cx + cos(a).toFloat() * outer,
                    cy + sin(a).toFloat() * outer,
                    paint
                )
            }
        }
    }

    override fun onDetachedFromWindow() {
        animator.cancel()
        super.onDetachedFromWindow()
    }
}
