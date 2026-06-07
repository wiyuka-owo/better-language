package dev.betterlang.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class BetterLanguageGradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: Project) {
        val ext = target.extensions.create("betterLanguage", BetterLanguageExtension::class.java)

        val fragmentsRoot = target.layout.buildDirectory.dir("generated/betterlang/fragments")
        val outputDir = target.layout.buildDirectory.dir("generated/betterlang/resources")

        val generate = target.tasks.register("generateLang", GenerateLangTask::class.java) { task ->
            task.fragmentsDir.set(fragmentsRoot)
            task.outputDir.set(outputDir)
            task.format.set(target.provider { ext.format })
            task.langPath.set(target.provider { ext.langPath })
        }

        target.plugins.withId("org.jetbrains.kotlin.jvm") {
            val sourceSets = target.extensions.getByType(SourceSetContainer::class.java)
            sourceSets.getByName("main").resources.srcDir(outputDir)
            generate.configure { it.dependsOn("compileKotlin") }
            target.tasks.named("processResources").configure { it.dependsOn(generate) }
        }
    }

    override fun getCompilerPluginId(): String = "dev.betterlang"

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(groupId = "dev.betterlang", artifactId = "compiler-plugin", version = VERSION)

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> {
        val project = kotlinCompilation.target.project
        val dir = project.layout.buildDirectory
            .dir("generated/betterlang/fragments/${kotlinCompilation.name}")
            .get().asFile.absolutePath
        return project.provider {
            listOf(SubpluginOption(key = "fragmentsDir", value = dir))
        }
    }

    private companion object {
        const val VERSION = "0.1.0"
    }
}
