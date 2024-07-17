package org.fossify.keyboard.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.emoji2.text.EmojiCompat
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.extensions.adjustAlpha
import org.fossify.commons.extensions.getProperTextColor
import org.fossify.keyboard.databinding.ItemEmojiBinding
import org.fossify.keyboard.databinding.ItemEmojiCategoryTitleBinding
import org.fossify.keyboard.helpers.EmojiData
import org.fossify.keyboard.helpers.getCategoryTitleRes

class EmojisAdapter(
    val context: Context,
    val items: List<Item>,
    val itemClick: (emoji: EmojiData) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val layoutInflater = LayoutInflater.from(context)
    private val textColor = context.getProperTextColor()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_TYPE_EMOJI -> EmojiViewHolder(
                ItemEmojiBinding.inflate(layoutInflater, parent, false).root
            )

            ITEM_TYPE_CATEGORY -> EmojiCategoryViewHolder(
                ItemEmojiCategoryTitleBinding.inflate(layoutInflater, parent, false).root
            )

            else -> throw IllegalArgumentException("Unsupported view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is EmojiViewHolder -> holder.bindView(item as Item.Emoji)
            is EmojiCategoryViewHolder -> holder.bindView(item as Item.Category)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position] is Item.Emoji) {
            ITEM_TYPE_EMOJI
        } else {
            ITEM_TYPE_CATEGORY
        }
    }

    override fun getItemCount() = items.size

    inner class EmojiViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindView(emoji: Item.Emoji) {
            val processed = EmojiCompat.get().process(emoji.emojiData.emoji)
            itemView.apply {
                ItemEmojiBinding.bind(this).emojiValue.text = processed
                setOnClickListener {
                    itemClick.invoke(emoji.emojiData)
                }
            }
        }
    }

    inner class EmojiCategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindView(category: Item.Category) {
            ItemEmojiCategoryTitleBinding.bind(itemView).emojiCategoryTitle.apply {
                text = context.getString(getCategoryTitleRes(category.value))
                setTextColor(textColor.adjustAlpha(0.6f))
            }
        }
    }

    sealed interface Item {
        data class Emoji(val emojiData: EmojiData) : Item
        data class Category(val value: String) : Item
    }

    companion object {
        private const val ITEM_TYPE_EMOJI = 0
        private const val ITEM_TYPE_CATEGORY = 1
    }
}
