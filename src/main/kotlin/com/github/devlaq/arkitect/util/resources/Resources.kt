package com.github.devlaq.arkitect.util.resources

fun <T> Class<T>.getResourceAsText(name: String): String? {
    return getResourceAsStream(name)?.bufferedReader()?.readText()
}
