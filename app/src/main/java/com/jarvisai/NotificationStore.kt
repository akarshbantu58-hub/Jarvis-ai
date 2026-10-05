package com.jarvisai

/** Process-local notification summaries; raw notification content is not persisted to disk. */
object NotificationStore {
    data class Item(val packageName: String, val title: String, val text: String)

    private const val MAX_ITEMS = 30
    private val items = ArrayDeque<Item>()

    @Synchronized
    fun add(item: Item) {
        items.removeAll { it.packageName == item.packageName && it.title == item.title && it.text == item.text }
        items.addFirst(item)
        while (items.size > MAX_ITEMS) items.removeLast()
    }

    @Synchronized
    fun snapshot(): List<Item> = items.toList()

    @Synchronized
    fun clear() = items.clear()
}
