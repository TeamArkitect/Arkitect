package com.github.devlaq.arkitect

import arc.Events
import arc.func.Cons

abstract class ServerEventListener<T>: Cons<T> {

    abstract val type: Class<T>

    fun remove() {
        Events.remove(type, this)
    }

}

object EventManager {

    val eventListeners = mutableMapOf<Class<out Any>, MutableList<ServerEventListener<out Any>>>()

    inline fun <reified T : Any> register(clazz: Class<out Any>, crossinline listener: (T) -> Unit) {
        val serverEventListener = object : ServerEventListener<T>() {
            override val type = T::class.java
            override fun get(t: T) = listener(t)
        }

        if(eventListeners.containsKey(clazz)) {
            eventListeners[clazz]?.add(serverEventListener)
        } else {
            eventListeners[clazz] = mutableListOf(serverEventListener)
        }

        Events.on(serverEventListener.type, serverEventListener)
    }

    fun removeForClass(clazz: Class<out Any>) {
        eventListeners[clazz]?.forEach(ServerEventListener<out Any>::remove)
    }

    fun removeAll() {
        eventListeners.flatMap { it.value }.forEach { it.remove() }
    }

    fun dispose() {
        removeAll()
    }

}
