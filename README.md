# AI-Accessibility-Project

An open-source Android Accessibility app designed to help individuals manage daily tasks hands-free not provided by Gemini. By bridging **Gemini's voice command layer** with a custom background **Android Accessibility Service**, this system converts spoken commands into screen actions, providing a bridge for users with limited mobility.

---

## Project Scope & Task Inventory

### Core Automation Infrastructure

* **System-Wide Intercepts:** Automated UI scanning across active system windows and multi-layered applications.
* **Eyes-Free Feedback:** Dedicated Text-to-Speech (TTS) engine that forces all confirmation responses.
* **Hands-Free Activation:** Eliminates the need to physically press or hold a "Record Speech" button by leveraging continuous "Hey Google" ambient wake-word integration.
* **Identity & Authentication:** Deep window indexing capable of overriding system isolation blocks to locate and select "Autofill", "Sign In", or "Continue" credential prompts.
* **Voice Messages:** Hands-free setup of outgoing text-to-speech voice messages.

### Supported Tasks & Target Ecosystems

* **Media & Entertainment:** 
  * **YouTube Navigation:** Multi-step macro to automatically launch the native app, navigate past layout walls (the "You" profile page or expanded "Playlists" menus), and surface the **Watch Later** queue.

* **Food Delivery & Shopping:**
  * Contextual intent monitoring and button targeting for **SkipTheDishes**, **UberEats**, and **Amazon Shopping** checkout pages.

### Provided by Gemini default:
* **Spotify Integration:** Specialized remote routing targeting an Android tablet acting as a dedicated music player.
* **Television:** Hands-free surfing of TV channels.

### Scheduling & Productivity:
* **Calendar Management:** Conversational scheduling engine triggered via sequential voice tokens (*"Add to calendar"* followed by event parameters).
* **Proactive Reminders:** Automated alerting layers configured for 1 week, 1 day, day-of, and 15-minute intervals.
* **ETA Coordination:** Dynamic lookup of current calendar status paired with automated message generation to broadcast ETAs.

---

## Technical Setup & Handset Deployment Guide

Follow these steps to compile the application (`VoiceAssistantBridge`) and deploy it directly onto your physical testing device.

### Prerequisites
* Android Studio Jellyfish (or newer) installed on your machine.
* A physical Android smartphone running **Android 8.0 (API level 26)** or higher.
* A high-quality USB data cable.

### Step 1: Prepare the Device for Deployment
To install your custom background service, you must unlock developer privileges on your physical phone:
1. Open your phone's **Settings** and navigate to **About Phone**.
2. Find the **Build Number** row and tap it rapidly **7 times** until a popup says *"You are now a developer!"*
3. Go back to the main Settings menu, find **System > Developer Options**, and switch on **USB Debugging**.

### Step 2: Compile and Build the APK
1. Connect your phone to your computer via USB. If prompted on the phone screen, select **Allow USB Debugging**.
2. Open your project folder inside **Android Studio**.
3. Look at the top toolbar and verify your physical device's name is selected in the deployment dropdown menu.
4. Click the green **Run button (Play icon)** or press `Shift + F10`.
5. Android Studio will compile the Kotlin source code, package it, and push the file directly onto your phone.

### Step 3: Grant System Accessibility Authorization
Because this application acts directly on the interface, Android safely isolates its execution until you explicitly authorize it:
1. On your phone, open **Settings** and search for **Accessibility**.
2. Scroll down to the **Downloaded Services** or **Installed Apps** sub-section.
3. Select **Voice Automation Service** (labeled by your project framework).
4. Turn the toggle **ON**. Agree to the system warning dialog.

---

## Developer Verification & Testing (Via ADB)

Google's IDE plugin ecosystem is transitioning from legacy Assistant hooks to Gemini, the most reliable method to simulate voice parameters during local development is using Android Debug Bridge (ADB) activity intents. This directly mimics Gemini's text extraction capabilities by feeding strings straight into your invisible `VoiceProxyActivity`.

Open the **Terminal** tab at the bottom of Android Studio and run these commands to test your routes:

### 1. Test the Diagnostic Failsafe Route
Verify that your proxy intercepts queries and communicates successfully with your background Accessibility Service:
```bash
adb shell am start -a android.intent.action.VIEW -n com.example.voiceassistantbridge/.VoiceProxyActivity --es query "run a system test"
```

### 2. Test the Youtube Automation Route
Verify that the navigation macro works
```bash
adb shell am start -a android.intent.action.VIEW -n com.example.voiceassistantbridge/.VoiceProxyActivity --es query "open watch later"
```

### 3. Test the Universal Click Macro
Simulate clicking a button labeled "Continue" on whichever screen layer is currently open
```bash
adb shell am start -a android.intent.action.VIEW -n com.example.voiceassistantbridge/.VoiceProxyActivity --es query "click Continue"
```

---

## Gemini Voice Automations Configuration

Because Gemini processes system instructions on a highly literal level compared to legacy voice engines, raw background intent broadcasts are restricted for device security. To trigger your accessibility workflows hands-free, this project passes parameters using explicit app-scoped voice routing.

By defining an implicit intent view layer on your `VoiceProxyActivity`, Gemini can process a conversational search command, parse the trailing query string, and pass it directly into your local app framework without cloud-side routine compiling.

### How it Works
1. You say the explicit wakeup phrase targeting the app namespace: `"Hey Gemini, open [VoiceAssistantBridge] and search for..."`
2. Gemini opens your transparent `VoiceProxyActivity`, passing your spoken phrase into the intent bundle extra.
3. The invisible proxy inspects the string (`.contains("test")`, `.contains("watch later")`), triggers the designated accessibility action, and terminates instantly without interrupting the active screen layout.

Example:\n
1. **The Diagnostic Failsafe Route:**
> *"Open VoiceAssistantBridge and search for **run a system test**"*
> *(or simply "**run test**")*

2. **The YouTube Automation Route:**
> *"Open VoiceAssistantBridge and search for **open watch later**"*

3. **The Universal Click Macro:**
> *"Open VoiceAssistantBridge and search for **click Continue**"*
> *(or any other button name you want to tap, like "**click (FILL HERE)**")*
---

## Operational Vocabulary Matrix

To execute these macros hands-free, wake up your default digital assistant (**"Hey Gemini"** or **"Hey Google"**) and state the following literal phrase patterns. 

| Intent Context | Voice Phrase to Speak | System Execution Result |
| :--- | :--- | :--- |
| **System Check** | *"Hey Gemini, open VoiceAssistantBridge and search for **run a system test**"* | Launches the transparent proxy, fires the internal `RUN_TEST` broadcast, and loops an audible verification success message out of the phone speaker. |
| **YouTube Playlist** | *"Hey Google, open VoiceAssistantBridge and search for **open watch later**"* | Bypasses standard screen restrictions, forces open the native YouTube app package, and executes the multi-stage playlist layout sweeps. |
| **Universal Clicker** | *"Hey Gemini, open VoiceAssistantBridge and search for **click Autofill**"* | Strips out control action verbs, extracts the target keyword string ("Autofill"), and tells the background service to click the matching text node. |
| **Form Navigation** | *"Hey Google, open VoiceAssistantBridge and search for **click Continue**"* | Scans layout boundary nodes for active, unclickable confirmation elements matching "Continue" and fires a virtual touch input layer. |

> **Production Troubleshooting Tip:** Gemini expects literal structural commands. If your device defaults to a generic Google web search instead of executing your macro, verify your syntax explicitly matches the `open [App Name] and search for [Command]` pattern. This forces the Android operating system to treat the trailing string as a local application variable.