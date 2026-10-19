package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.local.AppDatabase
import com.example.data.local.BookDao
import com.example.data.local.BookEntity
import com.example.data.local.CategoryEntity
import com.example.data.local.CategoryStat
import com.example.data.local.FavoriteEntity
import com.example.data.local.GenreStat
import com.example.data.local.ReadingProgressEntity
import com.example.data.local.toBook
import com.example.data.local.toFavoriteEntity
import com.example.data.local.toReadingProgress
import com.example.data.model.Book
import com.example.data.model.BookDto
import com.example.data.model.BooksResponseDto
import com.example.data.model.CategoryDto
import com.example.data.model.QuotaDto
import com.example.data.model.ReadingProgress
import com.example.data.model.extractAuthorAndTitle
import com.example.data.model.resolveGenre
import com.example.data.model.toBook
import com.example.data.remote.YatalApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class BookRepository(
    private val apiService: YatalApiService,
    private val bookDao: BookDao
) {

    val allFavorites: Flow<List<Book>> = bookDao.getAllFavorites().map { list ->
        list.map { it.toBook() }
    }

    val cachedCategories: Flow<List<CategoryEntity>> = bookDao.getAllCategories()

    val cachedBooks: Flow<List<Book>> = bookDao.getAllCachedBooks().map { list ->
        list.map { it.toBook() }
    }

    val categoryStats: Flow<List<CategoryStat>> = bookDao.getCategoryStats()

    val genreStats: Flow<List<GenreStat>> = bookDao.getGenreStats()

    val cachedBookCount: Flow<Int> = bookDao.getCachedBookCount()

    val allAuthors: Flow<List<String>> = bookDao.getAllAuthors()

    val readingProgressList: Flow<List<ReadingProgress>> = bookDao.getAllReadingProgress().map { list ->
        list.map { it.toReadingProgress() }
    }

    fun getReadingProgress(bookId: Long): Flow<ReadingProgress?> =
        bookDao.getReadingProgress(bookId).map { it?.toReadingProgress() }

    suspend fun getReadingProgressDirect(bookId: Long): ReadingProgress? = withContext(Dispatchers.IO) {
        bookDao.getReadingProgressDirect(bookId)?.toReadingProgress()
    }

    fun getFilteredBooksFromRoom(
        genre: String = "ALL",
        category: String = "ALL",
        query: String = "",
        sortOrder: String = "recent"
    ): Flow<List<Book>> {
        val trimmed = query.trim()
        val tokens = if (trimmed.isNotBlank()) {
            trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
        } else {
            emptyList()
        }
        val t1 = tokens.getOrNull(0) ?: ""
        val t2 = tokens.getOrNull(1) ?: ""

        return combine(
            bookDao.getFilteredBooks(
                genre = genre,
                category = category,
                query = trimmed,
                token1 = t1,
                token2 = t2,
                sortOrder = sortOrder
            ),
            bookDao.getAllFavorites()
        ) { bookEntities, favEntities ->
            val favIds = favEntities.map { it.bookId }.toSet()
            bookEntities.map { entity ->
                entity.toBook(isFavorite = favIds.contains(entity.id))
            }
        }
    }

    fun isBookFavorite(bookId: Long): Flow<Boolean> = bookDao.isFavorite(bookId)

    suspend fun fetchCategories(): Result<List<CategoryDto>> = withContext(Dispatchers.IO) {
        try {
            val categories = apiService.getCategories()
            val entities = categories.mapIndexed { index, cat ->
                CategoryEntity(
                    name = cat.name,
                    count = cat.count,
                    displayOrder = index
                )
            }
            bookDao.insertCategories(entities)
            Result.success(categories)
        } catch (e: Exception) {
            Log.w("BookRepository", "Failed to fetch categories online, fallback to cache", e)
            Result.failure(e)
        }
    }

    suspend fun fetchBooks(
        category: String = "ALL",
        query: String = "",
        page: Int = 1,
        limit: Int = 60
    ): Result<BooksResponseDto> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getBooks(
                category = category,
                query = query,
                page = page,
                limit = limit
            )

            if (response.books.isNotEmpty()) {
                val entities = response.books.map { dto ->
                    val resolvedGenre = resolveGenre(dto.category, dto.title)
                    val extracted = extractAuthorAndTitle(dto.title)
                    BookEntity(
                        id = dto.id,
                        category = dto.category.ifBlank { "အထွေထွေ" },
                        rawCategory = dto.rawCategory,
                        title = dto.title,
                        author = extracted.author,
                        rawFilename = dto.rawFilename,
                        size = dto.size,
                        genre = resolvedGenre
                    )
                }
                bookDao.insertBooks(entities)
            }

            Result.success(response)
        } catch (e: Exception) {
            Log.w("BookRepository", "Failed to fetch books online: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchQuota(): QuotaDto? = withContext(Dispatchers.IO) {
        try {
            apiService.getQuota()
        } catch (e: Exception) {
            Log.w("BookRepository", "Failed to fetch quota: ${e.message}")
            null
        }
    }

    suspend fun toggleFavorite(book: Book): Boolean = withContext(Dispatchers.IO) {
        if (book.isFavorite) {
            bookDao.deleteFavorite(book.id)
            false
        } else {
            bookDao.insertFavorite(book.toFavoriteEntity())
            true
        }
    }

    suspend fun saveFavorite(book: Book) = withContext(Dispatchers.IO) {
        bookDao.insertFavorite(book.toFavoriteEntity())
    }

    suspend fun removeFavorite(bookId: Long) = withContext(Dispatchers.IO) {
        bookDao.deleteFavorite(bookId)
    }

    suspend fun clearAllFavorites() = withContext(Dispatchers.IO) {
        bookDao.clearAllFavorites()
    }

    suspend fun saveReadingProgress(
        book: Book,
        currentPage: Int,
        totalPages: Int = 1,
        scrollRatio: Float = 0f,
        lastLocationDescription: String = ""
    ) = withContext(Dispatchers.IO) {
        val entity = ReadingProgressEntity(
            bookId = book.id,
            title = book.title,
            author = book.author,
            category = book.category,
            rawCategory = book.rawCategory,
            rawFilename = book.rawFilename,
            size = book.size,
            genre = book.genre,
            currentPage = currentPage.coerceAtLeast(1),
            totalPages = totalPages.coerceAtLeast(1),
            scrollRatio = scrollRatio.coerceIn(0f, 1f),
            lastLocationDescription = lastLocationDescription.ifBlank { "စာမျက်နှာ $currentPage" },
            lastReadAt = System.currentTimeMillis()
        )
        bookDao.insertReadingProgress(entity)
    }

    suspend fun deleteReadingProgress(bookId: Long) = withContext(Dispatchers.IO) {
        bookDao.deleteReadingProgress(bookId)
    }

    suspend fun clearAllReadingProgress() = withContext(Dispatchers.IO) {
        bookDao.clearAllReadingProgress()
    }

    suspend fun seedInitialBooksIfEmpty() = withContext(Dispatchers.IO) {
        if (bookDao.getCachedBookCountDirect() == 0) {
            val seedBooks = listOf(
                // Literature
                BookEntity(
                    id = 1001L,
                    category = "Literature",
                    rawCategory = "Literature",
                    title = "The Great Gatsby - F. Scott Fitzgerald",
                    author = "F. Scott Fitzgerald",
                    rawFilename = "the_great_gatsby.pdf",
                    size = 1_450_000L,
                    genre = "Literature"
                ),
                BookEntity(
                    id = 1002L,
                    category = "Literature",
                    rawCategory = "Literature",
                    title = "Pride and Prejudice - Jane Austen",
                    author = "Jane Austen",
                    rawFilename = "pride_and_prejudice.pdf",
                    size = 2_150_000L,
                    genre = "Literature"
                ),
                BookEntity(
                    id = 1003L,
                    category = "ရသစာပေ",
                    rawCategory = "ရသစာပေ",
                    title = "ဂျူး - အမှတ်တရ ဝတ္ထုတိုများ",
                    author = "ဂျူး",
                    rawFilename = "juu_memories.pdf",
                    size = 1_850_000L,
                    genre = "Literature"
                ),
                BookEntity(
                    id = 1004L,
                    category = "ဝတ္ထုရှည်",
                    rawCategory = "ဝတ္ထုရှည်",
                    title = "မင်းလူ - နှလုံးသားထဲက အလင်းတန်း",
                    author = "မင်းလူ",
                    rawFilename = "min_lu_light.pdf",
                    size = 2_400_000L,
                    genre = "Literature"
                ),
                BookEntity(
                    id = 1005L,
                    category = "Literature",
                    rawCategory = "Literature",
                    title = "To Kill a Mockingbird - Harper Lee",
                    author = "Harper Lee",
                    rawFilename = "to_kill_a_mockingbird.pdf",
                    size = 1_950_000L,
                    genre = "Literature"
                ),
                // History
                BookEntity(
                    id = 2001L,
                    category = "History",
                    rawCategory = "History",
                    title = "A Brief History of Humankind - Sapiens",
                    author = "Yuval Noah Harari",
                    rawFilename = "sapiens_brief_history.pdf",
                    size = 3_800_000L,
                    genre = "History"
                ),
                BookEntity(
                    id = 2002L,
                    category = "History",
                    rawCategory = "History",
                    title = "World History: Ancient Civilizations to Modern Era",
                    author = "Historical Society",
                    rawFilename = "world_history_chronicles.pdf",
                    size = 4_500_000L,
                    genre = "History"
                ),
                BookEntity(
                    id = 2003L,
                    category = "သမိုင်း",
                    rawCategory = "သမိုင်း",
                    title = "မြန်မာ့သမိုင်းမှတ်တမ်းနှင့် ရှေးဟောင်းယဉ်ကျေးမှု",
                    author = "ဒေါက်တာသန်းထွန်း",
                    rawFilename = "myanmar_history_records.pdf",
                    size = 3_200_000L,
                    genre = "History"
                ),
                BookEntity(
                    id = 2004L,
                    category = "History",
                    rawCategory = "History",
                    title = "Guns, Germs, and Steel - Jared Diamond",
                    author = "Jared Diamond",
                    rawFilename = "guns_germs_steel.pdf",
                    size = 4_100_000L,
                    genre = "History"
                ),
                // Education
                BookEntity(
                    id = 3001L,
                    category = "Education",
                    rawCategory = "Education",
                    title = "English Grammar in Use - Raymond Murphy",
                    author = "Raymond Murphy",
                    rawFilename = "english_grammar_in_use.pdf",
                    size = 5_200_000L,
                    genre = "Education"
                ),
                BookEntity(
                    id = 3002L,
                    category = "Education",
                    rawCategory = "Education",
                    title = "Cambridge Academic English & Vocabulary Guide",
                    author = "Cambridge University Press",
                    rawFilename = "cambridge_academic_english.pdf",
                    size = 6_100_000L,
                    genre = "Education"
                ),
                BookEntity(
                    id = 3003L,
                    category = "Education",
                    rawCategory = "Education",
                    title = "Oxford Advanced Learner's Dictionary",
                    author = "Oxford University Press",
                    rawFilename = "oxford_advanced_learners.pdf",
                    size = 8_500_000L,
                    genre = "Education"
                ),
                BookEntity(
                    id = 3004L,
                    category = "Education",
                    rawCategory = "Education",
                    title = "Educational Psychology & Modern Learning Techniques",
                    author = "Education Research Group",
                    rawFilename = "educational_psychology.pdf",
                    size = 3_400_000L,
                    genre = "Education"
                ),
                BookEntity(
                    id = 3005L,
                    category = "Education",
                    rawCategory = "Education",
                    title = "Headway English Course: Advanced Student Guide",
                    author = "Liz and John Soars",
                    rawFilename = "headway_advanced.pdf",
                    size = 4_800_000L,
                    genre = "Education"
                ),
                // Technology
                BookEntity(
                    id = 4001L,
                    category = "Technology",
                    rawCategory = "Technology",
                    title = "Clean Code: Handbook of Agile Software Craftsmanship",
                    author = "Robert C. Martin",
                    rawFilename = "clean_code.pdf",
                    size = 3_900_000L,
                    genre = "Technology"
                ),
                BookEntity(
                    id = 4002L,
                    category = "Technology",
                    rawCategory = "Technology",
                    title = "Kotlin in Action - Modern Android Programming",
                    author = "Dmitry Jemerov",
                    rawFilename = "kotlin_in_action.pdf",
                    size = 4_200_000L,
                    genre = "Technology"
                ),
                // Business
                BookEntity(
                    id = 5001L,
                    category = "Business",
                    rawCategory = "Business",
                    title = "Rich Dad Poor Dad - Financial Education",
                    author = "Robert Kiyosaki",
                    rawFilename = "rich_dad_poor_dad.pdf",
                    size = 2_600_000L,
                    genre = "Business"
                ),
                BookEntity(
                    id = 5002L,
                    category = "Business",
                    rawCategory = "Business",
                    title = "Atomic Habits - Tiny Changes, Remarkable Results",
                    author = "James Clear",
                    rawFilename = "atomic_habits.pdf",
                    size = 2_900_000L,
                    genre = "Business"
                )
            )

            val seedCategories = listOf(
                CategoryEntity("Literature", 5, 0),
                CategoryEntity("History", 4, 1),
                CategoryEntity("Education", 5, 2),
                CategoryEntity("Technology", 2, 3),
                CategoryEntity("Business", 2, 4),
                CategoryEntity("စာအုပ်အထွေထွေ", 12, 5),
                CategoryEntity("English Books", 15, 6),
                CategoryEntity("သုတ၊ ရသ", 8, 7)
            )

            bookDao.insertBooks(seedBooks)
            bookDao.insertCategories(seedCategories)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: BookRepository? = null

        fun getInstance(context: Context): BookRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val api = YatalApiService.create()
                val repo = BookRepository(api, db.bookDao())
                INSTANCE = repo
                repo
            }
        }
    }
}
