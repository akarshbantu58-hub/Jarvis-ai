package com.jarvisai

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
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
    private val clock = object : Runnable { override fun run() { binding.timeText.text=SimpleDateFormat("HH:mm",Locale.getDefault()).format(Date()); handler.postDelayed(this,1000) } }
    private val assistantRoleLauncher: ActivityResultLauncher<Intent> = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { updateAssistantCard() }
    private val speechLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> result.data?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { processCommand(it) } }
    private val microphonePermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if(granted) beginVoice() else showResponse("Microphone permission denied.") }
    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if(granted) launchCamera() else showResponse("Camera permission denied.") }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); binding=ActivityMainBinding.inflate(layoutInflater); setContentView(binding.root)
        jarvis=JarvisCore(this); actions=AppActionEngine(this)
        binding.statusText.text="● SYSTEM ONLINE  •  JARVIS READY"; binding.responseText.text="JARVIS is ready. Ask a question or say a command."
        binding.listenButton.setOnClickListener { startListening() }; binding.sendButton.setOnClickListener { processCommand(binding.commandInput.text?.toString().orEmpty()) }
        binding.devCard.setOnClickListener { showResponse("DEV STUDIO is ready.") }; binding.cameraCard.setOnClickListener { requestCamera() }; binding.settingsCard.setOnClickListener { startActivity(Intent(this,PermissionActivity::class.java)) }
        binding.aiCard.setOnClickListener { startActivity(Intent(this,ApiHubActivity::class.java)) }; binding.personalCard.setOnClickListener { showResponse("Personalization is available in settings.") }; binding.apiCard.setOnClickListener { startActivity(Intent(this,ApiHubActivity::class.java)) }; binding.automationCard.setOnClickListener { startActivity(Intent(this,AutomationActivity::class.java)) }
        binding.dockVoice.setOnClickListener { startListening() }; binding.dockAI.setOnClickListener { startActivity(Intent(this,ApiHubActivity::class.java)) }; binding.dockApps.setOnClickListener { startActivity(Intent(this,AutomationActivity::class.java)) }; binding.dockSettings.setOnClickListener { startActivity(Intent(this,PermissionActivity::class.java)) }; binding.dockHome.setOnClickListener { binding.contentScroll.smoothScrollTo(0,0) }
        binding.defaultAssistantCard.setOnClickListener { requestDefaultAssistant() }
        binding.voiceOrb.setOnClickListener { startListening() }
        handler.post(clock); updateAssistantCard()
    }
    override fun onResume(){super.onResume();if(::binding.isInitialized)updateAssistantCard()}
    private fun updateAssistantCard(){val d=PermissionCenter.hasAssistantRole(this);binding.defaultAssistantTitle.text=if(d)"JARVIS IS YOUR DEFAULT ASSISTANT" else "SET JARVIS AS DEFAULT ASSISTANT";binding.defaultAssistantSubtitle.text=if(d)"Voice interaction is enabled by Android." else "Tap to open Android's confirmation dialog.";binding.orbStateText.text=if(d)"ASSISTANT READY" else "ONLINE"}
    private fun requestDefaultAssistant(){if(android.os.Build.VERSION.SDK_INT<android.os.Build.VERSION_CODES.Q){showResponse("Android 10 or newer is required.");return};val rm=getSystemService(RoleManager::class.java);when{rm==null||!rm.isRoleAvailable(RoleManager.ROLE_ASSISTANT)->showResponse("Assistant role is unavailable on this device.");rm.isRoleHeld(RoleManager.ROLE_ASSISTANT)->updateAssistantCard();else->assistantRoleLauncher.launch(rm.createRequestRoleIntent(RoleManager.ROLE_ASSISTANT))}}
    private fun startListening(){if(checkSelfPermission(Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)beginVoice() else microphonePermission.launch(Manifest.permission.RECORD_AUDIO)}
    private fun beginVoice(){binding.voiceOrb.setListening(true);binding.orbStateText.text="LISTENING";speechLauncher.launch(jarvis.voiceIntent())}
    private fun requestCamera(){if(checkSelfPermission(Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)launchCamera() else cameraPermission.launch(Manifest.permission.CAMERA)}
    private fun launchCamera(){runCatching{startActivity(Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE))}.onFailure{showResponse("No compatible camera app is available.")}}
    private fun processCommand(command:String){binding.voiceOrb.setListening(false);binding.orbStateText.text="THINKING";if(command.isBlank()){binding.orbStateText.text="ONLINE";return};binding.commandInput.setText(command);val local=runCatching{actions.execute(command)}.getOrNull();if(local!=null){showResponse(local);jarvis.speak(local);binding.orbStateText.text="SPEAKING";return};val lower=command.lowercase(Locale.getDefault());when{lower.contains("api hub")||lower.contains("api settings")->startActivity(Intent(this,ApiHubActivity::class.java));lower.contains("permissions")->startActivity(Intent(this,PermissionActivity::class.java));lower.contains("automation")->startActivity(Intent(this,AutomationActivity::class.java));lower.contains("default assistant")||lower.contains("make jarvis default")->requestDefaultAssistant();else->{showResponse("Thinking…");ApiClient.ask(this,command){response->showResponse(response);jarvis.speak(response);binding.voiceOrb.setListening(false);binding.orbStateText.text="SPEAKING"}}}}
    private fun showResponse(text:String){binding.responseText.text=text;Toast.makeText(this,text.take(120),Toast.LENGTH_SHORT).show()}
    fun enableOverlay(){if(!Settings.canDrawOverlays(this))startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:$packageName")))else startService(Intent(this,JarvisOverlayService::class.java))}
    override fun onDestroy(){handler.removeCallbacks(clock);binding.voiceOrb.setListening(false);jarvis.release();super.onDestroy()}
}
