# Orbit AI 🌌

**Orbit AI** is a next-generation Android chatbot built with **Jetpack Compose** and Google's **Gemini API**. It features a "Sonic Elegance" design language with dynamic animated backgrounds, glass-morphic UI elements, and a focus on premium aesthetics.

## ✨ Features

- **🧠 Dual Intelligence:** Seamlessly switch between **Gemini 2.5 Flash** (for speed) and **Gemini 3 Pro** (for reasoning).
- **🎨 Sonic UI:** A deep, immersive dark theme with a dynamic, animated nebula background that breathes with the app.
- **🔮 Glassmorphism:** Modern, translucent input bars and menus with frosted glass effects.
- **📝 Markdown Rendering:** Custom-built text engine that renders **Bold**, *Italics*, `Code Blocks`, and Bullet points natively in Compose.
- **📸 Multimodal Support:** Send images directly from your Camera or Gallery for AI analysis.
- **🔒 Privacy First:** API keys are secured via local properties and never exposed in version control.

## 🛠️ Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **AI Engine:** [Google Generative AI SDK](https://ai.google.dev/sdks)
- **Build System:** Gradle Kotlin DSL
- **Architecture:** MVVM (Lightweight)

## 🚀 Setup & Installation

1.  **Clone the repository:**
    ```bash
    git clone [https://github.com/YOUR_USERNAME/Orbit-AI.git](https://github.com/YOUR_USERNAME/Orbit-AI.git)
    ```

2.  **Open in Android Studio:**
    Open the project folder in the latest version of Android Studio (Koala or newer recommended).

3.  **Configure API Key:**
    * Get your API key from [Google AI Studio](https://aistudio.google.com/).
    * Create a file named `local.properties` in the root directory of the project (if it's not already there).
    * Add the following line to `local.properties`:
        ```properties
        apiKey=YOUR_ACTUAL_API_KEY_HERE
        ```
    *> **Note:** This file is ignored by Git to keep your key safe.*

4.  **Build & Run:**
    * Sync Gradle files.
    * Select an emulator or connect your physical Android device.
    * Click **Run**.

## 🤝 Contributing

Contributions are welcome! If you have ideas for new features (like Voice Input or History storage), feel free to fork the repo and submit a Pull Request.

1.  Fork the Project
2.  Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3.  Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4.  Push to the Branch (`git push origin feature/AmazingFeature`)
5.  Open a Pull Request

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---
*Built with ❤️ by Apurba*
