package com.github.devlaq.arkitect.command

import com.github.devlaq.arkitect.Arkitect
import com.github.devlaq.arkitect.core.command.Command
import com.github.devlaq.arkitect.core.command.Commands
import com.github.devlaq.arkitect.core.command.ConsoleCommandCaller
import com.github.devlaq.arkitect.core.module.Modules

fun Arkitect.registerModuleCommands() {
    val logger = Arkitect.createLogger("Arkitect/Modules")

    Commands.register {
        name = "modules"
        description = "Manage Arkitect modules"
        scope = Command.Scope.Server

        handler {
            (caller as ConsoleCommandCaller).setLogger(logger)

            if(args.isEmpty()) {
                caller.info("<%command.modules.help%>")
                return@handler
            }

            fun subcommandList() {
                val modules = Modules.loaded.joinToString("\n") { "<lightblue>${it.meta.name}</> - <lightgreen>${it.meta.version}</> <lightred>from</> <cyan>${it.file.name}</>" }
                caller.info(modules)
            }

            fun subcommandLoad() {
                if(args.size <= 1) {
                    caller.info("<%command.general.missing_arguments%>", 2, "module name")
                    return
                }

                val moduleName = args[1]

                // 1. Find in module meta
                Modules.readMetas().filter { it.value.name == moduleName }.keys.firstOrNull()?.let {
                    Modules.load(it)
                    return
                }

                // 2. Find in file names
                Modules.modulesDir.listFiles()?.firstOrNull { it.isFile && it.name == moduleName }?.let {
                    Modules.load(it)
                    return
                }

                // 2. Find in file names (without extension)
                Modules.modulesDir.listFiles()?.firstOrNull { it.isFile && it.name == moduleName }?.let {
                    Modules.load(it)
                    return
                }

                caller.info("<%command.modules.not_found%>", moduleName)
            }

            fun subcommandUnload() {
                if(args.size <= 1) {
                    caller.info("<%command.general.missing_arguments%>", 2, "module name")
                    return
                }

                val moduleName = args[1]

                // 1. Find in module names
                Modules.loaded.firstOrNull { it.meta.name == moduleName }?.let {
                    Modules.unload(it.file)
                    return
                }

                // 2. Find in file names
                Modules.loaded.firstOrNull { it.file.name == moduleName }?.let {
                    Modules.unload(it.file)
                    return
                }

                // 3. Find in file names (without extension)
                Modules.loaded.firstOrNull { it.file.nameWithoutExtension == moduleName }?.let {
                    Modules.unload(it.file)
                    return
                }

                caller.info("<%command.modules.not_found%>", moduleName)
            }

            fun subcommandReload() {
                if(args.size <= 1) {
                    caller.info("<%command.general.missing_arguments%>", 2, "module name")
                    return
                }

                val moduleName = args[1]

                // 1. Find in module names
                Modules.loaded.firstOrNull { it.meta.name == moduleName }?.let {
                    Modules.reload(it.file)
                    return
                }

                // 2. Find in file names
                Modules.loaded.firstOrNull { it.file.name == moduleName }?.let {
                    Modules.reload(it.file)
                    return
                }

                // 3. Find in file names (without extension)
                Modules.loaded.firstOrNull { it.file.nameWithoutExtension == moduleName }?.let {
                    Modules.reload(it.file)
                    return
                }

                caller.info("<%command.modules.not_found%>", moduleName)
            }

            when(args[0].lowercase()) {
                "list" -> subcommandList()
                "load" -> subcommandLoad()
                "unload" -> subcommandUnload()
                "reload" -> subcommandReload()
            }

        }

        completer {
            if(args.size < 2) {
                return@completer listOf(
                    "list",
                    "load",
                    "unload",
                    "reload"
                )
            }

            if(args.size < 3) {
                when(args[0]) {
                    "load" -> {
                        return@completer Modules.readMetas().map { it.value.name }.filter { !Modules.loaded.map { m -> m.meta.name }.contains(it) }
                    }
                    "unload", "reload" -> {
                        return@completer Modules.loaded.map { it.meta.name }
                    }
                }
            }

            return@completer null
        }
    }
}
