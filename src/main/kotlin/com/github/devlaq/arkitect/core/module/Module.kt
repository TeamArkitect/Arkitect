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
            if(!noLog) logger.infoln("<%module.unload%>")
            val module = loaded.firstOrNull { it.file.canonicalPath == file.canonicalPath } ?: return
            module.disable()
            loaded.remove(module)
        } catch (_: Exception) {}
    }

    fun reload(file: File, noLog: Boolean = false) {
        if(!noLog) logger.infoln("<%module.reload%>")
        unload(file, noLog = noLog)
        load(file, noLog = noLog)
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

/*
object Modules {

    private val logger = Logger("Arkitect/Modules")
    private val moduleClassLoader = ModClassLoader(javaClass.classLoader)

    val modulesDirectory = DataFile("modules", mkdirs = true)
    val loadedModules = mutableMapOf<String, Pair<ArkitectModule, ModuleMeta>>()

    private var watcherJob: Job? = null

    fun init() {
        if(Arkitect.settings.watchModules) {
            watchModules()
            logger.infoT("module.watching", modulesDirectory)
        }

        loadDirectory(modulesDirectory)
    }

    fun watchModules() {
        watcherJob = CoroutineScope(Dispatchers.IO).launch {
            val watchService = withContext(Dispatchers.IO) {
                FileSystems.getDefault().newWatchService()
            }

            withContext(Dispatchers.IO) {
                modulesDirectory.toPath().register(
                    watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY
                )
            }

            while (watcherJob != null) {
                val key = withContext(Dispatchers.IO) {
                    watchService.take()
                }

                val events = key.pollEvents()

                val loaded = mutableListOf<String>()
                val unloaded = mutableListOf<String>()
                val reloaded = mutableListOf<String>()
                events.forEach {
                    val kind = it.kind()
                    val path = it.context() as Path
                    val file = File(modulesDirectory, path.name)

                    when(kind) {
                        StandardWatchEventKinds.ENTRY_CREATE -> {
                            if(file.extension == "jar") {
                                val (instance, meta) = load(file) ?: return@forEach
                                if(loadedModules.values.map { it.second.name }.contains(meta.name)) return@forEach logger.errorT("module.load_fail.already_loaded")
                                loadedModules[file.name] = instance to meta
                                instance.load()
                                loaded.add(file.name)
                            }
                        }
                        StandardWatchEventKinds.ENTRY_DELETE -> {
                            if(loadedModules.keys.contains(file.name)) {
                                unload(file)
                                unloaded.add(file.name)
                            }
                        }
                        StandardWatchEventKinds.ENTRY_MODIFY -> {
                            if(loadedModules.keys.contains(file.name)) {
                                unload(file)
                                load(file)
                                reloaded.add(file.name)
                            }
                        }
                    }
                }

                if(!key.reset()) break

                logger.infoT("module.changed", loaded.size, unloaded.size, reloaded.size)
            }
        }
    }

    private fun checkFile(file: File): Boolean {
        return file.isFile && file.extension == "jar"
    }

    /**
     * Load all mods in the directory.
     */
    fun loadDirectory(directory: File) {
        if(!directory.isDirectory) throw RuntimeException("Provided file is not a directory!")

        var moduleFiles = 0
        var successfullyLoaded = 0
        directory.listFiles { file -> checkFile(file) }?.forEach { file ->
            moduleFiles++
            val (instance, meta) = load(file) ?: return
            if(loadedModules.values.map { it.second.name }.contains(meta.name)) return logger.errorT("module.load_fail.already_loaded", meta.name)
            loadedModules[file.name] = instance to meta
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

    fun load(file: File, dontCheck: Boolean = false): Pair<ArkitectModule, ModuleMeta>? {
        if(!checkFile(file) && !dontCheck) return null

        lateinit var zipFile: ZipFi
        fun validateJar(): Boolean {
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

    fun load(fileName: String) = load(File(modulesDirectory, fileName))

    fun unload(file: File) {
        loadedModules.remove(file.name)?.first?.unload()
    }

    fun unload(fileName: String) = unload(File(modulesDirectory, fileName))

    fun dispose() {
        watcherJob = null

        loadedModules.values.forEach {
            it.first.unload()
        }
    }

}
*/
