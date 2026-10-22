package io.github.rainboom7.cosmetics.core.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IndexQueryTest {
    @Test
    fun `normalize folds case, accents, yo and apostrophes`() {
        assertEquals("loreal paris creme", SearchText.normalize("L'Oréal  Paris, Crème!"))
        assertEquals("еж", SearchText.normalize("Ёж"))
        assertEquals("spf 50", SearchText.normalize("SPF-50"))
    }

    @Test
    fun `words get prefix, dictionary and transliteration variants`() {
        val words = IndexQuery.plan("крем для рук")
        assertEquals(listOf("крем", "рук"), words.map { it.text })
        val cream = words[0].expression
        assertTrue(cream, "крем*" in cream && "cream*" in cream && "creme*" in cream)
        assertTrue(IndexQuery.plan("нивея").single().expression.contains("nivea*"))
        assertTrue(IndexQuery.plan("гарньерр").single().expression.contains("garn"))
    }

    @Test
    fun `two-word dictionary entries become one word`() {
        val words = IndexQuery.plan("ив роше шампунь")
        assertEquals(listOf("ив роше", "шампунь"), words.map { it.text })
        assertTrue(words[0].expression.contains("\"yves rocher\""))
    }

    @Test
    fun `short tokens are exact, stop words only dropped next to other words`() {
        assertEquals("50", IndexQuery.plan("spf 50")[1].expression)
        assertEquals(listOf("для"), IndexQuery.plan("для").map { it.text })
        assertEquals(emptyList<IndexQuery.Word>(), IndexQuery.plan(" ,. "))
    }
}
