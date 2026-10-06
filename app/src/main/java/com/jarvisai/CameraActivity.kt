package com.jarvisai

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/** Uses Android's trusted camera UI; JARVIS never activates the camera silently. */
class CameraActivity : AppCompatActivity() {
    private lateinit var image: ImageView
    private lateinit var status: TextView

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) takePhoto() else status.text = "Camera permission was denied."
    }

    private val takePreview = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap == null) {
            status.text = "No image was returned by the camera."
            return@registerForActivityResult
        }
        image.setImageBitmap(bitmap)
        status.text = "CAPTURED • AI VISION READY"
        VisionClient.describe(this, bitmap) { result ->
            status.text = result.fold(
                onSuccess = { "JARVIS VISION\n$it" },
                onFailure = { "Vision failed: ${it.message ?: "unknown error"}" }
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    private fun buildUi() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(24, 32, 24, 24)
        setBackgroundColor(Color.BLACK)
        addView(TextView(this@CameraActivity).apply {
            text = "JARVIS • CAMERA / VISION"
            textSize = 25f
            setTextColor(Color.CYAN)
        })
        status = TextView(this@CameraActivity).apply {
            text = "Press CAPTURE to open Android's camera preview."
            setTextColor(Color.WHITE)
            setPadding(0, 16, 0, 16)
        }
        addView(status)
        image = ImageView(this@CameraActivity).apply {
            adjustViewBounds = true
            setBackgroundColor(Color.rgb(10, 16, 24))
            contentDescription = "Latest JARVIS camera capture"
        }
        addView(image, LinearLayout.LayoutParams(-1, 0, 1f))
        addView(Button(this@CameraActivity).apply {
            text = "CAPTURE + ASK JARVIS WHAT IT SEES"
            setOnClickListener {
                if (checkSelfPermission(Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) takePhoto()
                else cameraPermission.launch(Manifest.permission.CAMERA)
            }
        })
        addView(Button(this@CameraActivity).apply {
            text = "OPEN FULL ANDROID CAMERA"
            setOnClickListener { runCatching { startActivity(android.content.Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) } }
        })
    }

    private fun takePhoto() {
        status.text = "CAMERA ACTIVE • WAITING FOR CAPTURE"
        takePreview.launch(null)
    }
}
