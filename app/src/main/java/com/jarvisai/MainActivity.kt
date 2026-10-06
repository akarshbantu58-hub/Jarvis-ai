package com.jarvisai

import android.Manifest
import android.app.AlertDialog
import android.app.role.RoleManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jarvisai.core.CommandPolicy
import com.jarvisai.core.CommandRisk
import com.jarvisai.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main JARVIS screen.
 *
 * Heavy runtime services are created lazily so a missing/broken device service
 * (TTS, speech recognition, sensors, etc.) cannot prevent the main UI from
 * opening. Android permissions and security boundaries remain unchanged.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var jarvis: JarvisCore? = null
    private var actions: AppActionEngine? = null
    private var voice: VoiceAssistantController? = null
    private val handler = Handler(Looper.getMainLooper())
    private var continuousVoice = false
    private var wakeWordEnabled = false

    private val clock = object : Runnable {
        override fun run() {
            if (::binding.isInitialized && !isFinishing && !isDestroyed) {
                binding.timeText.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                handler.postDelayed(this, 1000)
            }
        }
    }

    private val wakeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != JarvisVoiceForegroundService.ACTION_WAKE_DETECTED) return
            stopWakeWordService()
            showResponse("Wake word detected. I'm listening.")
            startListening()
        }
    }

    private val assistantRoleLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { updateAssistantCard() }

    private val microphonePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) beginVoice() else showResponse("Microphone permission denied.")
        }

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted || android.os.Build.VERSION.SDK_INT < 33) {
                startWakeWordService()
            } else {
                showResponse("Notification permission is needed to show the active wake-word service status.")
            }
        }

    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) launchCamera() else showResponse("Camera permission denied.")
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Layout inflation is kept as the first operation so the UI remains
        // available even when an optional subsystem is unavailable.
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.logoImage.setImageResource(R.drawable.jarvis_logo)

        binding.statusText.text = "● SYSTEM ONLINE  •  JARVIS READY"
        binding.responseText.text = "JARVIS is ready. Ask a question or say a command."

        binding.listenButton.setOnClickListener { startListening() }
        binding.voiceModeButton.setOnClickListener {
            continuousVoice = !continuousVoice
            binding.voiceModeButton.text = if (continuousVoice) "LIVE" else "PUSH"
            showResponse(if (continuousVoice) "Continuous conversation enabled." else "Push-to-talk mode enabled.")
        }
        binding.voiceModeButton.setOnLongClickListener { toggleWakeWord(); true }
        binding.sendButton.setOnClickListener { processCommand(binding.commandInput.text?.toString().orEmpty()) }
        binding.devCard.setOnClickListener { startActivity(Intent(this, DeveloperStudioActivity::class.java)) }
        binding.cameraCard.setOnClickListener { startActivity(Intent(this, CameraActivity::class.java)) }
        binding.settingsCard.setOnClickListener { startActivity(Intent(this, FeatureCenterActivity::class.java)) }
        binding.aiCard.setOnClickListener { startActivity(Intent(this, ApiHubActivity::class.java)) }
        binding.personalCard.setOnClickListener { startActivity(Intent(this, MemoryActivity::class.java)) }
        binding.apiCard.setOnClickListener { startActivity(Intent(this, ApiHubActivity::class.java)) }
        binding.automationCard.setOnClickListener { startActivity(Intent(this, AutomationActivity::class.java)) }
        binding.dockVoice.setOnClickListener { startListening() }
        binding.dockAI.setOnClickListener { startActivity(Intent(this, ApiHubActivity::class.java)) }
        binding.dockApps.setOnClickListener { startActivity(Intent(this, AutomationActivity::class.java)) }
        binding.dockSettings.setOnClickListener { startActivity(Intent(this, FeatureCenterActivity::class.java)) }
        binding.dockHome.setOnClickListener { binding.contentScroll.smoothScrollTo(0, 0) }
        binding.defaultAssistantCard.setOnClickListener { requestDefaultAssistant() }
        binding.voiceOrb.setOnClickListener { startListening() }

        handler.post(clock)
        runCatching { binding.motionBubbles.start() }
            .onFailure { showResponse("Motion effects are unavailable on this device. JARVIS will continue without them.") }
        updateAssistantCard()
    }

    private val voiceListener = object : VoiceAssistantController.Listener {
        override fun onListeningChanged(listening: Boolean) {
            if (!::binding.isInitialized) return
            binding.voiceOrb.setListening(listening)
            binding.orbStateText.text = if (listening) "LISTENING" else "ONLINE"
        }

        override fun onPartialText(text: String) {
            if (::binding.isInitialized) binding.commandInput.setText(text)
        }

        override fun onFinalText(text: String) {
            if (::binding.isInitialized) processCommand(text)
        }

        override fun onAudioLevel(level: Float) {
            if (::binding.isInitialized) binding.voiceOrb.setAudioLevel(level)
        }

        override fun onError(message: String) {
            if (::binding.isInitialized) {
                binding.voiceOrb.setListening(false)
                binding.orbStateText.text = "ERROR"
                showResponse(message)
            }
        }
    }

    private fun ensureVoice(): VoiceAssistantController? {
        voice?.let { return it }
        return runCatching {
            VoiceAssistantController(this, voiceListener).also { voice = it }
        }.onFailure {
            showResponse("Voice input could not start on this device. You can still use text commands and AI.")
        }.getOrNull()
    }

    private fun ensureJarvis(): JarvisCore? {
        jarvis?.let { return it }
        return runCatching {
            JarvisCore(this).also { jarvis = it }
        }.onFailure {
            showResponse("JARVIS speech services are unavailable. Android controls will remain available.")
        }.getOrNull()
    }

    private fun ensureActions(): AppActionEngine? {
        actions?.let { return it }
        return runCatching {
            AppActionEngine(this).also { actions = it }
        }.onFailure {
            showResponse("Device-action services are temporarily unavailable.")
        }.getOrNull()
    }

    override fun onStart() {
        super.onStart()
        ContextCompat.registerReceiver(
            this,
            wakeReceiver,
            IntentFilter(JarvisVoiceForegroundService.ACTION_WAKE_DETECTED),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) {
            runCatching { binding.motionBubbles.start() }
            updateAssistantCard()
            if (wakeWordEnabled) binding.voiceModeButton.text = "WAKE ON"
        }
    }

    override fun onPause() {
        if (::binding.isInitialized) runCatching { binding.motionBubbles.stop() }
        super.onPause()
    }

    override fun onStop() {
        runCatching { unregisterReceiver(wakeReceiver) }
        super.onStop()
    }

    private fun updateAssistantCard() {
        runCatching {
            val defaultAssistant = PermissionCenter.hasAssistantRole(this)
            binding.defaultAssistantTitle.text = if (defaultAssistant) {
                "JARVIS IS YOUR DEFAULT ASSISTANT"
            } else {
                "SET JARVIS AS DEFAULT ASSISTANT"
            }
            binding.defaultAssistantSubtitle.text = if (defaultAssistant) {
                "Android has assigned JARVIS as the assistant."
            } else {
                "Tap to open Android's confirmation dialog."
            }
            binding.orbStateText.text = if (defaultAssistant) "ASSISTANT READY" else "ONLINE"
        }.onFailure {
            binding.defaultAssistantTitle.text = "SET JARVIS AS DEFAULT ASSISTANT"
            binding.defaultAssistantSubtitle.text = "Android assistant role is unavailable right now."
            binding.orbStateText.text = "ONLINE"
        }
    }

    private fun requestDefaultAssistant() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            showResponse("Android 10 or newer is required for the assistant role.")
            return
        }
        runCatching {
            val roleManager = getSystemService(RoleManager::class.java)
            when {
                roleManager == null || !roleManager.isRoleAvailable(RoleManager.ROLE_ASSISTANT) ->
                    showResponse("The Android assistant role is unavailable on this device.")
                roleManager.isRoleHeld(RoleManager.ROLE_ASSISTANT) -> updateAssistantCard()
                else -> assistantRoleLauncher.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT))
            }
        }.onFailure { showResponse("Android could not open the default-assistant setup.") }
    }

    private fun startListening() {
        val controller = ensureVoice() ?: return
        if (!controller.isAvailable()) {
            showResponse("No compatible Android speech-recognition service is available.")
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            beginVoice()
        } else {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun beginVoice() {
        val controller = ensureVoice() ?: return
        binding.orbStateText.text = "LISTENING"
        runCatching {
            controller.start(continuousMode = continuousVoice, preferOffline = false)
        }.onFailure {
            binding.orbStateText.text = "ERROR"
            showResponse("Unable to start Android speech recognition.")
        }
    }

    private fun toggleWakeWord() {
        if (wakeWordEnabled) {
            stopWakeWordService()
            wakeWordEnabled = false
            binding.voiceModeButton.text = if (continuousVoice) "LIVE" else "PUSH"
            showResponse("Wake-word mode disabled.")
            return
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            microphonePermission.launch(Manifest.permission.RECORD_AUDIO)
            showResponse("Grant microphone permission, then long-press the voice mode button again to enable Hey JARVIS.")
            return
        }
        if (android.os.Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        startWakeWordService()
    }

    private fun startWakeWordService() {
        val intent = Intent(this, JarvisVoiceForegroundService::class.java)
            .setAction(JarvisVoiceForegroundService.ACTION_START_WAKE)
        runCatching {
            ContextCompat.startForegroundService(this, intent)
            wakeWordEnabled = true
            binding.voiceModeButton.text = "WAKE ON"
            binding.orbStateText.text = "WAKE READY"
            showResponse("Wake-word mode enabled. Say “Hey JARVIS”. Long-press again to stop it.")
        }.onFailure {
            wakeWordEnabled = false
            showResponse("Unable to start wake-word service: ${it.message ?: "Android rejected the service"}")
        }
    }

    private fun stopWakeWordService() {
        runCatching { stopService(Intent(this, JarvisVoiceForegroundService::class.java)) }
        wakeWordEnabled = false
    }

    private fun requestCamera() {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) launchCamera()
        else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun launchCamera() {
        runCatching { startActivity(Intent(MediaStore.ACTION_IMAGE_CAPTURE)) }
            .onFailure { showResponse("No compatible camera app is available.") }
    }

    private fun processCommand(command: String) {
        ensureVoice()?.stop()
        binding.voiceOrb.setListening(false)
        binding.orbStateText.text = "THINKING"
        if (command.isBlank()) {
            binding.orbStateText.text = "ONLINE"
            return
        }
        binding.commandInput.setText(command)
        MemoryStore.addConversation(this, "user", command)
        val lower = command.lowercase(Locale.getDefault()).trim()

        if (lower == "stop speaking" || lower == "be quiet" || lower == "stop") {
            ensureJarvis()?.stopSpeaking()
            showResponse("Speech stopped.")
            binding.orbStateText.text = "IDLE"
            return
        }

        val risk = CommandPolicy().classify(command)
        if (risk == CommandRisk.DANGEROUS) {
            showResponse("JARVIS blocked this dangerous command. Android does not permit JARVIS to bypass device security or perform destructive actions.")
            binding.orbStateText.text = "BLOCKED"
            return
        }
        if (risk == CommandRisk.SENSITIVE) {
            AlertDialog.Builder(this)
                .setTitle("JARVIS confirmation")
                .setMessage("This command may have an external or irreversible effect:\n\n$command\n\nContinue?")
                .setNegativeButton("CANCEL") { _, _ -> binding.orbStateText.text = "IDLE" }
                .setPositiveButton("CONTINUE") { _, _ -> executeApprovedCommand(command) }
                .show()
            return
        }
        executeApprovedCommand(command)
    }

    private fun executeApprovedCommand(command: String) {
        val lower = command.lowercase(Locale.getDefault()).trim()
        val local = runCatching { ensureActions()?.execute(command) }.getOrNull()
        if (local != null) {
            showResponse(local)
            MemoryStore.addConversation(this, "assistant", local)
            ensureJarvis()?.speak(local)
            binding.orbStateText.text = "SPEAKING"
            return
        }
        when {
            lower.contains("api hub") || lower.contains("api settings") -> startActivity(Intent(this, ApiHubActivity::class.java))
            lower.contains("permissions") -> startActivity(Intent(this, PermissionActivity::class.java))
            lower.contains("automation") -> startActivity(Intent(this, AutomationActivity::class.java))
            lower.contains("memory") -> startActivity(Intent(this, MemoryActivity::class.java))
            lower.contains("developer studio") || lower.contains("dev studio") -> startActivity(Intent(this, DeveloperStudioActivity::class.java))
            lower.contains("screen capture") || lower.contains("share screen") -> startActivity(Intent(this, ScreenCaptureActivity::class.java))
            lower.contains("camera vision") || lower.contains("look at this") -> startActivity(Intent(this, CameraActivity::class.java))
            lower.contains("system center") || lower.contains("feature center") -> startActivity(Intent(this, FeatureCenterActivity::class.java))
            lower.contains("default assistant") || lower.contains("make jarvis default") -> requestDefaultAssistant()
            else -> {
                runCatching {
                    ApiClient.ask(this, command) { response ->
                        showResponse(response)
                        MemoryStore.addConversation(this, "assistant", response)
                        ensureJarvis()?.speak(response)
                        binding.voiceOrb.setListening(false)
                        binding.orbStateText.text = "SPEAKING"
                    }
                }.onFailure {
                    showResponse("AI request could not be started. Check the configured provider and API key.")
                    binding.orbStateText.text = "ERROR"
                }
            }
        }
    }

    private fun showResponse(text: String) {
        if (::binding.isInitialized && !isFinishing && !isDestroyed) binding.responseText.text = text
    }

    fun enableOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
        } else {
            runCatching { startService(Intent(this, JarvisOverlayService::class.java)) }
        }
    }

    override fun onDestroy() {
        handler.removeCallbacks(clock)
        stopWakeWordService()
        if (::binding.isInitialized) {
            runCatching { binding.motionBubbles.stop() }
            runCatching { binding.voiceOrb.setListening(false) }
        }
        runCatching { voice?.release() }
        runCatching { jarvis?.release() }
        voice = null
        jarvis = null
        actions = null
        super.onDestroy()
    }
}
