package dev.betterlang

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class TranslationKeyMarker

@DslMarker
annotation class TranslationDsl

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.BINARY)
annotation class TranslationLocaleEntry

@TranslationDsl
class TranslationBuilder {
    var zh_cn: String = ""
    var en_us: String = ""

    @TranslationLocaleEntry
    fun locale(tag: String, text: String) {
    }
}

@TranslationKeyMarker
fun tr(key: String = "", block: TranslationBuilder.() -> Unit): String {
    if (key.isEmpty()) TranslationBuilder().block()
    return key
}
