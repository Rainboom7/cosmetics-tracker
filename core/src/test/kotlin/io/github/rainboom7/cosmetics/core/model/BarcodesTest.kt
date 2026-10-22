package io.github.rainboom7.cosmetics.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BarcodesTest {
    @Test
    fun `accepts valid EAN-13, EAN-8 and UPC-A`() {
        assertTrue(Barcodes.isValid("3600523307593"))
        assertTrue(Barcodes.isValid("96385074"))
        assertTrue(Barcodes.isValid("036000291452"))
    }

    @Test
    fun `rejects wrong check digit, letters and wrong length`() {
        assertFalse(Barcodes.isValid("4006381333932"))
        assertFalse(Barcodes.isValid("40063813339A1"))
        assertFalse(Barcodes.isValid("12345"))
        assertFalse(Barcodes.isValid(""))
    }

    @Test
    fun `normalize strips spaces and dashes`() {
        assertEquals("3600523307593", Barcodes.normalize("3 600523-307593"))
        assertNull(Barcodes.normalize("not a code"))
    }
}
