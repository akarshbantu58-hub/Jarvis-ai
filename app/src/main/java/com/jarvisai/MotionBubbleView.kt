package com.jarvisai

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.AttributeSet
import android.view.View
import kotlin.math.sin
import kotlin.random.Random

/** Lightweight foreground-only motion layer for the Ask Anything glass UI. */
class MotionBubbleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs), SensorEventListener {
    private data class Bubble(var x: Float, var y: Float, var vx: Float, var vy: Float, val radius: Float, val phase: Float)

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager?.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val bubbles = MutableList(18) { index ->
        Bubble(
            x = Random.nextFloat(),
            y = Random.nextFloat(),
            vx = 0f,
            vy = 0f,
            radius = 7f + (index % 5) * 3f,
            phase = Random.nextFloat() * 6.28f
        )
    }
    private var targetX = 0f
    private var targetY = 0f
    private var motionEnabled = true
    private var reduceMotion = false
    private var running = false
    private var lastFrameNs = 0L

    init {
        isClickable = false
        isFocusable = false
        alpha = 0.55f
    }

    fun setMotionEnabled(enabled: Boolean) {
        motionEnabled = enabled
        if (!enabled) {
            targetX = 0f
            targetY = 0f
        }
        invalidate()
    }

    fun setReduceMotion(enabled: Boolean) {
        reduceMotion = enabled
        invalidate()
    }

    fun start() {
        if (running) return
        running = true
        lastFrameNs = 0L
        sensorManager?.let { manager ->
            accelerometer?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
            gyroscope?.let { manager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
        }
        postInvalidateOnAnimation()
    }

    fun stop() {
        if (!running) return
        running = false
        sensorManager?.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!motionEnabled || reduceMotion) return
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                targetX = (-event.values[0] / 12f).coerceIn(-1f, 1f)
                targetY = (event.values[1] / 12f).coerceIn(-1f, 1f)
            }
            Sensor.TYPE_GYROSCOPE -> {
                targetX = (targetX + event.values[1] * 0.04f).coerceIn(-1f, 1f)
                targetY = (targetY + event.values[0] * 0.04f).coerceIn(-1f, 1f)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val now = System.nanoTime()
        val dt = if (lastFrameNs == 0L) 0.016f else ((now - lastFrameNs) / 1_000_000_000f).coerceIn(0.008f, 0.04f)
        lastFrameNs = now

        val widthF = width.toFloat().coerceAtLeast(1f)
        val heightF = height.toFloat().coerceAtLeast(1f)
        val motion = if (reduceMotion) 0.15f else 1f

        bubbles.forEach { bubble ->
            bubble.vx += targetX * 10f * dt * motion
            bubble.vy += targetY * 10f * dt * motion
            bubble.vx *= 0.985f
            bubble.vy *= 0.985f
            bubble.x += bubble.vx * dt
            bubble.y += bubble.vy * dt
            if (bubble.x < -0.05f) bubble.x = 1.05f
            if (bubble.x > 1.05f) bubble.x = -0.05f
            if (bubble.y < -0.05f) bubble.y = 1.05f
            if (bubble.y > 1.05f) bubble.y = -0.05f

            val drift = sin((now / 1_000_000_000f) * 0.55f + bubble.phase) * 4f
            paint.color = 0x4490E8FF
            canvas.drawCircle(bubble.x * widthF + drift, bubble.y * heightF, bubble.radius, paint)
        }

        if (running) postInvalidateOnAnimation()
    }
}
