package dev.betterlang.gradle

enum class LangFormat(val extension: String) {
    FLAT_JSON("json"),
    NESTED_JSON("json"),
    FLAT_YAML("yml"),
    NESTED_YAML("yml");

    companion object {
        fun from(value: String?): LangFormat =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: FLAT_JSON
    }
}
