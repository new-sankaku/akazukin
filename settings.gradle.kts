pluginManagement {
    val quarkusPluginVersion: String by settings
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("io.quarkus") version quarkusPluginVersion
        id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention")
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
include("akazukin-sdk:akazukin-sdk-tiktok")

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
include("akazukin-adapter-sns:akazukin-adapter-tiktok")

// AI modules
include("akazukin-ai")
include("akazukin-ai:akazukin-ai-core")
include("akazukin-ai:akazukin-ai-ollama")
include("akazukin-ai:akazukin-ai-external")

// New SDK modules
include("akazukin-sdk:akazukin-sdk-ollama")
include("akazukin-sdk:akazukin-sdk-mixi2")
include("akazukin-sdk:akazukin-sdk-note")
include("akazukin-sdk:akazukin-sdk-niconico")

// New adapter modules
include("akazukin-adapter-sns:akazukin-adapter-mixi2")
include("akazukin-adapter-sns:akazukin-adapter-note")
include("akazukin-adapter-sns:akazukin-adapter-niconico")
include("akazukin-adapter-sns:akazukin-adapter-devkit")

// Worker module
include("akazukin-worker")

include("akazukin-web")
