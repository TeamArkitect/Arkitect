package com.github.devlaq.arkitect.command

import arc.util.CommandHandler.CommandRunner
import com.github.devlaq.arkitect.Arkitect
import com.github.devlaq.arkitect.command.commands.registerBasicCommands
import com.github.devlaq.arkitect.command.commands.registerDebuggingCommands
import com.github.devlaq.arkitect.command.commands.registerModuleCommands
import com.github.devlaq.arkitect.util.console.Logger
import com.github.devlaq.arkitect.util.console.Logging
import com.github.devlaq.arkitect.util.getServerControl
import com.github.devlaq.arkitect.util.sendTranslated
import mindustry.Vars
import mindustry.gen.Player

class CommandContext(val player: Player?, val args: Array<out String>) {

    private val logger = Arkitect.createLogger("Arkitect")

    val argsLowerCase get() = args.map { it.lowercase() }

    fun isConsole() = player == null
    fun isPlayer() = player != null

    fun loggerTag(tag: String) {
        logger.tag = tag
    }

    fun send(text: String, vararg args: Any) {
        if(isPlayer()) {
            var formatted = text.replace("<indent>", "")
            Logging.styles.forEach { formatted = formatted.replace("<${it.key}>", "[${it.value}]") }
            player?.sendMessage(formatted)
        } else {
            logger.infoln(text, *args)
        }
    }

    fun sendMessage(text: String) {
        if(player != null) {
            player.sendMessage(text)
        } else {
            logger.info(text)
        }
    }

    fun sendTranslated(key: String, vararg args: Any?) {
        if(player != null) {
            player.sendTranslated(key, *args)
        } else {
            logger.infoln("<%${key}%>", *args.map { it.toString() }.toTypedArray())
        }
    }

}

class CommandBuilder {
    var name: String = ""
    var description: String = ""

    private var action: (CommandContext.() -> Unit)? = null

    fun action(body: CommandContext.() -> Unit) {
        action = body
    }

    fun validate(): Boolean {
        return name.isNotEmpty() && action != null
    }

    fun actionToCommandRunner(): CommandRunner<Any> {
        val runner = CommandRunner<Any> { args, _ ->
            val context = CommandContext(null, args.firstOrNull()?.split(" ")?.filter { it.isNotBlank() }?.toTypedArray() ?: arrayOf())
            action!!(context)
        }
        return runner
    }

    fun actionToClientCommandRunner(): CommandRunner<Player> {
        val runner = CommandRunner<Player> { args, player ->
            val context = CommandContext(player, args.firstOrNull()?.split(" ")?.filter { it.isNotBlank() }?.toTypedArray() ?: arrayOf())
            action!!(context)
        }
        return runner
    }
}

fun buildCommand(scope: CommandScope = CommandScope.All, body: CommandBuilder.() -> Unit) {
    val builder = CommandBuilder()
    body(builder)

    val clientHandler = Vars.netServer.clientCommands
    val consoleHandler = getServerControl()!!.handler

    if(scope == CommandScope.Client) {
        clientHandler.register(builder.name, "[params...]", builder.description, builder.actionToClientCommandRunner())
    } else if(scope == CommandScope.Console) {
        consoleHandler.register(builder.name, "[params...]", builder.description, builder.actionToCommandRunner())
    } else if(scope == CommandScope.All) {
        buildCommand(CommandScope.Client, body)
        buildCommand(CommandScope.Console, body)
    }
}

enum class CommandScope {
    Client,
    Console,
    All
}

fun registerCommands() {
    registerDebuggingCommands()
    registerModuleCommands()
    registerBasicCommands()
}
