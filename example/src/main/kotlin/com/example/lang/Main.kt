package com.example.lang

import dev.betterlang.TranslationBuilder
import dev.betterlang.tr

object Menu {

    fun titleKey(): String = tr {
        zh_cn = "标题"
        en_us = "Title"
        locale("ja_jp", "タイトル")
        locale("ko_kr", "제목")
    }

    fun startKey(): String = tr {
        zh_cn = "开始游戏"
        en_us = "Start Game"
        ko_kr = "게임 시작"
    }
}

val greeting: String = tr {
    zh_cn = "你好 %s"
    en_us = "Hello %s"
}

val score: String = tr {
    zh_cn = "玩家 %1\$s 得分 %2\$s"
    en_us = "Player %1\$s scored %2\$s"
}

var TranslationBuilder.ko_kr: String
    get() = ""
    set(value) { locale("ko_kr", value) }

fun main() {
    println("Keys resolved at runtime:")
    println("  " + Menu.titleKey())
    println("  " + Menu.startKey())
    println("  " + greeting)
}
