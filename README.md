# 📸🧠✍️ AI Smart Notebook

![License](https://img.shields.io/badge/License-MIT-blue.svg) ![Kotlin](https://img.shields.io/badge/Kotlin-100%25-blueviolet.svg) ![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-UI-brightgreen.svg) ![Gemini AI](https://img.shields.io/badge/AI-Gemini%202.5%20Flash-orange.svg)

Transform your handwritten lecture notes or textbook pages into organized, structured study guides. Use **Multimodal AI** to transcribe messy handwriting, generate concise summaries, and create interactive flashcards—all in a modern, high-performance **Jetpack Compose** interface.

---

## ✨ Features

-   📷 **Batch Note Capture**: Snap multiple photos of consecutive pages in one session. The app combines them into a single, cohesive digital note.
-   🖼️ **Gallery Import**: Select multiple images from your phone's gallery to generate study materials from existing photos.
-   🎓 **AI Study Guides**: Automatically extracts **Key Concepts** and generates **Interactive Flashcards** from every scan.
-   📂 **Subject Organization**: Keep your academic life organized with a hierarchical **Subject -> Notes** structure.
-   🚀 **Instant Study Mode**: Flip through AI-generated questions with **scrollable content** to test your knowledge right in the app.
-   ⚡ **Speed Optimized**: Uses **Gemini 2.5 Flash** with client-side image compression and resizing for near-instant processing.
-   🎨 **Modern MVVM Architecture**: Built with professional **Clean Architecture** patterns, ensuring a scalable and maintainable codebase.

---

## 🛠 Tech Stack

-   **Language** → **Kotlin**
-   **UI** → **Jetpack Compose** (Material 3, Flow Layouts, Dynamic Grids)
-   **Architecture** → **MVVM** with StateFlow and Repository patterns.
-   **AI** → **Google Gemini 2.5 Flash** (Multimodal Vision capabilities).
-   **Camera** → **CameraX** with multi-capture session support.
-   **Database** → **Room** with relational mapping (Foreign Keys) and destructive migration support.
-   **Build** → **Gradle KSP** and secure API key injection via `local.properties`.

---

## 💰 Cost & Scalability

This app is architected for **zero-cost operation** and high scalability:
-   **Free Tier Optimized**: Leverages Google's 1,500 free daily requests for Gemini Flash.
-   **Token Efficient**: Implements **Image Pre-processing** (1024px resizing & JPEG compression) to minimize token consumption and reduce latency.

---

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone https://github.com/Ankit-git801/AI-Note-Summarizer-App.git
cd AI-Note-Summarizer-App
```

### 2. Set Up the Gemini API Key
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

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.
