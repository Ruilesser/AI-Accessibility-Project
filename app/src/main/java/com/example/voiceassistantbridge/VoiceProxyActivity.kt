package com.example.voiceassistantbridge

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout

class VoiceProxyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lavender background for better visibility and aesthetics
        val lavenderColor = Color.parseColor("#E6E6FA")
        // High-contrast indigo for buttons
        val indigoColor = Color.parseColor("#4B0082")
        val textColor = Color.WHITE

        // Create a simple UI so Voice Access has buttons to "see" and click
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
            setBackgroundColor(lavenderColor)
        }

        val buttonParams = LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, 20, 0, 20)
        }

        val btnWatchLater = Button(this).apply {
            text = "Open Watch Later"
            setBackgroundColor(indigoColor)
            setTextColor(textColor)
            layoutParams = buttonParams
            setOnClickListener {
                sendActionBroadcast(VoiceCommandReceiver.ACTION_OPEN_WATCH_LATER)
            }
        }

        val btnTest = Button(this).apply {
            text = "Run Diagnostics"
            setBackgroundColor(indigoColor)
            setTextColor(textColor)
            layoutParams = buttonParams
            setOnClickListener {
                sendActionBroadcast(VoiceCommandReceiver.ACTION_RUN_TEST)
            }
        }

        val btnExit = Button(this).apply {
            text = "Exit"
            setBackgroundColor(Color.DKGRAY) // Distinct color for exit
            setTextColor(textColor)
            layoutParams = buttonParams
            setOnClickListener {
                finish()
            }
        }

        layout.addView(btnWatchLater)
        layout.addView(btnTest)
        layout.addView(btnExit)
        setContentView(layout)

        // If query, try to handle it
        val spokenQuery = intent.getStringExtra("query") ?: ""
        if (spokenQuery.isNotEmpty()) {
            handleQuery(spokenQuery)
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