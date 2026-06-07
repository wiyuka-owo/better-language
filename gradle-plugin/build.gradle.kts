import org.gradle.kotlin.dsl.internal.relocated.kotlin.metadata.internal.metadata.deserialization.VersionRequirementTable.Companion.create

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

val versionResourceDir = layout.buildDirectory.dir("generated/betterlang-version")

val generateVersionResource by tasks.registering {
    val outputDir = versionResourceDir
    val versionValue = project.version.toString()
    inputs.property("version", versionValue)
    outputs.dir(outputDir)
    doLast {
        val target = outputDir.get().file("dev/betterlang/gradle/version.properties").asFile
        target.parentFile.mkdirs()
        target.writeText("version=$versionValue\n")
    }
}

sourceSets.named("main").configure {
    resources.srcDir(generateVersionResource)
}
