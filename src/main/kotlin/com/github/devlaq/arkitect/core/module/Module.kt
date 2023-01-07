package com.github.devlaq.arkitect.core.module

import arc.files.Fi
import arc.files.ZipFi
import com.github.devlaq.arkitect.util.Logger
import com.github.devlaq.arkitect.util.fi
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import mindustry.Vars
import mindustry.mod.ModClassLoader
import java.io.File

interface ArkitectModule {

    val meta: ModuleMeta
        get() = Modules.loadedModules.values.first { it.first == this }.second

    fun load()

    fun unload()

}

@Serializable
data class ModuleMeta(
    val name: String,
    val authors: List<String>,
    val version: String,
    val mainClass: String
)

object Modules {

    private val logger = Logger("Arkitect/Modules")

    private val moduleClassLoader = ModClassLoader(javaClass.classLoader)

    val loadedModules = mutableMapOf<String, Pair<ArkitectModule, ModuleMeta>>()

    /**
     * Load all mods in the directory.
     */
    fun loadDirectory(directory: File) {
        if(!directory.isDirectory) throw RuntimeException("Provided file is not a directory!")

        var moduleFiles = 0
        var successfullyLoaded = 0
        directory.listFiles { file -> file.isFile && file.extension == "jar" }?.forEach {
            moduleFiles++
            val (instance, meta) = load(it) ?: return
            if(loadedModules.containsKey(meta.name)) return logger.errorT("module.load_fail.already_loaded")
            loadedModules[meta.name] = instance to meta
            successfullyLoaded++
        }

        loadedModules.values.forEach {
            val (module, _) = it
            module.load()
        }

        if(moduleFiles == 0) {
            logger.infoT("module.loaded_directory.no_modules_found", directory.path)
        } else {
            logger.infoT("module.loaded_directory", directory.path, successfullyLoaded, moduleFiles)
        }
    }

    fun load(file: File): Pair<ArkitectModule, ModuleMeta>? {
        if(file.isDirectory) return null

        lateinit var zipFile: ZipFi
        fun validateJar(): Boolean {
            if (file.extension != "jar") return false
            try {
                zipFile = ZipFi(Fi(file))
            } catch (_: Exception) {
                return false
            }
            return true
        }

        fun findMeta(): ModuleMeta? {
            val metaFile = zipFile.child("module.json")
            if(!metaFile.exists()) logger.errorT("module.load_fail.no_meta", file.name).run { return null }
            val text = metaFile.reader().readText()
            return try {
                Json.decodeFromString<ModuleMeta>(text)
            } catch (_: Exception) {
                logger.errorT("module.load_fail.invalid_meta", file.name)
                null
            }
        }

        fun checkMainClass(meta: ModuleMeta): Fi {
            val mainClass = meta.mainClass
            val filePath = "${mainClass.replace(".", "/")}.class"
            var mainClassFile = zipFile as Fi
            filePath.split("/").forEach {
                if (it.isNotEmpty()) mainClassFile = mainClassFile.child(it)
            }
            return mainClassFile
        }

        if (!validateJar()) logger.errorT("module.load_fail.not_a_jar", file.name, file.name).run { return null }

        val meta = findMeta() ?: return null
        val mainClassFile = checkMainClass(meta)

        if (!mainClassFile.exists()) logger.errorT("module.load_fail.main_class_not_found", file.name).run { return null }

        val loader = Vars.platform.loadJar(file.fi(), moduleClassLoader)
        moduleClassLoader.addChild(loader)

        val mainClass = Class.forName(meta.mainClass, true, loader)

        if (!ArkitectModule::class.java.isAssignableFrom(mainClass)) logger.errorT("module.load_fail.main_class_not_implementing", file.name)
            .run { return null }

        return try {
            val constructor = mainClass.getDeclaredConstructor()
            val instance = constructor.newInstance() as ArkitectModule
            (instance to meta)
        } catch (_: Exception) {
            logger.errorT("module.load_fail.main_class_constructor_invalid", file.name)
            null
        }
    }

    fun dispose() {
        loadedModules.values.forEach {
            it.first.unload()
        }
    }

}
