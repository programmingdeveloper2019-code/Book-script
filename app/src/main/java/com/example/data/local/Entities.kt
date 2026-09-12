package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.Book
import com.example.data.model.ReadingProgress
import com.example.data.model.cleanBookTitle
import com.example.data.model.extractAuthorAndTitle
import com.example.data.model.formatBytes
import com.example.data.model.normalizeGenre
import com.example.data.model.resolveGenre

@Entity(tableName = "cached_books")
data class BookEntity(
    @PrimaryKey val id: Long,
    val category: String,
    val rawCategory: String?,
    val title: String,
    val author: String = "",
    val rawFilename: String?,
    val size: Long,
    val genre: String = "General",
    val cachedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cached_categories")
data class CategoryEntity(
    @PrimaryKey val name: String,
    val count: Int,
    val displayOrder: Int = 0
)

@Entity(tableName = "favorite_books")
data class FavoriteEntity(
    @PrimaryKey val bookId: Long,
    val title: String,
    val author: String = "",
    val category: String,
    val rawCategory: String?,
    val rawFilename: String?,
    val size: Long,
    val genre: String = "အထူးနှင့် အထွေထွေ",
    val savedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "reading_progress")
data class ReadingProgressEntity(
    @PrimaryKey val bookId: Long,
    val title: String,
    val author: String = "",
    val category: String = "",
    val rawCategory: String? = null,
    val rawFilename: String? = null,
    val size: Long = 0L,
    val genre: String = "အထူးနှင့် အထွေထွေ",
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val scrollRatio: Float = 0f,
    val lastLocationDescription: String = "",
    val lastReadAt: Long = System.currentTimeMillis()
)

fun BookEntity.toBook(isFavorite: Boolean = false): Book {
    val extracted = if (this.author.isNotBlank()) {
        cleanBookTitle(this.title)
    } else {
        extractAuthorAndTitle(this.title).title.ifBlank { cleanBookTitle(this.title) }
    }
    val resolvedAuthor = if (this.author.isNotBlank()) {
        this.author
    } else {
        extractAuthorAndTitle(this.title).author
    }
    val cat = this.category.ifBlank { "အထွေထွေ" }
    return Book(
        id = this.id,
        category = cat,
        rawCategory = this.rawCategory,
        title = this.title,
        cleanTitle = extracted,
        author = resolvedAuthor,
        rawFilename = this.rawFilename,
        size = this.size,
        formattedSize = formatBytes(this.size),
        genre = if (this.genre.isNotBlank()) normalizeGenre(this.genre) else resolveGenre(cat, this.title),
        jacketIndex = (kotlin.math.abs(this.id) % 6).toInt(),
        previewUrl = "https://library.yatal.tech/api/preview/${this.id}",
        downloadUrl = "https://library.yatal.tech/api/download/${this.id}",
        webUrl = "https://library.yatal.tech/?book=${this.id}",
        isFavorite = isFavorite
    )
}

fun FavoriteEntity.toBook(): Book {
    val extracted = if (this.author.isNotBlank()) {
        cleanBookTitle(this.title)
    } else {
        extractAuthorAndTitle(this.title).title.ifBlank { cleanBookTitle(this.title) }
    }
    val resolvedAuthor = if (this.author.isNotBlank()) {
        this.author
    } else {
        extractAuthorAndTitle(this.title).author
    }
    val cat = this.category.ifBlank { "အထွေထွေ" }
    return Book(
        id = this.bookId,
        category = cat,
        rawCategory = this.rawCategory,
        title = this.title,
        cleanTitle = extracted,
        author = resolvedAuthor,
        rawFilename = this.rawFilename,
        size = this.size,
        formattedSize = formatBytes(this.size),
        genre = if (this.genre.isNotBlank()) normalizeGenre(this.genre) else resolveGenre(cat, this.title),
        jacketIndex = (kotlin.math.abs(this.bookId) % 6).toInt(),
        previewUrl = "https://library.yatal.tech/api/preview/${this.bookId}",
        downloadUrl = "https://library.yatal.tech/api/download/${this.bookId}",
        webUrl = "https://library.yatal.tech/?book=${this.bookId}",
        isFavorite = true
    )
}

fun Book.toFavoriteEntity(): FavoriteEntity {
    return FavoriteEntity(
        bookId = this.id,
        title = this.title,
        author = this.author,
        category = this.category,
        rawCategory = this.rawCategory,
        rawFilename = this.rawFilename,
        size = this.size,
        genre = this.genre,
        savedAt = System.currentTimeMillis()
    )
}

fun Book.toEntity(): BookEntity {
    return BookEntity(
        id = this.id,
        category = this.category,
        rawCategory = this.rawCategory,
        title = this.title,
        author = this.author,
        rawFilename = this.rawFilename,
        size = this.size,
        genre = this.genre
    )
}

fun ReadingProgressEntity.toReadingProgress(): ReadingProgress {
    return ReadingProgress(
        bookId = this.bookId,
        title = this.title,
        author = this.author,
        category = this.category,
        rawCategory = this.rawCategory,
        rawFilename = this.rawFilename,
        size = this.size,
        genre = this.genre,
        currentPage = this.currentPage,
        totalPages = this.totalPages,
        scrollRatio = this.scrollRatio,
        lastLocationDescription = this.lastLocationDescription,
        lastReadAt = this.lastReadAt
    )
}

fun ReadingProgress.toBook(isFavorite: Boolean = false): Book {
    val extracted = if (this.author.isNotBlank()) {
        cleanBookTitle(this.title)
    } else {
        extractAuthorAndTitle(this.title).title.ifBlank { cleanBookTitle(this.title) }
    }
    val resolvedAuthor = if (this.author.isNotBlank()) {
        this.author
    } else {
        extractAuthorAndTitle(this.title).author
    }
    val cat = this.category.ifBlank { "အထွေထွေ" }
    return Book(
        id = this.bookId,
        category = cat,
        rawCategory = this.rawCategory,
        title = this.title,
        cleanTitle = extracted,
        author = resolvedAuthor,
        rawFilename = this.rawFilename,
        size = this.size,
        formattedSize = formatBytes(this.size),
        genre = if (this.genre.isNotBlank()) normalizeGenre(this.genre) else resolveGenre(cat, this.title),
        jacketIndex = (kotlin.math.abs(this.bookId) % 6).toInt(),
        previewUrl = "https://library.yatal.tech/api/preview/${this.bookId}",
        downloadUrl = "https://library.yatal.tech/api/download/${this.bookId}",
        webUrl = "https://library.yatal.tech/?book=${this.bookId}",
        isFavorite = isFavorite
    )
}

fun ReadingProgress.toEntity(): ReadingProgressEntity {
    return ReadingProgressEntity(
        bookId = this.bookId,
        title = this.title,
        author = this.author,
        category = this.category,
        rawCategory = this.rawCategory,
        rawFilename = this.rawFilename,
        size = this.size,
        genre = this.genre,
        currentPage = this.currentPage,
        totalPages = this.totalPages,
        scrollRatio = this.scrollRatio,
        lastLocationDescription = this.lastLocationDescription,
        lastReadAt = this.lastReadAt
    )
}
