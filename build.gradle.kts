plugins {
    java
    id("org.owasp.dependencycheck") version "12.1.0"
}

subprojects {
    apply(plugin = "java-library")

    repositories {
        mavenCentral()
    }


    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
        options.isFork = true
        options.forkOptions.jvmArgs = listOf("-Duser.language=ja", "-Dfile.encoding=UTF-8", "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    dependencies {
        testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }
}

dependencyCheck {
    scanConfigurations = listOf("runtimeClasspath")
    failBuildOnCVSS = 9.0f
    formats = listOf("HTML", "JSON")
    outputDirectory = layout.buildDirectory.dir("reports").get().asFile.absolutePath
    nvd {
        apiKey = System.getenv("NVD_API_KEY") ?: ""
        delay = 2000
    }
}
