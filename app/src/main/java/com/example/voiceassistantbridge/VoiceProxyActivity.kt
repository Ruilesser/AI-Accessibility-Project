package com.example.voiceassistantbridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.view.Gravity

class VoiceProxyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a simple UI so Voice Access has buttons to "see" and click
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
        }

        val btnWatchLater = Button(this).apply {
            text = "Open Watch Later"
            setOnClickListener {
                sendActionBroadcast(VoiceCommandReceiver.ACTION_OPEN_WATCH_LATER)
            }
        }

        val btnTest = Button(this).apply {
            text = "Run Diagnostics"
            setOnClickListener {
                sendActionBroadcast(VoiceCommandReceiver.ACTION_RUN_TEST)
            }
        }

        val btnExit = Button(this).apply {
            text = "Exit"
            setOnClickListener {
                finish()
            }
        }

        layout.addView(btnWatchLater)
        layout.addView(btnTest)
        layout.addView(btnExit)
        setContentView(layout)

        // If Gemini passed a query, try to handle it immediately
        val spokenQuery = intent.getStringExtra("query") ?: ""
        if (spokenQuery.isNotEmpty()) {
            handleQuery(spokenQuery)
            // Note: We don't finish() immediately anymore if we want the UI to stay visible for Voice Access
        }
    }

    private fun handleQuery(query: String) {
        val lowered = query.lowercase()
        when {
            lowered.contains("test") || lowered.contains("diagnostics") -> {
                sendActionBroadcast(VoiceCommandReceiver.ACTION_RUN_TEST)
            }
            lowered.contains("watch later") || lowered.contains("later") -> {
                sendActionBroadcast(VoiceCommandReceiver.ACTION_OPEN_WATCH_LATER)
            }
        }
    }

    private fun sendActionBroadcast(actionStr: String) {
        val intent = Intent(actionStr).apply {
            setPackage(packageName)
        }
        sendBroadcast(intent)
    }
}