package com.github.devlaq.arkitect.util

import arc.util.Log
import arc.util.Log.LogLevel
import arc.util.Log.err
import com.github.devlaq.arkitect.i18n.I18N

fun getArkitectLogger(): Logger {
    return Logger("Arkitect")
}

class Logger(val name: String) {

    fun log(level: LogLevel, msg: String) {
        Log.log(level, "[$name] $msg")
    }

    fun info(msg: String) {
        Log.info("[$name] $msg")
    }

    fun warn(msg: String) {
        Log.warn("[$name] $msg")
    }

    fun error(msg: String) {
        Log.err("[$name] $msg")
    }

    fun infoT(key: String, vararg args: Any?) {
        info(I18N.translate(key, *args))
    }

    fun warnT(key: String, vararg args: Any?) {
        warn(I18N.translate(key, *args))
    }

    fun errorT(key: String, vararg args: Any?) {
        err(I18N.translate(key, *args))
    }

}