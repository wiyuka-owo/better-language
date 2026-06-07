package dev.betterlang.gradle

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import java.util.Properties

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
        SubpluginArtifact(groupId = "dev.betterlang", artifactId = "compiler-plugin", version = pluginVersion())

    private fun pluginVersion(): String {
        val stream = javaClass.getResourceAsStream("/dev/betterlang/gradle/version.properties")
            ?: error("betterlang version.properties not found on classpath")
        return stream.use {
            Properties().apply { load(it) }.getProperty("version")
                ?: error("version missing in version.properties")
        }
    }

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
}
