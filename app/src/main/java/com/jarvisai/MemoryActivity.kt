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
import java.text.DateFormat
import java.util.Date

class MemoryActivity : AppCompatActivity() {
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 32, 24, 24)
            setBackgroundColor(Color.rgb(4, 8, 14))
        }
        root.addView(TextView(this).apply {
            text = "JARVIS MEMORY"
            textSize = 28f
            setTextColor(Color.CYAN)
        })
        root.addView(TextView(this).apply {
            text = "Encrypted local conversation history and user-defined memories."
            setTextColor(Color.LTGRAY)
            setPadding(0, 12, 0, 16)
        })

        val key = EditText(this).apply { hint = "Memory name (e.g. preferred_language)"; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY) }
        val value = EditText(this).apply { hint = "Memory value"; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY) }
        root.addView(key)
        root.addView(value)
        root.addView(Button(this).apply {
            text = "SAVE USER MEMORY"
            setOnClickListener {
                if (key.text.isNotBlank() && value.text.isNotBlank()) {
                    MemoryStore.saveUserMemory(this@MemoryActivity, key.text.toString(), value.text.toString())
                    key.text.clear(); value.text.clear(); refresh()
                }
            }
        })
        root.addView(Button(this).apply {
            text = "CLEAR ALL MEMORY"
            setOnClickListener { MemoryStore.clearAll(this@MemoryActivity); refresh() }
        })

        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(ScrollView(this).apply {
            addView(list)
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        })
        setContentView(root)
        refresh()
    }

    private fun refresh() {
        list.removeAllViews()
        MemoryStore.userMemories(this).forEach { (key, value) ->
            list.addView(TextView(this).apply {
                text = "$key = $value"
                textSize = 15f
                setTextColor(Color.WHITE)
                setPadding(0, 12, 0, 4)
            })
            list.addView(Button(this).apply {
                text = "DELETE $key"
                setOnClickListener { MemoryStore.deleteUserMemory(this@MemoryActivity, key); refresh() }
            })
        }
        MemoryStore.conversations(this).takeLast(30).reversed().forEach { entry ->
            list.addView(TextView(this).apply {
                text = "${entry.role.uppercase()} • ${DateFormat.getDateTimeInstance().format(Date(entry.time))}\n${entry.text}"
                textSize = 14f
                setTextColor(Color.LTGRAY)
                setPadding(0, 16, 0, 16)
            })
        }
    }
}
