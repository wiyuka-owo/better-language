plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

publishing {
    publications {
        create<MavenPublication>("maven") { from(components["java"]) }
    }
}
