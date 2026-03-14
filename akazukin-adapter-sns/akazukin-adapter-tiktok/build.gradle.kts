dependencies {
    api(project(":akazukin-adapter-sns:akazukin-adapter-core"))
    implementation(project(":akazukin-sdk:akazukin-sdk-tiktok"))
    testImplementation("org.wiremock:wiremock-standalone:3.4.2")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
}
