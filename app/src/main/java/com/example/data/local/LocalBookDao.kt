package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for managing local book records.
 * Provides CRUD operations and reactive queries using Kotlin Flow.
 */
@Dao
interface LocalBookDao {

    @Query("SELECT * FROM books ORDER BY created_at DESC")
    fun getAllBooks(): Flow<List<LocalBookEntity>>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    fun getBookById(id: Long): Flow<LocalBookEntity?>

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getBookByIdDirect(id: Long): LocalBookEntity?

    @Query("SELECT * FROM books WHERE local_file_path = :localFilePath LIMIT 1")
    suspend fun getBookByLocalFilePath(localFilePath: String): LocalBookEntity?

    @Query("""
        SELECT * FROM books 
        WHERE title LIKE '%' || :query || '%' 
           OR author LIKE '%' || :query || '%' 
           OR description LIKE '%' || :query || '%' 
        ORDER BY title ASC
    """)
    fun searchBooks(query: String): Flow<List<LocalBookEntity>>

    @Query("SELECT * FROM books WHERE author LIKE '%' || :author || '%' ORDER BY title ASC")
    fun getBooksByAuthor(author: String): Flow<List<LocalBookEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: LocalBookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<LocalBookEntity>): List<Long>

    @Update
    suspend fun updateBook(book: LocalBookEntity)

    @Delete
    suspend fun deleteBook(book: LocalBookEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Query("DELETE FROM books")
    suspend fun clearAllBooks()

    @Query("SELECT COUNT(*) FROM books")
    fun getBookCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM books")
    suspend fun getBookCountDirect(): Int
}
