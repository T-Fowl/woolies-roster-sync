plugins {
    kotlin("jvm") version "1.6.20"
    kotlin("plugin.serialization") version "1.6.20" apply false
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
        kotlinOptions.jvmTarget = "17"
        kotlinOptions.freeCompilerArgs = listOf("-Xinline-classes", "-Xopt-in=kotlin.RequiresOptIn")
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.2")

    implementation("com.github.ajalt.clikt:clikt:3.4.0")

    implementation("com.google.api-client:google-api-client:1.33.4")
    implementation("com.google.oauth-client:google-oauth-client-jetty:1.33.1")
    implementation("com.google.apis:google-api-services-calendar:v3-rev20220203-1.32.1")

    implementation("com.michael-bull.kotlin-result:kotlin-result:1.1.14")
    implementation("com.michael-bull.kotlin-result:kotlin-result-coroutines:1.1.14")

    implementation(project(":workjam"))

    implementation("org.apache.logging.log4j:log4j-slf4j18-impl:2.17.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
    testImplementation("io.mockk:mockk:1.12.3")
}

tasks.withType<Test> {
    useJUnitPlatform()
}