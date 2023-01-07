package com.github.devlaq.arkitect

import arc.ApplicationListener
import arc.Core
import com.github.devlaq.arkitect.command.registerCommands
import com.github.devlaq.arkitect.core.module.Modules
import com.github.devlaq.arkitect.i18n.I18N
import com.github.devlaq.arkitect.util.DataFile
import com.github.devlaq.arkitect.util.Logger
import mindustry.mod.Plugin
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@Suppress("unused")
class Arkitect: Plugin() {

    private val logger = Logger("Arkitect")

    @OptIn(ExperimentalTime::class)
    override fun init() {
        Locale.setDefault(Locale(Locale.getDefault().language))

        Core.app.addListener(object : ApplicationListener {
            override fun dispose() {
                this@Arkitect.dispose()
            }
        })

        I18N.loadBundles(
            Locale.KOREAN,
            Locale.ENGLISH
        )

        logger.infoT("arkitect.starting")

        val timeToInitialize = measureTime {
            registerCommands()


            Modules.loadDirectory(DataFile("modules", mkdirs = true))
        }

        logger.infoT("arkitect.started", timeToInitialize)
    }

    fun dispose() {
        logger.infoT("arkitect.stopping")

        Modules.dispose()
    }

}
