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
import com.example.data.model.calculateGenreCounts
import com.example.data.model.getCategoriesForGenre
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
    val titleSuggestions: List<String> = emptyList(),
    val searchMatchCount: Int = 0,
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
                    rankAndFilterBooks(
                        books = rawList,
                        query = params.query,
                        scope = params.scope,
                        sortOrder = params.sort
                    )
                }
            }.collectLatest { filteredList: List<Book> ->
                val q = _searchQuery.value.trim()
                val suggestions = if (q.isNotBlank()) {
                    filteredList.map { it.cleanTitle }.distinct().take(6)
                } else {
                    emptyList()
                }
                _uiState.update { state ->
                    val resolvedTotal = when {
                        q.isNotBlank() -> filteredList.size
                        state.selectedCategory != "ALL" -> {
                            val catCount = state.categories.find { it.name == state.selectedCategory }?.count ?: 0
                            if (catCount > 0) catCount else (if (state.totalBooksCount > 0) state.totalBooksCount else filteredList.size)
                        }
                        state.selectedGenre != "ALL" -> {
                            val gCount = state.genreCounts[state.selectedGenre] ?: 0
                            if (gCount > 0) gCount else (if (state.totalBooksCount > 0) state.totalBooksCount else filteredList.size)
                        }
                        else -> {
                            val allCatTotal = state.categories.sumOf { it.count }
                            if (allCatTotal > 0) allCatTotal else (if (state.totalBooksCount > 0) state.totalBooksCount else filteredList.size)
                        }
                    }

                    state.copy(
                        books = filteredList,
                        titleSuggestions = suggestions,
                        searchMatchCount = filteredList.size,
                        totalBooksCount = resolvedTotal,
                        isLoading = false
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
                if (_uiState.value.genreCounts.isEmpty()) {
                    val countMap = mutableMapOf<String, Int>()
                    for (stat in stats) {
                        val normalized = BookGenre.fromDisplayNameOrId(stat.genre).displayName
                        countMap[normalized] = (countMap[normalized] ?: 0) + stat.count
                    }
                    _uiState.update { it.copy(genreCounts = countMap) }
                }
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
                    val dtos = cachedCats.map { CategoryDto(name = it.name, count = it.count) }
                    val genreTotals = calculateGenreCounts(dtos)
                    val totalInLibrary = dtos.sumOf { it.count }
                    _uiState.update { state ->
                        state.copy(
                            categories = dtos,
                            genreCounts = genreTotals,
                            totalBooksCount = if (state.selectedCategory == "ALL" && state.selectedGenre == "ALL" && state.searchQuery.isBlank()) {
                                if (totalInLibrary > 0) totalInLibrary else state.totalBooksCount
                            } else state.totalBooksCount
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
        _selectedCategory.value = "ALL"
        _searchQuery.value = ""

        val expectedTotal = if (targetGenre == "ALL") {
            val totalInAll = _uiState.value.categories.sumOf { it.count }
            if (totalInAll > 0) totalInAll else 10287
        } else {
            _uiState.value.genreCounts[targetGenre] ?: 0
        }

        _uiState.update {
            it.copy(
                selectedGenre = targetGenre,
                selectedCategory = "ALL",
                searchQuery = "",
                currentPage = 1,
                totalPages = 1,
                isLoadingMore = false,
                totalBooksCount = if (expectedTotal > 0) expectedTotal else it.totalBooksCount,
                activeTab = LibraryTab.BOOKS,
                isLoading = true
            )
        }

        // Fetch online books for matching categories in this genre
        viewModelScope.launch {
            if (targetGenre != "ALL") {
                val matchingCats = getCategoriesForGenre(normalized, _uiState.value.categories)
                for (catName in matchingCats.take(3)) {
                    repository.fetchBooks(category = catName, page = 1, limit = 50)
                }
            } else {
                repository.fetchBooks(category = "ALL", page = 1, limit = 60)
            }
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    /**
     * Filter books by Category using Room database
     */
    fun selectCategory(category: String) {
        val targetCat = if (category == "ALL") "ALL" else category
        _selectedCategory.value = targetCat
        _selectedGenre.value = "ALL"
        _searchQuery.value = ""

        val expectedTotal = if (targetCat == "ALL") {
            val totalInAll = _uiState.value.categories.sumOf { it.count }
            if (totalInAll > 0) totalInAll else 10287
        } else {
            _uiState.value.categories.find { it.name == targetCat }?.count ?: 0
        }

        _uiState.update {
            it.copy(
                selectedCategory = targetCat,
                selectedGenre = "ALL",
                searchQuery = "",
                currentPage = 1,
                totalPages = 1,
                isLoadingMore = false,
                totalBooksCount = if (expectedTotal > 0) expectedTotal else it.totalBooksCount,
                activeTab = LibraryTab.BOOKS,
                isLoading = true
            )
        }

        // If specific category selected, trigger online fetch to refresh Room DB for this category
        viewModelScope.launch {
            val result = repository.fetchBooks(category = targetCat, page = 1, limit = 60)
            if (result.isSuccess) {
                val resp = result.getOrNull()
                if (resp != null) {
                    _uiState.update {
                        it.copy(
                            totalBooksCount = resp.total,
                            totalPages = resp.totalPages,
                            currentPage = 1,
                            isLoading = false
                        )
                    }
                    // If category has multiple pages, proactively fetch page 2 immediately so 120 books appear
                    if (resp.totalPages > 1) {
                        repository.fetchBooks(category = targetCat, page = 2, limit = 60)
                        _uiState.update { it.copy(currentPage = 2) }
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
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
        val willSearch = newQuery.isNotBlank()
        _uiState.update {
            it.copy(
                searchQuery = newQuery,
                activeTab = if (willSearch && it.activeTab != LibraryTab.BOOKS) LibraryTab.BOOKS else it.activeTab
            )
        }
        searchDebounceJob?.cancel()
        if (newQuery.isBlank()) {
            _searchQuery.value = ""
        } else {
            searchDebounceJob = viewModelScope.launch {
                delay(60) // Ultra-fast 60ms debounce for instantaneous real-time typing response
                _searchQuery.value = newQuery.trim()
            }
        }
    }

    fun selectTitleSuggestion(title: String) {
        val trimmed = title.trim()
        searchDebounceJob?.cancel()
        _searchScope.value = SearchFilterScope.TITLE
        _searchQuery.value = trimmed
        _uiState.update {
            it.copy(
                searchQuery = trimmed,
                searchScope = SearchFilterScope.TITLE,
                activeTab = LibraryTab.BOOKS
            )
        }
    }

    fun clearSearch() {
        searchDebounceJob?.cancel()
        _searchScope.value = SearchFilterScope.ALL
        _searchQuery.value = ""
        _uiState.update {
            it.copy(
                searchQuery = "",
                searchScope = SearchFilterScope.ALL,
                titleSuggestions = emptyList(),
                searchMatchCount = 0
            )
        }
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
            val cats = if (catResult.isSuccess) {
                catResult.getOrNull().orEmpty()
            } else {
                _uiState.value.categories
            }

            val totalInAllCats = cats.sumOf { it.count }
            val genreTotals = calculateGenreCounts(cats)

            _uiState.update {
                it.copy(
                    categories = cats,
                    genreCounts = genreTotals,
                    totalBooksCount = if (totalInAllCats > 0) totalInAllCats else it.totalBooksCount
                )
            }

            // 2. Fetch Quota
            val quota = repository.fetchQuota()

            // 3. Fetch books with generous limit (60)
            val allResult = repository.fetchBooks(category = "ALL", page = 1, limit = 60)
            val serverTotal = allResult.getOrNull()?.total ?: totalInAllCats
            if (serverTotal > 0) {
                _uiState.update { it.copy(totalBooksCount = serverTotal) }
            }
            if ((allResult.getOrNull()?.totalPages ?: 1) > 1) {
                repository.fetchBooks(category = "ALL", page = 2, limit = 60)
            }

            // Fetch top categories to ensure Room has rich variety across genres
            val topCategories = listOf("English Books", "Cambridge English Books", "ကွန်ပျူတာနှင့် နည်းပညာ", "စာအုပ်အထွေထွေ", "ပုညခင်", "ရုပ်ပြကာတွန်း")
            for (cat in topCategories) {
                repository.fetchBooks(category = cat, page = 1, limit = 40)
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

    /**
     * Pull-to-refresh action: refreshes categories, active category/genre books, and quota
     */
    fun refreshBooks() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true, error = null) }
            try {
                val activeCategory = _selectedCategory.value
                val activeGenre = _selectedGenre.value
                if (activeCategory != "ALL") {
                    repository.fetchBooks(category = activeCategory, page = 1, limit = 50)
                } else if (activeGenre != "ALL") {
                    val matchingCats = getCategoriesForGenre(BookGenre.fromDisplayNameOrId(activeGenre), _uiState.value.categories)
                    for (catName in matchingCats.take(3)) {
                        repository.fetchBooks(category = catName, page = 1, limit = 50)
                    }
                }
                syncAllFromWebsite()
            } catch (e: Exception) {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun loadMoreBooks() {
        if (_uiState.value.isLoadingMore) return
        val current = _uiState.value.currentPage
        val total = _uiState.value.totalPages
        if (total > 1 && current >= total) return // all pages loaded

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val nextPage = current + 1
            val result = repository.fetchBooks(
                category = _uiState.value.selectedCategory,
                query = _uiState.value.searchQuery,
                page = nextPage,
                limit = 60
            )
            if (result.isSuccess) {
                val resp = result.getOrNull()
                _uiState.update {
                    it.copy(
                        currentPage = nextPage,
                        totalPages = resp?.totalPages ?: it.totalPages,
                        totalBooksCount = resp?.total ?: it.totalBooksCount,
                        isLoadingMore = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingMore = false) }
            }
        }
    }
}

/**
 * High-performance real-time search ranking and filtering engine.
 * Prioritizes book titles (cleanTitle / title) with exact match, prefix match,
 * continuous substring match, and multi-token matching for instantaneous title discovery.
 */
fun rankAndFilterBooks(
    books: List<Book>,
    query: String,
    scope: SearchFilterScope,
    sortOrder: SortOption = SortOption.RECENT
): List<Book> {
    if (query.isBlank()) return books

    val cleanQ = query.trim().lowercase()
    val tokens = cleanQ.split(Regex("\\s+")).filter { it.isNotBlank() }

    val scored = books.mapNotNull { book ->
        val titleLower = book.title.lowercase()
        val cleanTitleLower = book.cleanTitle.lowercase()
        val authorLower = book.author.lowercase()
        val categoryLower = book.category.lowercase()
        val filenameLower = book.rawFilename?.lowercase() ?: ""

        var score = 0

        when (scope) {
            SearchFilterScope.TITLE -> {
                // Title-focused discovery
                if (cleanTitleLower == cleanQ || titleLower == cleanQ) {
                    score += 3000
                } else if (cleanTitleLower.startsWith(cleanQ) || titleLower.startsWith(cleanQ)) {
                    score += 1800
                } else if (cleanTitleLower.contains(cleanQ) || titleLower.contains(cleanQ)) {
                    score += 1000
                }

                val matchedTokens = tokens.count { token ->
                    cleanTitleLower.contains(token) || titleLower.contains(token) || filenameLower.contains(token)
                }
                if (matchedTokens == tokens.size) {
                    score += 600 + (matchedTokens * 50)
                } else if (matchedTokens > 0) {
                    score += (matchedTokens * 70)
                }
            }

            SearchFilterScope.AUTHOR -> {
                // Author-focused discovery
                if (authorLower == cleanQ) {
                    score += 3000
                } else if (authorLower.startsWith(cleanQ)) {
                    score += 1800
                } else if (authorLower.contains(cleanQ)) {
                    score += 1000
                }

                val matchedTokens = tokens.count { token ->
                    authorLower.contains(token) || (authorLower.isBlank() && titleLower.contains(token))
                }
                if (matchedTokens == tokens.size) {
                    score += 600
                } else if (matchedTokens > 0) {
                    score += (matchedTokens * 70)
                }
            }

            SearchFilterScope.ALL -> {
                // Multi-field search prioritizing title discovery
                if (cleanTitleLower == cleanQ || titleLower == cleanQ) {
                    score += 3000
                } else if (cleanTitleLower.startsWith(cleanQ) || titleLower.startsWith(cleanQ)) {
                    score += 2000
                } else if (cleanTitleLower.contains(cleanQ) || titleLower.contains(cleanQ)) {
                    score += 1200
                }

                if (authorLower == cleanQ) {
                    score += 1500
                } else if (authorLower.startsWith(cleanQ)) {
                    score += 900
                } else if (authorLower.contains(cleanQ)) {
                    score += 700
                }

                val matchedTitleTokens = tokens.count { cleanTitleLower.contains(it) || titleLower.contains(it) }
                val matchedAuthorTokens = tokens.count { authorLower.contains(it) }
                val matchedCategoryTokens = tokens.count { categoryLower.contains(it) }
                val matchedFilenameTokens = tokens.count { filenameLower.contains(it) }

                if (matchedTitleTokens == tokens.size) {
                    score += 700 + (tokens.size * 30)
                } else if (matchedAuthorTokens == tokens.size) {
                    score += 500
                } else if ((matchedTitleTokens + matchedAuthorTokens + matchedCategoryTokens) >= tokens.size) {
                    score += 400
                } else {
                    val anyMatched = matchedTitleTokens + matchedAuthorTokens + matchedCategoryTokens + matchedFilenameTokens
                    if (anyMatched > 0) {
                        score += (anyMatched * 60)
                    }
                }
            }
        }

        if (score > 0) Pair(book, score) else null
    }

    return scored
        .sortedWith(
            compareByDescending<Pair<Book, Int>> { it.second }
                .thenBy {
                    when (sortOrder) {
                        SortOption.TITLE_ASC -> it.first.cleanTitle
                        else -> ""
                    }
                }
        )
        .map { it.first }
}
