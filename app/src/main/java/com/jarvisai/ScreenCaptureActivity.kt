package com.jarvisai

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.OutputStream

/** Explicit one-shot MediaProjection capture. Android always shows its consent dialog. */
class ScreenCaptureActivity : AppCompatActivity() {
    private lateinit var status: TextView
    private var projection: MediaProjection? = null
    private var reader: ImageReader? = null

    private val captureCode = 7007

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 36, 28, 28)
            setBackgroundColor(android.graphics.Color.rgb(4, 8, 14))
            addView(TextView(this@ScreenCaptureActivity).apply {
                text = "JARVIS • SCREEN CONTEXT"
                textSize = 26f
                setTextColor(android.graphics.Color.CYAN)
            })
            status = TextView(this@ScreenCaptureActivity).apply {
                text = "Nothing is captured until you press START."
                setTextColor(android.graphics.Color.WHITE)
                setPadding(0, 16, 0, 16)
            }
            addView(status)
            addView(Button(this@ScreenCaptureActivity).apply {
                text = "START SCREEN CAPTURE"
                setOnClickListener { requestProjection() }
            })
            addView(Button(this@ScreenCaptureActivity).apply {
                text = "STOP / RELEASE CAPTURE"
                setOnClickListener { releaseProjection(); status.text = "Screen capture stopped." }
            })
        })
    }

    private fun requestProjection() {
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        status.text = "Waiting for Android screen-capture confirmation…"
        startActivityForResult(manager.createScreenCaptureIntent(), captureCode)
    }

    @Deprecated("Android activity result API compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != captureCode || resultCode != RESULT_OK || data == null) {
            status.text = "Screen capture permission was cancelled."
            return
        }
        val manager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection = manager.getMediaProjection(resultCode, data)
        captureOneFrame()
    }

    private fun captureOneFrame() {
        val metrics = resources.displayMetrics
        val width = metrics.widthPixels.coerceAtLeast(320)
        val height = metrics.heightPixels.coerceAtLeast(320)
        reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        val imageReader = reader ?: return
        val density = metrics.densityDpi
        projection?.createVirtualDisplay(
            "JARVIS_SCREEN_CONTEXT",
            width,
            height,
            density,
            0,
            imageReader.surface,
            null,
            Handler(Looper.getMainLooper())
        )
        imageReader.setOnImageAvailableListener({ source ->
            val image = runCatching { source.acquireLatestImage() }.getOrNull() ?: return@setOnImageAvailableListener
            try {
                val plane = image.planes.firstOrNull() ?: return@setOnImageAvailableListener
                val buffer = plane.buffer
                val pixelStride = plane.pixelStride
                val rowStride = plane.rowStride
                val rowPadding = rowStride - pixelStride * width
                val bitmap = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)
                val cropped = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                saveScreenshot(cropped)
                VisionClient.describe(this, cropped) { result ->
                    status.text = result.fold(
                        { "SCREEN ANALYSIS\n$it" },
                        { "Captured locally, but AI analysis failed: ${it.message ?: "unknown error"}" }
                    )
                }
                bitmap.recycle()
                cropped.recycle()
                releaseProjection()
            } finally {
                image.close()
            }
        }, Handler(Looper.getMainLooper()))
        status.text = "CAPTURE ACTIVE • acquiring one frame…"
    }

    private fun saveScreenshot(bitmap: Bitmap) {
        val values = android.content.ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "JARVIS_screen_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/JARVIS")
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return
        runCatching {
            contentResolver.openOutputStream(uri)?.use { out: OutputStream ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
        }
    }

    private fun releaseProjection() {
        runCatching { projection?.stop() }
        projection = null
        reader?.close()
        reader = null
    }

    override fun onDestroy() {
        releaseProjection()
        super.onDestroy()
    }
}
