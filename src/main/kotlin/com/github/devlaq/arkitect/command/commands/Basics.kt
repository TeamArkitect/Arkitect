package com.github.devlaq.arkitect.command.commands

import com.github.devlaq.arkitect.Arkitect
import com.github.devlaq.arkitect.command.CommandContext
import com.github.devlaq.arkitect.command.buildCommand
import mindustry.Vars

fun registerBasicCommands() {
    ArkitectCommand.build()
}

private object ArkitectCommand {
    fun build() {
        buildCommand {
            name = "arkitect"
            description = "See arkitect info and change configurations."

            action {
                fun sendHelp() {
                    val modMeta = Vars.mods.list().find { it.meta.name.equals("Arkitect", ignoreCase = true) }.meta
                    send("<%command.arkitect%>", modMeta.version)
                }

                if(args.isEmpty()) {
                    sendHelp()
                } else {
                    when(args[0]) {
                        "locale" -> subcommandLocale()
                    }
                }
            }
        }
    }

    fun CommandContext.subcommandLocale() {
        if (args.size <= 1) {
            send("<%command.arkitect.locale.available%>")
            Arkitect.bundleManager.availableLocales().forEach {
                send("<%command.arkitect.locale.available_element%>", it.getDisplayName(it), it.toLanguageTag())
            }
            return
        }

        val languageTag = args[1]
        val locale = Arkitect.bundleManager.availableLocales().find { it.toLanguageTag().equals(languageTag, ignoreCase = true) }
        if(locale == null) {
            send("<%not_found.locale%>", languageTag)
            return
        }

        if(Arkitect.bundleManager.localeProvider() == locale) {
            send("<%command.arkitect.locale.not_changed%>", languageTag)
            return
        }

        Arkitect.settings?.locale = locale.toLanguageTag()
        send("<%command.arkitect.locale.changed%>", languageTag)
    }
}
