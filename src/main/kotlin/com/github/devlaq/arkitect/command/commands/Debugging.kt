package com.github.devlaq.arkitect.command.commands

import com.github.devlaq.arkitect.command.buildCommand

fun registerDebuggingCommands() {
    buildTestCommand()
}

private fun buildTestCommand() {
    buildCommand {
        name = "ping"
        description = "Pong!"

        action {
            sendMessage("Pong! $player")
        }
    }
}
