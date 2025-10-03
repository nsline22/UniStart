package com.nsline22.UniStart

object CommandManager {
    private var pendingCommand: String? = null

    @Synchronized
    fun setCommand(command: String) {
        pendingCommand = command
    }

    @Synchronized
    fun getCommand(): String? {
        val command = pendingCommand
        pendingCommand = null
        return command
    }

    @Synchronized
    fun hasPendingCommand(): Boolean {
        return pendingCommand != null
    }
}