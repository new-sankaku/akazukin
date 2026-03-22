dependencies {
    api(project(":akazukin-ai:akazukin-ai-core"))
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
    implementation("jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.1")
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
}
