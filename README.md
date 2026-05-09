# 📸🧠✍️ AI Notes Summarizer

![License](https://img.shields.io/badge/License-MIT-blue.svg) ![Kotlin](https://img.shields.io/badge/Kotlin-100%25-blueviolet.svg) ![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-brightgreen.svg)

Turn scanned notes or pasted text into clean, concise summaries using **on-device OCR** and **Google Gemini AI**, then **save, tag, search, pin, and share**—all in a modern, high-performance **Jetpack Compose UI**.

---

## ✨ Features

-   📷 **Live OCR Feedback**: Scan text with a live camera preview featuring **real-time visual boxes** over detected words, powered by **CameraX** and **ML Kit**.
-   🤖 **AI Streaming Summaries**: Generate summaries in real-time with **Google Gemini 2.5 Flash**. Watch the text flow word-by-word as it's generated.
-   🏷️ **AI-Powered Smart Tagging**: Let AI automatically categorize your notes with relevant tags (#Business, #Ideas, #Health) for effortless organization.
-   🚀 **Instant Search**: Lightning-fast history lookups using **Room Full-Text Search (FTS4)**, capable of querying thousands of notes instantly.
-   🎨 **Premium UI/UX**: A modern **Glassmorphism** theme with vibrant gradients, bold typography, and smooth micro-animations.
-   📄 **Professional Export**: Export your summaries as clean **Markdown (.md)** files to share with professional apps like Notion, Obsidian, or via email.
-   📌 **Pin & Manage**: Keep your most important summaries at the top. Edit, share, and manage your history with ease.

---

## 🛠 Tech Stack

-   **Language** → **Kotlin**
-   **UI** → **Jetpack Compose** (Material 3, Glassmorphism, Animations)
-   **Architecture** → **MVVM** with StateFlow and Coroutines.
-   **AI** → **Google Gemini API** (`gemini-2.5-flash`) with real-time streaming.
-   **OCR** → **ML Kit Text Recognition** with live detection overlays.
-   **Database** → **Room** with **FTS4** optimization for high-speed text search.
-   **Navigation** → **Accompanist Navigation Animation**.
-   **Build** → **Gradle KSP** and secure API key injection via **local.properties**.

---

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone https://github.com/Ankit-git801/AI-Note-Summarizer-App.git
cd AI-Note-Summarizer-App
```

### 2. Set Up the Gemini API Key

The app uses the **BuildConfig** method to securely handle the API key.

1.  Create a file named `local.properties` in the root directory.
2.  Add your Gemini API key:
    ```
    GEMINI_API_KEY="YOUR_API_KEY_HERE"
    ```
3.  Ensure the **Generative Language API** is enabled in your [Google AI Studio](https://aistudio.google.com/).

### 3. Open and Run the Project

1.  Open in Android Studio.
2.  Perform a **Gradle Sync**.
3.  Run on an Android device (API 26+).

---

## 🔧 Project Details

### Database Migration
The app handles automated migrations (v1 -> v2 -> v3) to support new features like pinning and Full-Text Search without data loss.

### Performance Tuning
Model parameters are tuned for **latency reduction**, ensuring that the 2.5-Flash model provides the fastest possible results for mobile users.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
