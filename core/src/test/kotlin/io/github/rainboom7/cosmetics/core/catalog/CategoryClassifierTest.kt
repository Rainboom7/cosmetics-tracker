package io.github.rainboom7.cosmetics.core.catalog

import io.github.rainboom7.cosmetics.core.model.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryClassifierTest {
    @Test
    fun `classifies Open Beauty Facts tags`() {
        assertEquals(Category.SKINCARE, CategoryClassifier.classify(listOf("en:face-creams")))
        assertEquals(Category.MAKEUP, CategoryClassifier.classify(listOf("en:mascaras")))
        assertEquals(Category.HAIR, CategoryClassifier.classify(listOf("en:shampoos")))
        assertEquals(Category.FRAGRANCE, CategoryClassifier.classify(listOf("en:eau-de-toilette")))
        assertEquals(Category.NAILS, CategoryClassifier.classify(listOf("en:nail-polishes")))
        assertEquals(Category.BODY, CategoryClassifier.classify(listOf("en:shower-gels")))
    }

    @Test
    fun `falls back to the product name, including Russian`() {
        assertEquals(Category.MAKEUP, CategoryClassifier.classify(emptyList(), "Губная помада Matte"))
        assertEquals(Category.HAIR, CategoryClassifier.classify(emptyList(), "Шампунь для волос"))
    }

    @Test
    fun `more specific categories win over generic words`() {
        // "nail" beats "hand" and "cream"
        assertEquals(Category.NAILS, CategoryClassifier.classify(listOf("en:hand-and-nail-creams")))
    }

    @Test
    fun `unknown is OTHER`() {
        assertEquals(Category.OTHER, CategoryClassifier.classify(listOf("en:toothbrushes"), "Brush"))
    }
}
