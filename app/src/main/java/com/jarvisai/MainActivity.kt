package com.jarvisai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.jarvisai.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var jarvis: JarvisCore
    private val handler = Handler(Looper.getMainLooper())
    private val clock = object : Runnable {
        override fun run() {
            binding.timeText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            handler.postDelayed(this, 1000)
        }
    }

    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!text.isNullOrBlank()) processCommand(text)
    }

    private val microphonePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) speechLauncher.launch(jarvis.voiceIntent()) else showResponse("Microphone permission denied. You can enable it later in Permission Center.")
    }

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else showResponse("Camera permission denied. Enable it from Permission Center when you need Vision.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        jarvis = JarvisCore(this)
        binding.statusText.text = "● SYSTEM ONLINE  •  PERMISSION-SAFE MODE"
        binding.responseText.text = "JARVIS is ready. Permissions are requested only when a feature needs them."
        binding.listenButton.setOnClickListener { startListening() }
        binding.sendButton.setOnClickListener { processCommand(binding.commandInput.text?.toString().orEmpty()) }

        binding.devCard.setOnClickListener { showResponse("DEV STUDIO foundation ready.") }
        binding.cameraCard.setOnClickListener { requestCamera() }
        binding.settingsCard.setOnClickListener { startActivity(Intent(this, PermissionActivity::class.java)) }
        binding.aiCard.setOnClickListener { showResponse("AI CORE ready. Configure an AI provider in API HUB.") }
        binding.personalCard.setOnClickListener { showResponse("Personalization module ready for theme and voice settings.") }
        binding.apiCard.setOnClickListener { showResponse("API HUB: store your provider configuration locally on this device.") }
        binding.automationCard.setOnClickListener { startActivity(Intent(this, AutomationActivity::class.java)) }

        binding.dockVoice.setOnClickListener { startListening() }
        binding.dockAI.setOnClickListener { showResponse("AI CORE ready.") }
        binding.dockApps.setOnClickListener { startActivity(Intent(this, AutomationActivity::class.java)) }
        binding.dockSettings.setOnClickListener { startActivity(Intent(this, PermissionActivity::class.java)) }
        binding.dockHome.setOnClickListener { binding.contentScroll.smoothScrollTo(0, 0) }
        handler.post(clock)
    }

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            speechLauncher.launch(jarvis.voiceIntent())
        } else {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun requestCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCamera()
        else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        runCatching {
            startActivity(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE))
        }.onFailure { showResponse("No compatible camera app is available.") }
    }

    private fun processCommand(command: String) {
        if (command.isBlank()) return
        binding.commandInput.setText(command)
        val response = jarvis.executeCommand(command)
        showResponse(response)
        jarvis.speak(response)
    }

    private fun showResponse(text: String) {
        binding.responseText.text = text
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        handler.removeCallbacks(clock)
        jarvis.release()
        super.onDestroy()
    }
}
