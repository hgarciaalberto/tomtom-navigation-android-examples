/*
 * © 2023 TomTom NV. All rights reserved.
 *
 * This software is the proprietary copyright of TomTom NV and its subsidiaries and may be
 * used for internal evaluation purposes or commercial use strictly subject to separate
 * license agreement between you and TomTom NV. If you are the licensee, you are only permitted
 * to use this software in accordance with the terms of your license agreement. If you are
 * not the licensee, you are not authorized to use this software in any manner and should
 * immediately return or destroy it.
 */

plugins {
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.androidApplication)
}

android {
    namespace = "com.example.usecase"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "TOMTOM_API_KEY", "\"${extra["tomtomApiKey"].toString()}\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
//        allWarningsAsErrors = true
        jvmTarget = "1.8"
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
        compose = true
    }
}

dependencies {
    // TomTom SDK dependencies.
    implementation(libs.locationProvider)
    implementation(libs.locationSimulation)
    implementation(libs.locationMapmatched)
    implementation(libs.mapsDisplay)
    implementation(libs.navigationOnline)
    implementation(libs.navigationUi)
    implementation(libs.routePlannerOnline)
    implementation(libs.routeReplannerOnline)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose.android)

    // Accompanist
    implementation(libs.accompanist.permissions)

    // Flexible Bottom Sheet
    implementation(libs.flexible.bottomsheet.material3)

    // Android dependencies.
    implementation(libs.bundles.androidCommon)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.androidCompose)

    // Timber
    implementation(libs.timber)
}
