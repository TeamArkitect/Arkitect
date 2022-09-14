package com.github.devlaq.arkitect.util

import com.github.devlaq.arkitect.i18n.I18N
import mindustry.gen.Player

fun Player.sendTranslated(key: String, vararg args: Any?) {
    sendMessage(I18N.translate(key, args))

}