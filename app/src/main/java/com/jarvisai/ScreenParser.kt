package com.jarvisai

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Converts the active Android accessibility tree into bounded, privacy-aware data.
 *
 * Only nodes exposed by Android are visible. Password fields are never copied
 * into the snapshot, and traversal is bounded to avoid runaway trees.
 */
object ScreenParser {
    private const val MAX_DEPTH = 20
    private const val MAX_NODES = 500
    private const val MAX_TEXT_LENGTH = 400

    fun parse(root: AccessibilityNodeInfo?): UiNode? {
        if (root == null) return null
        val budget = TraversalBudget()
        return parseNode(root, depth = 0, budget = budget)
    }

    private class TraversalBudget(var visited: Int = 0)

    private fun parseNode(
        node: AccessibilityNodeInfo,
        depth: Int,
        budget: TraversalBudget
    ): UiNode? {
        if (depth > MAX_DEPTH || budget.visited >= MAX_NODES) return null
        budget.visited++

        val bounds = Rect().also(node::getBoundsInScreen)
        val isPassword = runCatching { node.isPassword }.getOrDefault(false)
        val safeText = if (isPassword) null else sanitize(node.text?.toString())
        val safeDescription = if (isPassword) null else sanitize(node.contentDescription?.toString())

        val children = buildList {
            if (depth < MAX_DEPTH && budget.visited < MAX_NODES) {
                for (i in 0 until node.childCount) {
                    if (budget.visited >= MAX_NODES) break
                    val child = runCatching { node.getChild(i) }.getOrNull() ?: continue
                    val parsed = runCatching { parseNode(child, depth + 1, budget) }.getOrNull()
                    if (parsed != null) add(parsed)
                }
            }
        }

        return UiNode(
            id = sanitize(node.viewIdResourceName),
            className = sanitize(node.className?.toString()),
            text = safeText,
            contentDescription = safeDescription,
            clickable = node.isClickable,
            enabled = node.isEnabled,
            scrollable = node.isScrollable,
            bounds = "${bounds.left},${bounds.top},${bounds.right},${bounds.bottom}",
            children = children
        )
    }

    private fun sanitize(value: String?): String? {
        if (value.isNullOrBlank()) return null
        return value.replace(Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]"), "")
            .take(MAX_TEXT_LENGTH)
    }

    fun flatten(root: UiNode?): List<UiNode> {
        if (root == null) return emptyList()
        return buildList {
            fun visit(node: UiNode) {
                if (size >= MAX_NODES) return
                add(node)
                node.children.forEach { child ->
                    if (size < MAX_NODES) visit(child)
                }
            }
            visit(root)
        }
    }
}
