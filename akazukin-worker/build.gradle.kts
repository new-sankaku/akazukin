plugins {
    id("io.quarkus")
}

val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation(project(":akazukin-application"))
    implementation(project(":akazukin-infrastructure"))
    implementation("io.quarkus:quarkus-scheduler")
    implementation("io.quarkiverse.amazonservices:quarkus-amazon-sqs:3.15.0")
    implementation("software.amazon.awssdk:url-connection-client:2.25.27")
    testImplementation("io.quarkus:quarkus-junit5")
}
