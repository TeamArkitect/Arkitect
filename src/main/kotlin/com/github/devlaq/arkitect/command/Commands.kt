package com.github.devlaq.arkitect.command

import com.github.devlaq.arkitect.Arkitect
import com.github.devlaq.arkitect.core.command.Commands

fun Arkitect.registerCommands() {
    registerModuleCommands()
    registerArkitectCommand()

    Commands.updateCommandRegistry()
}
