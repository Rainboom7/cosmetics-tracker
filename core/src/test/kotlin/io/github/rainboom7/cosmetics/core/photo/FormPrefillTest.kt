package io.github.rainboom7.cosmetics.core.photo

import io.github.rainboom7.cosmetics.core.model.Category
import org.junit.Assert.assertEquals
import org.junit.Test

class FormPrefillTest {
    @Test
    fun `biggest line is the brand, next ones the name, PAO is parsed`() {
        val recognition = PhotoRecognition(
            lines = listOf(
                RecognizedLine("Moisturizing face cream", 40),
                RecognizedLine("NATURA SIBERICA", 90),
                RecognizedLine("50 ml", 12),
                RecognizedLine("12M", 8),
                RecognizedLine("for dry skin", 30),
            ),
        )

        val prefill = FormPrefiller.prefill(recognition)

        assertEquals("NATURA SIBERICA", prefill.brand)
        assertEquals("Moisturizing face cream dry skin", prefill.name)
        assertEquals(Category.SKINCARE, prefill.category)
        assertEquals(12, prefill.periodAfterOpeningMonths)
    }

    @Test
    fun `nothing readable gives an empty prefill`() {
        assertEquals(FormPrefill(), FormPrefiller.prefill(PhotoRecognition(lines = listOf(RecognizedLine("50 ml", 10)))))
    }
}
