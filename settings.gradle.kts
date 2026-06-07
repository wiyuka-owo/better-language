rootProject.name = "better-language"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
    }
}

// `example` is a standalone consumer build (it applies the published plugin),
// so it is intentionally NOT included here. See example/settings.gradle.kts.
include("runtime", "compiler-plugin", "gradle-plugin")
