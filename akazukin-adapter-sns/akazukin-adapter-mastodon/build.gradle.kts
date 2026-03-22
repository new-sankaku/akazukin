dependencies {
    api(project(":akazukin-adapter-sns:akazukin-adapter-core"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.2")
    testImplementation("org.wiremock:wiremock-standalone:3.4.2")
}
