package dev.betterlang.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
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
            collector.finish()
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
        private val varStack = ArrayDeque<String>()
        private val records = mutableListOf<Record>()

        private class Record(
            val call: IrCall,
            val callee: IrSimpleFunction,
            val member: String,
            val varName: String?,
            val texts: List<Pair<String, String>>,
        )

        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitProperty(declaration: IrProperty) {
            memberStack.addLast(declaration.name.asString())
            super.visitProperty(declaration)
            memberStack.removeLast()
        }

        override fun visitVariable(declaration: IrVariable) {
            varStack.addLast(declaration.name.asString())
            super.visitVariable(declaration)
            varStack.removeLast()
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
                    val texts = expression.arguments
                        .filterIsInstance<IrFunctionExpression>()
                        .firstOrNull()
                        ?.function?.body
                        ?.let { collectSetters(it) }
                        ?: emptyList()
                    records.add(Record(expression, callee, member, varStack.lastOrNull(), texts))
                }
            }
            super.visitCall(expression)
        }

        fun finish() {
            val unnamedCount = HashMap<String, Int>()
            for (r in records) {
                if (r.varName == null) unnamedCount[r.member] = (unnamedCount[r.member] ?: 0) + 1
            }
            val unnamedIndex = HashMap<String, Int>()
            for (r in records) {
                val segments = when {
                    r.varName != null -> listOf(r.member, r.varName)
                    (unnamedCount[r.member] ?: 0) <= 1 -> listOf(r.member)
                    else -> {
                        val i = unnamedIndex.getOrElse(r.member) { 0 }
                        unnamedIndex[r.member] = i + 1
                        listOf(r.member, i.toString())
                    }
                }
                val key = deriveKey(packageName, segments)
                for ((locale, text) in r.texts) entries.add(Triple(locale, key, text))
                injectKey(r.call, r.callee, key)
            }
        }

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        private fun collectSetters(body: IrBody): List<Pair<String, String>> {
            val result = mutableListOf<Pair<String, String>>()
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
                            result.add(prop.owner.name.asString() to text)
                        }
                    } else if (fn.hasAnnotation(LOCALE_ENTRY_FQN)) {
                        val tag = (regularArgs.getOrNull(0) as? IrConst)?.value as? String
                        val text = (regularArgs.getOrNull(1) as? IrConst)?.value as? String
                        if (!tag.isNullOrEmpty() && !text.isNullOrEmpty()) {
                            result.add(tag to text)
                        }
                    }
                    super.visitCall(expression)
                }
            })
            return result
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

        private fun deriveKey(pkg: String, members: List<String>): String {
            val path = members.joinToString(".") { toSnake(it) }
            return if (pkg.isEmpty()) path else "$pkg.$path"
        }

        private fun toSnake(name: String): String = buildString {
            name.forEachIndexed { i, c ->
                if (c.isUpperCase()) {
                    if (i > 0 && name[i - 1] != '_') append('_')
                    append(c.lowercaseChar())
                } else append(c)
            }
        }

        companion object {
            val MARKER_FQN = FqName("dev.betterlang.TranslationKeyMarker")
            val LOCALE_ENTRY_FQN = FqName("dev.betterlang.TranslationLocaleEntry")
        }
    }
}
