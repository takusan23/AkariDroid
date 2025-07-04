plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    alias(libs.plugins.compose.compiler)
    kotlin("plugin.serialization").version(libs.versions.kotlin.get())
}

android {
    namespace = "io.github.takusan23.akaridroid"
    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.takusan23.akaridroid"
        minSdk = 21
        targetSdk = 36
        versionCode = 12
        versionName = "5.0.0"

        // アプリのビルド時間をアプリ側で取得できるように
        resValue("string", "build_date", System.currentTimeMillis().toString())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    // Strong Skipping Mode を有効
    // React のメモ化を自動でやってくれるやつみたいな
    // タイムラインの操作がちょっとだけ軽くなるかも
    composeCompiler {
        enableStrongSkippingMode = true
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // エンコーダー
    implementation(project(":akari-core"))

    // Gradle Version Catalog でライブラリのバージョンを一元管理しています。
    // libs.versions.toml ファイルを参照してください

    implementation(libs.kotlinx.coroutine)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.core)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.glide)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
