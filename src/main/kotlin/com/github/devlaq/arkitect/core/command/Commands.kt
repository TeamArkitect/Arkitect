package com.github.devlaq.arkitect.core.command

import arc.util.CommandHandler
import com.github.devlaq.arkitect.Arkitect
import com.github.devlaq.arkitect.util.getServerControl
import mindustry.Vars
import mindustry.gen.Player

object Commands {

    val registry = mutableMapOf<String, Command>()

    fun updateCommandRegistry() {
        // Update client commands
        registry.filterValues { it.scope == Command.Scope.All || it.scope == Command.Scope.Client }.forEach {
            Vars.netServer.clientCommands.removeCommand(it.key)
            Vars.netServer.clientCommands.register(it.key, "[params...]", it.value.description, CommandHandler.CommandRunner<Player> { args, player ->
                val args = args?.firstOrNull()?.split(" ")?.filter { it.isNotBlank() }?.toTypedArray() ?: arrayOf()
                val handlerContext = Command.HandlerContext(
                    name = it.key,
                    args = args,
                    caller = PlayerCommandCaller(player)
                )

                try {
                    it.value.handler.handle(handlerContext)
                } catch (_: Throwable) {
                    player.sendMessage("[scarlet]An error occured while handling the command. Please report to the administrator.")
                }
            })
        }

        // Update server commands
        registry.filterValues { it.scope == Command.Scope.All || it.scope == Command.Scope.Server }.forEach {
            getServerControl()?.handler?.removeCommand(it.key)
            getServerControl()?.handler?.register(it.key, "[params...]", it.value.description, CommandHandler.CommandRunner<Any> { args, _ ->
                val args = args?.firstOrNull()?.split(" ")?.filter { it.isNotBlank() }?.toTypedArray() ?: arrayOf()
                val handlerContext = Command.HandlerContext(
                    name = it.key,
                    args = args,
                    caller = ConsoleCommandCaller()
                )

                try {
                    it.value.handler.handle(handlerContext)
                } catch (_: Throwable) {
                    Arkitect.createLogger("Arkitect").error("<red>Error occured while handling the command. Report to the developer.</>")
                }
            })
        }
    }

    fun register(command: Command): Command {
        registry[command.name] = command
        return command
    }

    fun register(body: CommandBuilder.() -> Unit): Command {
        return register(buildCommand(body))
    }

    fun buildCommand(body: CommandBuilder.() -> Unit): Command {
        val commandBuilder = CommandBuilder()
        body(commandBuilder)

        val name =
            commandBuilder.name ?: throw IllegalArgumentException("\"name\" of the CommandBuilder can't be null!")
        val description = commandBuilder.description
        val scope = commandBuilder.scope

        val handler = object : Command.Handler {
            override fun handle(context: Command.HandlerContext) {
                commandBuilder.handler?.invoke(context)
            }
        }

        val completer = object : Command.Completer {
            override fun complete(context: Command.CompleterContext): List<String> {
                return commandBuilder.completer?.invoke(context) ?: emptyList()
            }
        }

        return Command(
            name = name,
            description = description,
            scope = scope,
            handler = handler,
            completer = completer
        )
    }

}
