plugins {
    kotlin("jvm") version "1.9.0"
    kotlin("plugin.serialization") version "1.9.0" apply false
    application
}

group = "com.tfowl.woolies"
version = "1.0-SNAPSHOT"

application {
    mainClass.set("com.tfowl.woolies.sync.MainKt")

    findProperty("playwright.nodejs.path")?.let { path ->
        applicationDefaultJvmArgs = listOf("-Dplaywright.nodejs.path=$path")
    }

}
allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")

    repositories {
        mavenCentral()
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_20
        targetCompatibility = JavaVersion.VERSION_20
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
        kotlinOptions.jvmTarget = "20"
        kotlinOptions.freeCompilerArgs = listOf("-Xinline-classes", "-Xopt-in=kotlin.RequiresOptIn")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    implementation("com.github.ajalt.clikt:clikt:3.4.0")

    implementation(project(":gcal-sync-kt"))

    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.14")
    implementation("com.michael-bull.kotlin-result:kotlin-result-coroutines:1.1.14")

    implementation(project(":workjam"))

    implementation("com.microsoft.playwright:playwright:1.44.0")

    implementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.17.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("io.mockk:mockk:1.12.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}