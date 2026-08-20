plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.kingkharnivore.skillz"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.kingkharnivore.skillz"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "1.2.0"

        buildConfigField("boolean", "SHOW_SCORE", "true")
        buildConfigField("int", "PRIMARY_COLOR", "0xFF2F4F6F") // GryffindorRed

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        compose = true
        buildConfig = true
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    flavorDimensions += "mode"

    productFlavors {
        create("aera") {
            dimension = "mode"
            applicationIdSuffix = ".aera"
            versionNameSuffix = "-aera"
            resValue("string", "app_name", "Aera")
            buildConfigField("boolean", "SHOW_SCORE", "false")
            buildConfigField("int", "PRIMARY_COLOR", "0xFF3F8F8B") // RavenclawBlue
        }

        create("scyra") {
            dimension = "mode"
            applicationIdSuffix = ".scyra"
            versionNameSuffix = "-scyra"
            resValue("string", "app_name", "Scyra")
            buildConfigField("boolean", "SHOW_SCORE", "true")
            buildConfigField("int", "PRIMARY_COLOR", "0xFF2F4F6F") // GryffindorRed
        }
    }

}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

hilt {
    enableAggregatingTask = false
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.animation:animation")

    implementation(libs.androidx.navigation.compose)
    implementation("androidx.lifecycle:lifecycle-service:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")

    implementation(libs.hilt.android)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.unit)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.appcompat)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.health.connect.client)

    implementation(libs.androidx.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.datetime)
    implementation("com.mohamedrejeb.richeditor:richeditor-compose:1.0.0-rc13")

    implementation(libs.compose)
    implementation(libs.compose.m3)

    implementation("androidx.core:core-splashscreen:1.0.1")

    // Chronicle keeps a durable private copy; confirmed captures are also published to MediaStore.
    val cameraXVersion = "1.6.1"
    implementation("androidx.camera:camera-core:$cameraXVersion")
    implementation("androidx.camera:camera-camera2:$cameraXVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraXVersion")
    implementation("androidx.camera:camera-video:$cameraXVersion")
    implementation("androidx.camera:camera-view:$cameraXVersion")
    implementation("androidx.exifinterface:exifinterface:1.4.2")

    val media3Version = "1.10.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")

    // Kept behind Scyra-owned speech interfaces because this API is still alpha.
    implementation("com.google.mlkit:genai-speech-recognition:1.0.0-alpha1")

    // Room 2.8 migration bundles are generated against the 1.8 serialization ABI.
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.room:room-testing:2.8.4")
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
