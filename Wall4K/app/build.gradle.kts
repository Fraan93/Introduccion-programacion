plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

// Firebase se activa automáticamente solo si añades tu google-services.json.
// Sin ese archivo, el proyecto compila igual y la app funciona en modo local.
if (project.file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.fraan.kroma"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.fraan.kroma"
        minSdk = 24
        targetSdk = 35
        versionCode = 16
        versionName = "2.5"
        vectorDrawables { useSupportLibrary = true }

        // API keys for the extra catalogs. Provided via environment variables
        // (GitHub Secrets in CI) or -PPEXELS_API_KEY=... Gradle properties.
        // With empty keys the app still works using the Wallhaven catalog only.
        val pexelsKey = System.getenv("PEXELS_API_KEY")
            ?: (project.findProperty("PEXELS_API_KEY") as? String ?: "")
        val unsplashKey = System.getenv("UNSPLASH_ACCESS_KEY")
            ?: (project.findProperty("UNSPLASH_ACCESS_KEY") as? String ?: "")
        buildConfigField("String", "PEXELS_API_KEY", "\"$pexelsKey\"")
        buildConfigField("String", "UNSPLASH_ACCESS_KEY", "\"$unsplashKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.03")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")

    implementation("androidx.navigation:navigation-compose:2.8.2")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // --- Backend opcional: Firebase (Storage + Firestore) ---
    // Se usan solo si existe google-services.json; si no, la app cae a modo local.
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
