dependencies {
    api(project(":akazukin-ai:akazukin-ai-core"))
    implementation(project(":akazukin-sdk:akazukin-sdk-ollama"))
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
    implementation("jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.1")
    implementation("org.eclipse.microprofile.config:microprofile-config-api:3.1")
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
}
