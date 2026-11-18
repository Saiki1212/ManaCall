package com.saiki1212.manacall.contacts

import java.text.Normalizer
import java.util.Locale

internal class ContactNameSearch(query: String) {
    private val terms = words(query)

    fun rank(name: String?): Int? {
        if (terms.isEmpty() || name.isNullOrBlank()) return null
        val nameWords = words(name)
        if (terms == nameWords) return 0

        var cost = 0
        for (term in terms) {
            cost += nameWords.mapNotNull { word -> matchCost(term, word) }.minOrNull() ?: return null
        }
        return cost + 1
    }

    private fun matchCost(term: String, word: String): Int? {
        if (word == term) return 0
        if (word.startsWith(term)) return 1
        if (word.contains(term)) return 2

        val limit = when {
            term.length < 3 -> return null
            term.length < 6 -> 1
            else -> 2
        }
        if (kotlin.math.abs(word.length - term.length) > limit) return null
        val edits = editDistance(term, word)
        return (3 + edits).takeIf { edits <= limit }
    }

    // A neighboring swap counts as one typo, alongside insertions, deletions, and substitutions.
    private fun editDistance(term: String, word: String): Int {
        var twoBack = IntArray(word.length + 1)
        var previous = IntArray(word.length + 1) { it }
        var current = IntArray(word.length + 1)
        for (i in 1..term.length) {
            current[0] = i
            for (j in 1..word.length) {
                val substitution = if (term[i - 1] == word[j - 1]) 0 else 1
                current[j] = minOf(previous[j] + 1, current[j - 1] + 1, previous[j - 1] + substitution)
                if (i > 1 && j > 1 && term[i - 1] == word[j - 2] && term[i - 2] == word[j - 1]) {
                    current[j] = minOf(current[j], twoBack[j - 2] + 1)
                }
            }
            val scratch = twoBack
            twoBack = previous
            previous = current
            current = scratch
        }
        return previous[word.length]
    }

    private companion object {
        val separators = Regex("[^\\p{L}\\p{M}\\p{N}]+")

        fun words(text: String): List<String> =
            Normalizer.normalize(text, Normalizer.Form.NFKC).lowercase(Locale.ROOT)
                .split(separators).filter(String::isNotEmpty)
    }
}
