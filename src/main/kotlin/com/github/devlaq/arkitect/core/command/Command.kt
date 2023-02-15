package com.github.devlaq.arkitect.core.command

class Command(
    val name: String,
    val description: String?,
    val handler: Handler,
    val completer: Completer? = null,
    val scope: Scope = Scope.All
) {

    interface Handler {
        fun handle(context: HandlerContext)
    }

    class HandlerContext(
        val name: String,
        val args: Array<String>,
        val caller: CommandCaller
    )

    interface Completer {
        fun complete(context: CompleterContext): List<String>?
    }

    class CompleterContext(
        val name: String,
        val rawString: String,
        val args: Array<String>,
        val current: String?
    )

    enum class Scope {
        Server,
        Client,
        All
    }

}
