import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

group = "com.tfowl.google-apis"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.freeCompilerArgs = listOf("-Xinline-classes", "-Xopt-in=kotlin.RequiresOptIn")
}

dependencies {
    implementation(kotlin("stdlib"))

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC")

    api("com.google.oauth-client:google-oauth-client-jetty:1.23.0") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }
    api("com.google.apis:google-api-services-calendar:v3-rev305-1.23.0") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }

    implementation(project(":google-api-okhttp-transport"))
}