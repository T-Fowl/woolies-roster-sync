import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

group = "com.tfowl"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs = listOf("-Xinline-classes", "-Xopt-in=kotlin.RequiresOptIn")
}

dependencies {
    implementation(kotlin("stdlib"))

    api("com.squareup.okhttp3:okhttp:3.14.9")

    api("com.google.api-client:google-api-client:1.23.0") {

        // Point of this module is to not have to pull in apache httpcomponents
        exclude(group = "org.apache.httpcomponents", module = "httpclient")

        // No reason to export this jackson in our api
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-core")
    }
}