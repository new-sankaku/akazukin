dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
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
