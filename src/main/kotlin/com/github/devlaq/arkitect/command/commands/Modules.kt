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
                    send("<%command.modules.help%>")
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
        if(args.size <= 1) {
            send("<%command.general.missing_arguments%>", 2, "module name")
            return@run null
        }

        val moduleName = args[1]

        // 1. Find in module meta
        Modules.readMetas().filter { it.value.name == moduleName }.keys.firstOrNull()?.let {
            Modules.load(it)
            return@run it
        }

        // 2. Find in file names
        Modules.modulesDir.listFiles()?.firstOrNull { it.isFile && it.name == moduleName }?.let {
            Modules.load(it)
            return@run it
        }

        // 2. Find in file names (without extension)
        Modules.modulesDir.listFiles()?.firstOrNull { it.isFile && it.name == moduleName }?.let {
            Modules.load(it)
            return@run it
        }

        send("<%command.modules.not_found%>", moduleName)
        return@run null
    }

    fun subcommandUnload(context: CommandContext) = context.run {
        if(args.size <= 1) {
            send("<%command.general.missing_arguments%>", 2, "module name")
            return@run null
        }

        val moduleName = args[1]

        // 1. Find in module names
        Modules.loaded.firstOrNull { it.meta.name == moduleName }?.let {
            Modules.unload(it.file)
            return@run it.file
        }

        // 2. Find in file names
        Modules.loaded.firstOrNull { it.file.name == moduleName }?.let {
            Modules.unload(it.file)
            return@run it.file
        }

        // 3. Find in file names (without extension)
        Modules.loaded.firstOrNull { it.file.nameWithoutExtension == moduleName }?.let {
            Modules.unload(it.file)
            return@run it.file
        }

        send("<%command.modules.not_found%>", moduleName)
        return@run null
    }

    fun subcommandReload(context: CommandContext) = context.run {
        if(args.size <= 1) {
            send("<%command.general.missing_arguments%>", 2, "module name")
            return@run null
        }

        val moduleName = args[1]

        // 1. Find in module names
        Modules.loaded.firstOrNull { it.meta.name == moduleName }?.let {
            Modules.reload(it.file)
            return@run it.file
        }

        // 2. Find in file names
        Modules.loaded.firstOrNull { it.file.name == moduleName }?.let {
            Modules.reload(it.file)
            return@run it.file
        }

        // 3. Find in file names (without extension)
        Modules.loaded.firstOrNull { it.file.nameWithoutExtension == moduleName }?.let {
            Modules.reload(it.file)
            return@run it.file
        }

        send("<%command.modules.not_found%>", moduleName)
        return@run null
    }

}
