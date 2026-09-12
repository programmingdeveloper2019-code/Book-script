package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BookGenre
import com.example.data.model.normalizeGenre
import com.example.data.model.resolveGenre
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

    assertEquals(BookGenre.LITERATURE, literature)
    assertEquals("Literature", literature.displayName)
    assertEquals(BookGenre.HISTORY, history)
    assertEquals("History", history.displayName)
    assertEquals(BookGenre.EDUCATION, education)
    assertEquals(BookGenre.EDUCATION.displayName, "Education")
  }

  @Test
  fun `verify genre resolution from category and title`() {
    assertEquals("Literature", resolveGenre("Literature", "Pride and Prejudice"))
    assertEquals("Literature", resolveGenre("ရသစာပေ", "ဂျူး ဝတ္ထုတိုများ"))
    assertEquals("History", resolveGenre("သမိုင်း", "World War II Chronicles"))
    assertEquals("History", resolveGenre("History", "A Brief History of Time"))
    assertEquals("Education", resolveGenre("Education", "Cambridge Grammar in Use"))
    assertEquals("Education", resolveGenre("ဘာသာစကား", "English Speaking Practice"))
  }

  @Test
  fun `verify normalizeGenre maps aliases correctly`() {
    assertEquals("Literature", normalizeGenre("literature"))
    assertEquals("Literature", normalizeGenre("စာပေနှင့် ရသ"))
    assertEquals("History", normalizeGenre("history"))
    assertEquals("History", normalizeGenre("သမိုင်း"))
    assertEquals("Education", normalizeGenre("education"))
    assertEquals("Education", normalizeGenre("ဘာသာစကား"))
  }
}

