dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.apache.pdfbox:pdfbox:2.0.25")
    implementation("com.jakewharton.picnic:picnic:0.5.0")
    implementation("org.joml:joml:1.10.4")
    implementation("org.joml:joml-primitives:1.10.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
