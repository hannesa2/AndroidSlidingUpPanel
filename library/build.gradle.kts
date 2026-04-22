plugins {
    id("com.android.library")
    id("maven-publish")
}

android {
    namespace = "com.sothree.slidinguppanel.library"
    defaultConfig {
        minSdk = 21
        compileSdk = 36

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        abortOnError = false
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            consumerProguardFile("proguard-sdk.pro")
        }
    }
    publishing {
        singleVariant("release") {}
    }
}

dependencies {
    implementation(libs.recyclerview)
    implementation(libs.core.ktx)
    implementation(libs.kotlin.stdlib.jdk7)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["release"])
                pom {
                    licenses {
                        license {
                            name = "Apache License Version 2.0"
                            url = "https://github.com/hannesa2/AndroidSlidingUpPanel/blob/master/LICENSE"
                        }
                    }
                }
            }
        }
    }
}
