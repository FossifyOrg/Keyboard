package org.fossify.keyboard.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.emoji2.text.EmojiCompat
import androidx.recyclerview.widget.RecyclerView
import org.fossify.commons.databinding.DividerBinding
import org.fossify.commons.extensions.beInvisible
import org.fossify.keyboard.databinding.ItemEmojiBinding
import org.fossify.keyboard.helpers.EmojiData

class EmojisAdapter(val context: Context, private val items: List<Item>, val itemClick: (emoji: EmojiData) -> Unit) :
    RecyclerView.Adapter<EmojisAdapter.ViewHolder>() {
    private val layoutInflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EmojisAdapter.ViewHolder {
        return when (viewType) {
            ITEM_TYPE_EMOJI -> {
                val view = ItemEmojiBinding.inflate(layoutInflater, parent, false).root
                ViewHolder(view)
            }

            else -> {
                val view = DividerBinding.inflate(layoutInflater, parent, false).root.apply { beInvisible() }
                ViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: EmojisAdapter.ViewHolder, position: Int) {
        val item = items[position]
        if (item is Item.Emoji) {
            holder.bindView(item) { itemView ->
                setupEmoji(itemView, item)
            }
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

    private fun setupEmoji(view: View, emoji: Item.Emoji) {
        val processed = EmojiCompat.get().process(emoji.value.emoji)
        ItemEmojiBinding.bind(view).emojiValue.text = processed
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bindView(emoji: Item.Emoji, callback: (itemView: View) -> Unit): View {
            return itemView.apply {
                callback(this)

                setOnClickListener {
                    itemClick.invoke(emoji.value)
                }
            }
        }
    }

    sealed interface Item {
        data class Emoji(val value: EmojiData) : Item
        data class Category(val value: String) : Item
    }

    companion object {
        private const val ITEM_TYPE_EMOJI = 0
        private const val ITEM_TYPE_CATEGORY = 1
    }
}
