package com.github.devlaq.arkitect.core.module

import arc.util.Disposable
import com.github.devlaq.arkitect.Arkitect
import com.github.devlaq.arkitect.util.DataFile
import java.io.File

// Rework
object Modules: Disposable {

    private val logger = Arkitect.createLogger("Arkitect/Modules")

    private lateinit var moduleLoader: ModuleLoader
    private lateinit var moduleWatcher: ModulesWatcher

    val modulesDir = DataFile("modules", mkdirs = true)
    val loaded = mutableListOf<LoadedModuleMeta>()

    fun init(moduleLoaderOverride: ModuleLoader? = null) {
        moduleLoader = moduleLoaderOverride ?: DefaultModuleLoader()
        moduleWatcher = ModulesWatcher(modulesDir)

        loadDirectory(modulesDir)

        if(Arkitect.settings!!.modules.watchModules) watchModules()
    }

    fun watchModules() {
        moduleWatcher.watch()
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
            logger.error("<%module.already_loaded%>", file.name)
            return null
        }

        if(!noLog) logger.info("<%module.load%>... ", file.name)
        return try {
            val loadedMeta = moduleLoader.load(file)
            try {
                loadedMeta.enable()
            } catch (t: Throwable) {
                loadedMeta.disable()
                throw t
            }
            loaded.add(loadedMeta)
            loadedMeta
        } catch (t: Throwable) {
            var logStr = "<%fail%>"
            if(Arkitect.settings!!.modules.fail == "message") logStr += "<red>(${t.localizedMessage})</>"
            if(Arkitect.settings!!.modules.fail == "stacktrace") logStr += "<red>(\n${t.stackTraceToString()})</>"
            if(!noLog) logger.error(logStr, file.name)
            null
        }
    }

    fun unload(file: File, noLog: Boolean = false) {
        if(!file.isFile) return

        try {
            if(!noLog) logger.info("<%module.unload%>", file.name)
            val module = loaded.firstOrNull { it.file.canonicalPath == file.canonicalPath } ?: return
            module.disable()
            loaded.remove(module)
        } catch (_: Exception) {}
    }

    fun reload(file: File, noLog: Boolean = false) {
        if(!noLog) logger.info("<%module.reload%>", file.name)
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

    inline fun <reified T> get(): LoadedModuleMeta? {
        return loaded.firstOrNull { it.instance is T }
    }

    override fun dispose() {
        loaded.forEach {
            try {
                it.instance.disable()
            } catch (_: Throwable) { }
        }
    }
}
