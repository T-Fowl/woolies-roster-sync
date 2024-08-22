plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization) apply false
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
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.clikt)
    implementation(libs.bundles.kotlinresult)
    implementation(libs.ical4j)
    implementation(libs.playwright)
    implementation(libs.log4j.slf4j2impl)

    implementation(project(":gcal-sync-kt"))
    implementation(project(":workjam"))

    testImplementation(libs.bundles.testing)
}

tasks.withType<Test> {
    useJUnitPlatform()
}