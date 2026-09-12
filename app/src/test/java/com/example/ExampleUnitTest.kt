package com.example

import com.example.data.model.extractAuthorAndTitle
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
}
