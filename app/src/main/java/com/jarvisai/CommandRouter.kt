package com.jarvisai

/** Routes normalized intents to the appropriate application layer. */
class CommandRouter(
    private val intentEngine: IntentEngine = IntentEngine()
) {
    sealed interface Route {
        data class Local(val intent: JarvisIntent) : Route
        data class Ai(val intent: JarvisIntent) : Route
        data class Navigation(val intent: JarvisIntent) : Route
        data object Empty : Route
    }

    fun route(command: String): Route {
        val intent = intentEngine.classify(command)
        return when (intent.type) {
            JarvisIntent.Type.EMPTY -> Route.Empty
            JarvisIntent.Type.AI_QUERY -> Route.Ai(intent)
            JarvisIntent.Type.LOCAL_ACTION -> Route.Local(intent)
            JarvisIntent.Type.OPEN_SETTINGS,
            JarvisIntent.Type.OPEN_AUTOMATION,
            JarvisIntent.Type.OPEN_API_HUB,
            JarvisIntent.Type.DEFAULT_ASSISTANT -> Route.Navigation(intent)
        }
    }
}
