package com.github.devlaq.arkitect.core.command

class CommandBuilder {

    var name: String? = null
    var description: String? = null
    var scope = Command.Scope.All

    var handler: (Command.HandlerContext.() -> Unit)? = null
    var completer: (Command.CompleterContext.() -> List<String>?)? = null

    fun handler(body: Command.HandlerContext.() -> Unit) {
        this.handler = body
    }

    fun completer(body: Command.CompleterContext.() -> List<String>?) {
        this.completer = body
    }

}
