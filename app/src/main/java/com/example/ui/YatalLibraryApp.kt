package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Book
import com.example.data.model.BookGenre
import com.example.ui.components.BookCard
import com.example.ui.components.BookDetailModal
import com.example.ui.components.CategoriesView
import com.example.ui.components.CategoryBar
import com.example.ui.components.ContinueReadingSection
import com.example.ui.components.FavoritesView
import com.example.ui.components.FilterBottomSheet
import com.example.ui.components.GenreFilterBar
import com.example.ui.components.LibraryTopBar
import com.example.ui.components.PdfReaderScreen
import com.example.ui.components.SwipeRefresh
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GreenOnline
import com.example.ui.theme.RedError
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.DownloadHelper
import java.text.NumberFormat
import java.util.Locale

@Composable
fun YatalLibraryApp(
    viewModel: BookViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val snackbarHostState = remember { SnackbarHostState() }

    val currentFeedback = uiState.favoriteFeedback
    LaunchedEffect(currentFeedback) {
        if (currentFeedback != null) {
            val result = snackbarHostState.showSnackbar(
                message = currentFeedback.message,
                actionLabel = "ပြန်ပြင်မည်",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoFavoriteAction(currentFeedback)
            } else {
                viewModel.clearFavoriteFeedback()
            }
        }
    }

    // If PDF reader is open, show reader view
    if (uiState.readingBook != null) {
        val activeBook = uiState.readingBook!!
        PdfReaderScreen(
            book = activeBook,
            savedProgress = uiState.activeReadingProgress,
            onClose = { viewModel.closeReader() },
            onDownloadClick = { book -> DownloadHelper.downloadBook(context, book) },
            onSaveProgress = { curPage, totalPgs, scrollRatio, locDesc ->
                viewModel.saveReadingProgress(
                    book = activeBook,
                    currentPage = curPage,
                    totalPages = totalPgs,
                    scrollRatio = scrollRatio,
                    lastLocationDescription = locDesc
                )
            }
        )
        return
    }

    // Android back navigation support for modal, filter sheet, search, and secondary tabs
    BackHandler(enabled = uiState.selectedBookForDetail != null) {
        viewModel.selectBookForDetail(null)
    }
    BackHandler(enabled = uiState.isFilterSheetOpen && uiState.selectedBookForDetail == null) {
        viewModel.openFilterSheet(false)
    }
    BackHandler(enabled = uiState.searchQuery.isNotBlank() && uiState.selectedBookForDetail == null && !uiState.isFilterSheetOpen) {
        viewModel.clearSearch()
    }
    BackHandler(enabled = uiState.activeTab != LibraryTab.BOOKS && uiState.selectedBookForDetail == null && !uiState.isFilterSheetOpen && uiState.searchQuery.isBlank()) {
        viewModel.setTab(LibraryTab.BOOKS)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("yatal_library_scaffold"),
        containerColor = DarkBackground,
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 12.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = DarkSurface,
                    contentColor = TextPrimary,
                    actionColor = AmberPrimary,
                    shape = RoundedCornerShape(8.dp)
                )
            }
        },
        topBar = {
            LibraryTopBar(
                searchQuery = uiState.searchQuery,
                onSearchChange = { viewModel.onSearchQueryChanged(it) },
                onClearSearch = { viewModel.clearSearch() },
                searchScope = uiState.searchScope,
                onSearchScopeChange = { viewModel.setSearchScope(it) },
                titleSuggestions = uiState.titleSuggestions,
                onSelectTitleSuggestion = { viewModel.selectTitleSuggestion(it) },
                searchMatchCount = uiState.searchMatchCount,
                suggestedAuthors = uiState.suggestedAuthors,
                onSelectAuthorSuggestion = { viewModel.searchByAuthor(it) },
                isRefreshing = uiState.isRefreshing,
                isOffline = uiState.isOfflineMode,
                lastSyncTime = uiState.lastSyncTime,
                totalBooks = if (uiState.selectedCategory != "ALL") {
                    uiState.categories.find { it.name == uiState.selectedCategory }?.count ?: uiState.totalBooksCount
                } else if (uiState.selectedGenre != "ALL") {
                    uiState.genreCounts[uiState.selectedGenre] ?: uiState.totalBooksCount
                } else {
                    val totalInAll = uiState.categories.sumOf { it.count }
                    if (totalInAll > 0) totalInAll else uiState.totalBooksCount
                },
                onSyncClick = { viewModel.syncAllFromWebsite() }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .border(1.dp, DarkCardBorder.copy(alpha = 0.5f))
                    .testTag("bottom_nav_bar")
            ) {
                // Tab 1: Books Catalog
                NavigationBarItem(
                    selected = uiState.activeTab == LibraryTab.BOOKS,
                    onClick = { viewModel.setTab(LibraryTab.BOOKS) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.activeTab == LibraryTab.BOOKS) Icons.Filled.MenuBook else Icons.Outlined.MenuBook,
                            contentDescription = "စာအုပ်စင်"
                        )
                    },
                    label = { Text("စာအုပ်စင်", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = AmberPrimary,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextMuted,
                        indicatorColor = AmberPrimary
                    )
                )

                // Tab 2: Categories
                NavigationBarItem(
                    selected = uiState.activeTab == LibraryTab.CATEGORIES,
                    onClick = { viewModel.setTab(LibraryTab.CATEGORIES) },
                    icon = {
                        Icon(
                            imageVector = if (uiState.activeTab == LibraryTab.CATEGORIES) Icons.Filled.Category else Icons.Outlined.Category,
                            contentDescription = "ကဏ္ဍများ"
                        )
                    },
                    label = { Text("ကဏ္ဍများ", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = AmberPrimary,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextMuted,
                        indicatorColor = AmberPrimary
                    )
                )

                // Tab 3: Favorites (With Room DB Count Badge)
                NavigationBarItem(
                    selected = uiState.activeTab == LibraryTab.FAVORITES,
                    onClick = { viewModel.setTab(LibraryTab.FAVORITES) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (uiState.favoriteBooks.isNotEmpty()) {
                                    Badge(
                                        containerColor = AmberPrimary,
                                        contentColor = Color.Black
                                    ) {
                                        Text(
                                            text = "${uiState.favoriteBooks.size}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (uiState.activeTab == LibraryTab.FAVORITES) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "သိမ်းထားမှု"
                            )
                        }
                    },
                    label = { Text("သိမ်းထားမှု", fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = AmberPrimary,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextMuted,
                        indicatorColor = AmberPrimary
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            when (uiState.activeTab) {
                LibraryTab.BOOKS -> {
                    BooksCatalogContent(
                        uiState = uiState,
                        numberFormat = numberFormat,
                        onGenreSelected = { viewModel.selectGenre(it) },
                        onCategorySelected = { viewModel.selectCategory(it) },
                        onResetGenre = { viewModel.selectGenre("ALL") },
                        onResetCategory = { viewModel.selectCategory("ALL") },
                        onResetSort = { viewModel.setSortOrder(SortOption.RECENT) },
                        onResetAllFilters = { viewModel.resetFilters() },
                        onOpenFilterSheet = { viewModel.openFilterSheet(true) },
                        onLoadMore = { viewModel.loadMoreBooks() },
                        onBookClick = { viewModel.selectBookForDetail(it) },
                        onReadClick = { viewModel.openReader(it) },
                        onDownloadClick = { book -> DownloadHelper.downloadBook(context, book) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onDismissProgress = { viewModel.deleteReadingProgress(it) },
                        onRetry = { viewModel.syncAllFromWebsite() },
                        onRefresh = { viewModel.refreshBooks() }
                    )
                }

                LibraryTab.CATEGORIES -> {
                    CategoriesView(
                        categories = uiState.categories,
                        selectedCategory = uiState.selectedCategory,
                        selectedGenre = uiState.selectedGenre,
                        genreCounts = uiState.genreCounts,
                        totalCachedCount = uiState.totalRoomCachedCount,
                        onGenreClick = { genreName ->
                            viewModel.selectGenre(genreName)
                        },
                        onCategoryClick = { catName ->
                            viewModel.selectCategory(catName)
                        }
                    )
                }

                LibraryTab.FAVORITES -> {
                    FavoritesView(
                        favorites = uiState.favoriteBooks,
                        readingProgressMap = uiState.readingProgressMap,
                        onBookClick = { viewModel.selectBookForDetail(it) },
                        onReadClick = { viewModel.openReader(it) },
                        onDownloadClick = { book -> DownloadHelper.downloadBook(context, book) },
                        onToggleFavorite = { viewModel.toggleFavorite(it) },
                        onNavigateToBooks = { viewModel.setTab(LibraryTab.BOOKS) },
                        onClearAllFavorites = { viewModel.clearAllFavorites() }
                    )
                }
            }

            // Book Details Bottom Sheet
            if (uiState.selectedBookForDetail != null) {
                val detailBook = uiState.selectedBookForDetail!!
                BookDetailModal(
                    book = detailBook,
                    readingProgress = uiState.readingProgressMap[detailBook.id],
                    onDismiss = { viewModel.selectBookForDetail(null) },
                    onReadClick = { viewModel.openReader(it) },
                    onDownloadClick = { book -> DownloadHelper.downloadBook(context, book) },
                    onToggleFavorite = { viewModel.toggleFavorite(it) },
                    onSearchByAuthor = { viewModel.searchByAuthor(it) },
                    onResetProgress = { viewModel.deleteReadingProgress(detailBook.id) }
                )
            }

            // Filter Bottom Sheet (Room DB powered genre, topic, sort filter)
            if (uiState.isFilterSheetOpen) {
                FilterBottomSheet(
                    selectedGenre = uiState.selectedGenre,
                    selectedCategory = uiState.selectedCategory,
                    sortOrder = uiState.sortOrder,
                    categories = uiState.categories,
                    genreCounts = uiState.genreCounts,
                    totalCachedCount = uiState.totalRoomCachedCount,
                    onSelectGenre = { viewModel.selectGenre(it) },
                    onSelectCategory = { viewModel.selectCategory(it) },
                    onSelectSort = { viewModel.setSortOrder(it) },
                    onResetFilters = { viewModel.resetFilters() },
                    onDismiss = { viewModel.openFilterSheet(false) }
                )
            }
        }
    }
}

@Composable
private fun BooksCatalogContent(
    uiState: LibraryUiState,
    numberFormat: NumberFormat,
    onGenreSelected: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onResetGenre: () -> Unit,
    onResetCategory: () -> Unit,
    onResetSort: () -> Unit,
    onResetAllFilters: () -> Unit,
    onOpenFilterSheet: () -> Unit,
    onLoadMore: () -> Unit,
    onBookClick: (Book) -> Unit,
    onReadClick: (Book) -> Unit,
    onDownloadClick: (Book) -> Unit,
    onToggleFavorite: (Book) -> Unit,
    onDismissProgress: (Long) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit
) {
    val gridState = rememberLazyGridState()

    val shouldLoadMore = remember {
        derivedStateOf {
            val totalItemCount = gridState.layoutInfo.totalItemsCount
            val lastVisibleItem = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItemCount > 0 && lastVisibleItem >= totalItemCount - 4
        }
    }

    LaunchedEffect(shouldLoadMore.value) {
        if (shouldLoadMore.value) {
            onLoadMore()
        }
    }

    val isFilterActive = uiState.selectedGenre != "ALL" || uiState.selectedCategory != "ALL" || uiState.sortOrder != SortOption.RECENT

    Column(modifier = Modifier.fillMaxSize()) {
        val totalLibCount = if (uiState.totalBooksCount > 0) uiState.totalBooksCount else uiState.categories.sumOf { it.count }

        // Genre / Topic Bar (Room Database Filter)
        GenreFilterBar(
            selectedGenre = uiState.selectedGenre,
            genreCounts = uiState.genreCounts,
            totalCount = totalLibCount,
            onSelectGenre = onGenreSelected,
            onOpenFilterSheet = onOpenFilterSheet
        )

        // Subcategory Carousel (Filtered or full)
        CategoryBar(
            categories = uiState.categories,
            selectedCategory = uiState.selectedCategory,
            onSelectCategory = onCategorySelected,
            totalCount = totalLibCount
        )

        // Active Filter Badges Bar (Room DB filter indicators)
        if (isFilterActive) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (uiState.selectedGenre != "ALL") {
                    item {
                        ActiveFilterChip(
                            label = "Genre: ${uiState.selectedGenre}",
                            onRemove = onResetGenre
                        )
                    }
                }

                if (uiState.selectedCategory != "ALL") {
                    item {
                        ActiveFilterChip(
                            label = "Category: ${uiState.selectedCategory}",
                            onRemove = onResetCategory
                        )
                    }
                }

                if (uiState.sortOrder != SortOption.RECENT) {
                    item {
                        ActiveFilterChip(
                            label = "Sort: ${uiState.sortOrder.displayName}",
                            onRemove = onResetSort
                        )
                    }
                }

                item {
                    Text(
                        text = "Reset All",
                        color = AmberPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onResetAllFilters() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    )
                }
            }
        }

        val totalAvailable = when {
            uiState.searchQuery.isNotBlank() -> uiState.searchMatchCount.coerceAtLeast(uiState.books.size)
            uiState.selectedCategory != "ALL" -> {
                uiState.categories.find { it.name == uiState.selectedCategory }?.count
                    ?: (if (uiState.totalBooksCount > 0) uiState.totalBooksCount else uiState.books.size)
            }
            uiState.selectedGenre != "ALL" -> {
                uiState.genreCounts[uiState.selectedGenre]
                    ?: (if (uiState.totalBooksCount > 0) uiState.totalBooksCount else uiState.books.size)
            }
            else -> {
                val sumAll = uiState.categories.sumOf { it.count }
                if (sumAll > 0) sumAll else (if (uiState.totalBooksCount > 0) uiState.totalBooksCount else uiState.books.size)
            }
        }

        // Room Database & Real-Time Search status banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (uiState.searchQuery.isNotBlank()) AmberPrimary.copy(alpha = 0.12f)
                    else DarkSurfaceVariant.copy(alpha = 0.6f)
                )
                .border(
                    0.5.dp,
                    if (uiState.searchQuery.isNotBlank()) AmberPrimary.copy(alpha = 0.4f) else DarkCardBorder,
                    RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (uiState.searchQuery.isNotBlank()) Icons.Default.Search else Icons.Default.Storage,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when {
                            uiState.searchQuery.isNotBlank() -> "“${uiState.searchQuery}” ရှာဖွေမှု"
                            uiState.selectedCategory != "ALL" -> "«${uiState.selectedCategory}» စာအုပ်များ"
                            uiState.selectedGenre != "ALL" -> "«${uiState.selectedGenre}» စာအုပ်များ"
                            else -> "စာအုပ်အားလုံး"
                        },
                        color = if (uiState.searchQuery.isNotBlank()) AmberPrimary else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (uiState.isSearchingOnline) {
                        Spacer(modifier = Modifier.width(6.dp))
                        CircularProgressIndicator(
                            color = AmberPrimary,
                            strokeWidth = 1.5.dp,
                            modifier = Modifier.size(10.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ဝက်ဘ်ဆိုက်မှ ရှာဖွေနေဆဲ…",
                            color = AmberPrimary,
                            fontSize = 10.sp
                        )
                    }
                }

                Text(
                    text = if (totalAvailable > uiState.books.size) {
                        "${numberFormat.format(uiState.books.size)} / ဝက်ဘ်ဆိုက်ရှိ စုစုပေါင်း ${numberFormat.format(totalAvailable)} အုပ်"
                    } else {
                        "စုစုပေါင်း ${numberFormat.format(uiState.books.size)} အုပ်"
                    },
                    color = AmberPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Books Grid Container with SwipeRefresh / Pull-to-refresh
        SwipeRefresh(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxSize()
                .testTag("book_list_swipe_refresh")
                .testTag("swipe_refresh")
                .testTag("pull_to_refresh")
        ) {
            if (uiState.isLoading && uiState.books.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = AmberPrimary,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "စာအုပ်များ စစ်ထုတ်နေပါသည်…",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Room Database မှ အချက်အလက်များ ဆွဲယူနေပါသည်",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            } else if (uiState.books.isEmpty() && !uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) {
                                "“${uiState.searchQuery}” နှင့် ပတ်သက်သော စာအုပ် မတွေ့ပါ"
                            } else {
                                "ဤကဏ္ဍတွင် စာအုပ် ရှာမတွေ့ပါ"
                            },
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (uiState.searchQuery.isNotBlank()) {
                                "စာအုပ်ခေါင်းစဉ် (သို့) စာရေးဆရာ အမည် စာလုံးပေါင်း စစ်ဆေးပါ သို့မဟုတ် စာရေးဆရာ အကြံပြုချက်များကို အသုံးပြုပါ"
                            } else {
                                "အခြား Genre သို့မဟုတ် ကဏ္ဍတစ်ခုကို ပြောင်းလဲ ရွေးချယ်ကြည့်ပါ"
                            },
                            color = TextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onResetAllFilters,
                            colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                        ) {
                            Text(
                                text = if (uiState.searchQuery.isNotBlank()) "ရှာဖွေမှု ဖျက်မည်" else "စစ်ထုတ်မှု အားလုံး ဖျက်မည်",
                                color = Color.Black,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 155.dp),
                    state = gridState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                        .testTag("books_grid"),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                ) {
                    items(uiState.books, key = { it.id }) { book ->
                        BookCard(
                            book = book,
                            highlightQuery = uiState.searchQuery,
                            onBookClick = onBookClick,
                            onReadClick = onReadClick,
                            onDownloadClick = onDownloadClick,
                            onToggleFavorite = onToggleFavorite
                        )
                    }

                    if (uiState.isLoadingMore) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = AmberPrimary,
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "နောက်ထပ် စာအုပ်များ ခေါ်ယူနေပါသည်…",
                                        color = TextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    } else if (uiState.books.isNotEmpty() && totalAvailable > uiState.books.size) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            OutlinedButton(
                                onClick = onLoadMore,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                                    .testTag("load_more_books_button"),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AmberPrimary.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = AmberPrimary
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "နောက်ထပ် စာအုပ်များ ထပ်မံခေါ်ယူမည် (+60) • ${numberFormat.format(uiState.books.size)} / ${numberFormat.format(totalAvailable)} အုပ်",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveFilterChip(
    label: String,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AmberPrimary.copy(alpha = 0.15f))
            .border(1.dp, AmberPrimary.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                color = AmberPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "ဖယ်ရှားမည်",
                tint = AmberPrimary,
                modifier = Modifier
                    .size(14.dp)
                    .clickable { onRemove() }
            )
        }
    }
}
