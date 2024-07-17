package org.fossify.keyboard.helpers

import android.content.Context
import org.fossify.keyboard.R
import org.json.JSONObject
import java.io.InputStream

private var cachedEmojiData: MutableList<EmojiData>? = null
val cachedVNTelexData: HashMap<String, String> = HashMap()

/**
 * Reads the emoji list at the given [path] and returns an parsed [MutableList]. If the
 * given file path does not exist, an empty [MutableList] is returned.
 *
 * @param context The initiating view's context.
 * @param path The path to the asset file.
 */
fun parseRawEmojiSpecsFile(context: Context, path: String): MutableList<EmojiData> {
    if (cachedEmojiData != null) {
        return cachedEmojiData!!
    }

    val emojis = mutableListOf<EmojiData>()
    var emojiEditorList: MutableList<String>? = null
    var category: String? = null

    fun commitEmojiEditorList() {
        emojiEditorList?.let {
            // add only the base emoji for now, ignore the variations
            val base = it.first()
            val variants = it.drop(1)
            emojis.add(EmojiData(category ?: "none", base, variants))
        }
        emojiEditorList = null
    }

    context.assets.open(path).bufferedReader().useLines { lines ->
        for (line in lines) {
            if (line.startsWith("#")) {
                // Comment line
            } else if (line.startsWith("[")) {
                commitEmojiEditorList()
                category = line.replace("[", "").replace("]", "")
            } else if (line.trim().isEmpty()) {
                // Empty line
                continue
            } else {
                if (!line.startsWith("\t")) {
                    commitEmojiEditorList()
                }

                // Assume it is a data line
                val data = line.split(";")
                if (data.size == 3) {
                    val emoji = data[0].trim()
                    if (emojiEditorList != null) {
                        emojiEditorList!!.add(emoji)
                    } else {
                        emojiEditorList = mutableListOf(emoji)
                    }
                }
            }
        }
        commitEmojiEditorList()
    }

    cachedEmojiData = emojis
    return emojis
}

fun parseRawJsonSpecsFile(context: Context, path: String): HashMap<String, String> {
    if (cachedVNTelexData.isNotEmpty()) {
        return cachedVNTelexData
    }

    try {
        val inputStream: InputStream = context.assets.open(path)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val jsonData = JSONObject(jsonString)
        val rulesObj = jsonData.getJSONObject("rules")
        val ruleKeys = rulesObj.keys()
        while (ruleKeys.hasNext()) {
            val key = ruleKeys.next()
            val value = rulesObj.getString(key)
            cachedVNTelexData[key] = value
        }
    } catch (ignored: Exception) {
        return HashMap()
    }
    return cachedVNTelexData
}

data class EmojiData(
    val category: String,
    val emoji: String,
    val variants: List<String>
)

fun getCategoryIconRes(category: String): Int =
    when (category) {
        "smileys_emotion" -> R.drawable.ic_emoji_category_smileys
        "people_body" -> R.drawable.ic_emoji_category_people
        "animals_nature" -> R.drawable.ic_emoji_category_animals
        "food_drink" -> R.drawable.ic_emoji_category_food
        "travel_places" -> R.drawable.ic_emoji_category_travel
        "activities" -> R.drawable.ic_emoji_category_activities
        "objects" -> R.drawable.ic_emoji_category_objects
        "symbols" -> R.drawable.ic_emoji_category_symbols
        "flags" -> R.drawable.ic_emoji_category_flags
        else -> R.drawable.ic_clock_filled_vector
    }


fun getCategoryTitleRes(category: String) =
    when (category) {
        "smileys_emotion" -> R.string.smileys_and_emotions
        "people_body" -> R.string.people_and_body
        "animals_nature" -> R.string.animals_and_nature
        "food_drink" -> R.string.food_and_drink
        "travel_places" -> R.string.travel_and_places
        "activities" -> R.string.activities
        "objects" -> R.string.objects
        "symbols" -> R.string.symbols
        "flags" -> R.string.flags
        else -> R.string.recently_used
    }
