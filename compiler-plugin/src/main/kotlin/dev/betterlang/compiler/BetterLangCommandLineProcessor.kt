package dev.betterlang.compiler

import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey

@OptIn(ExperimentalCompilerApi::class)
class BetterLangCommandLineProcessor : CommandLineProcessor {

    override val pluginId: String = PLUGIN_ID

    override val pluginOptions: Collection<CliOption> = listOf(
        CliOption(
            optionName = OPTION_FRAGMENTS_DIR,
            valueDescription = "<path>",
            description = "Directory where per-source-file translation fragments are written",
            required = true,
        ),
    )

    override fun processOption(
        option: AbstractCliOption,
        value: String,
        configuration: CompilerConfiguration,
    ) {
        when (option.optionName) {
            OPTION_FRAGMENTS_DIR -> configuration.put(KEY_FRAGMENTS_DIR, value)
            else -> error("Unexpected betterlang plugin option: ${option.optionName}")
        }
    }

    companion object {
        const val PLUGIN_ID = "dev.betterlang"
        const val OPTION_FRAGMENTS_DIR = "fragmentsDir"
        val KEY_FRAGMENTS_DIR = CompilerConfigurationKey<String>(OPTION_FRAGMENTS_DIR)
    }
}
