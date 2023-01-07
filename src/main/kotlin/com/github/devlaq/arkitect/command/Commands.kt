package com.github.devlaq.arkitect.command

import arc.util.CommandHandler.CommandRunner
import com.github.devlaq.arkitect.command.commands.registerDebuggingCommands
import com.github.devlaq.arkitect.util.Logger
import com.github.devlaq.arkitect.util.getServerControl
import com.github.devlaq.arkitect.util.sendTranslated
import mindustry.Vars
import mindustry.gen.Player

class CommandContext(val player: Player?, val args: Array<out String>) {

    private val logger = Logger("Arkitect")

    fun isConsole() = player == null
    fun isPlayer() = player != null

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
            logger.infoT(key, *args)
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
            val context = CommandContext(null, args ?: arrayOf())
            action!!(context)
        }
        return runner
    }

    fun actionToClientCommandRunner(): CommandRunner<Player> {
        val runner = CommandRunner<Player> { args, player ->
            val context = CommandContext(player, args ?: arrayOf())
            action!!(context)
        }
        return runner
    }
}

fun buildCommand(commandScope: CommandScope = CommandScope.All, body: CommandBuilder.() -> Unit) {
    val builder = CommandBuilder()
    body(builder)

    val clientHandler = Vars.netServer.clientCommands
    val consoleHandler = getServerControl()!!.handler

    if(commandScope == CommandScope.Client) {
        clientHandler.register(builder.name, "[params...]", builder.description, builder.actionToClientCommandRunner())
    } else if(commandScope == CommandScope.Console) {
        consoleHandler.register(builder.name, "[params...]", builder.description, builder.actionToCommandRunner())
    } else if(commandScope == CommandScope.All) {
        buildCommand(CommandScope.Client, body)
        buildCommand(CommandScope.Console, body)
    }
}

fun registerCommands() {
    registerDebuggingCommands()
}

enum class CommandScope {
    Client,
    Console,
    All
}
