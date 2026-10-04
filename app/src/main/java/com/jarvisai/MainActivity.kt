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
        if (granted) speechLauncher.launch(jarvis.voiceIntent()) else showResponse("Microphone permission is required for voice control.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        jarvis = JarvisCore(this)
        binding.statusText.text = "● SYSTEM ONLINE  •  VOICE READY"
        binding.responseText.text = "Good evening, Akarsh. How can I assist you?"
        binding.listenButton.setOnClickListener { startListening() }
        binding.sendButton.setOnClickListener { processCommand(binding.commandInput.text?.toString().orEmpty()) }
        bindModule(binding.devCard, "DEV STUDIO")
        bindModule(binding.cameraCard, "VISION")
        bindModule(binding.settingsCard, "SYSTEM SETTINGS")
        bindModule(binding.aiCard, "AI CORE")
        bindModule(binding.personalCard, "PERSONALIZATION")
        bindModule(binding.apiCard, "API HUB")
        binding.dockVoice.setOnClickListener { startListening() }
        binding.dockAI.setOnClickListener { showResponse("AI CORE ready. Add an API key in API HUB to connect a model.") }
        binding.dockApps.setOnClickListener { showResponse("APP DOCK ready. Voice commands can launch supported apps.") }
        binding.dockSettings.setOnClickListener { openSystemSettings() }
        binding.dockHome.setOnClickListener { binding.contentScroll.smoothScrollTo(0, 0) }
        handler.post(clock)
    }

    private fun bindModule(view: TextView, name: String) {
        view.setOnClickListener { showResponse("$name module selected. UI shell is ready for its Android feature integration.") }
    }

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) speechLauncher.launch(jarvis.voiceIntent())
        else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
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

    private fun openSystemSettings() {
        startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
    }

    override fun onDestroy() {
        handler.removeCallbacks(clock)
        jarvis.release()
        super.onDestroy()
    }
}
