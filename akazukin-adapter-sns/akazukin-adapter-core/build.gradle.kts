val quarkusPlatformGroupId: String by project
val quarkusPlatformArtifactId: String by project
val quarkusPlatformVersion: String by project

dependencies {
    api(project(":akazukin-domain"))

    implementation(enforcedPlatform("${quarkusPlatformGroupId}:${quarkusPlatformArtifactId}:${quarkusPlatformVersion}"))
    implementation("org.eclipse.microprofile.fault-tolerance:microprofile-fault-tolerance-api")
    implementation("io.smallrye:smallrye-fault-tolerance-api")
}
