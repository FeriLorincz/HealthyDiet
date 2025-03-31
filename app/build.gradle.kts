plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
}

configurations.all {
    resolutionStrategy {
        force("androidx.databinding:databinding-common:8.9.0")
        force("androidx.databinding:databinding-runtime:8.9.0")
        force("androidx.databinding:databinding-adapters:8.9.0")
        force("org.jetbrains:annotations:23.0.0")
        exclude(group = "androidx.databinding", module = "library")
        exclude(group = "androidx.databinding", module = "adapters")
        exclude(group = "androidx.databinding", module = "baseLibrary")
        exclude(group = "com.intellij", module = "annotations")
    }
}

android {
    namespace = "com.feri.healthydiet"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.feri.healthydiet"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Adaugă aceste linii
        val properties = org.jetbrains.kotlin.konan.properties.Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
            val apiKey = properties.getProperty("anthropic.api.key", "")
            buildConfigField("String", "ANTHROPIC_API_KEY", "\"$apiKey\"")
        } else {
            buildConfigField("String", "ANTHROPIC_API_KEY", "\"\"")
        }

        packaging {
            resources {
                excludes += listOf(
                    "META-INF/DEPENDENCIES",
                    "META-INF/LICENSE",
                    "META-INF/LICENSE.txt",
                    "META-INF/license.txt",
                    "META-INF/NOTICE",
                    "META-INF/NOTICE.txt",
                    "META-INF/notice.txt",
                    "META-INF/ASL2.0",
                    "META-INF/{AL2.0,LGPL2.1}",
                    "META-INF/INDEX.LIST"
                )
            }
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.6"
    }
}

dependencies {

    //implementation(libs.androidx.core.ktx)
    implementation("androidx.core:core-ktx:1.12.0")
    implementation(libs.androidx.lifecycle.runtime.ktx)
    //implementation(libs.androidx.activity.compose)
    implementation("androidx.activity:activity-compose:1.8.2")
    //implementation(platform(libs.androidx.compose.bom))
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    //implementation(libs.androidx.ui)
    implementation("androidx.compose.ui:ui")
    //implementation(libs.androidx.ui.graphics)
    implementation("androidx.compose.ui:ui-graphics")
    implementation(libs.androidx.ui.tooling.preview)
    //implementation(libs.androidx.material3)
    implementation("androidx.compose.material3:material3")
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.androidx.adapters)
    implementation(libs.androidx.databinding.adapters)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    //androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // For MVVM architecture
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.5.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.5.1")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.5.4")

    // Room
    implementation("androidx.room:room-runtime:2.4.3")
    implementation("androidx.room:room-ktx:2.4.3")
    implementation("androidx.room:room-compiler:2.4.3")

// For Networking
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.okhttp3:logging-interceptor:4.9.3")

// For Image Processing and OCR
    implementation ("com.google.mlkit:text-recognition:16.0.0")
    implementation ("androidx.camera:camera-camera2:1.2.0")
    implementation ("androidx.camera:camera-lifecycle:1.2.0")
    implementation ("androidx.camera:camera-view:1.2.0")

// For Voice Assistant
    implementation ("com.google.android.gms:play-services-auth:20.3.0")
    implementation ("com.google.cloud:google-cloud-texttospeech:2.4.0")
    implementation ("com.google.cloud:google-cloud-speech:2.5.1")

// For Dependency Injection
    implementation ("io.insert-koin:koin-android:3.2.2")

    implementation(libs.androidx.databinding.adapters) {
        exclude(group = "androidx.databinding", module = "library")
        exclude(group = "androidx.databinding", module = "baseLibrary")
    }



    //MaterialDesign
    implementation("com.google.android.material:material:1.10.0")
}