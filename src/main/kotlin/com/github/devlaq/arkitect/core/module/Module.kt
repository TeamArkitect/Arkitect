package com.github.devlaq.arkitect.core.module

import arc.util.Disposable
import com.github.devlaq.arkitect.Arkitect
import com.github.devlaq.arkitect.util.DataFile
import dev.vishna.watchservice.KWatchEvent.Kind.*
import dev.vishna.watchservice.asWatchChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URLClassLoader
import java.util.zip.ZipException
import java.util.zip.ZipFile

interface ArkitectModule {

    fun enable()

    fun disable()

}

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

// Rework
object Modules: Disposable {

    private val logger = Arkitect.createLogger("Arkitect/Modules")

    private lateinit var moduleLoader: ModuleLoader

    val modulesDir = DataFile("modules", mkdirs = true)
    val loaded = mutableListOf<LoadedModuleMeta>()

    fun init(moduleLoaderOverride: ModuleLoader? = null) {
        moduleLoader = moduleLoaderOverride ?: DefaultModuleLoader()

        loadDirectory(modulesDir)

        if(Arkitect.settings!!.modules.watchModules) watchModules()
    }

    fun watchModules() {
        logger.infoln("<%module.watching%>", modulesDir.path)

        val watchChannel = modulesDir.asWatchChannel()
        CoroutineScope(Dispatchers.Default).launch { watchChannel.consumeEach {
            val isLoaded = loaded.map { meta -> meta.file.name }.contains(it.file.name)
            when(it.kind) {
                Created -> run {
                    //try load
                    if(isLoaded) return@run
                    load(it.file)
                }
                Modified -> run {
                    //try reload
                    if(isLoaded) {
                        reload(it.file)
                    } else {
                        load(it.file)
                    }
                }
                Deleted -> run {
                    //try unload
                    if(!isLoaded) return@run

                    unload(it.file)
                }
                else -> {}
            }
        } }
    }

    fun loadDirectory(dir: File) {
        if(!dir.isDirectory) return

        dir.listFiles()?.forEach {
            load(it)
        }
    }

    fun load(file: File, noLog: Boolean = false): LoadedModuleMeta? {
        if(!file.isFile) return null
        if(loaded.map { it.file.name }.contains(file.name)) {
            logger.infoln("<%module.already_loaded%>", file.name)
            return null
        }

        if(!noLog) logger.infoln("<%module.load%> ... ", file.name)
        return try {
            val loadedMeta = moduleLoader.load(file)
            loadedMeta.enable()
            loaded.add(loadedMeta)
            loadedMeta
        } catch (e: Throwable) {
            if(!noLog) logger.print("<%module.load%> ... <%fail%>", file.name)
            if(Arkitect.settings!!.modules.fail == "message") logger.print("<red>(${e.localizedMessage})</>")
            if(Arkitect.settings!!.modules.fail == "stacktrace") logger.print("<red>(${e.stackTraceToString()})</>")
            logger.println()
            null
        }
    }

    fun unload(file: File, noLog: Boolean = false) {
        if(!file.isFile) return

        try {
            if(!noLog) logger.infoln("<%module.unload%>", file.name)
            val module = loaded.firstOrNull { it.file.canonicalPath == file.canonicalPath } ?: return
            module.disable()
            loaded.remove(module)
        } catch (_: Exception) {}
    }

    fun reload(file: File, noLog: Boolean = false) {
        if(!noLog) logger.infoln("<%module.reload%>", file.name)
        unload(file, noLog = true)
        load(file, noLog = true)
    }

    fun readMetas(): Map<File, ModuleMeta> {
        val metas = mutableMapOf<File, ModuleMeta>()
        modulesDir.listFiles()?.filter { it.isFile }?.forEach {
            try {
                moduleLoader.readMeta(it)?.let { meta -> metas.put(it, meta) }
            } catch (_: Throwable) {}
        }
        return metas.toMap()
    }

    override fun dispose() {
        loaded.forEach {
            try {
                it.instance.disable()
            } catch (_: Throwable) { }
        }
    }
}
