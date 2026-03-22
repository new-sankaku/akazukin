dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.wiremock:wiremock-standalone:3.4.2")
}

tasks.withType<Test> {
    val includeRealApi = System.getProperty("includeTags")
    useJUnitPlatform {
        if (includeRealApi == "real-api") {
            includeTags("real-api")
        } else {
            excludeTags("real-api")
        }
    }
}
