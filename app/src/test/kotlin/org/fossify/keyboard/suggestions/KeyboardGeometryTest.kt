package org.fossify.keyboard.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyboardGeometryTest {

    /** Key bounds in pixels of a layout with keys 100 px wide and 150 px high, each row shifted by [offsets] keys. */
    private fun bounds(rows: List<String>, offsets: List<Float>) = rows.flatMapIndexed { row, keys ->
        keys.mapIndexed { column, char ->
            val x = ((offsets[row] + column) * KEY_WIDTH).toInt()
            KeyboardGeometry.KeyBounds(char, x, row * KEY_HEIGHT, KEY_WIDTH, KEY_HEIGHT)
        }
    }

    private val qwertyRows = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")
    private val qwertyOffsets = listOf(0f, 0.5f, 1.5f)

    @Test
    fun keyBoundsGiveTheSameCostsAsTheGrid() {
        val geometry = KeyboardGeometry.fromKeyBounds(bounds(qwertyRows, qwertyOffsets))!!
        for (a in 'a'..'z') {
            for (b in 'a'..'z') {
                val sa = Alphabet.symbolOf(a)
                val sb = Alphabet.symbolOf(b)
                val grid = KeyboardGeometry.QWERTY
                assertEquals("$a$b", grid.substitutionCost(sa, sb), geometry.substitutionCost(sa, sb), DELTA)
                assertEquals("$a$b", grid.areNeighbours(sa, sb), geometry.areNeighbours(sa, sb))
            }
        }
    }

    @Test
    fun otherLayoutsGiveOtherCosts() {
        val dvorakRows = listOf("',.pyfgcrl", "aoeuidhtns", ";qjkxbmwvz")
        val dvorak = KeyboardGeometry.fromKeyBounds(bounds(dvorakRows, listOf(0f, 0f, 0f)))!!
        val e = Alphabet.symbolOf('e')
        val r = Alphabet.symbolOf('r')
        assertNotEquals(KeyboardGeometry.QWERTY.substitutionCost(e, r), dvorak.substitutionCost(e, r))
        val neighbourCost = EngineConstants.SUB_BASE + EngineConstants.SUB_PER_KEY
        assertEquals(neighbourCost, dvorak.substitutionCost(Alphabet.symbolOf('o'), Alphabet.symbolOf('e')), DELTA)
    }

    @Test
    fun keyboardsWithoutLettersAreIgnored() {
        val symbols = listOf("1234567890", "@#$%&-+()", "*\"':;!?")
        val russian = listOf("йцукенгшщз", "фывапролдж", "ячсмитьбю")
        assertNull(KeyboardGeometry.fromKeyBounds(bounds(symbols, qwertyOffsets)))
        assertNull(KeyboardGeometry.fromKeyBounds(bounds(russian, qwertyOffsets)))
    }

    private companion object {
        const val KEY_WIDTH = 100
        const val KEY_HEIGHT = 150
        const val DELTA = 1e-4f
    }
}
