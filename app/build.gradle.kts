// --- THIS IMPORT FIXES THE "util" ERROR ---
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    // FIX: Uses the standard Android plugin alias
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.apurba2509.chatbot"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.apurba2509.chatbot"
        // FIX: Changed from 24 to 26 to support Adaptive Icons
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // --- READ KEY FROM local.properties ---
        val properties = Properties()
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }
        val key = properties.getProperty("apiKey") ?: ""
        buildConfigField("String", "API_KEY", "\"$key\"")
        // --------------------------------------
    }

    buildFeatures {
        compose = true
        buildConfig = true // REQUIRED for API Key
    }

    // --- FIX: ALIGN JAVA AND KOTLIN VERSIONS ---
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    // -------------------------------------------

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Gemini Android SDK
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")

    // Compose UI
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    // ADD THIS LINE for better icons (Robot, Sparkles, etc.)
    implementation("androidx.compose.material:material-icons-extended:1.6.0")

    // Async & Lifecycle
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
}