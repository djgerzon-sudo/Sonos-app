// Top-level build file — App Sonos
// Versiones fijadas y probadas juntas: AGP 8.5.2 + Kotlin 1.9.24 + Gradle 8.7 + Java 17.
// No subir versiones sin probar la pareja completa.
plugins {
    id("com.android.application") version "8.5.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.24" apply false
    // Sin plugin compose aquí: con Kotlin 1.9.24 el plugin
    // org.jetbrains.kotlin.plugin.compose no existe (llegó en Kotlin 2.0).
    // Compose se activa con buildFeatures.compose + composeOptions
    // kotlinCompilerExtensionVersion 1.5.14 en app/build.gradle.kts.
}
