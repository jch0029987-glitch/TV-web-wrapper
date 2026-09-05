import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.example.messengerwrapper"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.messengerwrapper"
        minSdk = 21
        targetSdk = 34
        versionCode = (project.findProperty("versionCode") as? String)?.toInt() ?: 1
        versionName = (project.findProperty("versionName") as? String) ?: "1.0"
    }

    signingConfigs {
        create("release") {
            val hasFile = keystorePropertiesFile.exists()
            storeFile = file(if (hasFile) keystoreProperties.getProperty("storeFile") else "release.jks")
            storePassword = if (hasFile) keystoreProperties.getProperty("storePassword") else System.getenv("KEYSTORE_PASSWORD")
            keyAlias = if (hasFile) keystoreProperties.getProperty("keyAlias") else System.getenv("KEY_ALIAS")
            keyPassword = if (hasFile) keystoreProperties.getProperty("keyPassword") else System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
}
