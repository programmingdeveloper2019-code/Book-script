package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BookGenre
import com.example.data.model.normalizeGenre
import com.example.data.model.resolveGenre
import com.example.data.model.toBook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Yatal Library", appName)
  }

  @Test
  fun `verify genre definitions exist for Literature, History, and Education`() {
    val literature = BookGenre.fromDisplayNameOrId("Literature")
    val history = BookGenre.fromDisplayNameOrId("History")
    val education = BookGenre.fromDisplayNameOrId("Education")
    val fiction = BookGenre.fromDisplayNameOrId("Fiction")
    val science = BookGenre.fromDisplayNameOrId("Science")

    assertEquals(BookGenre.LITERATURE, literature)
    assertEquals("Literature", literature.displayName)
    assertEquals(BookGenre.HISTORY, history)
    assertEquals("History", history.displayName)
    assertEquals(BookGenre.EDUCATION, education)
    assertEquals(BookGenre.EDUCATION.displayName, "Education")
    assertEquals(BookGenre.FICTION, fiction)
    assertEquals("Fiction", fiction.displayName)
    assertEquals(BookGenre.SCIENCE, science)
    assertEquals("Science", science.displayName)
  }

  @Test
  fun `verify genre resolution from category and title`() {
    assertEquals("Literature", resolveGenre("Literature", "Pride and Prejudice"))
    assertEquals("Literature", resolveGenre("ရသစာပေ", "ဂျူး ဝတ္ထုတိုများ"))
    assertEquals("History", resolveGenre("သမိုင်း", "World War II Chronicles"))
    assertEquals("History", resolveGenre("History", "A Brief History of Time"))
    assertEquals("Education", resolveGenre("Education", "Cambridge Grammar in Use"))
    assertEquals("Education", resolveGenre("ဘာသာစကား", "English Speaking Practice"))
    assertEquals("Fiction", resolveGenre("Fiction", "Sherlock Holmes Mysteries"))
    assertEquals("Science", resolveGenre("Science", "The Origin of Species"))
  }

  @Test
  fun `verify normalizeGenre maps aliases correctly`() {
    assertEquals("Literature", normalizeGenre("literature"))
    assertEquals("Literature", normalizeGenre("စာပေနှင့် ရသ"))
    assertEquals("History", normalizeGenre("history"))
    assertEquals("History", normalizeGenre("သမိုင်း"))
    assertEquals("Education", normalizeGenre("education"))
    assertEquals("Education", normalizeGenre("ဘာသာစကား"))
    assertEquals("Fiction", normalizeGenre("fiction"))
    assertEquals("Fiction", normalizeGenre("ဝတ္ထု"))
    assertEquals("Science", normalizeGenre("science"))
    assertEquals("Science", normalizeGenre("သိပ္ပံ"))
  }

  @Test
  fun `verify SwipeRefreshState initialized and toggles state`() {
    val state = com.example.ui.components.SwipeRefreshState(isRefreshing = false)
    org.junit.Assert.assertFalse(state.isRefreshing)
    state.isRefreshing = true
    org.junit.Assert.assertTrue(state.isRefreshing)
  }

  @Test
  fun `verify book detailed view fields - synopsis, author, and publication date`() {
    val bookDto = com.example.data.model.BookDto(
        id = 101L,
        title = "စိမ်းရက်လေအား (ဂျူး)",
        category = "ဝတ္ထု",
        rawFilename = "Ju_Novel_2019.pdf",
        size = 1024000L,
        synopsis = "ဂျူး ၏ အချစ်နှင့် ဘဝရသဝတ္ထုရှည်",
        publicationDate = "၂၀၁၉ ခုနှစ်"
    )
    val book = bookDto.toBook()

    assertEquals("ဂျူး", book.author)
    assertEquals("ဂျူး", book.displayAuthor)
    assertEquals("၂၀၁၉ ခုနှစ်", book.displayPublicationDate)
    assertEquals("ဂျူး ၏ အချစ်နှင့် ဘဝရသဝတ္ထုရှည်", book.displaySynopsis)
  }

  @Test
  fun `verify fallback resolution for author, synopsis, and publication date`() {
    val bookDto = com.example.data.model.BookDto(
        id = 202L,
        title = "သမိုင်းမှတ်တမ်း (၁၉၉၅)",
        category = "History",
        rawFilename = "history_record.pdf",
        size = 2048000L
    )
    val book = bookDto.toBook()

    // Author should fallback gracefully to Unknown or extracted
    assertNotNull(book.displayAuthor)
    // Publication date should extract year 1995 from title
    assertEquals("၁၉၉၅ ခုနှစ်", book.displayPublicationDate)
    // Synopsis should be generated meaningfully based on History genre
    org.junit.Assert.assertTrue(book.displaySynopsis.isNotEmpty())
    org.junit.Assert.assertTrue(book.displaySynopsis.contains("သမိုင်း"))
  }
}

