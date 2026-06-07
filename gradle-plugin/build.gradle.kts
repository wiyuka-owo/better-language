plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-gradle-plugin`
    `maven-publish`
}

dependencies {
    val kgpApiJar = providers.gradleProperty("kgpApiJar").orNull
    if (kgpApiJar != null) {
        compileOnly(files(kgpApiJar))
        compileOnly("org.jetbrains.kotlin:kotlin-tooling-core:2.3.21")
    } else {
        compileOnly(libs.kotlin.gradle.plugin.api)
    }
    implementation(gradleApi())
}

gradlePlugin {
    plugins {
        create("betterLanguage") {
            id = "dev.betterlang"
            implementationClass = "dev.betterlang.gradle.BetterLanguageGradlePlugin"
        }
    }
}
