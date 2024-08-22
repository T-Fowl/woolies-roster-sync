plugins {
    kotlin("jvm") version "2.0.0"
    kotlin("plugin.serialization") version "2.0.0" apply false
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
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    implementation("com.github.ajalt.clikt:clikt:4.4.0")

    implementation(project(":gcal-sync-kt"))

    implementation("com.michael-bull.kotlin-result:kotlin-result:2.0.0")
    implementation("com.michael-bull.kotlin-result:kotlin-result-coroutines:2.0.0")


    implementation("org.mnode.ical4j:ical4j:4.0.1")


    implementation(project(":workjam"))

    implementation("com.microsoft.playwright:playwright:1.46.0")

    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.23.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation("io.mockk:mockk:1.13.12")
}

tasks.withType<Test> {
    useJUnitPlatform()
}