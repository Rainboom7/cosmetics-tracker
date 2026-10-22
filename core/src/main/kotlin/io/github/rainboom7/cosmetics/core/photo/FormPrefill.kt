package io.github.rainboom7.cosmetics.core.photo

import io.github.rainboom7.cosmetics.core.catalog.CategoryClassifier
import io.github.rainboom7.cosmetics.core.model.Category

/** Fields guessed from the package text for the "Нет в базе" form. The user checks and edits them. */
data class FormPrefill(
    val brand: String? = null,
    val name: String? = null,
    val category: Category = Category.OTHER,
    val periodAfterOpeningMonths: Int? = null,
)

/**
 * Guesses form fields from what OCR read on a package: the biggest line is usually the brand,
 * the next lines the product name. The PAO symbol is printed as "12M" / "12 M".
 */
object FormPrefiller {
    private val PAO = Regex("(?<![\\p{L}\\d])(3|6|9|12|18|24|36)\\s?[MmМм](?![\\p{L}\\d])")

    fun prefill(recognition: PhotoRecognition): FormPrefill {
        val lines = recognition.lines
            .filter { PhotoQueryPlanner.tokenize(it.text).isNotEmpty() }
            .sortedByDescending { it.height }
        val brand = lines.firstOrNull()?.text?.let(::clean)
        val name = lines.drop(1).take(2).joinToString(" ") { clean(it.text) }.takeIf { it.isNotBlank() }
        val pao = recognition.lines.firstNotNullOfOrNull { PAO.find(it.text)?.groupValues?.get(1)?.toInt() }
        val allText = recognition.lines.joinToString(" ") { it.text }
        return FormPrefill(
            brand = brand,
            name = name,
            category = CategoryClassifier.classify(emptyList(), allText),
            periodAfterOpeningMonths = pao,
        )
    }

    private fun clean(text: String): String = PhotoQueryPlanner.tokenize(text).joinToString(" ")
}
