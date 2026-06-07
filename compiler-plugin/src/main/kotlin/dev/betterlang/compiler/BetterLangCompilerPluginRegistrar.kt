package dev.betterlang.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.CompilerConfiguration
import java.io.File

@OptIn(ExperimentalCompilerApi::class)
class BetterLangCompilerPluginRegistrar : CompilerPluginRegistrar() {

    override val pluginId: String = BetterLangCommandLineProcessor.PLUGIN_ID

    override val supportsK2: Boolean = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        val fragmentsDir = configuration.get(BetterLangCommandLineProcessor.KEY_FRAGMENTS_DIR) ?: return
        IrGenerationExtension.registerExtension(
            BetterLangIrGenerationExtension(File(fragmentsDir)),
        )
    }
}
