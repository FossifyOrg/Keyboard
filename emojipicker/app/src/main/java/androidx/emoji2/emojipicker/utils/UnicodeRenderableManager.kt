

package com.rishabh.emojipicker.utils

import android.os.Build
import android.text.TextPaint
import androidx.annotation.VisibleForTesting
import androidx.core.graphics.PaintCompat
import com.rishabh.emojipicker.EmojiPickerView
import androidx.emoji2.text.EmojiCompat

/** Checks renderability of unicode characters. */
internal object UnicodeRenderableManager {

    private const val VARIATION_SELECTOR = "\uFE0F"

    private const val YAWNING_FACE_EMOJI = "\uD83E\uDD71"

    private val paint = TextPaint()

    /**
     * Some emojis were usual (non-emoji) characters. Old devices cannot render them with variation
     * selector (U+FE0F) so it's worth trying to check renderability again without variation
     * selector.
     */
    private val CATEGORY_MOVED_EMOJIS =
        listOf( // These three characters have been emoji since Unicode emoji version 4.
            // version 3: https://unicode.org/Public/emoji/3.0/emoji-data.txt
            // version 4: https://unicode.org/Public/emoji/4.0/emoji-data.txt
            "\u2695\uFE0F", // STAFF OF AESCULAPIUS
            "\u2640\uFE0F", // FEMALE SIGN
            "\u2642\uFE0F", // MALE SIGN
            // These three characters have been emoji since Unicode emoji version 11.
            // version 5: https://unicode.org/Public/emoji/5.0/emoji-data.txt
            // version 11: https://unicode.org/Public/emoji/11.0/emoji-data.txt
            "\u265F\uFE0F", // BLACK_CHESS_PAWN
            "\u267E\uFE0F" // PERMANENT_PAPER_SIGN
        )

    /**
     * For a given emoji, check it's renderability with EmojiCompat if enabled. Otherwise, use
     * [PaintCompat#hasGlyph].
     *
     * Note: For older API version, codepoints {@code U+0xFE0F} are removed.
     */
    internal fun isEmojiRenderable(emoji: String) =
        if (EmojiPickerView.emojiCompatLoaded)
            EmojiCompat.get().getEmojiMatch(emoji, Int.MAX_VALUE) == EmojiCompat.EMOJI_SUPPORTED
        else getClosestRenderable(emoji) != null

    // Yawning face is added in emoji 12 which is the first version starts to support gender
    // inclusive emojis.
    internal fun isEmoji12Supported() = isEmojiRenderable(YAWNING_FACE_EMOJI)

    @VisibleForTesting
    fun getClosestRenderable(emoji: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return emoji.replace(VARIATION_SELECTOR, "").takeIfHasGlyph()
        }
        return emoji.takeIfHasGlyph()
            ?: run {
                if (CATEGORY_MOVED_EMOJIS.contains(emoji))
                    emoji.replace(VARIATION_SELECTOR, "").takeIfHasGlyph()
                else null
            }
    }

    private fun String.takeIfHasGlyph() = takeIf { PaintCompat.hasGlyph(paint, this) }
}
