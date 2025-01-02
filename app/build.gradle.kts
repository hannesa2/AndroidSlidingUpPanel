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
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation(project(":library"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.1.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.2.1")
    androidTestUtil("androidx.test.services:test-services:1.5.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
