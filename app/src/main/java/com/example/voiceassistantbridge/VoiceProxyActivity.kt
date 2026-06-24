package com.example.voiceassistantbridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log

class VoiceProxyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Extract the spoken text parsed by Gemini from the shortcuts parameter
        val spokenQuery = intent.getStringExtra("query") ?: ""
        Log.d("VoiceProxyActivity", "Gemini Query Intercepted: $spokenQuery")

        if (spokenQuery.isNotEmpty()) {
            val broadcastIntent = Intent().apply {
                setPackage(packageName) // ensure only this app receives it
                
                // Determine if it's a test command or a click command based on phrasing
                if (spokenQuery.contains("test", ignoreCase = true) || spokenQuery.contains("diagnostics", ignoreCase = true)) {
                    action = VoiceCommandReceiver.ACTION_RUN_TEST
                } else if (spokenQuery.contains("watch later", ignoreCase = true)) {
                    action = VoiceCommandReceiver.ACTION_OPEN_WATCH_LATER
                } else {
                    // Extract core text targets (e.g., if user says "click Continue", we pass "Continue")
                    action = VoiceCommandReceiver.ACTION_CLICK_TEXT
                    val cleanedText = spokenQuery
                        .replace("click", "", ignoreCase = true)
                        .replace("tap", "", ignoreCase = true)
                        .trim()
                    putExtra("target_text", cleanedText)
                }
            }
            // Send to your running Accessibility Service Receiver safely
            sendBroadcast(broadcastIntent)
        }

        // Close instantly so no window UI blinks onto the user's screen
        finish()
    }
}