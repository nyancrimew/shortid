package ch.deletescape.shortid.utils

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CharArrayTest {
    @Test
    fun `Char array contains duplicates`() {
        assertTrue(charArrayOf('a', 'b', 'a').hasDuplicates())
    }

    @Test
    fun `Char array contains no duplicates`() {
        assertFalse(charArrayOf('a', 'b', 'c').hasDuplicates())
    }
}