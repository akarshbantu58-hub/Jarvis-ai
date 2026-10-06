package com.jarvisai

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class VoiceSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 32, 28, 28); setBackgroundColor(Color.rgb(4, 8, 14)) }
        root.addView(TextView(this).apply { text = "JARVIS VOICE"; textSize = 28f; setTextColor(Color.CYAN) })
        root.addView(TextView(this).apply { text = "Optional ElevenLabs configuration. The API key is encrypted locally. Android TTS remains the fallback."; setTextColor(Color.LTGRAY); setPadding(0, 12, 0, 16) })
        val key = EditText(this).apply { hint = "ElevenLabs API key"; inputType = 0x81; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setText(if (ElevenLabsConfig.configured(this@VoiceSettingsActivity)) "••••••••••••" else "") }
        val voice = EditText(this).apply { hint = "ElevenLabs voice ID"; setText(ElevenLabsConfig.voiceId(this@VoiceSettingsActivity)); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY) }
        val model = EditText(this).apply { hint = "Model"; setText(ElevenLabsConfig.model(this@VoiceSettingsActivity)); setTextColor(Color.WHITE); setHintTextColor(Color.GRAY) }
        root.addView(key); root.addView(voice); root.addView(model)
        root.addView(Button(this).apply { text = "SAVE VOICE CONFIG"; setOnClickListener { if (!key.text.toString().contains("•")) ElevenLabsConfig.save(this@VoiceSettingsActivity, key.text.toString(), voice.text.toString(), model.text.toString()); finish() } })
        root.addView(Button(this).apply { text = "USE ANDROID TTS / CLEAR ELEVENLABS"; setOnClickListener { SecureSecretStore.clear(this@VoiceSettingsActivity, "elevenlabs_key"); finish() } })
        setContentView(root)
    }
}
