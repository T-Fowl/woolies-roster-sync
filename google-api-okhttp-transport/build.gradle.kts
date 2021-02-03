group = "com.tfowl.google-apis"
version = "1.0-SNAPSHOT"

dependencies {
    implementation(kotlin("stdlib"))

    api("com.squareup.okhttp3:okhttp:3.14.9")

    implementation("com.google.api-client:google-api-client:1.23.0") {

        // Point of this module is to not have to pull in apache httpcomponents
        exclude(group = "org.apache.httpcomponents", module = "httpclient")

        // No reason to export this jackson in our api
        exclude(group = "com.fasterxml.jackson.core", module = "jackson-core")
    }
}