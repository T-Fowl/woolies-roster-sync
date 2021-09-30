dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.apache.pdfbox:pdfbox:2.0.24")
    implementation("com.jakewharton.picnic:picnic:0.5.0")
    implementation("org.joml:joml:1.10.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
