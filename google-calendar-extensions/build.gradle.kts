group = "com.tfowl.google-apis"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.0")

    api("com.google.api-client:google-api-client:1.32.1")
    api("com.google.oauth-client:google-oauth-client-jetty:1.32.1")
    api("com.google.apis:google-api-services-calendar:v3-rev20210804-1.32.1")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.0")
    testImplementation("io.mockk:mockk:1.12.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}