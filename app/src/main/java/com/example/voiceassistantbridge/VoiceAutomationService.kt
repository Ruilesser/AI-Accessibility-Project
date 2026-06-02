package com.example.voiceassistantbridge

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import java.util.Locale
import androidx.core.net.toUri

class VoiceAutomationService : AccessibilityService(), TextToSpeech.OnInitListener {

    private lateinit var commandReceiver: VoiceCommandReceiver
    private lateinit var tts: TextToSpeech
    private var isTtsReady = false
    private var watchLaterRetryCount = 0
    private val maxRetries = 6 // Will scan the screen for up to 3 seconds (6*500ms)

    override fun onCreate() {
        super.onCreate()
        // Initialize TTS so the app can talk back eyes-free
        tts = TextToSpeech(this, this)

        // Initialize and register the radio receiver
        commandReceiver = VoiceCommandReceiver(this)
        val filter = android.content.IntentFilter().apply {
            addAction(VoiceCommandReceiver.ACTION_OPEN_WATCH_LATER)
            addAction(VoiceCommandReceiver.ACTION_CLICK_TEXT)
        }
        registerReceiver(commandReceiver, filter, RECEIVER_EXPORTED)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
            isTtsReady = true
        }
    }

    // This triggers out loud whenever the app needs to announce an action
    private fun speak(text: String) {
        if (isTtsReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "AutomationID")
        }
        Log.d("VoiceAutomation", "TTS Spoke: $text")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // Detect when a user opens an app
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: ""
            Log.d("VoiceAutomation", "User switched to app: $packageName")

            // Automatically trigger an action when a specific app opens:
            if (packageName.contains("ubereats") || packageName.contains("skipthedishes")) {
                speak("Opened food delivery app. Looking for checkout targets.")
            }
        }
    }

    /**
     * Launches the native YouTube App hands-free.
     */
    fun openYouTubeWatchLaterHandsFree() {
        speak("Opening YouTube.")

        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = "https://www.youtube.com".toUri()
            setPackage("com.google.android.youtube") // Guarantees the native app opens
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            startActivity(intent)

            // Wait 2.5 seconds for the app launch animation before starting UI scans
            Handler(Looper.getMainLooper()).postDelayed({
                executeYouTubeNavigationSequence()
            }, 2500)

        } catch (e: Exception) {
            speak("YouTube application is not installed or could not be opened.")
        }
    }

    /**
     * Dynamically searches for and clicks the "You" or "Library" main profile menu.
     */
    private fun executeYouTubeNavigationSequence() {
        val rootNode: AccessibilityNodeInfo? = rootInActiveWindow
        if (rootNode == null) {
            speak("Unable to scan YouTube screen layout.")
            return
        }

        val targetNode = findNodeByTextAlternative(rootNode, "You")
            ?: findNodeByTextAlternative(rootNode, "Library")

        if (targetNode != null && performClickAction(targetNode)) {
            speak("Navigating to profile.")

            // Reset our retry counter for the first phase
            watchLaterRetryCount = 0

            // Pause briefly for the page transition, then search for Watch Later
            Handler(Looper.getMainLooper()).postDelayed({
                clickWatchLaterSubMenu()
            }, 1500)
        } else {
            // Fallback: If bottom tabs aren't accessible, try scanning the direct screen anyway
            clickWatchLaterSubMenu()
        }
    }

    /**
     * Searches layout for the specific "Watch later" item with automatic retry logic.
     * If it fails after retrying, expands searh into Playlists sub-menu
     */
    private fun clickWatchLaterSubMenu() {
        val freshRootNode: AccessibilityNodeInfo? = rootInActiveWindow
        if (freshRootNode == null) {
            retryWatchLaterNavigation()
            return
        }

        val watchLaterNode = findNodeByTextAlternative(freshRootNode, "Watch later")
        if (watchLaterNode != null && performClickAction(watchLaterNode)) {
            speak("Your watch later queue is now open.")
            watchLaterRetryCount = 0 // Success! Clear loop counter
        } else {
            retryWatchLaterNavigation()
        }
    }

    /**
     * Manages retries for finding the "Watch later" button directly.
     * If it exhausts all retries, it triggers the secondary fallback to open Playlists.
     */
    private fun retryWatchLaterNavigation() {
        if (watchLaterRetryCount < maxRetries) {
            watchLaterRetryCount++
            Log.d("VoiceAutomation", "Watch later not found yet. Retrying attempt $watchLaterRetryCount")

            Handler(Looper.getMainLooper()).postDelayed({
                clickWatchLaterSubMenu()
            }, 500)
        } else {
            // PHASE 2 FALLBACK: "Watch later" isn't visible on the main screen.
            // Let's find the Playlists or "See all" button to reveal it.
            Log.d("VoiceAutomation", "Watch later not visible. Attempting to expand Playlists menu.")
            watchLaterRetryCount = 0 // Reset counter for the next phase
            navigateToPlaylistsSection()
        }
    }

    /**
     * Finds and clicks "Playlists" or "See all" to expand hidden lists.
     */
    private fun navigateToPlaylistsSection() {
        val freshRootNode: AccessibilityNodeInfo = rootInActiveWindow ?: return

        // Because freshRootNode is now guaranteed non-null, this line works perfectly!
        val expandNode = findNodeByTextAlternative(freshRootNode, "Playlists")
            ?: findNodeByTextAlternative(freshRootNode, "See all")

        if (expandNode != null && performClickAction(expandNode)) {
            speak("Expanding playlists views.")

            // Give the sub-menu 1.5 seconds to open, then run final sweep
            Handler(Looper.getMainLooper()).postDelayed({
                lookForWatchLaterInPlaylists()
            }, 1500)
        } else {
            speak("Failed to find Watch Later or Playlists menu.")
        }
    }

    /**
     * Step 5 (Final Scan): Final sweep inside the expanded playlist window.
     */
    private fun lookForWatchLaterInPlaylists() {
        val finalRootNode: AccessibilityNodeInfo? = rootInActiveWindow
        if (finalRootNode == null) {
            speak("Screen content lost during playlist expansion.")
            return
        }

        val watchLaterNode = findNodeByTextAlternative(finalRootNode, "Watch later")
        if (watchLaterNode != null && performClickAction(watchLaterNode)) {
            speak("Your watch later queue is now open.")
        } else {
            speak("Could not find your Watch Later list anywhere on this screen.")
        }
    }

    /**
     * VERY IMPORTANT
     * Call this method when your custom voice command parser (or an incoming Intent) 
     * tells the app to click something specific on the screen.
     */
    fun findAndClickButtonByText(targetText: String) {
        // Get ALL interactive windows currently visible on the screen (including system)
        val windows = windows
        if (windows.isEmpty()) {
            // Fallback to active window if window list is empty
            val rootNode: AccessibilityNodeInfo? = rootInActiveWindow
            if (rootNode != null) {
                searchAndClickInNode(rootNode, targetText)
            } else {
                speak("Screen content is not available.")
            }
            return
        }

        var clickedSuccessfully = false

        // Loop through every window layers from top to bottom
        for (window in windows) {
            val rootNode = window.root
            if (rootNode != null) {
                if (searchAndClickInNode(rootNode, targetText)) {
                    clickedSuccessfully = true
                    break // Stop searching once we found and clicked it
                }
            }
        }

        if (!clickedSuccessfully) {
            speak("Could not find a button labeled $targetText on this screen.")
        }
    }

    // Helper method to break out the clicking logic
    private fun searchAndClickInNode(rootNode: AccessibilityNodeInfo, targetText: String): Boolean {
        val list: List<AccessibilityNodeInfo> = rootNode.findAccessibilityNodeInfosByText(targetText)
        if (list.isNotEmpty()) {
            for (node in list) {
                if (performClickAction(node)) {
                    speak("Successfully clicked $targetText.")
                    return true
                }
            }
        }
        return false
    }

    // Function to do the click
    // Added safetyDepth precaution so infinite loop is addressed
    private fun performClickAction(node: AccessibilityNodeInfo?): Boolean {
        var currentNode = node ?: return false
        var safetyDepthCounter = 0
        val maxLayoutDepth = 25 // Android layouts rarely exceed 15-20 layers deep

        while (currentNode != null && safetyDepthCounter < maxLayoutDepth) {
            if (currentNode.isClickable) {
                // Execute the physical virtual click
                currentNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
            currentNode = currentNode.parent
            safetyDepthCounter++ // Increment every time we climb up a layer
        }

        // If we hit null OR exceeded 25 layers, safely give up
        Log.w("VoiceAutomation", "Click failed: Reached root or hit safety depth limit.")
        return false
    }

    /**
     * Backup deep search utility to read hidden accessibility descriptions.
     */
    private fun findNodeByTextAlternative(root: AccessibilityNodeInfo, targetText: String): AccessibilityNodeInfo? {
        val matches = root.findAccessibilityNodeInfosByText(targetText)
        if (matches.isNotEmpty()) {
            return matches[0]
        }
        return recursiveSearchByDescription(root, targetText)
    }

    private fun recursiveSearchByDescription(node: AccessibilityNodeInfo?, targetText: String): AccessibilityNodeInfo? {
        if (node == null) return null

        val description = node.contentDescription?.toString() ?: ""
        if (description.contains(targetText, ignoreCase = true)) {
            return node
        }

        // Repeat until found
        for (i in 0 until node.childCount) {
            val found = recursiveSearchByDescription(node.getChild(i), targetText)
            if (found != null) return found
        }
        return null
    }

    override fun onInterrupt() {
        Log.e("VoiceAutomation", "Service Interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        // Unregister so it doesn't leak memory when turned off
        unregisterReceiver(commandReceiver)
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
    }
}