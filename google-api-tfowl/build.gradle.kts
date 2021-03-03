group = "com.tfowl.google-apis"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
    api("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.1")
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")

    api("com.google.oauth-client:google-oauth-client-jetty:1.23.0") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }
    api("com.google.apis:google-api-services-calendar:v3-rev305-1.23.0") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }

    implementation(project(":google-api-okhttp-transport"))
}