dependencies {
    api(project(":akazukin-domain"))

    // CDI interfaces only (no Quarkus runtime)
    implementation("jakarta.inject:jakarta.inject-api:2.0.1")
    implementation("jakarta.enterprise:jakarta.enterprise.cdi-api:4.0.1")
    implementation("jakarta.transaction:jakarta.transaction-api:2.0.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.2")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}
