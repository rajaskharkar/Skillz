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
        versionCode = 1
        versionName = "1.0"

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

hilt {
    enableAggregatingTask = false
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

    implementation(libs.hilt.android)
    implementation(libs.androidx.compose.animation.core)
    implementation(libs.androidx.compose.foundation)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.datetime)

    implementation(libs.compose)
    implementation(libs.compose.m3)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}