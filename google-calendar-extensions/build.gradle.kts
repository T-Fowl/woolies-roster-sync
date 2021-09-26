group = "com.tfowl.google-apis"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.1.0")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.1.0")

    api("com.google.api-client:google-api-client:1.31.3")
    api("com.google.oauth-client:google-oauth-client-jetty:1.31.4")
    api("com.google.apis:google-api-services-calendar:v3-rev20210215-1.31.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    testImplementation("io.mockk:mockk:1.10.6")
}

tasks.withType<Test> {
    useJUnitPlatform()
}