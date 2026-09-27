package org.fossify.keyboard.helpers

import android.content.Context
import android.os.Handler
import android.os.Looper
import org.fossify.keyboard.databases.UserWordsDatabase
import org.fossify.keyboard.extensions.safeStorageContext
import org.fossify.keyboard.models.UserWord
import org.fossify.keyboard.suggestions.EngineConstants
import org.fossify.keyboard.suggestions.LanguageRules
import org.fossify.keyboard.suggestions.UserLexicon
import java.util.concurrent.Executors

/**
 * The words the user taught the keyboard, one [UserLexicon] per dictionary locale. The lexicons are used on the main
 * thread and shared by the keyboard and the settings; the database is written in the background, in order.
 */
class UserWordsRepository private constructor(context: Context) {
    private val dao = UserWordsDatabase.getInstance(context).UserWordsDao()
    private val executor = Executors.newSingleThreadExecutor()
    private val handler = Handler(Looper.getMainLooper())
    private val lexicons = HashMap<String, UserLexicon>()

    /** Returns the lexicon of [locale]. The first time, its words are loaded in the background. */
    fun lexicon(locale: String): UserLexicon {
        return lexicons.getOrPut(locale) {
            val rules = LanguageRules.forLocale(locale)
            val lexicon = UserLexicon(keepsCapitals = rules.keepsLearnedCapitals, keepsAccents = rules.restoresAccents)
            executor.execute {
                val words = dao.getWords(locale)
                handler.post { words.forEach { lexicon.add(it.word, it.count) } }
            }
            lexicon
        }
    }

    /** Learns [word] for [locale], or counts [uses] more confirmations of it. */
    fun learn(locale: String, word: String, uses: Int) {
        val learned = lexicon(locale).learn(word, uses) ?: return
        val lastUsed = System.currentTimeMillis()
        executor.execute {
            learned.replaced?.let { dao.delete(it, locale) }
            dao.insertOrUpdate(UserWord(learned.word, locale, learned.count, lastUsed))
            dao.trim(locale, EngineConstants.MAX_USER_WORDS)
        }
    }

    /** Forgets every learned word. */
    fun clear() {
        lexicons.values.forEach { it.clear() }
        executor.execute { dao.deleteAll() }
    }

    companion object {
        private var instance: UserWordsRepository? = null

        /** Returns the repository shared by the keyboard and the settings. Call it on the main thread. */
        fun getInstance(context: Context): UserWordsRepository {
            return instance ?: UserWordsRepository(context.applicationContext.safeStorageContext).also { instance = it }
        }
    }
}
