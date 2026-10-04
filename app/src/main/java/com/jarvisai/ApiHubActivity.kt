package com.jarvisai

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.text.InputType
import android.widget.*

class ApiHubActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val c = ApiHub.load(this)
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(28,40,28,28); setBackgroundColor(Color.rgb(5,8,13)) }
        fun edit(value:String,hint:String,secret:Boolean=false)=EditText(this).apply { setText(value); this.hint=hint; setTextColor(Color.WHITE); setHintTextColor(Color.GRAY); setSingleLine(true); if(secret) inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD }
        root.addView(TextView(this).apply { text="JARVIS API HUB"; textSize=28f; setTextColor(Color.WHITE) })
        root.addView(TextView(this).apply { text="OpenAI-compatible API configuration"; setTextColor(Color.LTGRAY); setPadding(0,12,0,20) })
        val provider=edit(c.provider,"Provider"); val url=edit(c.baseUrl,"Base URL"); val key=edit(c.apiKey,"API Key",true); val model=edit(c.model,"Model")
        listOf(provider,url,key,model).forEach(root::addView)
        root.addView(Button(this).apply { text="SAVE API CONFIG"; setOnClickListener { ApiHub.save(this@ApiHubActivity,ApiHub.Config(provider.text.toString(),url.text.toString(),key.text.toString(),model.text.toString())); Toast.makeText(this@ApiHubActivity,"Saved",Toast.LENGTH_SHORT).show() } })
        root.addView(Button(this).apply { text="CLEAR CONFIG"; setOnClickListener { ApiHub.clear(this@ApiHubActivity); key.setText("") } })
        setContentView(root)
    }
}
