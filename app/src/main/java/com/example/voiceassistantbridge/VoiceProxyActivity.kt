package com.example.voiceassistantbridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

class VoiceProxyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val spokenQuery = intent.getStringExtra("query") ?: ""
        Log.d("VoiceProxyActivity", "Gemini Query Intercepted: $spokenQuery")

        if (spokenQuery.isNotEmpty()) {
            val broadcastIntent = Intent().apply {
                // Route spoken input to the text click handler
                action = VoiceCommandReceiver.ACTION_CLICK_TEXT
                putExtra("target_text", spokenQuery)
            }
            sendBroadcast(broadcastIntent)
        }

        finish()
    }
}