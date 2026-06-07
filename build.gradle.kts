plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

allprojects {
    group = "dev.betterlang"
    version = (findProperty("betterlangVersion") as String?)
        ?: System.getenv("RELEASE_VERSION")
        ?: "0.1.0"
}

subprojects {
    plugins.withId("maven-publish") {
        configure<org.gradle.api.publish.PublishingExtension> {
            repositories {
                maven {
                    name = "githubPages"
                    url = uri(rootProject.layout.projectDirectory.dir("docs/maven"))
                }
            }
        }
    }
}
