package io.github.rainboom7.cosmetics.core.catalog

import io.github.rainboom7.cosmetics.core.model.Category

/**
 * Maps free-form catalog categories (Open Beauty Facts tags like `en:face-creams`, or product
 * names) to our coarse [Category]. Keyword based on purpose: catalog taxonomies are messy and
 * multilingual, and a wrong guess is cheap because the user can change the category.
 */
object CategoryClassifier {
    // Order matters: the first category whose keyword matches wins.
    private val keywords: List<Pair<Category, List<String>>> = listOf(
        Category.NAILS to listOf("nail", "vernis", "лак для ногтей", "ногт"),
        Category.FRAGRANCE to listOf("perfume", "parfum", "fragrance", "eau-de", "eau de", "cologne", "духи", "туалетная вода"),
        Category.MAKEUP to listOf(
            "makeup", "make-up", "maquillage", "lipstick", "lip-gloss", "lip gloss", "mascara", "eyeliner",
            "eyeshadow", "eye-shadow", "foundation", "concealer", "blush", "bronzer", "highlighter", "powder",
            "primer", "brow", "помада", "тушь", "тени", "тональн", "консилер", "румяна", "пудра", "макияж",
        ),
        Category.HAIR to listOf(
            "hair", "shampoo", "conditioner", "cheveux", "shampooing", "волос", "шампун", "кондиционер", "бальзам для волос",
        ),
        Category.SKINCARE to listOf(
            "face", "facial", "serum", "cream", "moistur", "cleanser", "toner", "mask", "sunscreen", "spf",
            "visage", "skin", "крем", "сыворот", "лиц", "тоник", "умыван", "маска", "солнцезащит",
        ),
        Category.BODY to listOf(
            "body", "shower", "soap", "deodorant", "hand", "bath", "lotion", "corps", "savon", "тел", "душ", "мыло",
            "дезодорант", "рук",
        ),
    )

    fun classify(tags: List<String>, name: String? = null): Category {
        val haystacks = tags.map { it.substringAfter(':').lowercase() } + listOfNotNull(name?.lowercase())
        for ((category, words) in keywords) {
            if (haystacks.any { hay -> words.any { it in hay } }) return category
        }
        return Category.OTHER
    }
}
