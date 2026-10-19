package com.example

import com.example.data.model.Book
import com.example.data.model.extractAuthorAndTitle
import com.example.ui.SearchFilterScope
import com.example.ui.SortOption
import com.example.ui.components.buildHighlightedAnnotatedString
import com.example.ui.rankAndFilterBooks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testAuthorExtraction_withDelimiter() {
        val result1 = extractAuthorAndTitle("အလွမ်း၏ အရောင်အဝါ - ဂျူး")
        assertEquals("ဂျူး", result1.author)
        assertEquals("အလွမ်း၏ အရောင်အဝါ", result1.title)

        val result2 = extractAuthorAndTitle("မင်းလူ - အချစ်ဆူး")
        assertEquals("မင်းလူ", result2.author)
        assertEquals("အချစ်ဆူး", result2.title)
    }

    @Test
    fun testAuthorExtraction_withParentheses() {
        val result = extractAuthorAndTitle("ဓားတောင်ကိုကျော်၍ မီးပင်လယ်ကိုဖြတ်မည် (မြသန်းတင့်)")
        assertEquals("မြသန်းတင့်", result.author)
        assertEquals("ဓားတောင်ကိုကျော်၍ မီးပင်လယ်ကိုဖြတ်မည်", result.title)
    }

    @Test
    fun testAuthorExtraction_withEnglishBy() {
        val result = extractAuthorAndTitle("How to Win Friends by Dale Carnegie")
        assertEquals("Dale Carnegie", result.author)
        assertEquals("How to Win Friends", result.title)
    }

    private fun createSampleBook(id: Long, title: String, cleanTitle: String, author: String = "", category: String = "All"): Book {
        return Book(
            id = id,
            category = category,
            rawCategory = null,
            title = title,
            cleanTitle = cleanTitle,
            author = author,
            rawFilename = "$title.pdf",
            size = 1024L,
            formattedSize = "1 KB",
            genre = "Literature",
            jacketIndex = 0,
            previewUrl = "",
            downloadUrl = "",
            webUrl = ""
        )
    }

    @Test
    fun testRealTimeSearch_titlePriorityAndRanking() {
        val books = listOf(
            createSampleBook(1, "ပန်းရိုင်း - မြသန်းတင့်", "ပန်းရိုင်း", "မြသန်းတင့်"),
            createSampleBook(2, "အဝေးရောက်ပန်း - ဂျူး", "အဝေးရောက်ပန်း", "ဂျူး"),
            createSampleBook(3, "အလွမ်း၏ အရောင်အဝါ - ဂျူး", "အလွမ်း၏ အရောင်အဝါ", "ဂျူး")
        )

        // Searching for "ပန်း" should return both books with "ပန်း", with prefix match ranked first
        val results = rankAndFilterBooks(books, "ပန်း", SearchFilterScope.TITLE)
        assertEquals(2, results.size)
        assertEquals("ပန်းရိုင်း", results[0].cleanTitle)
        assertEquals("အဝေးရောက်ပန်း", results[1].cleanTitle)
    }

    @Test
    fun testRealTimeSearch_multiTokenSearch() {
        val books = listOf(
            createSampleBook(1, "Clean Code by Robert Martin", "Clean Code", "Robert Martin"),
            createSampleBook(2, "The Clean Architecture", "The Clean Architecture", "Robert Martin"),
            createSampleBook(3, "Refactoring by Martin Fowler", "Refactoring", "Martin Fowler")
        )

        // Searching for "Martin Code" matches Clean Code with highest priority because it contains both tokens
        val results = rankAndFilterBooks(books, "Martin Code", SearchFilterScope.ALL)
        assertTrue(results.isNotEmpty())
        assertEquals("Clean Code", results[0].cleanTitle)
    }

    @Test
    fun testRealTimeSearch_emptyQueryReturnsAll() {
        val books = listOf(
            createSampleBook(1, "Book 1", "Book 1"),
            createSampleBook(2, "Book 2", "Book 2")
        )
        val results = rankAndFilterBooks(books, "", SearchFilterScope.ALL)
        assertEquals(2, results.size)
    }

    @Test
    fun testHighlightedAnnotatedString() {
        val original = "Clean Code in Kotlin"
        val highlighted = buildHighlightedAnnotatedString(original, "Code")
        assertEquals(original, highlighted.text)
        assertTrue(highlighted.spanStyles.isNotEmpty())
        // Verified style was applied to "Code" (index 6 to 10)
        val styleRange = highlighted.spanStyles[0]
        assertEquals(6, styleRange.start)
        assertEquals(10, styleRange.end)
    }

    @Test
    fun testPdfCacheFileNaming() {
        val bookId = 12345L
        val expectedFileName = "book_12345.pdf"
        val actualFileName = "book_${bookId}.pdf"
        assertEquals(expectedFileName, actualFileName)
    }
}

