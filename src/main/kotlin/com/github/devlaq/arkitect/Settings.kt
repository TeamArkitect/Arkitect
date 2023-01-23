package com.github.devlaq.arkitect

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.*

private val json = Json {
    encodeDefaults = true
    prettyPrint = true
    ignoreUnknownKeys = true
}

@Serializable
data class ArkitectSettings(
    val modules: ArkitectModuleSettings = ArkitectModuleSettings(),
    var locale: String = Locale.getDefault().toLanguageTag()
) {

    companion object {

        fun load(file: File): ArkitectSettings {
            return if(!file.exists()) {
                file.createNewFile()
                val settings = ArkitectSettings()
                settings.save(file)
                settings
            }
            else json.decodeFromString(file.readText())
        }
    }

    fun save(file: File) {
        if(!file.exists()) file.createNewFile()
        file.writeText(json.encodeToString(this))
    }
}

@Serializable
data class ArkitectModuleSettings(
    val watchModules: Boolean = false,
    val fail: String = "message"
)
