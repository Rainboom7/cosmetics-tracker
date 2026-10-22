package io.github.rainboom7.cosmetics.core.search

import java.text.Normalizer

/**
 * Text normalization shared by the index builder and the app, so both sides tokenize the same
 * way: lower case, no accents ("crème" = "creme"), "ё" = "е", apostrophes dropped, anything else but
 * letters and digits is a separator. The index stores normalized text and is searched with the SQLite "simple" tokenizer.
 */
object SearchText {
    private val marks = Regex("\\p{M}+")
    private val apostrophes = Regex("['’`ʼ]")
    private val separators = Regex("[^\\p{L}\\p{N}]+")

    fun normalize(text: String): String =
        Normalizer.normalize(text.lowercase(), Normalizer.Form.NFKD)
            .replace(marks, "")
            .replace('ё', 'е')
            // "L'Oréal" is one word: "loreal".
            .replace(apostrophes, "")
            .replace(separators, " ")
            .trim()

    fun tokens(text: String): List<String> = normalize(text).split(' ').filter { it.isNotEmpty() }

    fun isCyrillic(token: String): Boolean = token.any { it in 'а'..'я' }

    private val cyrToLat = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'е' to "e", 'ж' to "zh", 'з' to "z",
        // "и" also stands for "й" after normalization (the breve is a combining mark).
        'и' to "i", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p", 'р' to "r",
        'с' to "s", 'т' to "t", 'у' to "u", 'ф' to "f", 'х' to "h", 'ц' to "ts", 'ч' to "ch", 'ш' to "sh",
        'щ' to "sch", 'ъ' to "", 'ы' to "y", 'ь' to "", 'э' to "e", 'ю' to "yu", 'я' to "ya",
    )

    /** Plain transliteration, good enough as a prefix: "гарньер" -> "garner", "нивея" -> "niveya". */
    fun transliterate(token: String): String = buildString { token.forEach { append(cyrToLat[it] ?: it.toString()) } }
}
