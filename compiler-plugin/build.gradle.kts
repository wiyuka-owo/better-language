plugins {
    alias(libs.plugins.kotlin.jvm)
    `maven-publish`
}

dependencies {
    compileOnly(libs.kotlin.compiler.embeddable)
}

publishing {
    publications {
        create<MavenPublication>("maven") { from(components["java"]) }
    }
}

// The plugin uses experimental compiler APIs.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        freeCompilerArgs.add("-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
    }
}
