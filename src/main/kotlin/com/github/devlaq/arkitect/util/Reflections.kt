package com.github.devlaq.arkitect.util

import arc.Core
import mindustry.server.ServerControl

fun getServerControl(): ServerControl? {
    Core.app.listeners.forEach {
        if(it is ServerControl) {
            return it
        }
    }
    getArkitectLogger().warn("Could not find ServerControl instance! (This may be critical to the plugin)")
    return null
}