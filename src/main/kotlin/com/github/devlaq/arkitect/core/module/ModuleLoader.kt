package com.github.devlaq.arkitect.core.module

import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URLClassLoader
import java.util.zip.ZipException
import java.util.zip.ZipFile

/**
 * Module loader interface
 *
 * Read jar file, parse module.json and load to classpath
 */
interface ModuleLoader {

    fun readMeta(file: File): ModuleMeta?
    fun load(file: File): LoadedModuleMeta

    fun loadOrNull(file: File): LoadedModuleMeta? {
        return try {
            load(file)
        } catch (_: Exception) {
            return null
        }
    }

}

class DefaultModuleLoader: ModuleLoader {

    class ModuleMetaFailException: RuntimeException("The module meta was not found or invalid")
    class MainClassNotFoundException(mainClassPath: String): RuntimeException("The main \"$mainClassPath\" class was not found")
    class MainClassInvalidException: RuntimeException("The main class must inherit ArkitectModule interface")
    class InstanceCreateFailException: RuntimeException("Failed to create an instance of the main class")

    override fun readMeta(file: File): ModuleMeta? {
        val zip = ZipFile(file)
        val metaEntry = zip.getEntry("module.json") ?: return null
        val metaContent = zip.getInputStream(metaEntry).bufferedReader().readText()
        return try {
            Json.decodeFromString<ModuleMeta>(metaContent)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Loads a module from jar file.
     *
     * @param file The jar file.
     *
     * @return Loaded module`s meta
     *
     * @throws ZipException If failed to read the jar file
     * @throws ModuleMetaFailException If failed to resolve module meta
     * @throws MainClassNotFoundException If the main class was not found
     * @throws MainClassInvalidException If the main class does not inherit ArkitectModule interface
     * @throws InstanceCreateFailException If failed to create an instance of the main class
     */
    override fun load(file: File): LoadedModuleMeta {
        val zip = ZipFile(file)
        val moduleMeta = readMeta(file) ?: throw ModuleMetaFailException()

        val mainClassPath = "${moduleMeta.mainClass.replace(".", "/")}.class"
        zip.getEntry(mainClassPath) ?: throw MainClassNotFoundException(mainClassPath)

        val classLoader = object : URLClassLoader(
            arrayOf(file.toURI().toURL()),
            javaClass.classLoader
        ) {}

        val mainClass = Class.forName(moduleMeta.mainClass, false, classLoader)
        if(!ArkitectModule::class.java.isAssignableFrom(mainClass)) throw MainClassInvalidException()

        val instance = try {
            mainClass.getDeclaredConstructor().newInstance() as ArkitectModule
        } catch (_: Throwable) {
            throw InstanceCreateFailException()
        }

        return LoadedModuleMeta(
            meta = moduleMeta,
            instance = instance,
            file = file,
            classLoader = classLoader,
            _enabled = false
        )
    }

}
