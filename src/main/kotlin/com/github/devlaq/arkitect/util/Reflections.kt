package com.github.devlaq.arkitect.util

import arc.Core
import com.github.devlaq.arkitect.util.console.Logger
import mindustry.server.ServerControl
import java.lang.reflect.Method

fun getServerControl(): ServerControl? {
    Core.app.listeners.forEach {
        if(it is ServerControl) {
            return it
        }
    }
    Logger("Arkitect").warn("Could not find ServerControl instance! (Is the plugin loaded in non-headless server?)")
    return null
}

class ReflectionContext(val `object`: Any) {

    fun <T : Any> declaredField(name: String): T {
        return `object`::class.java.getDeclaredField(name).get(`object`) as T
    }

    fun declaredMethod(name: String, vararg parameterTypes: Class<*>): Method {
        return `object`::class.java.getDeclaredMethod(name, *parameterTypes)
    }

    fun <T> callDeclaredMethod(name: String, vararg args: Any): T {
        val parameterTypes = args.map {
            if (it is NullValue) {
                it.clazz
            } else {
                it::class.java
            }
        }.toTypedArray()
        val mappedParameters = args.map {
            if (it is NullValue) null
            else it
        }.toTypedArray()

        val method = declaredMethod(name, *parameterTypes)

        return method.invoke(`object`, *mappedParameters) as T
    }

    data class NullValue(val clazz: Class<*>)

}

fun <T> Any.useReflection(body: ReflectionContext.() -> T): T {
    return body(ReflectionContext(this))
}
