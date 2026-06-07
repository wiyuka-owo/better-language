package dev.betterlang.gradle

object FormatWriter {

    fun render(format: LangFormat, entries: Map<String, String>): String = when (format) {
        LangFormat.FLAT_JSON -> flatJson(entries)
        LangFormat.NESTED_JSON -> nestedJson(buildTree(entries))
        LangFormat.FLAT_YAML -> flatYaml(entries)
        LangFormat.NESTED_YAML -> nestedYaml(buildTree(entries))
    }

    private fun flatJson(entries: Map<String, String>): String {
        val sorted = entries.toSortedMap()
        return buildString {
            append("{\n")
            sorted.entries.forEachIndexed { i, (k, v) ->
                append("  ").append(jsonString(k)).append(": ").append(jsonString(v))
                if (i < sorted.size - 1) append(',')
                append('\n')
            }
            append("}\n")
        }
    }

    private fun nestedJson(node: Node, indent: Int = 1): String {
        val pad = "  ".repeat(indent)
        val closePad = "  ".repeat(indent - 1)
        val keys = node.children.keys.sorted()
        return buildString {
            append("{\n")
            keys.forEachIndexed { i, k ->
                val child = node.children.getValue(k)
                append(pad).append(jsonString(k)).append(": ")
                if (child.value != null) append(jsonString(child.value!!)) else append(nestedJson(child, indent + 1))
                if (i < keys.size - 1) append(',')
                append('\n')
            }
            append(closePad).append('}')
            if (indent == 1) append('\n')
        }
    }

    private fun jsonString(s: String): String = buildString {
        append('"')
        for (c in s) when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> if (c < ' ') append("\\u%04x".format(c.code)) else append(c)
        }
        append('"')
    }

    private fun flatYaml(entries: Map<String, String>): String {
        val sorted = entries.toSortedMap()
        return buildString {
            for ((k, v) in sorted) append(yamlKey(k)).append(": ").append(yamlString(v)).append('\n')
        }
    }

    private fun nestedYaml(node: Node, indent: Int = 0): String {
        val pad = "  ".repeat(indent)
        val keys = node.children.keys.sorted()
        return buildString {
            for (k in keys) {
                val child = node.children.getValue(k)
                append(pad).append(yamlKey(k)).append(':')
                if (child.value != null) append(' ').append(yamlString(child.value!!)).append('\n')
                else append('\n').append(nestedYaml(child, indent + 1))
            }
        }
    }

    private fun yamlKey(s: String): String =
        if (s.isNotEmpty() && s.all { it.isLetterOrDigit() || it == '_' || it == '-' }) s else yamlString(s)

    private fun yamlString(s: String): String = buildString {
        append('"')
        for (c in s) when (c) {
            '"' -> append("\\\"")
            '\\' -> append("\\\\")
            '\n' -> append("\\n")
            '\r' -> append("\\r")
            '\t' -> append("\\t")
            else -> append(c)
        }
        append('"')
    }

    private class Node {
        var value: String? = null
        val children = linkedMapOf<String, Node>()
    }

    private fun buildTree(entries: Map<String, String>): Node {
        val root = Node()
        for ((key, text) in entries) {
            val parts = key.split('.')
            var cur = root
            for ((idx, part) in parts.withIndex()) {
                if (idx == parts.lastIndex) cur.children.getOrPut(part) { Node() }.value = text
                else cur = cur.children.getOrPut(part) { Node() }
            }
        }
        return root
    }
}
