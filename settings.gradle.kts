buildscript {
    val kotlinVersion: String by extra
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:1.6.10")
        classpath("com.lightningkite:deploy-helpers:0.0.4")
        // Add the Crashlytics Gradle plugin (be sure to add version
        // 2.0.0 or later if you built your app with Android Studio 4.1).
    }
}

rootProject.name = "kotliner-cli"

