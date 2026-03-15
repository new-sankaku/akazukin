dependencies {
    api(project(":akazukin-adapter-sns:akazukin-adapter-core"))
    implementation(project(":akazukin-sdk:akazukin-sdk-bluesky"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
}
