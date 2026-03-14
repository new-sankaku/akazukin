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

    // Quarkus REST
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")

    // Quarkiverse Renarde (server-side rendered web framework)
    implementation("io.quarkiverse.renarde:quarkus-renarde:3.1.1")

    // Quarkiverse Web Bundler
    implementation("io.quarkiverse.web-bundler:quarkus-web-bundler:1.7.3")

    // htmx
    implementation("org.mvnpm:htmx.org:2.0.8")

    // Security — JWT
    implementation("io.quarkus:quarkus-smallrye-jwt")
    implementation("io.quarkus:quarkus-smallrye-jwt-build")

    // Validation
    implementation("io.quarkus:quarkus-hibernate-validator")

    // Rate limiting
    implementation("com.bucket4j:bucket4j-core:8.10.1")

    // AWS Lambda HTTP adapter
    implementation("io.quarkus:quarkus-amazon-lambda-http")

    // AWS services
    implementation("io.quarkiverse.amazonservices:quarkus-amazon-sqs:3.15.0")
    implementation("io.quarkiverse.amazonservices:quarkus-amazon-dynamodb:3.15.0")
    implementation("io.quarkiverse.amazonservices:quarkus-amazon-secretsmanager:3.15.0")

    // Test
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}
