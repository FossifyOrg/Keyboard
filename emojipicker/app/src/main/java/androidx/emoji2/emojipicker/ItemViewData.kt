

package com.rishabh.emojipicker

internal enum class ItemType {
    CATEGORY_TITLE,
    PLACEHOLDER_TEXT,
    EMOJI,
}

/** Represents an item within the body RecyclerView. */
internal sealed class ItemViewData(val itemType: ItemType) {
    val viewType = itemType.ordinal
}

/** Title of each category. */
internal data class CategoryTitle(val title: String) : ItemViewData(ItemType.CATEGORY_TITLE)

/** Text to display when the category contains no items. */
internal data class PlaceholderText(val text: String) : ItemViewData(ItemType.PLACEHOLDER_TEXT)

/** Represents an emoji. */
internal data class EmojiViewData(
    var emoji: String,
    val updateToSticky: Boolean = true,
    // Needed to ensure uniqueness since we enabled stable Id.
    val dataIndex: Int = 0
) : ItemViewData(ItemType.EMOJI)

internal object Extensions {
    internal fun Int.toItemType() = ItemType.values()[this]
}
