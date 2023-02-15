package com.github.devlaq.arkitect.util.console

import com.github.devlaq.arkitect.i18n.BundleManager
import com.github.devlaq.arkitect.util.resources.getResourceAsText
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.text.MessageFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object Logging {

    lateinit var styles: Map<String, String>

    var logHandler = { text: String ->
        println(text)
    }

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

    fun useTranslation(bundleManager: BundleManager, text: String, vararg args: Any, locale: Locale = bundleManager.localeProvider()): String {
        val (prefix, suffix) = "<%" to "%>"

        val translated = text.split(prefix)[0] + text.split(prefix).drop(1).map {
            val key = it.substringBefore(suffix)
            if(key == it) return@map it
            bundleManager.translate(locale, key) + it.substringAfter(suffix)
        }.joinToString("")

        return bundleManager.format(translated, *args.map { useTranslation(bundleManager, it.toString()) }.toTypedArray())
    }

    fun applyIndent(text: String, indent: Int = 0): String {
        val lastNewlineRemoved = text.removeSuffix("\n")
        val indented = lastNewlineRemoved.replace("\n", "\n${" ".repeat(indent)}")
        return indented + if (text.endsWith("\n")) "\n" else ""
    }

    fun formatString(string: String, vararg args: Any): String {
        return try {
            MessageFormat.format(string, *args)
        } catch (e: Exception) {
            string
        }
    }

    fun format(bundleManager: BundleManager, text: String, vararg args: Any): String {
        return useStyle(useTranslation(bundleManager, text, *args))
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

    private var bufferLevel: Logging.Level = Logging.Level.Info
    private val buffer = mutableListOf<Pair<String, Array<out Any>>>()

    fun generatePrefix(level: Logging.Level): String {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd-yyyy HH:mm:ss")
        val time = "<bold><lightBlack>[${dateTimeFormatter.format(LocalDateTime.now())}]</>"
        return "$time ${level.display} [$tag] "
    }

    fun printBuffer() {
        val message = buffer.joinToString("\n") {
            if(bundleManager != null) Logging.format(bundleManager, it.first, *it.second)
            else Logging.formatString(it.first, *it.second)
        }

        val prefix = generatePrefix(bufferLevel)
        val indentedMessage = Logging.applyIndent(message, Logging.removeStyle(prefix).length)
        val finalMessage = Logging.useStyle("$prefix$indentedMessage")

        Logging.logHandler(finalMessage)

        buffer.clear()
    }

    fun log(level: Logging.Level, message: String, vararg args: Any, suspendLine: Boolean = false) {
        bufferLevel = level
        buffer.add(message to args)

        if(!suspendLine) printBuffer()
    }

    fun debug(message: String, vararg args: Any, suspendLine: Boolean = false) {
        log(Logging.Level.Debug, message, *args, suspendLine = suspendLine)
    }

    fun info(message: String, vararg args: Any, suspendLine: Boolean = false) {
        log(Logging.Level.Info, message, *args, suspendLine = suspendLine)
    }

    fun warn(message: String, vararg args: Any, suspendLine: Boolean = false) {
        log(Logging.Level.Warn, message, *args, suspendLine = suspendLine)
    }

    fun error(message: String, vararg args: Any, suspendLine: Boolean = false) {
        log(Logging.Level.Error, message, *args, suspendLine = suspendLine)
    }

    fun crit(message: String, vararg args: Any, suspendLine: Boolean = false) {
        log(Logging.Level.Critical, message, *args, suspendLine = suspendLine)
    }

}
