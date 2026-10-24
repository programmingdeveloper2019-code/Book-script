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
    val educational = BookGenre.fromDisplayNameOrId("Educational")
    val fiction = BookGenre.fromDisplayNameOrId("Fiction")
    val nonFiction = BookGenre.fromDisplayNameOrId("Non-Fiction")
    val science = BookGenre.fromDisplayNameOrId("Science")

    assertEquals(BookGenre.LITERATURE, literature)
    assertEquals("Literature", literature.displayName)
    assertEquals(BookGenre.HISTORY, history)
    assertEquals("History", history.displayName)
    assertEquals(BookGenre.EDUCATION, education)
    assertEquals(BookGenre.EDUCATION, educational)
    assertEquals(BookGenre.EDUCATION.displayName, "Education")
    assertEquals(BookGenre.FICTION, fiction)
    assertEquals("Fiction", fiction.displayName)
    assertEquals(BookGenre.NON_FICTION, nonFiction)
    assertEquals("Non-Fiction", nonFiction.displayName)
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
    assertEquals("Non-Fiction", resolveGenre("သုတစာပေ", "ဖေမြင့် နှလုံးသားအာဟာရ"))
    assertEquals("Science", resolveGenre("Science", "The Origin of Species"))
  }

  @Test
  fun `verify normalizeGenre maps aliases correctly`() {
    assertEquals("Literature", normalizeGenre("literature"))
    assertEquals("Literature", normalizeGenre("စာပေနှင့် ရသ"))
    assertEquals("History", normalizeGenre("history"))
    assertEquals("History", normalizeGenre("သမိုင်း"))
    assertEquals("Education", normalizeGenre("education"))
    assertEquals("Education", normalizeGenre("educational"))
    assertEquals("Education", normalizeGenre("ဘာသာစကား"))
    assertEquals("Fiction", normalizeGenre("fiction"))
    assertEquals("Fiction", normalizeGenre("ဝတ္ထု"))
    assertEquals("Non-Fiction", normalizeGenre("non-fiction"))
    assertEquals("Non-Fiction", normalizeGenre("nonfiction"))
    assertEquals("Non-Fiction", normalizeGenre("သုတစာပေ"))
    assertEquals("Science", normalizeGenre("science"))
    assertEquals("Science", normalizeGenre("သိပ္ပံ"))
  }

  @Test
  fun `verify authentic website archive volume total equals 43880`() {
    assertEquals(43_880, com.example.data.model.YATAL_WEBSITE_ARCHIVE_TOTAL)
    val sum = com.example.data.model.DEFAULT_YATAL_CATEGORIES.sumOf { it.count }
    assertEquals(43_880, sum)
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

  @Test
  fun `verify Room LocalBookEntity and LocalBookDao operations`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
        context,
        com.example.data.local.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val dao = db.localBookDao()

    val book = com.example.data.local.LocalBookEntity(
        id = 1L,
        title = "Clean Architecture",
        author = "Robert C. Martin",
        description = "A Craftsman's Guide to Software Structure and Design",
        localFilePath = "/data/user/0/com.aistudio.yatallibrary/files/clean_architecture.pdf",
        fileSize = 4500000L
    )

    // Verify entity fields
    assertEquals("Clean Architecture", book.title)
    assertEquals("Robert C. Martin", book.author)
    assertEquals("A Craftsman's Guide to Software Structure and Design", book.description)
    assertEquals("/data/user/0/com.aistudio.yatallibrary/files/clean_architecture.pdf", book.localFilePath)

    // Test insert
    val insertedId = dao.insertBook(book)
    assertEquals(1L, insertedId)

    // Test direct query by ID
    val retrieved = dao.getBookByIdDirect(1L)
    assertNotNull(retrieved)
    assertEquals("Clean Architecture", retrieved?.title)
    assertEquals("Robert C. Martin", retrieved?.author)
    assertEquals("A Craftsman's Guide to Software Structure and Design", retrieved?.description)
    assertEquals("/data/user/0/com.aistudio.yatallibrary/files/clean_architecture.pdf", retrieved?.localFilePath)

    // Test query by local file path
    val byPath = dao.getBookByLocalFilePath("/data/user/0/com.aistudio.yatallibrary/files/clean_architecture.pdf")
    assertNotNull(byPath)
    assertEquals(1L, byPath?.id)

    // Test update
    val updated = retrieved!!.copy(description = "Updated software design book")
    dao.updateBook(updated)
    val afterUpdate = dao.getBookByIdDirect(1L)
    assertEquals("Updated software design book", afterUpdate?.description)

    // Test delete
    dao.deleteBookById(1L)
    val afterDelete = dao.getBookByIdDirect(1L)
    org.junit.Assert.assertNull(afterDelete)

    db.close()
  }

  @Test
  fun `verify BookEntity supports title, author, description, and localFilePath`() {
    val entity = com.example.data.local.BookEntity(
        id = 55L,
        title = "Kotlin in Depth",
        author = "Alexey Soshin",
        description = "Deep dive into Kotlin programming language features and coroutines",
        localFilePath = "/cache/books/kotlin_in_depth.pdf"
    )

    assertEquals("Kotlin in Depth", entity.title)
    assertEquals("Alexey Soshin", entity.author)
    assertEquals("Deep dive into Kotlin programming language features and coroutines", entity.description)
    assertEquals("/cache/books/kotlin_in_depth.pdf", entity.localFilePath)
  }

  @Test
  fun `verify PdfCacheManager isValidPdf distinguishes genuine PDF from HTML error page`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val tempDir = context.cacheDir

    // Create a fake HTML error file
    val htmlFile = java.io.File(tempDir, "test_error.html")
    htmlFile.writeText("<!DOCTYPE html><html><body>503 Maintenance</body></html>")
    org.junit.Assert.assertFalse(com.example.util.PdfCacheManager.isValidPdf(htmlFile))

    // Create a valid PDF header file
    val validPdfFile = java.io.File(tempDir, "test_valid.pdf")
    val validBytes = byteArrayOf(0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34) // %PDF-1.4
    val dummyPadding = ByteArray(200) { 0x20 }
    validPdfFile.writeBytes(validBytes + dummyPadding)
    org.junit.Assert.assertTrue(com.example.util.PdfCacheManager.isValidPdf(validPdfFile))

    htmlFile.delete()
    validPdfFile.delete()
  }

  @Test
  fun `verify BookPdfGenerator produces valid readable PDF for direct reading`() {
    kotlinx.coroutines.runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val testBook = com.example.data.model.Book(
          id = 9999L,
          category = "Literature",
          rawCategory = "Literature",
          title = "To Kill a Mockingbird",
          cleanTitle = "To Kill a Mockingbird",
          author = "Harper Lee",
          synopsis = "A classic novel exploring justice and compassion in the American South.",
          publicationDate = "1960 ခုနှစ်",
          rawFilename = "mockingbird.pdf",
          size = 1500000L,
          formattedSize = "1.5 MB",
          genre = "Literature",
          jacketIndex = 0,
          previewUrl = "https://library.yatal.tech/api/preview/9999",
          downloadUrl = "https://library.yatal.tech/api/download/9999",
          webUrl = "https://library.yatal.tech/?book=9999"
      )

      val targetFile = java.io.File(context.cacheDir, "test_book_9999.pdf")
      val success = com.example.util.BookPdfGenerator.generateBookPdf(testBook, targetFile)
      org.junit.Assert.assertTrue(success)
      org.junit.Assert.assertTrue(targetFile.exists())
      org.junit.Assert.assertTrue(com.example.util.PdfCacheManager.isValidPdf(targetFile))

      // Test getOrFetchPdf
      val fetched = com.example.util.PdfCacheManager.getOrFetchPdf(context, testBook)
      assertNotNull(fetched)
      org.junit.Assert.assertTrue(com.example.util.PdfCacheManager.isValidPdf(fetched))

      targetFile.delete()
    }
  }

  @Test
  fun `verify BookPdfGenerator generates full 28 page edition and PdfCacheManager imports pdf`() {
    kotlinx.coroutines.runBlocking {
      val context = ApplicationProvider.getApplicationContext<Context>()
      val testBook = com.example.data.model.Book(
          id = 8888L,
          category = "History",
          rawCategory = "History",
          title = "Burmese History Chronicles",
          cleanTitle = "Burmese History Chronicles",
          author = "Dr. Than Tun",
          synopsis = "Detailed historical record of Myanmar dynasties and cultural evolution.",
          publicationDate = "1975 ခုနှစ်",
          rawFilename = "history_chronicles.pdf",
          size = 3500000L,
          formattedSize = "3.5 MB",
          genre = "History",
          jacketIndex = 1,
          previewUrl = "https://library.yatal.tech/api/preview/8888",
          downloadUrl = "https://library.yatal.tech/api/download/8888",
          webUrl = "https://library.yatal.tech/?book=8888"
      )

      val targetFile = java.io.File(context.cacheDir, "test_book_8888.pdf")
      val success = com.example.util.BookPdfGenerator.generateBookPdf(testBook, targetFile)
      org.junit.Assert.assertTrue(success)
      org.junit.Assert.assertTrue(targetFile.exists())
      org.junit.Assert.assertTrue(com.example.util.PdfCacheManager.isValidPdf(targetFile))

      // Verify page count is 28
      val helper = com.example.util.PdfRendererHelper(targetFile)
      assertEquals(28, helper.pageCount)
      helper.close()

      // Test importPdfStream
      val sampleStream = java.io.ByteArrayInputStream(targetFile.readBytes())
      val importSuccess = com.example.util.PdfCacheManager.importPdfStream(context, 7777L, sampleStream)
      org.junit.Assert.assertTrue(importSuccess)
      val importedFile = com.example.util.PdfCacheManager.getCachedPdfFile(context, 7777L)
      assertNotNull(importedFile)
      org.junit.Assert.assertTrue(com.example.util.PdfCacheManager.isValidPdf(importedFile))

      targetFile.delete()
      importedFile?.delete()
    }
  }

  @Test
  fun `verify loadNextArchiveBatch expands books in Room for genres`() = kotlinx.coroutines.runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
        context,
        com.example.data.local.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val api = com.example.data.remote.YatalApiService.create()
    val repo = com.example.data.repository.BookRepository(api, db.bookDao(), db.localBookDao())

    // Initial count is 0
    assertEquals(0, db.bookDao().getCachedBookCountDirect())

    // Seed initial books
    repo.seedInitialBooksIfEmpty()
    val countAfterSeed = db.bookDao().getCachedBookCountDirect()
    org.junit.Assert.assertTrue(countAfterSeed >= 30)

    // Load Next Archive Batch for Fiction
    val fictionAdded = repo.loadNextArchiveBatch(genre = "Fiction", page = 2)
    org.junit.Assert.assertTrue(fictionAdded >= 15)
    val countAfterFiction = db.bookDao().getCachedBookCountDirect()
    assertEquals(countAfterSeed + fictionAdded, countAfterFiction)

    // Load Next Archive Batch for Non-Fiction
    val nonFictionAdded = repo.loadNextArchiveBatch(genre = "Non-Fiction", page = 2)
    org.junit.Assert.assertTrue(nonFictionAdded >= 15)
    val countAfterNonFiction = db.bookDao().getCachedBookCountDirect()
    assertEquals(countAfterFiction + nonFictionAdded, countAfterNonFiction)

    // Load Next Archive Batch for Education
    val educationAdded = repo.loadNextArchiveBatch(genre = "Education", page = 2)
    org.junit.Assert.assertTrue(educationAdded >= 15)
    val countAfterEducation = db.bookDao().getCachedBookCountDirect()
    assertEquals(countAfterNonFiction + educationAdded, countAfterEducation)

    // Load Next Archive Batch for History
    val historyAdded = repo.loadNextArchiveBatch(genre = "History", page = 2)
    org.junit.Assert.assertTrue(historyAdded >= 15)
    val countAfterHistory = db.bookDao().getCachedBookCountDirect()
    assertEquals(countAfterEducation + historyAdded, countAfterHistory)
  }
}


