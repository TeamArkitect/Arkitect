package com.github.devlaq.arkitect.core.module

import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class ModuleMeta(
    val name: String,
    val authors: List<String>,
    val version: String,
    val mainClass: String
)

data class LoadedModuleMeta(
    val meta: ModuleMeta,
    val instance: ArkitectModule,
    val file: File,
    val classLoader: ClassLoader,
    private var _enabled: Boolean = false
) {
    val enabled get() = _enabled

    fun enable() {
        if(_enabled) return
        instance.enable()
        _enabled = true
    }

    fun disable() {
        if(!_enabled) return
        instance.disable()
        _enabled = false
    }
}
