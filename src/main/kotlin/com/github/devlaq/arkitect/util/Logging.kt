package com.github.devlaq.arkitect.util

import arc.util.Log
import arc.util.Log.LogLevel
import com.github.devlaq.arkitect.i18n.I18N

fun getArkitectLogger(): Logger {
    return Logger("Arkitect")
}

class Logger(val name: String) {

    val colorMap = mapOf(
        "flush" to "\u001b[H\u001b[2J",
        "reset" to "\u001B[0;0m",
        "bold" to "\u001B[1m",
        "/bold" to "\u001B[22m",
        "italic" to "\u001B[3m",
        "/italic" to "\u001B[23m",
        "underline" to "\u001B[4m",
        "/underline" to "\u001B[24m",
        "reverse" to "\u001B[7m",
        "/reverse" to "\u001B[27m",

        "black" to "\u001B[30m",
        "red" to "\u001B[31m",
        "green" to "\u001B[32m",
        "yellow" to "\u001B[33m",
        "blue" to "\u001B[34m",
        "purple" to "\u001B[35m",
        "cyan" to "\u001B[36m",
        "lightBlack" to "\u001b[90m",
        "lightRed" to "\u001B[91m",
        "lightGreen" to "\u001B[92m",
        "lightYellow" to "\u001B[93m",
        "lightBlue" to "\u001B[94m",
        "lightMagenta" to "\u001B[95m",
        "lightCyan" to "\u001B[96m",
        "lightWhite" to "\u001b[97m",
        "white" to "\u001B[37m",

        "backDefault" to "\u001B[49m",
        "backRed" to "\u001B[41m",
        "backGreen" to "\u001B[42m",
        "backYellow" to "\u001B[43m",
        "backBlue" to "\u001B[44m"
    )

    fun colorize(text: String): String {
        var replaced = text.replace("[]", colorMap["reset"]!!)
        colorMap.forEach {
            replaced = replaced.replace("[${it.key}]", it.value, ignoreCase = true)
        }
        return replaced
    }

    fun log(level: LogLevel, styles: String, msg: Any?) {
        Log.log(level, "[$name] ${colorize(msg.toString().replace("[]", "[]${styles}"))}")
    }

    fun log(level: LogLevel, msg: Any?) {
        Log.log(level, "[$name] ${colorize(msg.toString())}")
    }

    fun debug(msg: String) {
        log(LogLevel.debug, msg)
    }

    fun info(msg: Any?) {
        log(LogLevel.info, msg)
    }

    fun warn(msg: Any?) {
        log(LogLevel.warn, styles = "[lightyellow][bold]", msg)
    }

    fun error(msg: Any?) {
        log(LogLevel.err, styles = "[lightred][bold]", msg)
    }

    fun infoT(key: String, vararg args: Any?) {
        info(I18N.translate(key, *args))
    }

    fun warnT(key: String, vararg args: Any?) {
        warn(I18N.translate(key, *args))
    }

    fun errorT(key: String, vararg args: Any?) {
        error(I18N.translate(key, *args))
    }

}
