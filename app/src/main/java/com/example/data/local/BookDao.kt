package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class CategoryStat(
    val category: String,
    val count: Int
)

data class GenreStat(
    val genre: String,
    val count: Int
)

@Dao
interface BookDao {

    @Query("SELECT * FROM cached_books ORDER BY cachedAt DESC")
    fun getAllCachedBooks(): Flow<List<BookEntity>>

    @Query("SELECT * FROM cached_books WHERE category = :category ORDER BY cachedAt DESC")
    fun getCachedBooksByCategory(category: String): Flow<List<BookEntity>>

    @Query("""
        SELECT * FROM cached_books 
        WHERE genre = :genre 
           OR (:genre = 'Fiction' AND (genre = 'Fiction' OR genre = 'Literature' OR genre = 'ဝတ္ထု / စာပေ' OR category LIKE '%ဝတ္ထု%'))
           OR (:genre = 'Non-Fiction' AND (genre = 'Non-Fiction' OR genre = 'သုတစာပေ' OR category LIKE '%သုတ%'))
           OR (:genre = 'Literature' AND (genre = 'Literature' OR genre = 'Fiction' OR genre = 'စာပေနှင့် ရသ'))
           OR (:genre = 'History' AND (genre = 'History' OR genre = 'သုတနှင့် သမိုင်း' OR genre = 'သမိုင်းနှင့် ရှေးဟောင်း'))
           OR (:genre = 'Education' AND (genre = 'Education' OR genre = 'ဘာသာစကား' OR genre = 'ပညာရေးနှင့် သင်ကြားရေး'))
        ORDER BY cachedAt DESC
    """)
    fun getCachedBooksByGenre(genre: String): Flow<List<BookEntity>>

    @Query("""
        SELECT * FROM cached_books
        WHERE (:genre = 'ALL' 
            OR genre = :genre 
            OR (:genre = 'Fiction' AND (genre = 'Fiction' OR genre = 'Literature' OR genre = 'ဝတ္ထု / စာပေ' OR category LIKE '%ဝတ္ထု%'))
            OR (:genre = 'Non-Fiction' AND (genre = 'Non-Fiction' OR genre = 'သုတစာပေ' OR category LIKE '%သုတ%'))
            OR (:genre = 'Literature' AND (genre = 'Literature' OR genre = 'Fiction' OR genre = 'စာပေနှင့် ရသ'))
            OR (:genre = 'History' AND (genre = 'History' OR genre = 'သုတနှင့် သမိုင်း' OR genre = 'သမိုင်းနှင့် ရှေးဟောင်း'))
            OR (:genre = 'Education' AND (genre = 'Education' OR genre = 'ဘာသာစကား' OR genre = 'ပညာရေးနှင့် သင်ကြားရေး'))
            OR (:genre = 'Technology' AND (genre = 'Technology' OR genre = 'tech' OR genre = 'နည်းပညာနှင့် IT'))
            OR (:genre = 'Business' AND (genre = 'Business' OR genre = 'စီးပွားရေးနှင့် စိတ်ပညာ' OR genre = 'စီးပွားရေးနှင့် စီမံခန့်ခွဲမှု'))
            OR (:genre = 'Science' AND (genre = 'Science' OR genre = 'သိပ္ပံနှင့် သုတ'))
            OR (:genre = 'Comics & Art' AND (genre = 'Comics & Art' OR genre = 'comics' OR genre = 'ရုပ်ပြနှင့် အနုပညာ'))
            OR (:genre = 'General' AND (genre = 'General' OR genre = 'general' OR genre = 'အထူးနှင့် အထွေထွေ'))
        )
        AND (:category = 'ALL' OR category = :category)
        AND (
            :query = '' 
            OR title LIKE '%' || :query || '%' 
            OR author LIKE '%' || :query || '%' 
            OR category LIKE '%' || :query || '%'
            OR rawFilename LIKE '%' || :query || '%'
            OR (:token1 != '' AND (
                (title LIKE '%' || :token1 || '%' OR author LIKE '%' || :token1 || '%')
                AND (:token2 = '' OR title LIKE '%' || :token2 || '%' OR author LIKE '%' || :token2 || '%')
            ))
        )
        ORDER BY 
            CASE WHEN :sortOrder = 'title_asc' THEN title END ASC,
            CASE WHEN :sortOrder = 'size_desc' THEN size END DESC,
            CASE WHEN :sortOrder = 'size_asc' THEN size END ASC,
            cachedAt DESC
    """)
    fun getFilteredBooks(
        genre: String = "ALL",
        category: String = "ALL",
        query: String = "",
        token1: String = "",
        token2: String = "",
        sortOrder: String = "recent"
    ): Flow<List<BookEntity>>

    @Query("""
        SELECT DISTINCT author FROM cached_books 
        WHERE author != '' AND author IS NOT NULL
        ORDER BY author ASC
    """)
    fun getAllAuthors(): Flow<List<String>>

    @Query("""
        SELECT * FROM cached_books
        WHERE author LIKE '%' || :author || '%'
        ORDER BY title ASC
    """)
    fun getBooksByAuthor(author: String): Flow<List<BookEntity>>

    @Query("SELECT category, COUNT(*) as count FROM cached_books GROUP BY category ORDER BY count DESC")
    fun getCategoryStats(): Flow<List<CategoryStat>>

    @Query("SELECT genre, COUNT(*) as count FROM cached_books GROUP BY genre ORDER BY count DESC")
    fun getGenreStats(): Flow<List<GenreStat>>

    @Query("SELECT DISTINCT category FROM cached_books WHERE (:genre = 'ALL' OR genre = :genre) ORDER BY category ASC")
    fun getCategoriesForGenre(genre: String): Flow<List<String>>

    @Query("SELECT DISTINCT genre FROM cached_books ORDER BY genre ASC")
    fun getAllGenres(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    @androidx.room.Update
    suspend fun updateBook(book: BookEntity)

    @androidx.room.Delete
    suspend fun deleteBook(book: BookEntity)

    @Query("SELECT * FROM cached_books WHERE id = :id LIMIT 1")
    fun getBookById(id: Long): Flow<BookEntity?>

    @Query("DELETE FROM cached_books")
    suspend fun clearBooks()

    @Query("SELECT * FROM cached_categories ORDER BY count DESC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM cached_categories WHERE name LIKE '%' || :query || '%' ORDER BY count DESC")
    fun searchCategories(query: String): Flow<List<CategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CategoryEntity>)

    @Query("DELETE FROM cached_categories")
    suspend fun clearCategories()

    @Query("SELECT * FROM favorite_books ORDER BY savedAt DESC")
    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_books WHERE bookId = :bookId)")
    fun isFavorite(bookId: Long): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorite_books WHERE bookId = :bookId")
    suspend fun deleteFavorite(bookId: Long)

    @Query("DELETE FROM favorite_books")
    suspend fun clearAllFavorites()

    @Query("SELECT COUNT(*) FROM favorite_books")
    fun getFavoriteCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cached_books")
    fun getCachedBookCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cached_books")
    suspend fun getCachedBookCountDirect(): Int

    // Reading Progress Tracking (Continue Reading)
    @Query("SELECT * FROM reading_progress ORDER BY lastReadAt DESC")
    fun getAllReadingProgress(): Flow<List<ReadingProgressEntity>>

    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId LIMIT 1")
    fun getReadingProgress(bookId: Long): Flow<ReadingProgressEntity?>

    @Query("SELECT * FROM reading_progress WHERE bookId = :bookId LIMIT 1")
    suspend fun getReadingProgressDirect(bookId: Long): ReadingProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingProgress(progress: ReadingProgressEntity)

    @Query("DELETE FROM reading_progress WHERE bookId = :bookId")
    suspend fun deleteReadingProgress(bookId: Long)

    @Query("DELETE FROM reading_progress")
    suspend fun clearAllReadingProgress()

    @Query("SELECT COUNT(*) FROM reading_progress")
    fun getReadingProgressCount(): Flow<Int>
}
