package com.jarvisai.core

import java.util.Locale

/**
 * Central safety gate for voice commands.
 * This class classifies intent only; it never bypasses Android permissions.
 */
class CommandPolicy {
    fun classify(command: String): CommandRisk {
        val c = command.trim().lowercase(Locale.ROOT)
        if (c.isBlank()) return CommandRisk.SAFE

        val dangerousPatterns = listOf(
            "factory reset",
            "wipe device",
            "erase all data",
            "delete all files",
            "disable security"
        )
        if (dangerousPatterns.any(c::contains)) return CommandRisk.DANGEROUS

        val sensitivePatterns = listOf(
            "send message",
            "send sms",
            "send email",
            "make a call",
            "call ",
            "delete note",
            "delete conversation",
            "change password",
            "change account",
            "turn on do not disturb",
            "turn off do not disturb"
        )
        if (sensitivePatterns.any(c::contains)) return CommandRisk.SENSITIVE

        return CommandRisk.SAFE
    }

    fun requiresConfirmation(command: String): Boolean =
        classify(command) != CommandRisk.SAFE
}
