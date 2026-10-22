package io.github.rainboom7.cosmetics.core.model

/** GTIN helpers (EAN-8, UPC-A, EAN-13, GTIN-14), the barcodes printed on cosmetics. */
object Barcodes {
    private val VALID_LENGTHS = setOf(8, 12, 13, 14)

    /** Strips spaces and dashes; returns null for anything that is not a valid GTIN. */
    fun normalize(raw: String): String? {
        val digits = raw.filterNot { it == ' ' || it == '-' }
        if (digits.length !in VALID_LENGTHS || !digits.all { it in '0'..'9' }) return null
        return if (hasValidCheckDigit(digits)) digits else null
    }

    fun isValid(raw: String): Boolean = normalize(raw) != null

    private fun hasValidCheckDigit(digits: String): Boolean {
        val body = digits.dropLast(1)
        // Weights alternate 3,1,3,1... starting from the digit next to the check digit.
        val sum = body.reversed().withIndex().sumOf { (i, c) -> (c - '0') * if (i % 2 == 0) 3 else 1 }
        val check = (10 - sum % 10) % 10
        return check == digits.last() - '0'
    }
}
