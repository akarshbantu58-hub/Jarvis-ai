package com.jarvisai.core

/**
 * Holds a single pending sensitive action. The UI must explicitly approve it
 * before an action executor is allowed to run it.
 */
class ConfirmationManager {
    private var pendingCommand: String? = null

    fun request(command: String): CommandRisk {
        pendingCommand = command.trim().takeIf { it.isNotBlank() }
        return pendingCommand?.let { CommandPolicy().classify(it) } ?: CommandRisk.SAFE
    }

    fun pending(): String? = pendingCommand

    fun approve(): String? = pendingCommand.also { pendingCommand = null }

    fun reject() {
        pendingCommand = null
    }
}
