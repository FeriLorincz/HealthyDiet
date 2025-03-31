plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
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

        // Adăugăm Room schema location
        kapt {
            arguments {
                arg("room.schemaLocation", "$projectDir/schemas")
                arg("room.incremental", "true")
                arg("room.expandProjection", "true")
            }
        }
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
                "META-INF/AL2.0",
                "META-INF/LGPL2.1",
                "META-INF/{AL2.0,LGPL2.1}",
                "META-INF/INDEX.LIST"
            )
            // Exclude fișierele .kotlin_module
            pickFirsts += listOf(
                "META-INF/*.kotlin_module"
            )
        }
    }

//    configurations.all {
//        resolutionStrategy {
//            force("androidx.databinding:databinding-common:8.9.0")
//            force("androidx.databinding:databinding-runtime:8.9.0")
//            force("androidx.databinding:databinding-adapters:8.9.0")
//            force("org.jetbrains:annotations:23.0.0")
//            exclude(group = "androidx.databinding", module = "library")
//            exclude(group = "androidx.databinding", module = "adapters")
//            exclude(group = "androidx.databinding", module = "baseLibrary")
//            exclude(group = "com.intellij", module = "annotations")
//        }
//    }


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
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.ui:ui-graphics:1.5.4")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.5.4")
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.4")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.5.4")

    // For MVVM architecture
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // For Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

    // For Image Processing and OCR
    implementation("com.google.mlkit:text-recognition:16.0.0")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")

    // For Voice Assistant
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.cloud:google-cloud-texttospeech:2.4.0")
    implementation("com.google.cloud:google-cloud-speech:2.5.1")

    // For Dependency Injection
    implementation("io.insert-koin:koin-android:3.5.0")

    // MaterialDesign
    implementation("com.google.android.material:material:1.11.0")
}