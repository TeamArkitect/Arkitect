package com.github.devlaq.arkitect

import arc.ApplicationListener
import arc.Core
import arc.util.CommandHandler
import com.github.devlaq.arkitect.command.registerCommands
import com.github.devlaq.arkitect.core.module.Modules
import com.github.devlaq.arkitect.i18n.BundleManager
import com.github.devlaq.arkitect.util.DataFile
import com.github.devlaq.arkitect.util.console.Logger
import com.github.devlaq.arkitect.util.console.Logging
import mindustry.mod.Plugin
import java.util.*
import kotlin.time.ExperimentalTime
import kotlin.time.measureTime

@Suppress("unused")
class Arkitect: Plugin() {

    private val logger = createLogger("Arkitect")

    companion object {
        val bundleManager = BundleManager()
        var settings: ArkitectSettings? = null

        fun createLogger(tag: String) = Logger(tag, bundleManager)
    }

    @OptIn(ExperimentalTime::class)
    override fun init() {
        Core.app.addListener(object : ApplicationListener {
            override fun dispose() {
                this@Arkitect.dispose()
            }
        })

        Runtime.getRuntime().addShutdownHook(Thread(::dispose))

        bundleManager.loadBundles(
            Locale.KOREAN,
            Locale.ENGLISH
        )

        bundleManager.localeProvider = {
            Locale.forLanguageTag(settings?.locale ?: Locale.getDefault().toLanguageTag())
        }

        registerCommands()

        Logging.init()

        logger.info("<%arkitect.starting%>")

        val timeToInitialize = measureTime {
            settings = ArkitectSettings.load(DataFile("settings.json"))

            Modules.init()
        }

        logger.info("<%arkitect.started%>", timeToInitialize)
    }

    fun dispose() {
        logger.info("<%arkitect.stopping%>")

        Modules.dispose()
        EventManager.dispose()

        settings?.save(DataFile("settings.json"))
    }

    override fun registerClientCommands(handler: CommandHandler?) {
        super.registerClientCommands(handler)
    }

    override fun registerServerCommands(handler: CommandHandler?) {
        super.registerServerCommands(handler)
    }

}
