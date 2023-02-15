package com.github.devlaq.arkitect.command

import com.github.devlaq.arkitect.Arkitect
import com.github.devlaq.arkitect.core.command.Command
import com.github.devlaq.arkitect.core.command.Commands
import com.github.devlaq.arkitect.core.command.ConsoleCommandCaller
import com.github.devlaq.arkitect.core.command.PlayerCommandCaller
import java.util.*

fun Arkitect.registerArkitectCommand() {
    val logger = Arkitect.createLogger("Arkitect")

    Commands.register {
        name = "arkitect"
        description = "Manage arkitect settings"
        scope = Command.Scope.All

        handler {
            if(caller is PlayerCommandCaller) caller.setBundleManager(logger.bundleManager!!)
            if(caller is ConsoleCommandCaller) caller.setLogger(logger)

            if(args.isEmpty()) {
                caller.info("<%command.arkitect%>")
                return@handler
            }

            fun subcommandLocale() {
                if(caller !is ConsoleCommandCaller) {
                    caller.warn("<%command.general.console_only%>")
                    return
                }

                if(args.size < 2) {
                    caller.info("<%command.arkitect.locale.available%>", suspendLine = true)
                    Arkitect.bundleManager.availableLocales().forEach {
                        caller.info("<%command.arkitect.locale.available_element%>", it.getDisplayName(it), it.toLanguageTag(), suspendLine = true)
                    }
                    caller.printBuffer()
                    return
                }

                val localeString = args[1]
                val locale = Locale.forLanguageTag(localeString)

                if(!Arkitect.bundleManager.availableLocales().contains(locale)) {
                    caller.info("<%not_found.locale%>", localeString)
                    return
                }

                if(Arkitect.settings?.locale == locale.toLanguageTag()) {
                    caller.info(
                        message = "<%command.arkitect.locale.not_changed%>",
                        "${locale.getDisplayName(Arkitect.bundleManager.localeProvider())}(${locale.toLanguageTag()})"
                    )
                } else {
                    Arkitect.settings?.locale = locale.toLanguageTag()
                    caller.info(
                        message = "<%command.arkitect.locale.changed%>",
                        "${locale.getDisplayName(Arkitect.bundleManager.localeProvider())}(${locale.toLanguageTag()})"
                    )
                }

            }

            when(args[0].lowercase()) {
                "locale" -> subcommandLocale()
            }
        }

        completer {
            if(args.size < 2) {
                return@completer listOf("locale")
            }
            if(args.size < 3) {
                if(args[0] == "locale") {
                    return@completer Arkitect.bundleManager.availableLocales().map { it.toLanguageTag() }
                }
            }
            return@completer null
        }
    }
}
