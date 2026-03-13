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
    // TODO: Add SQS dependency with explicit version when implementing SqsPostPublisher
    // implementation("io.quarkiverse.amazonservices:quarkus-amazon-sqs:2.17.2")

    testImplementation("io.quarkus:quarkus-junit5")
}
