package org.fossify.keyboard.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

class ArrayTrieTest {

    private val entries = listOf(
        DictionaryEntry("a", 200),
        DictionaryEntry("an", 190),
        DictionaryEntry("and", 210),
        DictionaryEntry("ant", 90),
        DictionaryEntry("cafe", 80),
        DictionaryEntry("cafe", 95, surface = "café"),
        DictionaryEntry("don't", 150, surface = "don't"),
        DictionaryEntry("i", 220, surface = "I"),
        DictionaryEntry("iphone", 100, surface = "iPhone"),
        DictionaryEntry("monday", 120, surface = "Monday"),
        DictionaryEntry("nasa", 70, surface = "NASA"),
        DictionaryEntry("shit", 90, flags = ArrayTrie.OFFENSIVE),
        DictionaryEntry("the", 230),
        DictionaryEntry("then", 180),
        DictionaryEntry("x-ray", 60),
    )

    private val trie = TrieBuilder.build(entries)

    @Test
    fun roundTrip() {
        val words = HashMap<String, Int>()
        collect(ArrayTrie.ROOT, "", words)
        val expected = entries.associate { (it.surface ?: it.key) to it.freq }
        assertEquals(expected, words)
        assertEquals(entries.size, trie.wordCount)
    }

    @Test
    fun nonWordsAreAbsent() {
        for (word in listOf("", "t", "th", "then2", "ands", "caf", "b", "an t", "the the")) {
            assertFalse(word, word in trie)
        }
    }

    @Test
    fun lookupFoldsCaseAndAccents() {
        assertTrue("The" in trie)
        assertTrue("CAFÉ" in trie)
        assertTrue("don’t" in trie)
        assertEquals(ArrayTrie.CASE_VARIANT, trie.caseKind(trie.find("cafe")))
        assertEquals("café", trie.primarySurface(trie.find("cafe"), "cafe"))
        assertEquals("I", trie.primarySurface(trie.find("i"), "i"))
        assertEquals("Monday", trie.primarySurface(trie.find("monday"), "monday"))
        assertEquals("NASA", trie.primarySurface(trie.find("nasa"), "nasa"))
        assertEquals("iPhone", trie.primarySurface(trie.find("iphone"), "iphone"))
        assertEquals(ArrayTrie.OFFENSIVE, trie.wordFlags(trie.find("shit")))
    }

    @Test
    fun childrenAreIndexedByPopcount() {
        val an = trie.find("an")
        val bitmap = trie.childBitmap(an)
        assertEquals(2, bitmap.countOneBits())
        val d = trie.child(an, Alphabet.symbolOf('d'))
        val t = trie.child(an, Alphabet.symbolOf('t'))
        assertEquals(trie.firstChild(an), d)
        assertEquals(trie.firstChild(an) + 1, t)
        assertEquals(ArrayTrie.NO_NODE, trie.child(an, Alphabet.symbolOf('e')))
        assertTrue(trie.isTerminal(d))
        assertTrue(trie.isTerminal(t))
    }

    @Test
    fun nodesAreInBreadthFirstOrder() {
        var previousDepth = 0
        val depths = IntArray(trie.nodeCount)
        for (node in 0 until trie.nodeCount) {
            assertTrue(depths[node] >= previousDepth)
            previousDepth = depths[node]
            val first = trie.firstChild(node)
            for (i in 0 until trie.childBitmap(node).countOneBits()) {
                assertTrue(first + i > node)
                depths[first + i] = depths[node] + 1
            }
        }
    }

    @Test
    fun maxFreqCoversSubtree() {
        assertEquals(230, trie.maxFreq(ArrayTrie.ROOT))
        assertEquals(210, trie.maxFreq(trie.find("a")))
        assertEquals(200, trie.ownFreq(trie.find("a")))
        assertEquals(210, trie.maxFreq(trie.find("an")))
        assertEquals(90, trie.maxFreq(trie.find("ant")))
        assertEquals(0, trie.ownFreq(trie.find("th")))
        assertEquals(230, trie.maxFreq(trie.find("th")))
    }

    @Test
    fun loaderReadsPackedDictionary() {
        val packed = packed(
            words = listOf(0 to "cafe", 3 to "é", 0 to "don't", 0 to "shit", 0 to "teh", 1 to "he", 0 to "TV"),
            freqs = listOf(80, 95, 150, 90, 10, 230, 120),
            flags = listOf(0, 0, 0, 1, 2, 0, 0),
        )
        assertEquals(
            listOf(
                DictionaryEntry("cafe", 80),
                DictionaryEntry("cafe", 95, surface = "café"),
                DictionaryEntry("don't", 150),
                DictionaryEntry("shit", 90, ArrayTrie.OFFENSIVE),
                DictionaryEntry("teh", 10, ArrayTrie.NO_AUTOCORRECT_TO),
                DictionaryEntry("the", 230),
                DictionaryEntry("tv", 120, surface = "TV"),
            ),
            DictionaryLoader.read(packed.inputStream())
        )

        val loaded = DictionaryLoader.load(packed.inputStream())
        assertEquals("TV", loaded.primarySurface(loaded.find("tv"), "tv"))
        val surfaces = ArrayList<String>()
        loaded.forEachWord(loaded.find("cafe"), "cafe") { surface, _, _ -> surfaces.add(surface) }
        assertEquals(listOf("café", "cafe"), surfaces)
    }

    @Test
    fun loaderRejectsMalformedDictionaries() {
        val packed = packed(words = listOf(0 to "the"), freqs = listOf(230), flags = listOf(0))
        val malformed = listOf(
            packed.copyOf(packed.size - 1),
            packed + 0.toByte(),
            packed.copyOf().also { it[0] = 'X'.code.toByte() },
            packed(words = listOf(1 to "the"), freqs = listOf(230), flags = listOf(0)),
        )
        for (bytes in malformed) {
            val error = runCatching { DictionaryLoader.read(bytes.inputStream()) }.exceptionOrNull()
            assertTrue("$error", error is IllegalArgumentException)
        }
    }

    /**
     * Packs a dictionary like `tools/dictionary/build_wordlist.py`, from words given as the length of the prefix they
     * share with the previous word and the rest.
     */
    private fun packed(words: List<Pair<Int, String>>, freqs: List<Int>, flags: List<Int>): ByteArray {
        val out = ByteArrayOutputStream()
        out.write("FKD1".toByteArray())
        out.write(ByteBuffer.allocate(Int.SIZE_BYTES).putInt(words.size).array())
        for ((shared, rest) in words) {
            out.write(shared)
            out.write(rest.toByteArray())
            out.write('\n'.code)
        }

        freqs.forEach(out::write)
        flags.forEach(out::write)
        return out.toByteArray()
    }

    @Test(expected = IllegalArgumentException::class)
    fun unsortedInputIsRejected() {
        TrieBuilder.build(listOf(DictionaryEntry("b", 1), DictionaryEntry("a", 1)))
    }

    @Test
    fun alphabetFolds() {
        assertEquals("cafe", Alphabet.fold("Café"))
        assertEquals("don't", Alphabet.fold("DON’T"))
        assertEquals("naive", Alphabet.fold("naïve"))
        assertEquals(null, Alphabet.fold("b4"))
        assertEquals(null, Alphabet.fold("a b"))
        assertEquals(Alphabet.size, (0 until Alphabet.size).map { Alphabet.charOf(it) }.sorted().distinct().size)
        for (symbol in 0 until Alphabet.size) {
            assertEquals(symbol, Alphabet.symbolOf(Alphabet.charOf(symbol)))
            if (symbol > 0) assertTrue(Alphabet.charOf(symbol - 1) < Alphabet.charOf(symbol))
        }
    }

    private fun collect(node: Int, key: String, out: MutableMap<String, Int>) {
        trie.forEachWord(node, key) { surface, freq, _ -> out[surface] = freq }
        val first = trie.firstChild(node)
        var bitmap = trie.childBitmap(node)
        var i = 0
        while (bitmap != 0L) {
            val symbol = bitmap.countTrailingZeroBits()
            collect(first + i, key + Alphabet.charOf(symbol), out)
            bitmap = bitmap and (bitmap - 1)
            i++
        }
    }
}
