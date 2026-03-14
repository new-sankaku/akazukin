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
    implementation("io.quarkiverse.amazonservices:quarkus-amazon-sqs:3.15.0")
    implementation("software.amazon.awssdk:scheduler:2.25.27")

    implementation("io.quarkus:quarkus-redis-client")
    implementation("io.quarkus:quarkus-cache")

    testImplementation("io.quarkus:quarkus-junit5")
}
