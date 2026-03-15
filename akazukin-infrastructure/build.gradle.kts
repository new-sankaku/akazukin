plugins {
    id("io.quarkus")
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    api(project(":akazukin-domain"))

    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("io.quarkus:quarkus-hibernate-orm-panache")
    implementation("io.quarkus:quarkus-jdbc-postgresql")
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-smallrye-fault-tolerance")
    implementation("io.quarkiverse.amazonservices:quarkus-amazon-sqs:3.15.0")
    implementation("software.amazon.awssdk:url-connection-client:2.42.13")
    implementation("software.amazon.awssdk:scheduler:2.42.13")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")
    implementation("io.quarkus:quarkus-redis-client")
    implementation("io.quarkus:quarkus-cache")
    implementation("io.quarkus:quarkus-mailer")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")

    testImplementation("io.quarkus:quarkus-junit5")
}
