

package com.rishabh.emojipicker

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.annotation.UiThread
import androidx.core.view.ViewCompat

import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.rishabh.emojipicker.Extensions.toItemType

/** RecyclerView adapter for emoji body. */
internal class EmojiPickerBodyAdapter(
    private val context: Context,
    private val emojiGridColumns: Int,
    private val emojiGridRows: Float?,
    private val stickyVariantProvider: StickyVariantProvider,
    private val emojiPickerItemsProvider: () -> EmojiPickerItems,
    private val onEmojiPickedListener: EmojiPickerBodyAdapter.(EmojiViewItem) -> Unit,
) : Adapter<ViewHolder>() {
    private val layoutInflater: LayoutInflater = LayoutInflater.from(context)
    private var emojiCellWidth: Int? = null
    private var emojiCellHeight: Int? = null

    @UiThread
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        emojiCellWidth = emojiCellWidth ?: (getParentWidth(parent) / emojiGridColumns)
        emojiCellHeight =
            emojiCellHeight
                ?: emojiGridRows?.let { getEmojiCellTotalHeight(parent) / it }?.toInt()
                ?: emojiCellWidth

        return when (viewType.toItemType()) {
            ItemType.CATEGORY_TITLE -> createSimpleHolder(R.layout.category_text_view, parent)
            ItemType.PLACEHOLDER_TEXT ->
                createSimpleHolder(R.layout.empty_category_text_view, parent) {
                    minimumHeight = emojiCellHeight!!
                }
            ItemType.EMOJI -> {
                EmojiViewHolder(
                    context,
                    emojiCellWidth!!,
                    emojiCellHeight!!,
                    stickyVariantProvider,
                    onEmojiPickedListener = { emojiViewItem ->
                        onEmojiPickedListener(emojiViewItem)
                    },
                    onEmojiPickedFromPopupListener = { emoji ->
                        val baseEmoji = BundledEmojiListLoader.getEmojiVariantsLookup()[emoji]!![0]
                        emojiPickerItemsProvider().forEachIndexed { index, itemViewData ->
                            if (
                                itemViewData is EmojiViewData &&
                                    BundledEmojiListLoader.getEmojiVariantsLookup()[
                                            itemViewData.emoji]
                                        ?.get(0) == baseEmoji &&
                                    itemViewData.updateToSticky
                            ) {
                                itemViewData.emoji = emoji
                                notifyItemChanged(index)
                            }
                        }
                    }
                )
            }
        }
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val item = emojiPickerItemsProvider().getBodyItem(position)
        when (getItemViewType(position).toItemType()) {
            ItemType.CATEGORY_TITLE ->
                ViewCompat.requireViewById<TextView>(viewHolder.itemView, R.id.category_name).text =
                    (item as CategoryTitle).title
            ItemType.PLACEHOLDER_TEXT ->
                ViewCompat.requireViewById<TextView>(
                        viewHolder.itemView,
                        R.id.emoji_picker_empty_category_view
                    )
                    .text = (item as PlaceholderText).text
            ItemType.EMOJI -> {
                (viewHolder as EmojiViewHolder).bindEmoji((item as EmojiViewData).emoji)
            }
        }
    }

    override fun getItemId(position: Int): Long =
        emojiPickerItemsProvider().getBodyItem(position).hashCode().toLong()

    override fun getItemCount(): Int {
        return emojiPickerItemsProvider().size
    }

    override fun getItemViewType(position: Int): Int {
        return emojiPickerItemsProvider().getBodyItem(position).viewType
    }

    private fun getParentWidth(parent: ViewGroup): Int {
        return parent.measuredWidth - parent.paddingLeft - parent.paddingRight
    }

    private fun getEmojiCellTotalHeight(parent: ViewGroup) =
        parent.measuredHeight -
            context.resources.getDimensionPixelSize(R.dimen.emoji_picker_category_name_height) * 2 -
            context.resources.getDimensionPixelSize(R.dimen.emoji_picker_category_name_padding_top)

    private fun createSimpleHolder(
        @LayoutRes layoutId: Int,
        parent: ViewGroup,
        init: (View.() -> Unit)? = null,
    ) =
        object :
            ViewHolder(
                layoutInflater.inflate(layoutId, parent, /* attachToRoot= */ false).also {
                    it.layoutParams =
                        LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
                    init?.invoke(it)
                }
            ) {}
}
