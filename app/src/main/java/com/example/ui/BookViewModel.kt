package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.CategoryStat
import com.example.data.local.GenreStat
import com.example.data.model.Book
import com.example.data.model.BookGenre
import com.example.data.model.CategoryDto
import com.example.data.model.ReadingProgress
import com.example.data.repository.BookRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LibraryTab {
    BOOKS,
    CATEGORIES,
    FAVORITES
}

enum class SortOption(val id: String, val displayName: String) {
    RECENT("recent", "နောက်ဆုံးရ"),
    TITLE_ASC("title_asc", "အမည် (က - အ)"),
    SIZE_DESC("size_desc", "ဖိုင်အရွယ်အစား (ကြီးစဉ်)"),
    SIZE_ASC("size_asc", "ဖိုင်အရွယ်အစား (ငယ်စဉ်)")
}

enum class SearchFilterScope(val id: String, val displayName: String, val iconEmoji: String) {
    ALL("all", "အားလုံး", "🔍"),
    TITLE("title", "ခေါင်းစဉ်", "📖"),
    AUTHOR("author", "စာရေးဆရာ", "✍️")
}

val DEFAULT_POPULAR_AUTHORS = listOf(
    "ဂျူး", "မင်းလူ", "ချစ်ဦးညို", "မောင်ထွန်းသူ", "Dale Carnegie", "ပုညခင်", "ဖေမြင့်", "နီကိုရဲ", "မြသန်းတင့်", "အကြည်တော်"
)

private data class FilterParams(
    val genre: String,
    val category: String,
    val query: String,
    val scope: SearchFilterScope,
    val sort: SortOption
)

data class FavoriteFeedback(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val book: Book? = null,
    val wasAdded: Boolean = false
)

data class LibraryUiState(
    val books: List<Book> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
    val favoriteBooks: List<Book> = emptyList(),
    val readingProgressList: List<ReadingProgress> = emptyList(),
    val readingProgressMap: Map<Long, ReadingProgress> = emptyMap(),
    val activeReadingProgress: ReadingProgress? = null,
    val favoriteFeedback: FavoriteFeedback? = null,
    val selectedGenre: String = "ALL",
    val selectedCategory: String = "ALL",
    val searchQuery: String = "",
    val searchScope: SearchFilterScope = SearchFilterScope.ALL,
    val suggestedAuthors: List<String> = emptyList(),
    val sortOrder: SortOption = SortOption.RECENT,
    val genreCounts: Map<String, Int> = emptyMap(),
    val categoryCounts: Map<String, Int> = emptyMap(),
    val totalRoomCachedCount: Int = 0,
    val isFilterSheetOpen: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isRefreshing: Boolean = false,
    val isOfflineMode: Boolean = false,
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val totalBooksCount: Int = 0,
    val lastSyncTime: String = "",
    val activeTab: LibraryTab = LibraryTab.BOOKS,
    val selectedBookForDetail: Book? = null,
    val readingBook: Book? = null,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class BookViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BookRepository.getInstance(application)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _selectedGenre = MutableStateFlow("ALL")
    private val _selectedCategory = MutableStateFlow("ALL")
    private val _searchQuery = MutableStateFlow("")
    private val _searchScope = MutableStateFlow(SearchFilterScope.ALL)
    private val _sortOrder = MutableStateFlow(SortOption.RECENT)

    private var searchDebounceJob: Job? = null

    init {
        // 1. Reactive Room Database Filter Pipeline with Real-Time Search Scope
        viewModelScope.launch {
            combine(
                _selectedGenre,
                _selectedCategory,
                _searchQuery,
                _searchScope,
                _sortOrder
            ) { genre: String, category: String, query: String, scope: SearchFilterScope, sort: SortOption ->
                FilterParams(genre, category, query, scope, sort)
            }.flatMapLatest { params: FilterParams ->
                repository.getFilteredBooksFromRoom(
                    genre = params.genre,
                    category = params.category,
                    query = params.query,
                    sortOrder = params.sort.id
                ).map { rawList: List<Book> ->
                    if (params.query.isBlank() || params.scope == SearchFilterScope.ALL) {
                        rawList
                    } else {
                        val q = params.query.trim().lowercase()
                        rawList.filter { book ->
                            when (params.scope) {
                                SearchFilterScope.TITLE -> {
                                    book.cleanTitle.lowercase().contains(q) ||
                                            book.title.lowercase().contains(q) ||
                                            (book.rawFilename?.lowercase()?.contains(q) == true)
                                }
                                SearchFilterScope.AUTHOR -> {
                                    book.author.lowercase().contains(q) ||
                                            (book.author.isBlank() && book.title.lowercase().contains(q))
                                }
                                SearchFilterScope.ALL -> true
                            }
                        }
                    }
                }
            }.collectLatest { filteredList: List<Book> ->
                _uiState.update { state ->
                    state.copy(
                        books = filteredList,
                        totalBooksCount = if (filteredList.isNotEmpty()) filteredList.size else state.totalBooksCount,
                        isLoading = if (filteredList.isNotEmpty()) false else state.isLoading
                    )
                }
            }
        }

        // 2. Observe Room DB Authors for real-time author suggestions
        viewModelScope.launch {
            repository.allAuthors.collectLatest { dbAuthors ->
                val combined = (dbAuthors + DEFAULT_POPULAR_AUTHORS).filter { it.isNotBlank() }.distinct()
                _uiState.update { it.copy(suggestedAuthors = combined) }
            }
        }

        // 2. Observe Room DB Genre Stats
        viewModelScope.launch {
            repository.genreStats.collectLatest { stats ->
                val countMap = mutableMapOf<String, Int>()
                for (stat in stats) {
                    val normalized = BookGenre.fromDisplayNameOrId(stat.genre).displayName
                    countMap[normalized] = (countMap[normalized] ?: 0) + stat.count
                }
                _uiState.update { it.copy(genreCounts = countMap) }
            }
        }

        // 3. Observe Room DB Category Stats
        viewModelScope.launch {
            repository.categoryStats.collectLatest { stats ->
                val countMap = stats.associate { it.category to it.count }
                _uiState.update { it.copy(categoryCounts = countMap) }
            }
        }

        // 4. Observe Room DB Total Cached Book Count
        viewModelScope.launch {
            repository.cachedBookCount.collectLatest { total ->
                _uiState.update { it.copy(totalRoomCachedCount = total) }
            }
        }

        // 5. Observe favorites reactively from Room DB
        viewModelScope.launch {
            repository.allFavorites.collectLatest { favs ->
                _uiState.update { state ->
                    val favIds = favs.map { it.id }.toSet()
                    state.copy(
                        favoriteBooks = favs,
                        books = state.books.map { book ->
                            book.copy(isFavorite = favIds.contains(book.id))
                        },
                        selectedBookForDetail = state.selectedBookForDetail?.let {
                            it.copy(isFavorite = favIds.contains(it.id))
                        }
                    )
                }
            }
        }

        // 6. Observe cached categories from Room DB
        viewModelScope.launch {
            repository.cachedCategories.collectLatest { cachedCats ->
                if (cachedCats.isNotEmpty()) {
                    _uiState.update { state ->
                        state.copy(
                            categories = cachedCats.map { CategoryDto(name = it.name, count = it.count) }
                        )
                    }
                }
            }
        }

        // 7. Observe reading progress list from Room DB for Continue Reading
        viewModelScope.launch {
            repository.readingProgressList.collectLatest { progressList ->
                val map = progressList.associateBy { it.bookId }
                _uiState.update { state ->
                    state.copy(
                        readingProgressList = progressList,
                        readingProgressMap = map,
                        activeReadingProgress = if (state.readingBook != null) map[state.readingBook.id] else state.activeReadingProgress
                    )
                }
            }
        }

        // Pre-seed curated books across Literature, History, Education, etc. if Room DB is empty
        viewModelScope.launch {
            repository.seedInitialBooksIfEmpty()
        }

        // Initial fetch from website to populate Room database
        syncAllFromWebsite()
    }

    /**
     * Filter books by Genre using Room database
     */
    fun selectGenre(genreDisplayName: String) {
        val normalized = BookGenre.fromDisplayNameOrId(genreDisplayName)
        val targetGenre = if (normalized == BookGenre.ALL || genreDisplayName.equals("ALL", ignoreCase = true)) "ALL" else normalized.displayName
        _selectedGenre.value = targetGenre
        _uiState.update {
            it.copy(
                selectedGenre = targetGenre,
                activeTab = LibraryTab.BOOKS
            )
        }
    }

    /**
     * Filter books by Category using Room database
     */
    fun selectCategory(category: String) {
        val targetCat = if (category == "ALL") "ALL" else category
        _selectedCategory.value = targetCat
        _uiState.update {
            it.copy(
                selectedCategory = targetCat,
                activeTab = LibraryTab.BOOKS
            )
        }

        // If specific category selected, also trigger online fetch to refresh Room DB for this category
        if (targetCat != "ALL") {
            viewModelScope.launch {
                repository.fetchBooks(category = targetCat, page = 1)
            }
        }
    }

    /**
     * Set sorting order in Room database
     */
    fun setSortOrder(option: SortOption) {
        _sortOrder.value = option
        _uiState.update { it.copy(sortOrder = option) }
    }

    /**
     * Reset all genre and category filters
     */
    fun resetFilters() {
        _selectedGenre.value = "ALL"
        _selectedCategory.value = "ALL"
        _searchQuery.value = ""
        _searchScope.value = SearchFilterScope.ALL
        _sortOrder.value = SortOption.RECENT
        _uiState.update {
            it.copy(
                selectedGenre = "ALL",
                selectedCategory = "ALL",
                searchQuery = "",
                searchScope = SearchFilterScope.ALL,
                sortOrder = SortOption.RECENT
            )
        }
    }

    fun openFilterSheet(open: Boolean) {
        _uiState.update { it.copy(isFilterSheetOpen = open) }
    }

    fun setSearchScope(scope: SearchFilterScope) {
        _searchScope.value = scope
        _uiState.update { it.copy(searchScope = scope) }
    }

    fun searchByAuthor(author: String) {
        val trimmed = author.trim()
        searchDebounceJob?.cancel()
        _searchScope.value = SearchFilterScope.AUTHOR
        _searchQuery.value = trimmed
        _uiState.update {
            it.copy(
                searchQuery = trimmed,
                searchScope = SearchFilterScope.AUTHOR,
                activeTab = LibraryTab.BOOKS,
                selectedBookForDetail = null
            )
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(100) // Fast 100ms debounce for responsive typing
            _searchQuery.value = newQuery.trim()
        }
    }

    fun clearSearch() {
        searchDebounceJob?.cancel()
        _searchScope.value = SearchFilterScope.ALL
        _searchQuery.value = ""
        _uiState.update { it.copy(searchQuery = "", searchScope = SearchFilterScope.ALL) }
    }

    fun setTab(tab: LibraryTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun selectBookForDetail(book: Book?) {
        _uiState.update { it.copy(selectedBookForDetail = book) }
    }

    fun openReader(book: Book, resumeSaved: Boolean = true) {
        val saved = if (resumeSaved) _uiState.value.readingProgressMap[book.id] else null
        _uiState.update {
            it.copy(
                readingBook = book,
                activeReadingProgress = saved
            )
        }
    }

    fun closeReader() {
        _uiState.update { it.copy(readingBook = null, activeReadingProgress = null) }
    }

    fun saveReadingProgress(
        book: Book,
        currentPage: Int,
        totalPages: Int = 1,
        scrollRatio: Float = 0f,
        lastLocationDescription: String = ""
    ) {
        viewModelScope.launch {
            repository.saveReadingProgress(
                book = book,
                currentPage = currentPage,
                totalPages = totalPages,
                scrollRatio = scrollRatio,
                lastLocationDescription = lastLocationDescription
            )
        }
    }

    fun deleteReadingProgress(bookId: Long) {
        viewModelScope.launch {
            repository.deleteReadingProgress(bookId)
        }
    }

    fun clearAllReadingProgress() {
        viewModelScope.launch {
            repository.clearAllReadingProgress()
        }
    }

    fun toggleFavorite(book: Book) {
        viewModelScope.launch {
            val wasFavorite = book.isFavorite
            repository.toggleFavorite(book)
            val updatedBook = book.copy(isFavorite = !wasFavorite)

            // If the detail sheet is displaying this book, update its favorite state
            if (_uiState.value.selectedBookForDetail?.id == book.id) {
                _uiState.update { it.copy(selectedBookForDetail = updatedBook) }
            }

            val msg = if (!wasFavorite) {
                "«${book.cleanTitle.take(30)}…» ကို သိမ်းဆည်းပြီးပါပြီ"
            } else {
                "«${book.cleanTitle.take(30)}…» ကို သိမ်းဆည်းမှုမှ ဖယ်ရှားလိုက်ပါပြီ"
            }

            _uiState.update {
                it.copy(
                    favoriteFeedback = FavoriteFeedback(
                        message = msg,
                        book = book,
                        wasAdded = !wasFavorite
                    )
                )
            }
        }
    }

    fun removeFavorite(book: Book) {
        toggleFavorite(book.copy(isFavorite = true))
    }

    fun undoFavoriteAction(feedback: FavoriteFeedback) {
        val targetBook = feedback.book ?: run {
            _uiState.update { it.copy(favoriteFeedback = null) }
            return
        }
        viewModelScope.launch {
            if (feedback.wasAdded) {
                // Undo adding -> remove from Room
                repository.removeFavorite(targetBook.id)
                if (_uiState.value.selectedBookForDetail?.id == targetBook.id) {
                    _uiState.update { it.copy(selectedBookForDetail = targetBook.copy(isFavorite = false)) }
                }
            } else {
                // Undo removing -> re-save to Room
                repository.saveFavorite(targetBook.copy(isFavorite = true))
                if (_uiState.value.selectedBookForDetail?.id == targetBook.id) {
                    _uiState.update { it.copy(selectedBookForDetail = targetBook.copy(isFavorite = true)) }
                }
            }
            _uiState.update { it.copy(favoriteFeedback = null) }
        }
    }

    fun clearFavoriteFeedback() {
        _uiState.update { it.copy(favoriteFeedback = null) }
    }

    fun clearAllFavorites() {
        viewModelScope.launch {
            repository.clearAllFavorites()
            _uiState.update {
                it.copy(
                    favoriteFeedback = FavoriteFeedback(
                        message = "သိမ်းဆည်းထားသော စာအုပ်များ အားလုံး ဖျက်ပြီးပါပြီ",
                        book = null,
                        wasAdded = false
                    )
                )
            }
        }
    }

    /**
     * Auto sync / refresh data from https://library.yatal.tech/ and save to Room DB
     */
    fun syncAllFromWebsite() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }

            // 1. Fetch categories
            val catResult = repository.fetchCategories()
            if (catResult.isSuccess) {
                val cats = catResult.getOrNull().orEmpty()
                _uiState.update { it.copy(categories = cats) }
            }

            // 2. Fetch Quota
            val quota = repository.fetchQuota()

            // 3. Fetch books from popular categories to populate Room database with variety of genres
            val topCategories = listOf("ALL", "စာအုပ်အထွေထွေ", "English Books", "ကွန်ပျူတာနှင့် နည်းပညာ", "သုတ၊ ရသ", "Cambridge English Books")
            for (cat in topCategories) {
                repository.fetchBooks(category = cat, page = 1, limit = 25)
            }

            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    isOfflineMode = false,
                    lastSyncTime = timeFormat.format(Date())
                )
            }
        }
    }

    fun loadMoreBooks() {
        if (_uiState.value.isLoadingMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val nextPage = _uiState.value.currentPage + 1
            val result = repository.fetchBooks(
                category = _uiState.value.selectedCategory,
                query = _uiState.value.searchQuery,
                page = nextPage,
                limit = 20
            )
            if (result.isSuccess) {
                _uiState.update { it.copy(currentPage = nextPage, isLoadingMore = false) }
            } else {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }
}
