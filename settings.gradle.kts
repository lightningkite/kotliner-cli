buildscript {
    val kotlinVersion: String by extra
    val dokkaVersion: String by extra
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
        // Add the Crashlytics Gradle plugin (be sure to add version
        // 2.0.0 or later if you built your app with Android Studio 4.1).
    }
}

rootProject.name = "kotliner-cli"

