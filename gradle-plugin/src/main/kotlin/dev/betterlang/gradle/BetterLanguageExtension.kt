package dev.betterlang.gradle

open class BetterLanguageExtension {
    /** FLAT_JSON, NESTED_JSON, FLAT_YAML, NESTED_YAML. */
    var format: String = "FLAT_JSON"

    /** Resource path prefix for the generated lang files. */
    var langPath: String = "lang"
}
