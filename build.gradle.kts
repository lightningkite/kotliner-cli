import com.lightningkite.deployhelpers.*

buildscript {
    repositories {
        maven(url="https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
    dependencies {
        classpath("com.lightningkite:deploy-helpers:master-SNAPSHOT")
    }
}

plugins {
    kotlin("jvm")
    id("org.jetbrains.dokka")
    id("signing")
    `maven-publish`
}

group = "com.lightningkite"

repositories {
    mavenCentral()
}

val kotlinVersion: String by project
dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
}

kotlin {
    explicitApi = org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode.Strict
}

standardPublishing {
    name.set("Kotliner CLI")
    description.set("An easy way to set up a CLI")
    github("lightningkite", "kotliner-cli")

    licenses {
        mit()
    }

    developers {
        developer(
            id = "LightningKiteJoseph",
            name = "Joseph Ivie",
            email = "joseph@lightningkite.com",
        )
        developer(
            id = "bjsvedin",
            name = "Brady Svedin",
            email = "brady@lightningkite.com",
        )
    }
}
