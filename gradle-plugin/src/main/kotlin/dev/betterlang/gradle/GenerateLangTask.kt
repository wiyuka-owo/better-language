package dev.betterlang.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

@DisableCachingByDefault(because = "Trivial file merge; not worth caching")
open class GenerateLangTask : DefaultTask() {

    @get:Internal
    val fragmentsDir: DirectoryProperty = project.objects.directoryProperty()

    @get:OutputDirectory
    val outputDir: DirectoryProperty = project.objects.directoryProperty()

    @get:Input
    val format: Property<String> = project.objects.property(String::class.java)

    @get:Input
    val langPath: Property<String> = project.objects.property(String::class.java)

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val fragments: FileTree
        get() = fragmentsDir.asFileTree.matching { it.include("**/*.tsv") }

    @TaskAction
    fun generate() {
        val merged = sortedMapOf<String, LinkedHashMap<String, String>>() // locale -> (key -> text)
        fragments.files.forEach { file ->
            file.forEachLine { line ->
                if (line.isBlank()) return@forEachLine
                val parts = line.split('\t', limit = 3)
                if (parts.size < 3) return@forEachLine
                val (locale, key, escaped) = parts
                merged.getOrPut(locale) { linkedMapOf() }[key] = unescape(escaped)
            }
        }

        val fmt = LangFormat.from(format.get())
        val root = File(outputDir.get().asFile, langPath.get().trim('/'))
        if (root.exists()) root.deleteRecursively()
        root.mkdirs()
        merged.forEach { (locale, entries) ->
            File(root, "$locale.${fmt.extension}").writeText(FormatWriter.render(fmt, entries), Charsets.UTF_8)
        }
        logger.lifecycle("betterLanguage: generated ${merged.size} locale file(s) into $root")
    }

    private fun unescape(s: String): String = buildString {
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    'n' -> append('\n')
                    'r' -> append('\r')
                    't' -> append('\t')
                    '\\' -> append('\\')
                    else -> append(s[i + 1])
                }
                i += 2
            } else {
                append(c)
                i++
            }
        }
    }
}
