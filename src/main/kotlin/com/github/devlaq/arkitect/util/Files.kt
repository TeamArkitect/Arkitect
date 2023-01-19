package com.github.devlaq.arkitect.util

import arc.files.Fi
import java.io.File

fun dataDirectory() = File("config/mods/Arkitect")

class DataFile(pathname: String, mkdirs: Boolean = false, createNewFile: Boolean = false): File(dataDirectory(), pathname) {
    init {
        if(createNewFile && !this.exists()) {
            parentFile.mkdirs()
            this.createNewFile()
        }
        if(mkdirs && !this.exists()) this.mkdirs()
    }
}


fun File.fi() = Fi(this)
