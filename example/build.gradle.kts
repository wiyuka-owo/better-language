plugins {
    kotlin("jvm") version "2.4.0"
    id("dev.betterlang") version "0.1.0"
    application
}

dependencies {
    implementation("dev.betterlang:runtime:0.1.0")
}

betterLanguage {
    // FLAT_JSON | NESTED_JSON | FLAT_YAML | NESTED_YAML
    format = "FLAT_JSON"
    // Minecraft layout: assets/<modid>/lang/<locale>.json
    langPath = "assets/example/lang"
}

application {
    mainClass.set("com.example.lang.MainKt")
}
