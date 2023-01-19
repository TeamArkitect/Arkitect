package com.github.devlaq.arkitect.command.commands

import com.github.devlaq.arkitect.command.CommandContext
import com.github.devlaq.arkitect.command.CommandScope
import com.github.devlaq.arkitect.command.buildCommand
import com.github.devlaq.arkitect.core.module.Modules

fun registerModuleCommands() {
    ModuleCommand.build()
}

private object ModuleCommand {
    fun build() {
        buildCommand(CommandScope.Console) {
            name = "modules"
            description = "Manage modules"

            action {
                loggerTag("Arkitect/Modules")

                if(args.isEmpty()) {
                    sendMessage("/modules list/load/unload/reload")
                    return@action
                }

                when(argsLowerCase[0]) {
                    "list" -> subcommandList(this)
                    "load" -> subcommandLoad(this)
                    "unload" -> subcommandUnload(this)
                    "reload" -> subcommandReload(this)
                }
            }
        }
    }

    fun subcommandList(context: CommandContext) = context.run {
        val modules = Modules.loaded.joinToString("\n") { "<lightblue>${it.meta.name}</> - <lightgreen>${it.meta.version}</> <lightred>from</> <cyan>${it.file.name}</>" }
        send(modules)
    }

    fun subcommandLoad(context: CommandContext) = context.run {

    }

    fun subcommandUnload(context: CommandContext) = context.run {

    }

    fun subcommandReload(context: CommandContext) = context.run {

    }

}
