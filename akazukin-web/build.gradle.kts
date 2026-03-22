plugins {
    id("io.quarkus")
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))

    // Project dependencies
    implementation(project(":akazukin-application"))
    implementation(project(":akazukin-infrastructure"))
    implementation(project(":akazukin-worker"))
    implementation(project(":akazukin-adapter-sns:akazukin-adapter-core"))
    implementation(project(":akazukin-adapter-sns:akazukin-adapter-twitter"))
    implementation(project(":akazukin-adapter-sns:akazukin-adapter-bluesky"))
    implementation(project(":akazukin-adapter-sns:akazukin-adapter-mastodon"))
    implementation(project(":akazukin-adapter-sns:akazukin-adapter-threads"))
    implementation(project(":akazukin-adapter-sns:akazukin-adapter-instagram"))
    implementation(project(":akazukin-adapter-sns:akazukin-adapter-reddit"))
    implementation(project(":akazukin-adapter-sns:akazukin-adapter-telegram"))
    implementation(project(":akazukin-adapter-sns:akazukin-adapter-vk"))
    implementation(project(":akazukin-adapter-sns:akazukin-adapter-pinterest"))
    implementation(project(":akazukin-adapter-sns:akazukin-adapter-tiktok"))

    // AI modules
    implementation(project(":akazukin-ai:akazukin-ai-core"))
    implementation(project(":akazukin-ai:akazukin-ai-ollama"))

    // New SNS adapters
    implementation(project(":akazukin-adapter-sns:akazukin-adapter-mixi2"))
    implementation(project(":akazukin-adapter-sns:akazukin-adapter-note"))
    implementation(project(":akazukin-adapter-sns:akazukin-adapter-niconico"))

    // Quarkus REST
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")

    // Quarkiverse Renarde (server-side rendered web framework)
    implementation("io.quarkiverse.renarde:quarkus-renarde:3.1.1")

    // Quarkiverse Web Bundler
    implementation("io.quarkiverse.web-bundler:quarkus-web-bundler:1.7.3")

    // htmx (compileOnly for Web Bundler)
    compileOnly("org.mvnpm:htmx.org:2.0.8")

    // Security — JWT
    implementation("io.quarkus:quarkus-smallrye-jwt")
    implementation("io.quarkus:quarkus-smallrye-jwt-build")

    // Security — BCrypt
    implementation("at.favre.lib:bcrypt:0.10.2")

    // Validation
    implementation("io.quarkus:quarkus-hibernate-validator")

    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-smallrye-fault-tolerance")
    implementation("io.quarkus:quarkus-scheduler")
    implementation("com.bucket4j:bucket4j-core:8.10.1")

    // AWS Lambda HTTP adapter (excluded in dev/test mode - conflicts with standard HTTP server)
    if (!gradle.startParameter.taskNames.any { it.contains("quarkusDev") || it.contains("test") || it.contains("Test") }) {
        implementation("io.quarkus:quarkus-amazon-lambda-http")
    }

    // Observability
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-opentelemetry")
    implementation("io.quarkus:quarkus-logging-json")

    // AWS services
    implementation("io.quarkiverse.amazonservices:quarkus-amazon-sqs:3.15.0")
    implementation("io.quarkiverse.amazonservices:quarkus-amazon-dynamodb:3.15.0")
    implementation("io.quarkiverse.amazonservices:quarkus-amazon-secretsmanager:3.15.0")
    implementation("software.amazon.awssdk:url-connection-client:2.42.13")
    implementation("software.amazon.awssdk:scheduler:2.42.13")

    // H2 for dev mode
    implementation("io.quarkus:quarkus-jdbc-h2")

    // Test
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-test-security")
    testImplementation("io.rest-assured:rest-assured")
}
