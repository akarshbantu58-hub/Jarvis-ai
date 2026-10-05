package com.jarvisai.voice

import java.util.Locale

/**
 * Small, deterministic command classifier used before an AI provider is called.
 * It does not execute commands; AppActionEngine and the Android APIs remain the
 * execution layer. This keeps voice interpretation separate from device control.
 */
class VoiceCommandEngine {
    enum class Risk { SAFE, SENSITIVE, DANGEROUS, UNKNOWN }

    data class ParsedCommand(
        val original: String,
        val normalized: String,
        val risk: Risk,
        val isLocalCommand: Boolean
    )

    fun parse(command: String): ParsedCommand {
        val normalized = command.trim().lowercase(Locale.getDefault())
        if (normalized.isBlank()) {
            return ParsedCommand(command, normalized, Risk.UNKNOWN, false)
        }

        val dangerous = listOf(
            "factory reset", "wipe device", "delete all data", "erase all data",
            "remove all files", "uninstall all apps"
        )
        val sensitive = listOf(
            "send message", "send sms", "send whatsapp", "make a call", "call ",
            "delete note", "delete file", "delete photo", "change password",
            "turn off security", "disable security"
        )
        val local = listOf(
            "open ", "go home", "go back", "turn on flashlight", "turn off flashlight",
            "battery", "volume", "brightness", "open settings", "take a photo",
            "open camera", "show notifications", "bluetooth", "media", "stop speaking"
        )

        val risk = when {
            dangerous.any(normalized::contains) -> Risk.DANGEROUS
            sensitive.any(normalized::contains) -> Risk.SENSITIVE
            else -> Risk.SAFE
        }

        return ParsedCommand(
            original = command,
            normalized = normalized,
            risk = risk,
            isLocalCommand = local.any(normalized::contains)
        )
    }
}
