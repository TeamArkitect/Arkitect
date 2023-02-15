package com.github.devlaq.arkitect.core.module

import com.github.devlaq.arkitect.Arkitect
import dev.vishna.watchservice.KWatchEvent
import dev.vishna.watchservice.asWatchChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.launch
import java.io.File

class ModulesWatcher(val directory: File) {

    val logger = Arkitect.createLogger("Modules/Watcher")

    fun watch() {
        logger.info("<%module.watching%>", directory.path)

        val watchChannel = directory.asWatchChannel()
        CoroutineScope(Dispatchers.Default).launch { watchChannel.consumeEach {
            val isLoaded = Modules.loaded.map { meta -> meta.file.name }.contains(it.file.name)
            when(it.kind) {
                KWatchEvent.Kind.Created -> run {
                    //try load
                    if(isLoaded) return@run
                    Modules.load(it.file)
                }
                KWatchEvent.Kind.Modified -> run {
                    //try reload
                    if(isLoaded) {
                        Modules.reload(it.file)
                    } else {
                        Modules.load(it.file)
                    }
                }
                KWatchEvent.Kind.Deleted -> run {
                    //try unload
                    if(!isLoaded) return@run

                    Modules.unload(it.file)
                }
                else -> {}
            }
        } }
    }

}
