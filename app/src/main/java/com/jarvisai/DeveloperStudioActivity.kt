package com.jarvisai

import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Beginner-safe coding workspace: generation/explanation is AI-assisted; execution is not unrestricted. */
class DeveloperStudioActivity : AppCompatActivity() {
    private lateinit var code: EditText
    private lateinit var console: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(buildUi())
    }

    private fun buildUi() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(20, 28, 20, 20)
        setBackgroundColor(Color.rgb(3, 7, 12))
        addView(TextView(this@DeveloperStudioActivity).apply {
            text = "JARVIS DEV STUDIO"
            textSize = 26f
            setTextColor(Color.CYAN)
        })
        addView(TextView(this@DeveloperStudioActivity).apply {
            text = "AI coding • explain • fix • preview\nArbitrary code is never executed with unrestricted device privileges."
            setTextColor(Color.LTGRAY)
            setPadding(0, 10, 0, 12)
        })
        val prompt = EditText(this@DeveloperStudioActivity).apply {
            hint = "Ask JARVIS to create or fix code…"
            setTextColor(Color.WHITE); setHintTextColor(Color.GRAY)
        }
        addView(prompt)
        code = EditText(this@DeveloperStudioActivity).apply {
            hint = "Project file / code"
            gravity = android.view.Gravity.TOP
            minLines = 12
            setTextColor(Color.rgb(180, 240, 255)); setHintTextColor(Color.GRAY)
            setBackgroundColor(Color.rgb(10, 16, 24))
        }
        addView(ScrollView(this@DeveloperStudioActivity).apply {
            addView(code)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        val actions = LinearLayout(this@DeveloperStudioActivity).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        actions.addView(Button(this@DeveloperStudioActivity).apply {
            text = "GENERATE"
            setOnClickListener { ask(prompt.text.toString(), "Generate code for this request. Return only the code and a short setup note.") }
        })
        actions.addView(Button(this@DeveloperStudioActivity).apply {
            text = "EXPLAIN"
            setOnClickListener { ask(code.text.toString(), "Explain this code for a beginner. Identify important lines and risks.") }
        })
        actions.addView(Button(this@DeveloperStudioActivity).apply {
            text = "FIX"
            setOnClickListener { ask(code.text.toString(), "Find compile/runtime errors and return corrected code, followed by a short list of fixes.") }
        })
        addView(actions)
        console = TextView(this@DeveloperStudioActivity).apply {
            text = "CONSOLE • READY"
            setTextColor(Color.LTGRAY)
            setPadding(0, 10, 0, 0)
        }
        addView(console)
    }

    private fun ask(input: String, instruction: String) {
        if (input.isBlank()) { console.text = "Enter a request or code first."; return }
        console.text = "JARVIS AI • THINKING…"
        ApiClient.ask(this, "$instruction\n\nINPUT:\n$input") { response ->
            code.setText(response)
            console.text = "JARVIS AI • RESPONSE READY"
        }
    }
}
