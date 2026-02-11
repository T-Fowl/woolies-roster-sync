import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
    idea
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
        targetCompatibility = JavaVersion.VERSION_21
        sourceCompatibility = JavaVersion.VERSION_21
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs = listOf("-Xinline-classes", "-Xopt-in=kotlin.RequiresOptIn")
        }
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.clikt)
    implementation(libs.bundles.kotlinresult)
    implementation(libs.bundles.ical4j)
    implementation(libs.playwright)

    runtimeOnly(libs.logback.core)
    runtimeOnly(libs.logback.classic)

    implementation(project(":gcal-sync-kt"))
    implementation(project(":workjam"))

    testImplementation(libs.bundles.testing)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}