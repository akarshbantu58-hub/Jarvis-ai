package com.jarvisai

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/** Converts Android accessibility nodes into a small, LLM-safe data model. */
object ScreenParser {
    fun parse(root: AccessibilityNodeInfo?): UiNode? = root?.let { parseNode(it) }

    private fun parseNode(node: AccessibilityNodeInfo): UiNode {
        val bounds = Rect().also(node::getBoundsInScreen)
        val children = buildList {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                add(parseNode(child))
            }
        }
        return UiNode(
            id = node.viewIdResourceName,
            className = node.className?.toString(),
            text = node.text?.toString(),
            contentDescription = node.contentDescription?.toString(),
            clickable = node.isClickable,
            enabled = node.isEnabled,
            scrollable = node.isScrollable,
            bounds = "${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}",
            children = children
        )
    }

    fun flatten(root: UiNode?): List<UiNode> {
        if (root == null) return emptyList()
        return buildList {
            fun visit(node: UiNode) {
                add(node)
                node.children.forEach(::visit)
            }
            visit(root)
        }
    }
}
