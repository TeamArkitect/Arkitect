package com.github.devlaq.arkitect

import com.github.devlaq.arkitect.command.registerCommands
import com.github.devlaq.arkitect.i18n.I18N
import com.github.devlaq.arkitect.util.Logger
import mindustry.mod.Plugin
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@Suppress("unused")
class Arkitect: Plugin() {

    val logger = Logger("Arkitect")

    @OptIn(ExperimentalTime::class)
    override fun init() {
        logger.info("Loading bundles...")
        I18N.loadBundles()

        logger.infoT("arkitect.starting")

        val timeToInitialize = measureTime {
            registerCommands()
        }

        logger.infoT("arkitect.started", timeToInitialize)
    }

}