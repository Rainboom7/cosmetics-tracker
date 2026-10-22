package io.github.rainboom7.cosmetics.core.search

/**
 * Turns what the user typed into FTS4 MATCH expressions, one per meaningful word. Each expression
 * ORs the spellings of that word: the word itself as a prefix, its dictionary translations
 * ("тушь" -> mascara) and a transliteration prefix ("нивея" -> nive*). Ranking then counts how many
 * words a product matched, so a product matching every word comes first and a typo or an extra
 * word does not empty the results.
 */
object IndexQuery {
    data class Word(val text: String, val expression: String)

    fun plan(query: String): List<Word> {
        val all = SearchText.tokens(query)
        val meaningful = all.filterNot { it in SearchDictionary.stopWords }.ifEmpty { all }
        val words = mutableListOf<Word>()
        var i = 0
        while (i < meaningful.size) {
            // Two-word dictionary entries first ("ив роше", "чистая линия").
            val pair = if (i + 1 < meaningful.size) "${meaningful[i]} ${meaningful[i + 1]}" else null
            val pairTerms = pair?.let { SearchDictionary.words[it] }
            if (pair != null && pairTerms != null) {
                words += Word(pair, orOf(listOf(phrase(pair)) + pairTerms.map(::termFor)))
                i += 2
                continue
            }
            val token = meaningful[i]
            words += Word(token, orOf(variants(token)))
            i++
        }
        return words.distinctBy { it.expression }.take(MAX_WORDS)
    }

    internal fun variants(token: String): List<String> {
        val terms = mutableListOf(prefixOrExact(token))
        dictionaryEntry(token)?.let { terms += it.map(::termFor) }
        if (SearchText.isCyrillic(token) && token.length >= 4) {
            val latin = SearchText.transliterate(token)
            // Endings transliterate badly ("нивея" -> "niveya"), so keep only the stem.
            terms += latin.take(maxOf(3, latin.length - 2)) + "*"
        }
        return terms.distinct()
    }

    private fun dictionaryEntry(token: String): List<String>? =
        SearchDictionary.words.entries
            .filter { (key, _) -> ' ' !in key && token.startsWith(key) && token.length - key.length <= MAX_ENDING }
            .maxByOrNull { it.key.length }
            ?.value

    private fun termFor(value: String): String = if (' ' in value) phrase(value) else prefixOrExact(value)

    private fun prefixOrExact(token: String) = if (token.length >= MIN_PREFIX) "$token*" else token

    private fun phrase(text: String) = "\"$text\""

    private fun orOf(terms: List<String>) = terms.joinToString(" OR ")

    private const val MIN_PREFIX = 3
    private const val MAX_ENDING = 3
    private const val MAX_WORDS = 6
}
