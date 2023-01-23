package com.github.devlaq.arkitect.util.console

import com.github.devlaq.arkitect.i18n.BundleManager
import com.github.devlaq.arkitect.util.resources.getResourceAsText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Logging {

    lateinit var styles: Map<String, String>

    fun init() {
        loadStyles()
    }

    fun loadStyles() {
        val text = javaClass.getResourceAsText("/styles/markup.json") ?: return
        styles = Json.decodeFromString(text)
    }

    fun useStyle(text: String, affixes: Pair<String, String> = "<" to ">"): String {
        if (!::styles.isInitialized) return text
        var _text = text
        styles.forEach {
            _text = _text.replace("${affixes.first}${it.key}${affixes.second}", it.value, ignoreCase = true)
        }
        return _text
    }

    fun removeStyle(text: String): String {
        if(!::styles.isInitialized) return text
        var _text = text
        styles.forEach {
            _text = _text.replace("<${it.key}>", "", ignoreCase = true)
        }
        return _text
    }

    fun useTranslation(bundleManager: BundleManager, text: String, vararg args: Any): String {
        val (prefix, suffix) = "<%" to "%>"

        val translated = text.split(prefix)[0] + text.split(prefix).drop(1).map {
            val key = it.substringBefore(suffix)
            if(key == it) return@map it
            bundleManager.translate(key) + it.substringAfter(suffix)
        }.joinToString("")

        return bundleManager.format(translated, *args.map { useTranslation(bundleManager, it.toString()) }.toTypedArray())
    }

    fun format(bundleManager: BundleManager, text: String, vararg args: Any, indent: Int = 0): String {
        return useStyle(useTranslation(bundleManager, text, *args).replace("<indent>", " ".repeat(indent)))
    }

    fun formatClient(bundleManager: BundleManager, text: String, vararg args: Any): String {
        return useTranslation(bundleManager, text, *args)
    }

    enum class Level(val display: String) {
        Trace("<cyan>[T]</>"),
        Debug("<brown>[D]</>"),
        Info("<lightblue>[I]</>"),
        Warn("<lightyellow>[W]</>"),
        Error("<lightred>[E]</>"),
        Critical("<red>[C]</>"),
    }

}

class Logger(var tag: String, val bundleManager: BundleManager? = null) {

    fun generatePrefix(level: Logging.Level): String {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss")
        val time = "<bold><lightBlack>[${dateTimeFormatter.format(LocalDateTime.now())}]</>"
        return "$time ${level.display} [$tag] "
    }

    fun log(level: Logging.Level, message: String, vararg args: Any) {
        val prefix = generatePrefix(level)
        if(bundleManager == null) print(Logging.useStyle("$prefix$message"))
        else print(Logging.format(bundleManager, "$prefix$message", *args, indent = Logging.removeStyle(prefix).length))
    }

    fun logln(level: Logging.Level, message: String, vararg args: Any) {
        log(level, "$message\n", *args)
    }

    fun print(message: String, vararg args: Any) {
        if(bundleManager == null) kotlin.io.print(Logging.useStyle(message))
        else kotlin.io.print(Logging.format(bundleManager, message, *args, indent = generatePrefix(Logging.Level.Info).length))
    }

    fun println(message: String, vararg args: Any) {
        print("$message\n", *args)
    }

    fun println() = println("")

    fun trace(message: String, vararg args: Any) {
        log(Logging.Level.Trace, message, *args)
    }

    fun traceln(message: String, vararg args: Any) {
        logln(Logging.Level.Trace, message, *args)
    }

    fun debug(message: String, vararg args: Any) {
        log(Logging.Level.Debug, message, *args)
    }

    fun debugln(message: String, vararg args: Any) {
        logln(Logging.Level.Debug, message, *args)
    }

    fun info(message: String, vararg args: Any) {
        log(Logging.Level.Info, message, *args)
    }

    fun infoln(message: String, vararg args: Any) {
        logln(Logging.Level.Info, message, *args)
    }

    fun warn(message: String, vararg args: Any) {
        log(Logging.Level.Warn, message, *args)
    }

    fun warnln(message: String, vararg args: Any) {
        logln(Logging.Level.Warn, message, *args)
    }

    fun error(message: String, vararg args: Any) {
        log(Logging.Level.Error, message, *args)
    }

    fun errorln(message: String, vararg args: Any) {
        logln(Logging.Level.Error, message, *args)
    }

    fun crit(message: String, vararg args: Any) {
        log(Logging.Level.Critical, message, *args)
    }

    fun critln(message: String, vararg args: Any) {
        logln(Logging.Level.Critical, message, *args)
    }

}
