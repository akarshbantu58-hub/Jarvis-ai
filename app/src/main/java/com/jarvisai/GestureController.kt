package com.jarvisai

/** Accessibility-backed gesture executor. It cannot work until the user enables the service. */
class GestureController {
    private val service get() = JarvisAccessibilityService.instance

    fun clickText(text: String): Boolean = service?.clickText(text) == true
    fun typeText(text: String): Boolean = service?.typeText(text) == true
    fun scrollForward(): Boolean = service?.scrollForward() == true
    fun scrollBackward(): Boolean = service?.scrollBackward() == true
    fun back(): Boolean = service?.performBack() == true
    fun home(): Boolean = service?.performHome() == true
    fun swipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long = 350L): Boolean =
        service?.swipe(x1, y1, x2, y2, durationMs) == true
}
