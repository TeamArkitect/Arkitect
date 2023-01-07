package com.github.devlaq.arkitect.test_module

import com.github.devlaq.arkitect.core.module.ArkitectModule
import com.github.devlaq.arkitect.util.Logger

class TestModule: ArkitectModule {

    private val logger = Logger("Module/TestModule")

    override fun load() {
        logger.info("Hello, World")
    }

    override fun unload() {
        logger.info("Bye, World")
    }
}
