package com.example.cbz

import java.util.Comparator

/**
 * Natural alphanumeric comparator for filenames and page titles.
 * Correctly sorts: 1.jpg, 2.jpg, 9.jpg, 10.jpg, 11.jpg
 * and page1, page2, page10, 001, 002, 010, chapter_01_page_001.
 */
class NaturalOrderComparator : Comparator<String> {

    override fun compare(s1: String?, s2: String?): Int {
        if (s1 == null && s2 == null) return 0
        if (s1 == null) return -1
        if (s2 == null) return 1

        val chunks1 = splitIntoChunks(s1)
        val chunks2 = splitIntoChunks(s2)

        val minSize = minOf(chunks1.size, chunks2.size)
        for (i in 0 until minSize) {
            val c1 = chunks1[i]
            val c2 = chunks2[i]

            val isDigit1 = c1.all { it.isDigit() }
            val isDigit2 = c2.all { it.isDigit() }

            val result = if (isDigit1 && isDigit2) {
                // Compare numerically
                // Remove leading zeros for numerical comparison, but preserve count if equal
                val num1 = c1.toBigIntegerOrNull()
                val num2 = c2.toBigIntegerOrNull()
                if (num1 != null && num2 != null) {
                    val numCompare = num1.compareTo(num2)
                    if (numCompare != 0) {
                        numCompare
                    } else {
                        // If values are numerically equal (e.g. "01" vs "1"), shorter or fewer leading zeros
                        c1.length.compareTo(c2.length)
                    }
                } else {
                    c1.compareTo(c2, ignoreCase = true)
                }
            } else {
                c1.compareTo(c2, ignoreCase = true)
            }

            if (result != 0) return result
        }

        return chunks1.size.compareTo(chunks2.size)
    }

    private fun splitIntoChunks(s: String): List<String> {
        val chunks = mutableListOf<String>()
        val current = StringBuilder()
        var isDigitChunk: Boolean? = null

        for (ch in s) {
            val isDigit = ch.isDigit()
            if (isDigitChunk == null) {
                isDigitChunk = isDigit
                current.append(ch)
            } else if (isDigitChunk == isDigit) {
                current.append(ch)
            } else {
                chunks.add(current.toString())
                current.clear()
                isDigitChunk = isDigit
                current.append(ch)
            }
        }
        if (current.isNotEmpty()) {
            chunks.add(current.toString())
        }
        return chunks
    }

    companion object {
        val INSTANCE = NaturalOrderComparator()
    }
}
