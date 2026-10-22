package io.github.rainboom7.cosmetics.core.collection

import io.github.rainboom7.cosmetics.core.model.CosmeticItem
import java.text.Normalizer

/**
 * "Похожее уже есть у вас": finds items that look like the same product. Never blocks saving,
 * the UI only shows a hint. Same barcode is a sure match; otherwise brand and name are compared
 * as sets of folded words, so "L'Oréal Aloe Water 70ml" matches "Aloe water" by L'Oreal.
 */
object DuplicateFinder {
    private const val NAME_OVERLAP = 0.6

    fun findSimilar(
        candidate: CosmeticItem,
        existing: List<CosmeticItem>,
    ): List<CosmeticItem> = existing.filter { it.id != candidate.id && isSimilar(candidate, it) }

    fun isSimilar(a: CosmeticItem, b: CosmeticItem): Boolean {
        if (a.barcode != null && a.barcode == b.barcode) return true
        val brandA = a.brand?.let(::words).orEmpty()
        val brandB = b.brand?.let(::words).orEmpty()
        if (brandA.isNotEmpty() && brandB.isNotEmpty() && brandA.intersect(brandB).isEmpty()) return false
        val nameA = words(a.name) - brandA
        val nameB = words(b.name) - brandB
        if (nameA.isEmpty() || nameB.isEmpty()) return false
        val overlap = nameA.intersect(nameB).size.toDouble() / minOf(nameA.size, nameB.size)
        return overlap >= NAME_OVERLAP
    }

    internal fun words(text: String): Set<String> =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace("'", "")
            .split(Regex("[^\\p{L}\\p{N}]+"))
            .filter { it.length >= 2 && !it.matches(Regex("\\d+[\\p{L}]{0,2}")) }
            .toSet()
}
