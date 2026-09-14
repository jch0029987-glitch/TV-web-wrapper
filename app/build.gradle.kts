import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") // Added KSP plugin for Room compilation
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

        externalNativeBuild {
            cmake {
                cppFlags("")
                abiFilters("armeabi-v7a", "arm64-v8a", "x86_64")
            }
        }
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
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    externalNativeBuild {
        cmake {
            path(file("src/main/cpp/CMakeLists.txt"))
            version = "3.22.1"
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
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
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Room Database & KSP Compiler
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
}
