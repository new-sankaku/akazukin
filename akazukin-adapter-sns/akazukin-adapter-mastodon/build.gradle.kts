dependencies {
    api(project(":akazukin-adapter-sns:akazukin-adapter-core"))
    // BigBone Mastodon client - snapshot only, will add when stable release is available
    // To use snapshots, add Sonatype snapshots repository to settings.gradle.kts:
    //   maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    // implementation("io.github.pattafeufeu:bigbone:2.0.0-SNAPSHOT")
}
