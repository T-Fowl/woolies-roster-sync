dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.apache.pdfbox:pdfbox:2.0.21")
    implementation("com.jakewharton.picnic:picnic:0.5.0")
    implementation("org.joml:joml:1.9.25")

    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
