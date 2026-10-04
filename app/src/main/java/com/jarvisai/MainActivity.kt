package com.jarvisai

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
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
    private lateinit var actions: AppActionEngine
    private val handler = Handler(Looper.getMainLooper())
    private val clock = object : Runnable {
        override fun run() {
            binding.timeText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            handler.postDelayed(this, 1000)
        }
    }

    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { processCommand(it) }
    }
    private val microphonePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) speechLauncher.launch(jarvis.voiceIntent()) else showResponse("Microphone permission denied. Enable it in Permission Center.")
    }
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else showResponse("Camera permission denied. Enable it in Permission Center.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        jarvis = JarvisCore(this)
        actions = AppActionEngine(this)
        binding.statusText.text = "● SYSTEM ONLINE  •  JARVIS READY"
        binding.responseText.text = "JARVIS is ready. Ask a question or say a command."
        binding.listenButton.setOnClickListener { startListening() }
        binding.sendButton.setOnClickListener { processCommand(binding.commandInput.text?.toString().orEmpty()) }
        binding.devCard.setOnClickListener { showResponse("DEV STUDIO is ready for the configured development tools.") }
        binding.cameraCard.setOnClickListener { requestCamera() }
        binding.settingsCard.setOnClickListener { startActivity(Intent(this, PermissionActivity::class.java)) }
        binding.aiCard.setOnClickListener { startActivity(Intent(this, ApiHubActivity::class.java)) }
        binding.personalCard.setOnClickListener { showResponse("Personalization is available through your JARVIS settings.") }
        binding.apiCard.setOnClickListener { startActivity(Intent(this, ApiHubActivity::class.java)) }
        binding.automationCard.setOnClickListener { startActivity(Intent(this, AutomationActivity::class.java)) }
        binding.dockVoice.setOnClickListener { startListening() }
        binding.dockAI.setOnClickListener { startActivity(Intent(this, ApiHubActivity::class.java)) }
        binding.dockApps.setOnClickListener { startActivity(Intent(this, AutomationActivity::class.java)) }
        binding.dockSettings.setOnClickListener { startActivity(Intent(this, PermissionActivity::class.java)) }
        binding.dockHome.setOnClickListener { binding.contentScroll.smoothScrollTo(0, 0) }
        handler.post(clock)
    }

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) speechLauncher.launch(jarvis.voiceIntent())
        else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun requestCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCamera()
        else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        runCatching { startActivity(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE)) }
            .onFailure { showResponse("No compatible camera app is available.") }
    }

    private fun processCommand(command: String) {
        if (command.isBlank()) return
        binding.commandInput.setText(command)
        val local = runCatching { actions.execute(command) }.getOrNull()
        if (local != null) { showResponse(local); jarvis.speak(local); return }
        val lower = command.lowercase(Locale.getDefault())
        if (lower.contains("api hub") || lower.contains("api settings")) {
            startActivity(Intent(this, ApiHubActivity::class.java)); return
        }
        if (lower.contains("permissions") || lower.contains("permission center")) {
            startActivity(Intent(this, PermissionActivity::class.java)); return
        }
        if (lower.contains("automation")) {
            startActivity(Intent(this, AutomationActivity::class.java)); return
        }
        showResponse("Thinking…")
        ApiClient.ask(this, command) { response -> showResponse(response); jarvis.speak(response) }
    }

    private fun showResponse(text: String) {
        binding.responseText.text = text
        Toast.makeText(this, text.take(120), Toast.LENGTH_SHORT).show()
    }

    fun enableOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            return
        }
        startService(Intent(this, JarvisOverlayService::class.java))
    }

    override fun onDestroy() {
        handler.removeCallbacks(clock)
        jarvis.release()
        super.onDestroy()
    }
}
