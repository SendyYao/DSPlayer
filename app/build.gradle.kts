import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-parcelize")
    id("kotlin-kapt")
}

val localProperties = Properties().apply {
    load(rootProject.file("local.properties").inputStream())
}

android {
    namespace = "com.whisperyao.dsplayer"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.whisperyao.dsplayer"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        ndkVersion = "26.1.10909125"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "NAS_ADDRESS", "\"${localProperties["nas.address"]}\"")
        buildConfigField("int", "NAS_PORT", localProperties["nas.port"].toString())
        buildConfigField("String", "NAS_ACCOUNT", "\"${localProperties["nas.account"]}\"")
        buildConfigField("String", "NAS_PASSWORD", "\"${localProperties["nas.password"]}\"")
        buildConfigField("String", "NAS_SID", "\"${localProperties["nas.sid"]}\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.lyricviewx)
    implementation(libs.flexbox)
    implementation(libs.okhttp)
    implementation("com.squareup.okhttp3:okhttp-urlconnection:4.12.0")
    implementation(libs.fresco)
    implementation(libs.imagepipeline.okhttp3)
    implementation(libs.fresco.processors)
    // Core library (required)
    implementation(libs.flexible.adapter)
    // UI extensions (recommended)
    implementation(libs.flexible.adapter.ui)
    implementation(libs.dagger)
    implementation(libs.dagger.android)
    implementation(libs.dagger.android.support)
    kapt(libs.dagger.compiler)
    kapt(libs.dagger.android.processor)
    implementation(libs.rxkotlin)
    implementation(libs.rxandroid)
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.0.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.apache.commons:commons-lang3:3.17.0")
    implementation("com.google.guava:guava:33.4.0-android")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation(libs.androidx.preference.ktx)
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.dexter)
    implementation(libs.flatbuffers.java)
    implementation(libs.eventbus)
    implementation(libs.material)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.play.services.cast)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.lifecycle.service)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
