
---

# AI-Accessibility-Project

An open-source Android Accessibility app designed to help individuals manage daily tasks hands-free not provided by Gemini. By bridging **Gemini's voice command layer** with a custom background **Android Accessibility Service**, this system converts spoken commands into screen actions, providing a bridge for users with limited mobility.

---

## Project Scope & Task Inventory

### Core Automation Infrastructure

* **System-Wide Intercepts:** Automated UI scanning across active system windows and multi-layered applications.
* **Eyes-Free Feedback:** Dedicated Text-to-Speech (TTS) engine that forces all confirmation responses.
* **Hands-Free Activation:** Eliminates the need to physically press or hold a "Record Speech" button by leveraging continuous "Hey Google" ambient wake-word integration.

### Supported Tasks & Target Ecosystems

* **Media & Entertainment:** * **YouTube Navigation:** Multi-step macro to automatically launch the native app, navigate past layout walls (the "You" profile page or expanded "Playlists" menus), and surface the **Watch Later** queue.

### Provided by Gemini default:
* **Spotify Integration:** Specialized remote routing targeting an Android tablet acting as a dedicated music player.
* **Television:** Hands-free surfing of TV channels.


* **Food Delivery & Shopping:**
* Contextual intent monitoring and button targeting for **SkipTheDishes**, **UberEats**, and **Amazon Shopping** checkout pages.


* **Scheduling & Productivity:**
* **Calendar Management:** Conversational scheduling engine triggered via sequential voice tokens (*"Add to calendar"* followed by event parameters).
* **Proactive Reminders:** Automated alerting layers configured for 1 week, 1 day, day-of, and 15-minute intervals.
* **ETA Coordination:** Dynamic lookup of current calendar status paired with automated message generation to broadcast ETAs.


* **System Tools & Security:**
* **Identity & Authentication:** Deep window indexing capable of overriding system isolation blocks to locate and select "Autofill", "Sign In", or "Continue" credential prompts.
* **Secure Password Storage:** Integration with an encrypted file architecture hosted on a private server.
* **Voice Messages:** Hands-free setup of outgoing text-to-speech voice messages.



---

## Technical Setup & Deployment Guide

Follow these steps to build the custom bridge application (`VoiceAssistantBridge`) and deploy it directly onto your physical testing device.

### Prerequisites
* Android Studio Jellyfish (or newer) installed on your machine.
* A physical Android smartphone running **Android 8.0 (API level 26)** or higher.
* A high-quality USB data cable.

### Step 1: Prepare the Device for Deployment
To install your custom background service, you must unlock developer privileges on your physical phone:
1. Open your phone's **Settings** and navigate to **About Phone**.
2. Find the **Build Number** row and tap it **7 times** until a popup says *"You are now a developer!"*
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

## Gemini Voice Routines Configuration

To trigger your app's functions completely hands-free without manually executing command-line intent strings, you must configure Google Assistant/Gemini to act as a voice trigger mapper.

### Process: Creating a Universal Wildcard Click Tool
This routine maps a single conversational shortcut to your generic button-clicking engine, removing the need to hardcode words ahead of time.

1. Open the **Google Home** app on your phone.
2. Tap your profile icon in the top right and select **Assistant Settings** > **Routines**.
3. Tap the **"+" (New)** icon to build a custom routine.
4. **Configure the Trigger ("When I say to Google"):**
   * Select **Voice command**.
   * Type exact text: `Click $` (or `Tap $`). The `$` character acts as an on-the-fly wildcard parameter.
5. **Configure the System Action ("The Assistant will..."):**
   * Tap **Add action** and choose **Try adding your own**.
   * Paste the exact structural intent broadcast command block:
     ```text
     Send intent com.example.voiceassistantbridge.CLICK_TEXT with extra target_text string $
     ```
6. Tap **Save**.

### Process: Creating the YouTube Watch Later Macro
This routing allows the user to easily load their watch queue using natural phrasing.

1. Inside the **Routines** menu, click **New Routine**.
2. **Configure the Trigger:**
   * Select **Voice command**.
   * Type exact text: `Open my Watch Later queue` (or `Check my watch later list`).
3. **Configure the Action:**
   * Select **Try adding your own**.
   * Paste the package-restricted signal launcher:
     ```text
     Send intent com.example.voiceassistantbridge.OPEN_WATCH_LATER
     ```
4. Tap **Save**.

---

## Operational Vocabulary Matrix

Once the setups above are complete, the user can speak these fluid phrase variations to activate the background Kotlin architecture completely eyes-free. All feedback confirmations will automatically announce out of the phone's primary audio speaker.

| Intent Context | Natural Phrase to Speak | System Execution Result |
| :--- | :--- | :--- |
| **YouTube Playlist** | *"Hey Google, open my Watch Later queue"* | Launches native YouTube app, opens profile layout, checks for visibility, expands Playlists via fallback if hidden, and opens target. |
| **Credential Filling** | *"Hey Google, click Autofill"* | Intercepts system overlay window layer, scans for active text match, and simulates physical input tap. |
| **Form Navigation** | *"Hey Google, click Continue"* | Bypasses standard app boundaries to activate general confirmation workflows. |
| **Action Confirmation**| *"Hey Google, click Accept"* | Fires universal accessibility clicker tool directly over structural system prompts. |


*(Note: This holds the audio focus token open so the user can speak their follow-up event parameters natively into the connected cloud database.)*

---

## Project Directory Architecture

For reference, verify files match this folder structure:

```text
AI-Accessibility-Project/
│
├── app/
│   ├── src/main/
│   │   ├── java/com/example/voiceassistantbridge/
│   │   │   ├── VoiceAutomationService.kt  <-- Core UI Scanner & Driver Engine
│   │   │   └── VoiceCommandReceiver.kt   <-- System Intent Radio Interceptor
│   │   │
│   │   └── res/
│   │       ├── xml/
│   │       │   └── accessibility_service_config.xml <-- System Flags Window Configuration
│   │       └── layout/
│   │
│   └── build.gradle.kts
└── README.md

```