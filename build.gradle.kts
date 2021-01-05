plugins {
    kotlin("jvm") version "1.4.0"
    kotlin("plugin.serialization") version "1.4.0" apply false
}

group = "com.tfowl.woolies"
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.0-RC")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.6.0")

    implementation("com.google.oauth-client:google-oauth-client-jetty:1.23.0") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }
    implementation("com.google.apis:google-api-services-calendar:v3-rev305-1.23.0") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }

    implementation(project(":google-api-okhttp-transport"))

    implementation("com.github.spullara.mustache.java:compiler:0.9.6")

    implementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.13.3")
}