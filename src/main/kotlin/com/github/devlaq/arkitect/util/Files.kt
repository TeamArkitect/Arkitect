package com.github.devlaq.arkitect.util

import arc.files.Fi
import java.io.File

fun dataDirectory() = File("config/mods/Arkitect")

class DataFile(pathname: String, mkdirs: Boolean = false): File(dataDirectory(), pathname) {
    init {
        if(mkdirs && !this.exists()) this.mkdirs()
    }
}

fun File.fi() = Fi(this)
