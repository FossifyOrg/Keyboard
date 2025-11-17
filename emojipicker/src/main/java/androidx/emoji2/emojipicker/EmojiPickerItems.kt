

package com.rishabh.emojipicker

import androidx.annotation.DrawableRes
import androidx.annotation.IntRange

/**
 * A group of items in RecyclerView for emoji picker body. [titleItem] comes first. [listOfAllEmojiInAGroup]
 * comes after [titleItem]. [emptyPlaceholderItem] will be served after [titleItem] only if
 * [listOfAllEmojiInAGroup] is empty. [maxContentItemCount], if provided, will truncate [listOfAllEmojiInAGroup] to
 * certain size.
 *
 * [categoryIconId] is the corresponding category icon in emoji picker header.
 */
class ItemGroup(
    @DrawableRes internal val categoryIconId: Int,
    internal val titleItem: CategoryTitle,
    private val listOfAllEmojiInAGroup: List<EmojiViewData>,
    private val maxContentItemCount: Int? = null,
    private val emptyPlaceholderItem: PlaceholderText? = null
) {

    val size: Int
        get() {
            var totalSize = 1 // Start with the title item

            // Check for empty content items and adjust size accordingly
            if (listOfAllEmojiInAGroup.isEmpty()) {
                totalSize += if (emptyPlaceholderItem!= null) 1 else 0
            } else {
                // Adjust size based on maxContentItemCount if applicable
                if (maxContentItemCount!= null && listOfAllEmojiInAGroup.size > maxContentItemCount) {
                    totalSize = maxContentItemCount
                } else {
                    totalSize += listOfAllEmojiInAGroup.size // Add the actual number of content items
                }
            }

            return totalSize
        }

    operator fun get(index: Int): ItemViewData {
        if (index == 0) return titleItem
        val contentIndex = index - 1
        if (contentIndex < listOfAllEmojiInAGroup.size) return listOfAllEmojiInAGroup[contentIndex]
        if (contentIndex == 0 && emptyPlaceholderItem != null) return emptyPlaceholderItem
        throw IndexOutOfBoundsException()
    }

    fun getAll(): List<ItemViewData> = IntRange(0, size - 1).map { get(it) }
}

/** A view of concatenated list of [ItemGroup]. */
class EmojiPickerItems(private val groups: List<ItemGroup> ) : Iterable<ItemViewData> {
    val size: Int
        get() = groups.sumOf { it.size }



    fun getBodyItem(@IntRange(from = 0) absolutePosition: Int): ItemViewData {
        var localPosition = absolutePosition
        for (group in groups) {

            if (localPosition < group.size) return group[localPosition]
            else localPosition -= group.size
        }
        throw IndexOutOfBoundsException()
    }


    fun getRecentListIfExist(): ItemGroup{

        return if (groups[0].titleItem.title == "RECENTLY USED") {
            groups[0]
        } else {
            groups[1]
        }

    }

    val numGroups: Int
        get() = groups.size

    @DrawableRes
    fun getHeaderIconId(@IntRange(from = 0) index: Int): Int = groups[index].categoryIconId

    fun getHeaderIconDescription(@IntRange(from = 0) index: Int): String =
        groups[index].titleItem.title

    fun groupIndexByItemPosition(@IntRange(from = 0) absolutePosition: Int): Int {
        var localPosition = absolutePosition
        var index = 0
        for (group in groups) {
            if (localPosition < group.size) return index
            else {
                localPosition -= group.size
                index++
            }
        }
        throw IndexOutOfBoundsException()
    }

    fun firstItemPositionByGroupIndex(@IntRange(from = 0) groupIndex: Int): Int =
        groups.take(groupIndex).sumOf { it.size }

    fun groupRange(group: ItemGroup): kotlin.ranges.IntRange {
        check(groups.contains(group))
        val index = groups.indexOf(group)
        return firstItemPositionByGroupIndex(index).let { it until it + group.size }
    }

    override fun iterator(): Iterator<ItemViewData> = groups.flatMap { it.getAll() }.iterator()
}
