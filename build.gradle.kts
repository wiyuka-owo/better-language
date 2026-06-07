plugins {
    alias(libs.plugins.kotlin.jvm) apply false
}

allprojects {
    group = "dev.betterlang"
    version = "0.1.0"
}

subprojects {
    // Publish all artifacts into an in-repo Maven layout (docs/maven) so the
    // project can be hosted on GitHub (Pages or raw URL) without Maven Central.
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
