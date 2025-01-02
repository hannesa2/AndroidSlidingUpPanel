plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "info.hannes.slidinguppanel.demo"
    buildFeatures {
        viewBinding = true
    }
    defaultConfig {
        minSdk = 21
        compileSdk = 35
        targetSdk = 35

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments.putAll(
            mapOf(
                "clearPackageData" to "false",
                "disableAnalytics" to "true",
                "useTestStorageService" to "true",
            ),
        )
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    lint {
        abortOnError = false
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(project(":library"))
    implementation(libs.core.ktx)
    implementation(libs.kotlin.stdlib.jdk7)

    testImplementation(libs.junit)
    androidTestImplementation(libs.junit.ktx)
    androidTestUtil(libs.test.services)
    androidTestImplementation(libs.espresso.core)
}
