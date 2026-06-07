package dev.betterlang.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName
import java.io.File
import java.security.MessageDigest

class BetterLangIrGenerationExtension(
    private val fragmentsDir: File,
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        fragmentsDir.mkdirs()
        for (file in moduleFragment.files) {
            val collector = TrCollector(file.packageFqName.asString(), pluginContext)
            file.acceptChildrenVoid(collector)
            writeFragment(file, collector.entries)
        }
    }

    private fun writeFragment(file: IrFile, entries: List<Triple<String, String, String>>) {
        val fragment = fragmentFileFor(file)
        if (entries.isEmpty()) {
            fragment.delete()
            return
        }
        fragment.bufferedWriter(Charsets.UTF_8).use { out ->
            for ((locale, key, text) in entries) {
                out.append(locale).append('\t').append(key).append('\t').append(escape(text)).append('\n')
            }
        }
    }

    private fun fragmentFileFor(file: IrFile): File {
        val path = file.fileEntry.name
        val digest = MessageDigest.getInstance("SHA-1").digest(path.toByteArray())
        val hash = digest.joinToString("") { "%02x".format(it) }.take(16)
        return File(fragmentsDir, "${File(path).nameWithoutExtension}-$hash.tsv")
    }

    private fun escape(s: String): String = buildString {
        for (c in s) when (c) {
            '\\' -> append("\\\\"); '\n' -> append("\\n"); '\r' -> append("\\r"); '\t' -> append("\\t")
            else -> append(c)
        }
    }

    private class TrCollector(
        private val packageName: String,
        private val pluginContext: IrPluginContext,
    ) : IrVisitorVoid() {

        val entries = mutableListOf<Triple<String, String, String>>()
        private val memberStack = ArrayDeque<String>()

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitProperty(declaration: IrProperty) {
            memberStack.addLast(declaration.name.asString())
            super.visitProperty(declaration)
            memberStack.removeLast()
        }

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        override fun visitSimpleFunction(declaration: IrSimpleFunction) {
            if (declaration.correspondingPropertySymbol != null || declaration.name.isSpecial) {
                super.visitSimpleFunction(declaration)
                return
            }
            memberStack.addLast(declaration.name.asString())
            super.visitSimpleFunction(declaration)
            memberStack.removeLast()
        }

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        override fun visitCall(expression: IrCall) {
            val callee = expression.symbol.owner
            if (callee.hasAnnotation(MARKER_FQN)) {
                val member = memberStack.lastOrNull()
                if (member != null) {
                    val key = deriveKey(packageName, member)
                    expression.arguments
                        .filterIsInstance<IrFunctionExpression>()
                        .firstOrNull()
                        ?.function?.body
                        ?.let { collectSetters(it, key) }
                    injectKey(expression, callee, key)
                }
            }
            super.visitCall(expression)
        }

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        private fun collectSetters(body: IrBody, key: String) {
            body.acceptChildrenVoid(object : IrVisitorVoid() {
                override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

                override fun visitCall(expression: IrCall) {
                    val fn = expression.symbol.owner
                    val regularArgs = fn.parameters.zip(expression.arguments)
                        .filter { it.first.kind == IrParameterKind.Regular }
                        .map { it.second }

                    val prop = fn.correspondingPropertySymbol
                    if (prop != null && fn.name.asString().startsWith("<set-")) {
                        val text = (regularArgs.firstOrNull() as? IrConst)?.value as? String
                        if (!text.isNullOrEmpty()) {
                            entries.add(Triple(prop.owner.name.asString(), key, text))
                        }
                    } else if (fn.hasAnnotation(LOCALE_ENTRY_FQN)) {
                        val tag = (regularArgs.getOrNull(0) as? IrConst)?.value as? String
                        val text = (regularArgs.getOrNull(1) as? IrConst)?.value as? String
                        if (!tag.isNullOrEmpty() && !text.isNullOrEmpty()) {
                            entries.add(Triple(tag, key, text))
                        }
                    }
                    super.visitCall(expression)
                }
            })
        }

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        private fun injectKey(call: IrCall, callee: IrSimpleFunction, key: String) {
            val idx = callee.parameters.indexOfFirst {
                it.kind == IrParameterKind.Regular && it.name.asString() == "key"
            }
            if (idx < 0) return
            call.arguments[idx] = IrConstImpl.string(
                call.startOffset, call.endOffset, pluginContext.irBuiltIns.stringType, key,
            )
        }

        private fun deriveKey(pkg: String, member: String): String {
            val snake = buildString {
                member.forEachIndexed { i, c ->
                    if (c.isUpperCase()) {
                        if (i > 0 && member[i - 1] != '_') append('_')
                        append(c.lowercaseChar())
                    } else append(c)
                }
            }
            return if (pkg.isEmpty()) snake else "$pkg.$snake"
        }

        companion object {
            val MARKER_FQN = FqName("dev.betterlang.TranslationKeyMarker")
            val LOCALE_ENTRY_FQN = FqName("dev.betterlang.TranslationLocaleEntry")
        }
    }
}
