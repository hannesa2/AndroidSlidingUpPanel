buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradle)
        classpath(libs.kotlin.gradle.plugin)
    }
}

println("I use Java ${JavaVersion.current()}")

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
