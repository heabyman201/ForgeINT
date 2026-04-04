import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.gms.google-services")
    id("com.google.firebase.firebase-perf")

}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun readSecret(name: String): String {
    return (localProperties.getProperty(name) ?: providers.gradleProperty(name).orNull ?: "")
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
}

android {
    namespace = "com.example.forgeint"
    ndkVersion = "27.0.12077973"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.forgeint"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk {
            abiFilters += listOf(
                "armeabi-v7a",
                "arm64-v8a",
                "x86",
                "x86_64"
            )
        }
        buildConfigField("String", "OPENROUTER_API_KEY", "\"${readSecret("OPENROUTER_API_KEY")}\"")
        buildConfigField("String", "TAVILY_API_KEY", "\"${readSecret("TAVILY_API_KEY")}\"")
        buildConfigField("String", "HUGGING_FACE_TOKEN", "\"${readSecret("HUGGING_FACE_TOKEN")}\"")
        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17")
                arguments(
                    "-DANDROID_STL=c++_shared",
                    "-DANDROID_ARM_NEON=TRUE"
                )
            }
        }
    }
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    useLibrary("wear-sdk")
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.play.services.wearable)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.compose.material)
    implementation(libs.compose.foundation)
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
    implementation("androidx.wear.tiles:tiles:1.5.0")
    implementation("androidx.wear.tiles:tiles-material:1.5.0")
    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation ("com.google.firebase:firebase-perf")
    implementation("androidx.wear.compose:compose-navigation:1.5.6")
    implementation("androidx.datastore:datastore-preferences:1.2.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation(libs.wear.input)
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation(libs.compose.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.text)
    implementation(libs.androidx.ui.geometry)
    implementation(libs.androidx.foundation)
    ksp("androidx.room:room-compiler:2.6.1") // Or kapt if not using KSP
    implementation("androidx.compose.material:material-icons-extended:1.6.0")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("com.google.guava:guava:31.1-android")
    testImplementation(libs.junit)
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}
