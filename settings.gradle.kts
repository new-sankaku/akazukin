pluginManagement {
    val quarkusPluginVersion: String by settings
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("io.quarkus") version quarkusPluginVersion
    }
}

rootProject.name = "akazukin"

include("akazukin-domain")
include("akazukin-application")
include("akazukin-infrastructure")

include("akazukin-sdk")
include("akazukin-sdk:akazukin-sdk-twitter")
include("akazukin-sdk:akazukin-sdk-bluesky")
include("akazukin-sdk:akazukin-sdk-reddit")
include("akazukin-sdk:akazukin-sdk-pinterest")

include("akazukin-adapter-sns")
include("akazukin-adapter-sns:akazukin-adapter-core")
include("akazukin-adapter-sns:akazukin-adapter-twitter")
include("akazukin-adapter-sns:akazukin-adapter-bluesky")
include("akazukin-adapter-sns:akazukin-adapter-mastodon")
include("akazukin-adapter-sns:akazukin-adapter-threads")
include("akazukin-adapter-sns:akazukin-adapter-instagram")
include("akazukin-adapter-sns:akazukin-adapter-reddit")
include("akazukin-adapter-sns:akazukin-adapter-telegram")
include("akazukin-adapter-sns:akazukin-adapter-vk")
include("akazukin-adapter-sns:akazukin-adapter-pinterest")

include("akazukin-web")
