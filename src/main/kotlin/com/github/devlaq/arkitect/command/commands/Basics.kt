package com.github.devlaq.arkitect.command.commands

import com.github.devlaq.arkitect.Arkitect
import com.github.devlaq.arkitect.ArkitectSettings
import com.github.devlaq.arkitect.command.buildCommand
import com.github.devlaq.arkitect.core.module.Modules
import com.github.devlaq.arkitect.i18n.I18N
import mindustry.Vars
import java.util.Locale

fun registerBasicCommands() {
    buildArkitectCommand()
}

private fun buildArkitectCommand() {
    buildCommand {
        name = "arkitect"
        description = "Information about arkitect loaded in this server"

        action {
            fun sendHelp() {
                val modMeta = Vars.mods.list().find { it.meta.name.equals("Arkitect", ignoreCase = true) }.meta
                send("<%command.arkitect%>", modMeta.version)
            }

            if(args.isEmpty()) {
                sendHelp()
            } else {
                when(args[0]) {
                    "locale" -> {
                        if (args.size <= 1) {
                            sendHelp()
                            return@action
                        }

                        val languageTag = args[1]
                        val locale = Arkitect.bundleManager.availableLocales().find { it.toLanguageTag().equals(languageTag, ignoreCase = true) }
                        if(locale == null) {
                            send("<%not_found.locale%>", languageTag)
                            return@action
                        }

                        if(Arkitect.localeProvider() == locale) {
                            send("<%command.arkitect.locale.not_changed%>", languageTag)
                            return@action
                        }

                        Arkitect.settings?.locale = locale.toLanguageTag()
                        send("<%command.arkitect.locale.changed%>", languageTag)
                    }
                }
            }
        }
    }
}
