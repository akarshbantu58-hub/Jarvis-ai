package com.jarvisai

import android.content.Context

object RuntimeSettings {
    enum class Mode { SAVER, NORMAL, PERFORMANCE }
    private const val PREFS = "jarvis_runtime"
    private const val MODE = "mode"
    private const val REDUCE_MOTION = "reduce_motion"
    private const val BUBBLES = "bubbles"

    fun mode(context: Context): Mode = runCatching {
        Mode.valueOf(context.getSharedPreferences(PREFS, 0).getString(MODE, Mode.NORMAL.name) ?: Mode.NORMAL.name)
    }.getOrDefault(Mode.NORMAL)

    fun setMode(context: Context, mode: Mode) { context.getSharedPreferences(PREFS, 0).edit().putString(MODE, mode.name).apply() }
    fun reduceMotion(context: Context): Boolean = context.getSharedPreferences(PREFS, 0).getBoolean(REDUCE_MOTION, false)
    fun setReduceMotion(context: Context, value: Boolean) { context.getSharedPreferences(PREFS, 0).edit().putBoolean(REDUCE_MOTION, value).apply() }
    fun bubbles(context: Context): Boolean = context.getSharedPreferences(PREFS, 0).getBoolean(BUBBLES, true)
    fun setBubbles(context: Context, value: Boolean) { context.getSharedPreferences(PREFS, 0).edit().putBoolean(BUBBLES, value).apply() }
}
