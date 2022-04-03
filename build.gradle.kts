plugins {
    kotlin("jvm") version "1.5.31"
    kotlin("plugin.serialization") version "1.5.31" apply false
    application
}

group = "com.tfowl.woolies"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.tfowl.woolies.sync.MainKt")
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
        kotlinOptions.jvmTarget = "16"
        kotlinOptions.freeCompilerArgs = listOf("-Xinline-classes", "-Xopt-in=kotlin.RequiresOptIn")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")

    implementation("com.github.ajalt.clikt:clikt:3.2.0")

    implementation("com.google.api-client:google-api-client:1.32.1")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.32.1")
    implementation("com.google.apis:google-api-services-calendar:v3-rev20210804-1.32.1")

    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.13")
    implementation("com.michael-bull.kotlin-result:kotlin-result-coroutines:1.1.13")

    implementation(project(":workjam"))

    implementation("com.github.spullara.mustache.java:compiler:0.9.10")

    implementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.14.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.0")
    testImplementation("io.mockk:mockk:1.12.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}