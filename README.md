# better-language

使用编译器插件将翻译写进代码里，并在编译器自动生成翻译文件

```kotlin
fun titleKey(): String = tr {
    zh_cn = "标题"
    en_us = "Title"
}
```

构建后:

- `titleKey()` 在运行时返回 `com.example.lang.title_key`(由包名 + 成员名推导)
- 自动生成 `assets/<modid>/lang/zh_cn.json`、`en_us.json` 等文件,内容为该键对应的各语言文本

---

## 快速开始

### 1. 配置仓库

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://raw.githubusercontent.com/wiyuka-owo/better-language/master/docs/maven") }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://raw.githubusercontent.com/wiyuka-owo/better-language/master/docs/maven") }
    }
}
```

### 2. 应用插件与依赖

`build.gradle.kts`:

```kotlin
plugins {
    kotlin("jvm") version "2.4.0"
    id("dev.betterlang") version "0.1.0"
}

dependencies {
    implementation("dev.betterlang:runtime:0.1.0")
}

betterLanguage {
    // 格式 FLAT_JSON | NESTED_JSON | FLAT_YAML | NESTED_YAML
    format = "FLAT_JSON"
    // 资源内路径前缀
    langPath = "assets/mymod/lang"
}
```

### 3. 代码实现

```kotlin
package com.example.lang

import dev.betterlang.tr

object Menu {
    fun titleKey(): String = tr {
        zh_cn = "标题"
        en_us = "Title"
    }
}
```

---

## DSL 用法

### 内置语言

```kotlin
val greeting: String = tr {
    zh_cn = "你好"
    en_us = "Hello"
}
```

### 任意语言:`locale(tag, text)`

```kotlin
fun titleKey(): String = tr {
    zh_cn = "标题"
    en_us = "Title"
    locale("ja_jp", "タイトル")
    locale("ko_kr", "제목")
}
```
翻译内容需为字面量

### 扩展属性

```kotlin
import dev.betterlang.TranslationBuilder

var TranslationBuilder.ko_kr: String
    get() = ""
    set(value) { locale("ko_kr", value) }

fun startKey(): String = tr {
    zh_cn = "开始游戏"
    en_us = "Start Game"
    ko_kr = "게임 시작"
}
```

### 占位符参数

```kotlin
val greeting: String = tr {
    zh_cn = "你好 %s"
    en_us = "Hello %s"
}

val score: String = tr {
    zh_cn = "玩家 %1\$s 得分 %2\$s"
    en_us = "Player %1\$s scored %2\$s"
}
```

---

## 翻译键生成规则

翻译键会根据 `tr { }` 所在的位置自动推导，统一转为下划线格式（snake_case）。基础规则是 `<包名>.<函数名或属性名>`。

如果在方法里写了多个翻译，插件会根据具体写法做区分：
- 如果赋值给了局部变量，键名会带上变量名。
- 如果没有赋值，且方法内有多个独立的 `tr { }`，会按顺序自动加上数字后缀（如 `.0`, `.1`）。如果整个方法里只有单个没有赋值的 `tr { }`，则不加。

```kotlin
// 1. 独立函数，直接用函数名 -> com.example.lang.title_key
fun titleKey() = tr { }

// 2. 顶层/类属性，直接用属性名 -> com.example.lang.greeting
val greeting = tr { }

fun openMenu() {
    val title = tr { zh_cn = "标题"; en_us = "Title" }
    val start = tr { zh_cn = "开始"; en_us = "Start" } // ...open_menu.start

    tr { zh_cn = "甲"; en_us = "A" } // ...open_menu.0
    tr { zh_cn = "乙"; en_us = "B" } // ...open_menu.1
}
```
