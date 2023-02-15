package com.github.devlaq.arkitect.core.command

import com.github.devlaq.arkitect.i18n.BundleManager
import com.github.devlaq.arkitect.util.console.Logger
import com.github.devlaq.arkitect.util.console.Logging
import mindustry.gen.Player
import java.util.*

interface CommandCaller {
    fun t(key: String, vararg args: Any): String?
    fun debug(message: String, vararg args: Any)
    fun info(message: String, vararg args: Any)
    fun warn(message: String, vararg args: Any)
    fun error(message: String, vararg args: Any)
    fun critical(message: String, vararg args: Any)
}

class PlayerCommandCaller(
    val player: Player
): CommandCaller {
    private lateinit var bundleManager: BundleManager

    fun setBundleManager(bundleManager: BundleManager) {
        this.bundleManager = bundleManager
    }

    override fun t(key: String, vararg args: Any): String {
        return bundleManager.translate(Locale.forLanguageTag(player.locale) ?: bundleManager.localeProvider(), key, *args)
    }

    override fun debug(message: String, vararg args: Any) {
        player.sendMessage("[cyan]${Logging.useTranslation(bundleManager, message, *args)}")
    }

    override fun info(message: String, vararg args: Any) {
        player.sendMessage("[white]${Logging.useTranslation(bundleManager, message, *args)}")
    }

    override fun warn(message: String, vararg args: Any) {
        player.sendMessage("[yellow]${Logging.useTranslation(bundleManager, message, *args)}")
    }

    override fun error(message: String, vararg args: Any) {
        player.sendMessage("[scarlet]${Logging.useTranslation(bundleManager, message, *args)}")
    }

    override fun critical(message: String, vararg args: Any) {
        player.sendMessage("[maroon]${Logging.useTranslation(bundleManager, message, *args)}")
    }
}

class ConsoleCommandCaller: CommandCaller {

    private lateinit var logger: Logger

    fun setLogger(logger: Logger) {
        this.logger = logger
    }

    override fun t(key: String, vararg args: Any): String? {
        return logger.bundleManager?.translate(key, args)
    }

    fun printBuffer() {
        logger.printBuffer()
    }

    fun debug(message: String, vararg args: Any, suspendLine: Boolean = false) {
        logger.debug(message, *args, suspendLine = suspendLine)
    }

    fun info(message: String, vararg args: Any, suspendLine: Boolean = false) {
        logger.info(message, *args, suspendLine = suspendLine)
    }

    fun warn(message: String, vararg args: Any, suspendLine: Boolean = false) {
        logger.warn(message, *args, suspendLine = suspendLine)
    }

    fun error(message: String, vararg args: Any, suspendLine: Boolean = false) {
        logger.error(message, *args, suspendLine = suspendLine)
    }

    fun critical(message: String, vararg args: Any, suspendLine: Boolean = false) {
        logger.crit(message, *args, suspendLine = suspendLine)
    }

    override fun debug(message: String, vararg args: Any) {
        logger.debug(message, *args)
    }

    override fun info(message: String, vararg args: Any) {
        logger.info(message, *args)
    }

    override fun warn(message: String, vararg args: Any) {
        logger.warn(message, *args)
    }

    override fun error(message: String, vararg args: Any) {
        logger.error(message, *args)
    }

    override fun critical(message: String, vararg args: Any) {
        logger.crit(message, *args)
    }

}
