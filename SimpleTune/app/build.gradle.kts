import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val socialApiUrl = providers.gradleProperty("SOCIAL_API_URL").orNull ?: "https://social-music-server.onrender.com"

// Single shared signing key so every build (debug or release, local or CI) has the SAME
// signature, letting new APKs install over older ones. Credentials live in
// SimpleTune/keystore/key.properties locally (gitignored) and are recreated in CI from
// GitHub secrets. Without them the build falls back to the default debug signing.
val signingPropsFile = rootProject.file("keystore/key.properties")
val signingProps = Properties().apply {
    if (signingPropsFile.exists()) FileInputStream(signingPropsFile).use { load(it) }
}
val hasVibeonKey = signingProps.containsKey("storeFile")

android {
    namespace = "com.vibeon.music"
    compileSdk = 36

    signingConfigs {
        create("vibeon") {
            if (hasVibeonKey) {
                storeFile = rootProject.file("keystore/${signingProps.getProperty("storeFile")}")
                storePassword = signingProps.getProperty("storePassword")
                keyAlias = signingProps.getProperty("keyAlias")
                keyPassword = signingProps.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = "com.vibeon.music"
        minSdk = 29
        targetSdk = 36
        versionCode = 5
        versionName = providers.gradleProperty("VIBEON_VERSION_NAME").getOrElse("2.0.3")
        vectorDrawables { useSupportLibrary = true }

        buildConfigField("String", "SOCIAL_API_URL", "\"$socialApiUrl\"")
    }

    buildTypes {
        debug {
            if (hasVibeonKey) signingConfig = signingConfigs.getByName("vibeon")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (hasVibeonKey) {
                signingConfig = signingConfigs.getByName("vibeon")
            } else {
                signingConfig = signingConfigs.getByName("debug")
            }
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

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.foundation)
implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.exoplayer.hls)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.common)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.androidx.datastore)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.coil.compose)
}