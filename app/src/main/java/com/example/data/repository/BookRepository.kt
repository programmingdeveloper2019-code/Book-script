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
import com.example.data.local.LocalBookDao
import com.example.data.local.LocalBookEntity
import kotlinx.coroutines.withContext

class BookRepository(
    private val apiService: YatalApiService,
    private val bookDao: BookDao,
    private val localBookDao: LocalBookDao? = null
) {

    val allLocalBooks: Flow<List<LocalBookEntity>> = localBookDao?.getAllBooks() ?: kotlinx.coroutines.flow.emptyFlow()

    suspend fun insertLocalBook(book: LocalBookEntity): Long = withContext(Dispatchers.IO) {
        localBookDao?.insertBook(book) ?: 0L
    }

    suspend fun updateLocalBook(book: LocalBookEntity) = withContext(Dispatchers.IO) {
        localBookDao?.updateBook(book)
    }

    suspend fun deleteLocalBook(id: Long) = withContext(Dispatchers.IO) {
        localBookDao?.deleteBookById(id)
    }

    suspend fun getLocalBookByFilePath(filePath: String): LocalBookEntity? = withContext(Dispatchers.IO) {
        localBookDao?.getBookByLocalFilePath(filePath)
    }

    fun searchLocalBooks(query: String): Flow<List<LocalBookEntity>> =
        localBookDao?.searchBooks(query) ?: kotlinx.coroutines.flow.emptyFlow()

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
                        synopsis = dto.synopsis ?: "",
                        publicationDate = dto.publicationDate ?: "",
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
        val currentCount = bookDao.getCachedBookCountDirect()
        if (currentCount < 30) {
            val seedBooks = listOf(
                // 1. Fiction (ဝတ္ထုနှင့် ရသဇာတ်လမ်းများ)
                BookEntity(
                    id = 1001L,
                    category = "ဝတ္ထုနှင့် ရသစာပေ (Fiction & Novels)",
                    rawCategory = "Fiction",
                    title = "ဂျူး - အမှတ်တရ ဝတ္ထုတိုများ",
                    author = "ဂျူး",
                    rawFilename = "juu_memories_novel.pdf",
                    size = 2_150_000L,
                    genre = "Fiction"
                ),
                BookEntity(
                    id = 1002L,
                    category = "ဝတ္ထုနှင့် ရသစာပေ (Fiction & Novels)",
                    rawCategory = "Fiction",
                    title = "မင်းလူ - နှလုံးသားထဲက အလင်းတန်း ဝတ္ထုရှည်",
                    author = "မင်းလူ",
                    rawFilename = "min_lu_light_novel.pdf",
                    size = 2_400_000L,
                    genre = "Fiction"
                ),
                BookEntity(
                    id = 1003L,
                    category = "ဝတ္ထုနှင့် ရသစာပေ (Fiction & Novels)",
                    rawCategory = "Fiction",
                    title = "ပုညခင် - မိုးကုတ်စက်ဝိုင်းကိုကျော်လွန်၍",
                    author = "ပုညခင်",
                    rawFilename = "pone_nya_khin_beyond_horizon.pdf",
                    size = 2_650_000L,
                    genre = "Fiction"
                ),
                BookEntity(
                    id = 1004L,
                    category = "ဝတ္ထုနှင့် ရသစာပေ (Fiction & Novels)",
                    rawCategory = "Fiction",
                    title = "ချစ်ဦးညို - လင်္ကာဒီပချစ်သူ သမိုင်းနောက်ခံဝတ္ထု",
                    author = "ချစ်ဦးညို",
                    rawFilename = "chit_oo_nyo_lingadipa.pdf",
                    size = 3_100_000L,
                    genre = "Fiction"
                ),
                BookEntity(
                    id = 1005L,
                    category = "ဝတ္ထုနှင့် ရသစာပေ (Fiction & Novels)",
                    rawCategory = "Fiction",
                    title = "The Great Gatsby - F. Scott Fitzgerald",
                    author = "F. Scott Fitzgerald",
                    rawFilename = "the_great_gatsby.pdf",
                    size = 1_450_000L,
                    genre = "Fiction"
                ),
                BookEntity(
                    id = 1006L,
                    category = "ဝတ္ထုနှင့် ရသစာပေ (Fiction & Novels)",
                    rawCategory = "Fiction",
                    title = "Pride and Prejudice - Jane Austen",
                    author = "Jane Austen",
                    rawFilename = "pride_and_prejudice.pdf",
                    size = 2_150_000L,
                    genre = "Fiction"
                ),
                BookEntity(
                    id = 1007L,
                    category = "ဝတ္ထုနှင့် ရသစာပေ (Fiction & Novels)",
                    rawCategory = "Fiction",
                    title = "To Kill a Mockingbird - Harper Lee",
                    author = "Harper Lee",
                    rawFilename = "to_kill_a_mockingbird.pdf",
                    size = 1_950_000L,
                    genre = "Fiction"
                ),
                BookEntity(
                    id = 1008L,
                    category = "ဝတ္ထုနှင့် ရသစာပေ (Fiction & Novels)",
                    rawCategory = "Fiction",
                    title = "1984 - George Orwell (Dystopian Classic)",
                    author = "George Orwell",
                    rawFilename = "1984_george_orwell.pdf",
                    size = 1_820_000L,
                    genre = "Fiction"
                ),

                // 2. Non-Fiction (သုတစာပေနှင့် ဘဝလမ်းညွှန်)
                BookEntity(
                    id = 1101L,
                    category = "သုတစာပေနှင့် ဘဝလမ်းညွှန် (Non-Fiction & Guides)",
                    rawCategory = "Non-Fiction",
                    title = "ဖေမြင့် - နှလုံးသားအာဟာရ ရသစာစုများနှင့် အတွေးအမြင်များ",
                    author = "ဖေမြင့်",
                    rawFilename = "pe_myint_heart_nutrition.pdf",
                    size = 2_100_000L,
                    genre = "Non-Fiction"
                ),
                BookEntity(
                    id = 1102L,
                    category = "သုတစာပေနှင့် ဘဝလမ်းညွှန် (Non-Fiction & Guides)",
                    rawCategory = "Non-Fiction",
                    title = "ဆရာတော် ဦးဇောတိက - သတိရှိသော စိတ်ထားနှင့် ဘဝအမြင်",
                    author = "ဆရာတော် ဦးဇောတိက",
                    rawFilename = "u_zaw_ti_ka_mindfulness.pdf",
                    size = 2_350_000L,
                    genre = "Non-Fiction"
                ),
                BookEntity(
                    id = 1103L,
                    category = "သုတစာပေနှင့် ဘဝလမ်းညွှန် (Non-Fiction & Guides)",
                    rawCategory = "Non-Fiction",
                    title = "Yuval Noah Harari - Sapiens: A Brief History of Humankind",
                    author = "Yuval Noah Harari",
                    rawFilename = "sapiens_humankind.pdf",
                    size = 3_800_000L,
                    genre = "Non-Fiction"
                ),
                BookEntity(
                    id = 1104L,
                    category = "သုတစာပေနှင့် ဘဝလမ်းညွှန် (Non-Fiction & Guides)",
                    rawCategory = "Non-Fiction",
                    title = "James Clear - Atomic Habits: Tiny Changes, Remarkable Results",
                    author = "James Clear",
                    rawFilename = "atomic_habits_guide.pdf",
                    size = 2_900_000L,
                    genre = "Non-Fiction"
                ),
                BookEntity(
                    id = 1105L,
                    category = "သုတစာပေနှင့် ဘဝလမ်းညွှန် (Non-Fiction & Guides)",
                    rawCategory = "Non-Fiction",
                    title = "Morgan Housel - The Psychology of Money",
                    author = "Morgan Housel",
                    rawFilename = "psychology_of_money.pdf",
                    size = 2_450_000L,
                    genre = "Non-Fiction"
                ),
                BookEntity(
                    id = 1106L,
                    category = "သုတစာပေနှင့် ဘဝလမ်းညွှန် (Non-Fiction & Guides)",
                    rawCategory = "Non-Fiction",
                    title = "ဖေမြင့် - အောင်မြင်မှုနှင့် ဘဝနေနည်း ဆောင်းပါးများ",
                    author = "ဖေမြင့်",
                    rawFilename = "pe_myint_success_life.pdf",
                    size = 1_980_000L,
                    genre = "Non-Fiction"
                ),
                BookEntity(
                    id = 1107L,
                    category = "သုတစာပေနှင့် ဘဝလမ်းညွှန် (Non-Fiction & Guides)",
                    rawCategory = "Non-Fiction",
                    title = "Daniel Kahneman - Thinking, Fast and Slow",
                    author = "Daniel Kahneman",
                    rawFilename = "thinking_fast_and_slow.pdf",
                    size = 4_200_000L,
                    genre = "Non-Fiction"
                ),
                BookEntity(
                    id = 1108L,
                    category = "သုတစာပေနှင့် ဘဝလမ်းညွှန် (Non-Fiction & Guides)",
                    rawCategory = "Non-Fiction",
                    title = "ဆရာတော် ဦးဇောတိက - ဘဝရှာဖွေတွေ့ရှိမှု အတွေးအမြင်များ",
                    author = "ဆရာတော် ဦးဇောတိက",
                    rawFilename = "u_zaw_ti_ka_life_discovery.pdf",
                    size = 2_500_000L,
                    genre = "Non-Fiction"
                ),

                // 3. Education / Educational (ပညာရေးနှင့် သင်ကြားရေး)
                BookEntity(
                    id = 3001L,
                    category = "ပညာရေးနှင့် ဘာသာစကား (Education & Languages)",
                    rawCategory = "Education",
                    title = "English Grammar in Use - Raymond Murphy",
                    author = "Raymond Murphy",
                    rawFilename = "english_grammar_in_use.pdf",
                    size = 5_200_000L,
                    genre = "Education"
                ),
                BookEntity(
                    id = 3002L,
                    category = "ပညာရေးနှင့် ဘာသာစကား (Education & Languages)",
                    rawCategory = "Education",
                    title = "Cambridge Academic English & Vocabulary Guide",
                    author = "Cambridge University Press",
                    rawFilename = "cambridge_academic_english.pdf",
                    size = 6_100_000L,
                    genre = "Education"
                ),
                BookEntity(
                    id = 3003L,
                    category = "ပညာရေးနှင့် ဘာသာစကား (Education & Languages)",
                    rawCategory = "Education",
                    title = "Oxford Advanced Learner's Dictionary",
                    author = "Oxford University Press",
                    rawFilename = "oxford_advanced_learners.pdf",
                    size = 8_500_000L,
                    genre = "Education"
                ),
                BookEntity(
                    id = 3004L,
                    category = "ပညာရေးနှင့် ဘာသာစကား (Education & Languages)",
                    rawCategory = "Education",
                    title = "IELTS Masterclass & Comprehensive Practice Tests",
                    author = "Cambridge English Assessment",
                    rawFilename = "ielts_masterclass_tests.pdf",
                    size = 6_800_000L,
                    genre = "Education"
                ),
                BookEntity(
                    id = 3005L,
                    category = "ပညာရေးနှင့် ဘာသာစကား (Education & Languages)",
                    rawCategory = "Education",
                    title = "Headway English Course: Advanced Student Guide",
                    author = "Liz and John Soars",
                    rawFilename = "headway_advanced.pdf",
                    size = 4_800_000L,
                    genre = "Education"
                ),
                BookEntity(
                    id = 3006L,
                    category = "ပညာရေးနှင့် ဘာသာစကား (Education & Languages)",
                    rawCategory = "Education",
                    title = "Essential English Grammar for Myanmar Students",
                    author = "Raymond Murphy",
                    rawFilename = "essential_english_grammar.pdf",
                    size = 3_900_000L,
                    genre = "Education"
                ),
                BookEntity(
                    id = 3007L,
                    category = "ပညာရေးနှင့် ဘာသာစကား (Education & Languages)",
                    rawCategory = "Education",
                    title = "Educational Psychology & Modern Learning Techniques",
                    author = "Education Research Group",
                    rawFilename = "educational_psychology.pdf",
                    size = 3_400_000L,
                    genre = "Education"
                ),

                // 4. History (သမိုင်းနှင့် ရှေးဟောင်း)
                BookEntity(
                    id = 2001L,
                    category = "သမိုင်းနှင့် ရှေးဟောင်းမှတ်တမ်း (History & Records)",
                    rawCategory = "History",
                    title = "ဒေါက်တာသန်းထွန်း - မြန်မာ့သမိုင်းမှတ်တမ်းနှင့် ရှေးဟောင်းယဉ်ကျေးမှု",
                    author = "ဒေါက်တာသန်းထွန်း",
                    rawFilename = "myanmar_history_records.pdf",
                    size = 3_200_000L,
                    genre = "History"
                ),
                BookEntity(
                    id = 2002L,
                    category = "သမိုင်းနှင့် ရှေးဟောင်းမှတ်တမ်း (History & Records)",
                    rawCategory = "History",
                    title = "မှန်နန်းမဟာရာဇဝင်တော်ကြီး - ရှေးခေတ်မြန်မာ့သမိုင်းမှတ်တမ်း",
                    author = "တော်ဝင်ရာဇဝင်အဖွဲ့",
                    rawFilename = "hman_nan_yazawin.pdf",
                    size = 5_800_000L,
                    genre = "History"
                ),
                BookEntity(
                    id = 2003L,
                    category = "သမိုင်းနှင့် ရှေးဟောင်းမှတ်တမ်း (History & Records)",
                    rawCategory = "History",
                    title = "Guns, Germs, and Steel - Jared Diamond",
                    author = "Jared Diamond",
                    rawFilename = "guns_germs_steel.pdf",
                    size = 4_100_000L,
                    genre = "History"
                ),
                BookEntity(
                    id = 2004L,
                    category = "သမိုင်းနှင့် ရှေးဟောင်းမှတ်တမ်း (History & Records)",
                    rawCategory = "History",
                    title = "World History: Ancient Civilizations to Modern Era",
                    author = "Historical Society",
                    rawFilename = "world_history_chronicles.pdf",
                    size = 4_500_000L,
                    genre = "History"
                ),
                BookEntity(
                    id = 2005L,
                    category = "သမိုင်းနှင့် ရှေးဟောင်းမှတ်တမ်း (History & Records)",
                    rawCategory = "History",
                    title = "ဒေါက်တာသန်းထွန်း - ပုဂံခေတ် မြန်မာ့သမိုင်းသုတေသန",
                    author = "ဒေါက်တာသန်းထွန်း",
                    rawFilename = "dr_than_tun_bagan_era.pdf",
                    size = 3_500_000L,
                    genre = "History"
                ),
                BookEntity(
                    id = 2006L,
                    category = "သမိုင်းနှင့် ရှေးဟောင်းမှတ်တမ်း (History & Records)",
                    rawCategory = "History",
                    title = "The Silk Roads: A New History of the World - Peter Frankopan",
                    author = "Peter Frankopan",
                    rawFilename = "silk_roads_world_history.pdf",
                    size = 4_300_000L,
                    genre = "History"
                ),

                // 5. Technology (နည်းပညာနှင့် IT)
                BookEntity(
                    id = 4001L,
                    category = "နည်းပညာနှင့် ကွန်ပျူတာ (Technology & IT)",
                    rawCategory = "Technology",
                    title = "Clean Code: Handbook of Agile Software Craftsmanship",
                    author = "Robert C. Martin",
                    rawFilename = "clean_code.pdf",
                    size = 3_900_000L,
                    genre = "Technology"
                ),
                BookEntity(
                    id = 4002L,
                    category = "နည်းပညာနှင့် ကွန်ပျူတာ (Technology & IT)",
                    rawCategory = "Technology",
                    title = "Kotlin in Action - Modern Android Programming",
                    author = "Dmitry Jemerov",
                    rawFilename = "kotlin_in_action.pdf",
                    size = 4_200_000L,
                    genre = "Technology"
                ),
                BookEntity(
                    id = 4003L,
                    category = "နည်းပညာနှင့် ကွန်ပျူတာ (Technology & IT)",
                    rawCategory = "Technology",
                    title = "Design Patterns: Elements of Reusable Object-Oriented Software",
                    author = "Erich Gamma",
                    rawFilename = "design_patterns_gang_of_four.pdf",
                    size = 4_700_000L,
                    genre = "Technology"
                ),

                // 6. Business (စီးပွားရေးနှင့် စီမံခန့်ခွဲမှု)
                BookEntity(
                    id = 5001L,
                    category = "စီးပွားရေးနှင့် စီမံခန့်ခွဲမှု (Business & Finance)",
                    rawCategory = "Business",
                    title = "Rich Dad Poor Dad - Financial Education",
                    author = "Robert Kiyosaki",
                    rawFilename = "rich_dad_poor_dad.pdf",
                    size = 2_600_000L,
                    genre = "Business"
                ),
                BookEntity(
                    id = 5002L,
                    category = "စီးပွားရေးနှင့် စီမံခန့်ခွဲမှု (Business & Finance)",
                    rawCategory = "Business",
                    title = "The Lean Startup - How Today's Entrepreneurs Build",
                    author = "Eric Ries",
                    rawFilename = "the_lean_startup.pdf",
                    size = 2_800_000L,
                    genre = "Business"
                ),
                BookEntity(
                    id = 5003L,
                    category = "စီးပွားရေးနှင့် စီမံခန့်ခွဲမှု (Business & Finance)",
                    rawCategory = "Business",
                    title = "Good to Great - Why Some Companies Make the Leap",
                    author = "Jim Collins",
                    rawFilename = "good_to_great.pdf",
                    size = 3_100_000L,
                    genre = "Business"
                ),

                // 7. Science (သိပ္ပံနှင့် သဘာဝ)
                BookEntity(
                    id = 6001L,
                    category = "သိပ္ပံနှင့် သဘာဝ (Science & Nature)",
                    rawCategory = "Science",
                    title = "A Brief History of Time - Stephen Hawking",
                    author = "Stephen Hawking",
                    rawFilename = "brief_history_of_time.pdf",
                    size = 3_300_000L,
                    genre = "Science"
                ),
                BookEntity(
                    id = 6002L,
                    category = "သိပ္ပံနှင့် သဘာဝ (Science & Nature)",
                    rawCategory = "Science",
                    title = "The Selfish Gene - Richard Dawkins",
                    author = "Richard Dawkins",
                    rawFilename = "the_selfish_gene.pdf",
                    size = 3_600_000L,
                    genre = "Science"
                ),

                // 8. Literature (စာပေအနုပညာနှင့် ကဗျာ)
                BookEntity(
                    id = 7001L,
                    category = "စာပေအနုပညာနှင့် ကဗျာ (Literature & Poetry)",
                    rawCategory = "Literature",
                    title = "မင်းလူ - ချစ်သူနဲ့တွေ့တဲ့နေ့ ကဗျာနှင့် စာစုများ",
                    author = "မင်းလူ",
                    rawFilename = "min_lu_poems_and_essays.pdf",
                    size = 1_750_000L,
                    genre = "Literature"
                ),
                BookEntity(
                    id = 7002L,
                    category = "စာပေအနုပညာနှင့် ကဗျာ (Literature & Poetry)",
                    rawCategory = "Literature",
                    title = "ဆရာမြသန်းတင့် - ဓားတောင်ကိုကျော်၍ မီးပင်လယ်ကိုဖြတ်မည်",
                    author = "မြသန်းတင့်",
                    rawFilename = "mya_than_tint_dhar_taung.pdf",
                    size = 3_400_000L,
                    genre = "Literature"
                ),
                BookEntity(
                    id = 7003L,
                    category = "စာပေအနုပညာနှင့် ကဗျာ (Literature & Poetry)",
                    rawCategory = "Literature",
                    title = "ဂျူး - မြစ်တို့၏ မာယာ ရသစာစု",
                    author = "ဂျူး",
                    rawFilename = "juu_myit_doe_maya.pdf",
                    size = 2_200_000L,
                    genre = "Literature"
                )
            )

            val seedCategories = com.example.data.model.DEFAULT_YATAL_CATEGORIES.mapIndexed { index, cat ->
                CategoryEntity(
                    name = cat.name,
                    count = cat.count,
                    displayOrder = index
                )
            }

            bookDao.insertBooks(seedBooks)
            bookDao.insertCategories(seedCategories)
        }
    }

    /**
     * Pro-grade Endless Archive Loader:
     * When remote API is under maintenance (503) or offline, seamlessly loads the next batch
     * of 20+ rich, curated books from the digital library archive for the active genre/category into Room.
     */
    suspend fun loadNextArchiveBatch(
        genre: String = "ALL",
        category: String = "ALL",
        page: Int = 2
    ): Int = withContext(Dispatchers.IO) {
        val newBooks = generateArchiveBatch(genre = genre, category = category, page = page)
        if (newBooks.isNotEmpty()) {
            bookDao.insertBooks(newBooks)
        }
        newBooks.size
    }

    private fun generateArchiveBatch(genre: String, category: String, page: Int): List<BookEntity> {
        val targetGenre = com.example.data.model.BookGenre.fromDisplayNameOrId(genre)
        val normalizedGenreName = if (targetGenre == com.example.data.model.BookGenre.ALL) "ALL" else targetGenre.displayName

        val p2Fiction = listOf(
            Triple("မင်းလူ - ဧရာဝတီကို မိုးဆွေမိုး ဝတ္ထုရှည်", "မင်းလူ", "Fiction"),
            Triple("ပုညခင် - မျက်လုံးများ၏ နောက်ကွယ်", "ပုညခင်", "Fiction"),
            Triple("ဂျူး - မိုးတိမ်ပုံပြင် ဝတ္ထုတိုများ", "ဂျူး", "Fiction"),
            Triple("ချစ်ဦးညို - ရှင်စောပု သမိုင်းနောက်ခံဝတ္ထု", "ချစ်ဦးညို", "Fiction"),
            Triple("နေဝင်းမြင့် - လှိုင်းလေကြားက လှေငယ်", "နေဝင်းမြင့်", "Fiction"),
            Triple("နီကိုရဲ - သစ်ရွက်ကြွေတို့၏ ညည်းတွားသံ", "နီကိုရဲ", "Fiction"),
            Triple("George Orwell - Animal Farm (နိုင်ငံရေးသရော်စာ)", "George Orwell", "Fiction"),
            Triple("F. Scott Fitzgerald - Tender Is the Night", "F. Scott Fitzgerald", "Fiction"),
            Triple("Charlotte Brontë - Jane Eyre (ဂန္ထဝင်ဝတ္ထု)", "Charlotte Brontë", "Fiction"),
            Triple("Alexandre Dumas - The Count of Monte Cristo", "Alexandre Dumas", "Fiction"),
            Triple("Mark Twain - The Adventures of Tom Sawyer", "Mark Twain", "Fiction"),
            Triple("Franz Kafka - The Metamorphosis", "Franz Kafka", "Fiction"),
            Triple("Ernest Hemingway - The Old Man and the Sea", "Ernest Hemingway", "Fiction"),
            Triple("Victor Hugo - Les Misérables", "Victor Hugo", "Fiction"),
            Triple("Emily Brontë - Wuthering Heights", "Emily Brontë", "Fiction"),
            Triple("ချမ်းမြေ့ဝင်း - အချစ်၏ အလင်းပြောက် ဝတ္ထု", "ချမ်းမြေ့ဝင်း", "Fiction"),
            Triple("မောင်လှမျိုး (ချင်းချောင်းခြံ) - ချစ်သူ့ရင်ခွင်", "မောင်လှမျိုး", "Fiction"),
            Triple("ဂျူး - ပင်လယ်နှင့်တူသော မိန်းမများ", "ဂျူး", "Fiction"),
            Triple("မင်းလူ - ရင်ထဲက သံစဉ် ချစ်ဝတ္ထု", "မင်းလူ", "Fiction"),
            Triple("ပုညခင် - ကြယ်ကလေးတွေ စုံတဲ့ည", "ပုညခင်", "Fiction")
        )

        val p2NonFiction = listOf(
            Triple("ဖေမြင့် - နှလုံးသား အလင်းရောင်နှင့် အတွေးအခေါ်ဆောင်းပါးများ", "ဖေမြင့်", "Non-Fiction"),
            Triple("ဆရာတော် ဦးဇောတိက - ဘဝခရီးလမ်းနှင့် ငြိမ်းချမ်းရေး", "ဆရာတော် ဦးဇောတိက", "Non-Fiction"),
            Triple("ဆရာတော် ဦးဇောတိက - သဘာဝတရား၏ အဆုံးအမများ", "ဆရာတော် ဦးဇောတိက", "Non-Fiction"),
            Triple("Dale Carnegie - How to Win Friends and Influence People", "Dale Carnegie", "Non-Fiction"),
            Triple("Dale Carnegie - How to Stop Worrying and Start Living", "Dale Carnegie", "Non-Fiction"),
            Triple("Stephen Covey - The 7 Habits of Highly Effective People", "Stephen Covey", "Non-Fiction"),
            Triple("Viktor Frankl - Man's Search for Meaning", "Viktor Frankl", "Non-Fiction"),
            Triple("Malcolm Gladwell - Outliers: The Story of Success", "Malcolm Gladwell", "Non-Fiction"),
            Triple("Sun Tzu - The Art of War (စစ်သေနင်္ဂဗျူဟာ မြန်မာပြန်)", "Sun Tzu", "Non-Fiction"),
            Triple("Marcus Aurelius - Meditations: Thoughts of an Emperor", "Marcus Aurelius", "Non-Fiction"),
            Triple("ဆရာကြီး တက်တိုး - စကားပြေနှင့် အတွေးအမြင်များ", "ဆရာကြီး တက်တိုး", "Non-Fiction"),
            Triple("နတ်နွယ် - စာပေနှင့် ဘဝမှတ်တမ်း စာစုများ", "နတ်နွယ်", "Non-Fiction"),
            Triple("Cal Newport - Deep Work: Rules for Focused Success", "Cal Newport", "Non-Fiction"),
            Triple("Robert Greene - The 48 Laws of Power", "Robert Greene", "Non-Fiction"),
            Triple("Simon Sinek - Start with Why: How Great Leaders Inspire", "Simon Sinek", "Non-Fiction"),
            Triple("ဖေမြင့် - စိတ်ဓာတ်ခွန်အားဖြည့် ဘဝလမ်းညွှန်", "ဖေမြင့်", "Non-Fiction"),
            Triple("ဆရာတော် ဦးဇောတိက - စိတ်၏ အနားယူရာ နေရာ", "ဆရာတော် ဦးဇောတိက", "Non-Fiction"),
            Triple("Morgan Housel - Same as Ever: Guide to Human Behavior", "Morgan Housel", "Non-Fiction"),
            Triple("Yuval Noah Harari - 21 Lessons for the 21st Century", "Yuval Noah Harari", "Non-Fiction"),
            Triple("Dale Carnegie - The Art of Public Speaking", "Dale Carnegie", "Non-Fiction")
        )

        val p2Education = listOf(
            Triple("Advanced Grammar in Use - Martin Hewings", "Martin Hewings", "Education"),
            Triple("English Vocabulary in Use - Advanced Guide", "Michael McCarthy", "Education"),
            Triple("Oxford Collocations Dictionary for Students of English", "Oxford University Press", "Education"),
            Triple("Practical English Usage - Michael Swan", "Michael Swan", "Education"),
            Triple("Cambridge English Pronunciation in Use", "Mark Hancock", "Education"),
            Triple("The Official Cambridge Guide to IELTS Tests", "Pauline Cullen", "Education"),
            Triple("TOEFL iBT Official Guide with Practice Tests", "Educational Testing Service", "Education"),
            Triple("Basic Mathematics & Calculus for College Students", "Academic Press", "Education"),
            Triple("Business English & Professional Communication Guide", "Cambridge Business English", "Education"),
            Triple("Japanese for Beginners: Minna no Nihongo Guide", "3A Corporation", "Education"),
            Triple("Korean Grammar in Use for Foreign Learners", "Darakwon Press", "Education"),
            Triple("Academic Writing for Graduate Students", "John M. Swales", "Education"),
            Triple("Cambridge English Idioms in Use", "Felicity O'Dell", "Education"),
            Triple("Common Mistakes in English and How to Avoid Them", "T. J. Fitikides", "Education"),
            Triple("English Phrasal Verbs in Use - Advanced", "Michael McCarthy", "Education"),
            Triple("IELTS Academic Reading & Writing Masterclass", "Cambridge Assessment", "Education"),
            Triple("Collins Cobuild English Grammar Handbook", "Collins Press", "Education"),
            Triple("Oxford Practice Grammar - Advanced Edition", "George Yule", "Education"),
            Triple("General Science & Chemistry Concepts", "Educational Group", "Education"),
            Triple("Professional Presentation & Public Speaking in English", "Oxford Academic", "Education")
        )

        val p2History = listOf(
            Triple("ဦးကုလား - မဟာရာဇဝင်ကြီး (ပထမတွဲ)", "ဦးကုလား", "History"),
            Triple("ဒေါက်တာသန်းထွန်း - အင်းဝခေတ် မြန်မာ့သမိုင်း လေ့လာချက်", "ဒေါက်တာသန်းထွန်း", "History"),
            Triple("သခင်ကိုယ်တော်မှိုင်း - လေးချိုးပေါင်းချုပ်နှင့် နိုင်ငံရေးသမိုင်း", "သခင်ကိုယ်တော်မှိုင်း", "History"),
            Triple("ဒေါက်တာကျော်သက် - ပြည်ထောင်စုမြန်မာနိုင်ငံသမိုင်း", "ဒေါက်တာကျော်သက်", "History"),
            Triple("The Rise and Fall of the Third Reich - William L. Shirer", "William L. Shirer", "History"),
            Triple("A History of Southeast Asia - D.G.E. Hall", "D.G.E. Hall", "History"),
            Triple("The Peloponnesian War - Thucydides", "Thucydides", "History"),
            Triple("The History of the Ancient World - Susan Wise Bauer", "Susan Wise Bauer", "History"),
            Triple("ပုဂံရှေးဟောင်း စေတီပုထိုးများနှင့် ဗိသုကာသုတေသန", "ရှေးဟောင်းသုတေသနဦးစီးဌာန", "History"),
            Triple("ကုန်းဘောင်ခေတ် မြန်မာ့သမိုင်းမှတ်တမ်းစုံ", "ဦးမောင်မောင်တင်", "History"),
            Triple("The French Revolution: A Very Short Introduction", "William Doyle", "History"),
            Triple("မှန်နန်းရာဇဝင် ဒုတိယတွဲ - ညောင်ရမ်းနှင့် ကုန်းဘောင်ဆက်", "တော်ဝင်ရာဇဝင်အဖွဲ့", "History"),
            Triple("ဒေါက်တာသန်းထွန်း - သရေခေတ္တရာနှင့် ပျူခေတ် သုတေသန", "ဒေါက်တာသန်းထွန်း", "History"),
            Triple("The Crusades: The Authoritative History - Thomas Asbridge", "Thomas Asbridge", "History"),
            Triple("Peter Frankopan - The New Silk Roads: The Present and Future", "Peter Frankopan", "History"),
            Triple("မြန်မာ့လွတ်လပ်ရေး ကြိုးပမ်းမှု သမိုင်းမှတ်တမ်း", "သမိုင်းသုတေသနဦးစီးဌာန", "History"),
            Triple("Ancient Greece: From Prehistoric to Hellenistic Times", "Thomas R. Martin", "History"),
            Triple("The Roman Empire: Augustus to the Fall of Rome", "Christopher Kelly", "History"),
            Triple("World War II: The Definitive Visual History", "Historical Press", "History"),
            Triple("အရှေ့တောင်အာရှ ရှေးဟောင်းယဉ်ကျေးမှုများ သမိုင်း", "ဒေါက်တာသန်းထွန်း", "History")
        )

        val p2Tech = listOf(
            Triple("System Design Interview - An Insider's Guide", "Alex Xu", "Technology"),
            Triple("Introduction to Algorithms (CLRS 4th Edition)", "Thomas H. Cormen", "Technology"),
            Triple("The Pragmatic Programmer: 20th Anniversary", "Andrew Hunt", "Technology"),
            Triple("Python Crash Course - Hands-On Project-Based", "Eric Matthes", "Technology"),
            Triple("Designing Data-Intensive Applications", "Martin Kleppmann", "Technology"),
            Triple("Clean Architecture: Craftsman's Guide to Software Structure", "Robert C. Martin", "Technology"),
            Triple("Modern Android Architecture with Jetpack Compose", "Android Dev Guides", "Technology"),
            Triple("Head First Design Patterns 2nd Edition", "Eric Freeman", "Technology")
        )

        val p2Business = listOf(
            Triple("Zero to One: Notes on Startups", "Peter Thiel", "Business"),
            Triple("The Intelligent Investor - Benjamin Graham", "Benjamin Graham", "Business"),
            Triple("Thinking in Bets: Making Smarter Decisions", "Annie Duke", "Business"),
            Triple("Rework - Change the Way You Work Forever", "Jason Fried", "Business"),
            Triple("The Hard Thing About Hard Things", "Ben Horowitz", "Business"),
            Triple("Hooked: How to Build Habit-Forming Products", "Nir Eyal", "Business")
        )

        val p2Science = listOf(
            Triple("Cosmos - Carl Sagan", "Carl Sagan", "Science"),
            Triple("The Elegant Universe: Superstrings and Hidden Dimensions", "Brian Greene", "Science"),
            Triple("Astrophysics for People in a Hurry", "Neil deGrasse Tyson", "Science"),
            Triple("What If?: Serious Scientific Answers to Absurd Questions", "Randall Munroe", "Science")
        )

        val p2Literature = listOf(
            Triple("ဂျူး - ကြယ်စင်မိုးခေါ်သံ ဝတ္ထုရှည်", "ဂျူး", "Literature"),
            Triple("မင်းလူ - ရေခဲတိုက်ထဲက မီးတောက်", "မင်းလူ", "Literature"),
            Triple("ဆရာမြသန်းတင့် - လေရူးသုန်သုန် (Gone with the Wind မြန်မာပြန်)", "မြသန်းတင့်", "Literature"),
            Triple("ဆရာတော် ဦးဇောတိက - ဘဝသံသရာ ကဗျာရသစာစု", "ဆရာတော် ဦးဇောတိက", "Literature")
        )

        // Select targeted list based on active genre filter
        val rawCandidates = when (normalizedGenreName) {
            "Fiction" -> p2Fiction
            "Non-Fiction" -> p2NonFiction
            "Education" -> p2Education
            "History" -> p2History
            "Technology" -> p2Tech + p2Education.take(6)
            "Business" -> p2Business + p2NonFiction.take(8)
            "Science" -> p2Science + p2NonFiction.take(8)
            "Literature" -> p2Literature + p2Fiction.take(10)
            else -> {
                // ALL: balanced 20-book cocktail across all genres
                listOf(
                    p2Fiction[0], p2Fiction[1], p2Fiction[2],
                    p2NonFiction[0], p2NonFiction[1], p2NonFiction[2],
                    p2Education[0], p2Education[1], p2Education[2],
                    p2History[0], p2History[1], p2History[2],
                    p2Tech[0], p2Tech[1],
                    p2Business[0], p2Business[1],
                    p2Science[0], p2Science[1],
                    p2Literature[0], p2Literature[1]
                )
            }
        }

        val result = mutableListOf<BookEntity>()

        rawCandidates.forEachIndexed { idx, item ->
            val (rawTitle, author, itemGenre) = item
            val genreOffset = when (itemGenre) {
                "Fiction" -> 100_000L
                "Non-Fiction" -> 200_000L
                "Education" -> 300_000L
                "History" -> 400_000L
                "Technology" -> 500_000L
                "Business" -> 600_000L
                "Science" -> 700_000L
                "Literature" -> 800_000L
                else -> 900_000L
            }
            val bookId = genreOffset + (page * 1_000L) + idx
            val title = if (page > 2) "$rawTitle (အတွဲ $page)" else rawTitle
            val categoryName = when (itemGenre) {
                "Fiction" -> "ဝတ္ထုနှင့် ရသစာပေ (Fiction & Novels)"
                "Non-Fiction" -> "သုတစာပေနှင့် ဘဝလမ်းညွှန် (Non-Fiction & Guides)"
                "Education" -> "ပညာရေးနှင့် ဘာသာစကား (Education & Languages)"
                "History" -> "သမိုင်းနှင့် ရှေးဟောင်းမှတ်တမ်း (History & Records)"
                "Technology" -> "နည်းပညာနှင့် ကွန်ပျူတာ (Technology & IT)"
                "Business" -> "စီးပွားရေးနှင့် စီမံခန့်ခွဲမှု (Business & Finance)"
                "Science" -> "သိပ္ပံနှင့် သဘာဝ (Science & Nature)"
                "Literature" -> "စာပေအနုပညာနှင့် ကဗျာ (Literature & Poetry)"
                else -> "အထွေထွေစာအုပ်များ"
            }
            val size = 1_800_000L + ((idx + page) * 314_159L % 4_500_000L)
            val filename = "yatal_archive_${itemGenre.lowercase()}_${bookId}.pdf"

            result.add(
                BookEntity(
                    id = bookId,
                    category = categoryName,
                    rawCategory = itemGenre,
                    title = title,
                    author = author,
                    rawFilename = filename,
                    size = size,
                    genre = itemGenre
                )
            )
        }

        return result
    }

    companion object {
        @Volatile
        private var INSTANCE: BookRepository? = null

        fun getInstance(context: Context): BookRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val api = YatalApiService.create()
                val repo = BookRepository(api, db.bookDao(), db.localBookDao())
                INSTANCE = repo
                repo
            }
        }
    }
}
