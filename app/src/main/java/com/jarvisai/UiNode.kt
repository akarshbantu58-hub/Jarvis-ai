package com.jarvisai

/** Clean representation of an accessibility node exposed to the agent layer. */
data class UiNode(
    val id: String? = null,
    val className: String? = null,
    val text: String? = null,
    val contentDescription: String? = null,
    val clickable: Boolean = false,
    val enabled: Boolean = false,
    val scrollable: Boolean = false,
    val bounds: String? = null,
    val children: List<UiNode> = emptyList()
)
