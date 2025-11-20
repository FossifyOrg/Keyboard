package org.fossify.keyboard.helpers

import android.content.Context
import android.util.Log
import org.fossify.keyboard.R
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.lang.IndexOutOfBoundsException

private var cachedEmojiData: MutableList<EmojiData>? = null
val cachedVNTelexData: HashMap<String, String> = HashMap()

private const val MAX_SEARCH_RESULTS = 18

/**
 * Reads the emoji list at the given [path] and returns a parsed [MutableList].
 *
 * @param context The initiating view's context.
 * @param path The path to the asset file.
 */
fun parseRawEmojiSpecsFile(context: Context, path: String): MutableList<EmojiData> {
    if (cachedEmojiData != null) {
        return cachedEmojiData!!
    }

    val emojis = mutableListOf<EmojiData>()

    // Map CSV filenames to category names
    val csvFiles = mapOf(
        "emoji_category_emotions.csv" to "smileys_emotion",
        "emoji_category_people.csv" to "people_body",
        "emoji_category_animals_nature.csv" to "animals_nature",
        "emoji_category_food_drink.csv" to "food_drink",
        "emoji_category_travel_places.csv" to "travel_places",
        "emoji_category_activity.csv" to "activities",
        "emoji_category_objects.csv" to "objects",
        "emoji_category_symbols.csv" to "symbols",
        "emoji_category_flags.csv" to "flags"
    )

    csvFiles.forEach { (filename, category) ->
        val fullPath = if (path.isNotEmpty()) "$path/$filename" else filename

        try {
            context.assets.open(fullPath).bufferedReader().use { reader ->
                reader.forEachLine { line ->
                    try {
                        if (line.trim().isEmpty()) return@forEachLine

                        val parts = line.split(",")
                        if (parts.isEmpty()) return@forEachLine

                        val emoji = parts[0].trim()
                        if (emoji.isEmpty()) return@forEachLine

                        val searchTerms = if (parts.size > 1) {
                            parts[1].trim().split(" ").filter { it.isNotEmpty() }
                        } else emptyList()

                        val variants = if (parts.size > 2) {
                            parts.drop(2).map { it.trim() }.filter { it.isNotEmpty() }
                        } else emptyList()

                        emojis.add(
                            EmojiData(
                                category = category,
                                emoji = emoji,
                                variants = variants,
                                searchTerms = searchTerms
                            )
                        )
                    } catch (e: IllegalArgumentException) {
                        Log.e("EmojiHelper", "Invalid data in $filename: $line", e)
                    } catch (e: IOException) {
                        Log.e("EmojiHelper", "Error reading file: $fullPath", e)
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("EmojiHelper", "Error reading file: $fullPath", e)
        }
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

    } catch (e: IOException) {
        Log.e("EmojiHelper", "JSON file not found: $path", e)
        return HashMap()
    } catch (e: JSONException) {
        Log.e("EmojiHelper", "JSON parse error in $path", e)
        return HashMap()
    }

    return cachedVNTelexData
}

data class EmojiData(
    val category: String,
    val emoji: String,
    val variants: List<String>,
    val searchTerms: List<String> = emptyList()
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

/**
 * Search emojis by search query matching against search terms.
 */
fun searchEmojis(allEmojis: List<EmojiData>, query: String): List<EmojiData> {
    if (query.trim().isEmpty()) return emptyList()

    val searchQuery = query.trim().lowercase()

    return allEmojis.filter { emojiData ->
        emojiData.emoji.contains(searchQuery, ignoreCase = true) ||
            emojiData.searchTerms.any { term ->
                term.lowercase().contains(searchQuery)
            }
    }.take(MAX_SEARCH_RESULTS)
}
