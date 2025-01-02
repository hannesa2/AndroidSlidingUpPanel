buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    }
}

println("I use Java ${JavaVersion.current()}")

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
