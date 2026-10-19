package com.example.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatColorReset
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Book
import com.example.data.model.ReadingProgress
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.PdfCacheManager
import com.example.util.PdfRendererHelper
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.File

enum class ReaderTheme(val label: String) {
    LIGHT("အလင်း"),
    SEPIA("စက္ကူညို"),
    DARK("အမှောင်")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfReaderScreen(
    book: Book,
    savedProgress: ReadingProgress? = null,
    onClose: () -> Unit,
    onDownloadClick: (Book) -> Unit = {},
    onSaveProgress: (currentPage: Int, totalPages: Int, scrollRatio: Float, locationDescription: String) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Loading & Native Renderer State
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var rendererHelper by remember { mutableStateOf<PdfRendererHelper?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var downloadProgress by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Reading session state
    var totalPages by remember { mutableIntStateOf(savedProgress?.totalPages ?: 1) }
    var currentPage by remember { mutableIntStateOf(savedProgress?.currentPage ?: 1) }
    var locationDesc by remember { mutableStateOf(savedProgress?.lastLocationDescription.orEmpty()) }
    var isBookmarked by remember { mutableStateOf(savedProgress != null) }

    // UI View Settings
    var showControls by remember { mutableStateOf(true) }
    var readerTheme by remember { mutableStateOf(ReaderTheme.SEPIA) }
    var zoomScale by remember { mutableFloatStateOf(1.0f) }
    var showJumpDialog by remember { mutableStateOf(false) }

    // List State for continuous scroll
    val initialIndex = ((savedProgress?.currentPage ?: 1) - 1).coerceAtLeast(0)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)

    // Helper to persist reading progress
    fun saveCurrentPosition() {
        val count = totalPages.coerceAtLeast(1)
        val ratio = if (count > 1) {
            ((currentPage - 1).toFloat() / (count - 1).toFloat()).coerceIn(0f, 1f)
        } else 0f

        onSaveProgress(
            currentPage,
            count,
            ratio,
            if (locationDesc.isNotBlank()) locationDesc else "စာမျက်နှာ $currentPage / $count"
        )
        isBookmarked = true
    }

    // Automatically load or fetch the PDF
    fun loadBookPdf() {
        isLoading = true
        errorMessage = null
        downloadProgress = 0f

        coroutineScope.launch {
            try {
                val file = PdfCacheManager.getOrFetchPdf(
                    context = context,
                    bookId = book.id,
                    onProgress = { p -> downloadProgress = p }
                )
                val helper = PdfRendererHelper(file)
                pdfFile = file
                rendererHelper = helper
                totalPages = helper.pageCount.coerceAtLeast(1)
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
                errorMessage = e.localizedMessage ?: "စာအုပ် ဖတ်ရှုရန် ဖွင့်၍ မရနိုင်သေးပါ"
            }
        }
    }

    LaunchedEffect(book.id) {
        loadBookPdf()
    }

    // Clean up PdfRenderer when disposing
    DisposableEffect(Unit) {
        onDispose {
            saveCurrentPosition()
            rendererHelper?.close()
        }
    }

    // Sync current page with visible list item
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                val newPage = index + 1
                if (newPage != currentPage && newPage in 1..totalPages) {
                    currentPage = newPage
                    saveCurrentPosition()
                }
            }
    }

    // Intercept back button to safely save and close
    BackHandler {
        saveCurrentPosition()
        onClose()
    }

    // Background color based on reader theme
    val readerBgColor = when (readerTheme) {
        ReaderTheme.LIGHT -> Color(0xFFF3F3F5)
        ReaderTheme.SEPIA -> Color(0xFFECE3D2)
        ReaderTheme.DARK -> Color(0xFF141414)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pdf_reader_screen"),
        containerColor = readerBgColor,
        topBar = {
            AnimatedVisibility(
                visible = showControls,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = book.cleanTitle,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (book.author.isNotBlank()) book.author else "Yatal Library",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                saveCurrentPosition()
                                onClose()
                            },
                            modifier = Modifier.testTag("pdf_reader_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "နောက်သို့",
                                tint = AmberPrimary
                            )
                        }
                    },
                    actions = {
                        // Theme Switcher Button
                        IconButton(
                            onClick = {
                                readerTheme = when (readerTheme) {
                                    ReaderTheme.LIGHT -> ReaderTheme.SEPIA
                                    ReaderTheme.SEPIA -> ReaderTheme.DARK
                                    ReaderTheme.DARK -> ReaderTheme.LIGHT
                                }
                            }
                        ) {
                            Icon(
                                imageVector = when (readerTheme) {
                                    ReaderTheme.LIGHT -> Icons.Filled.WbSunny
                                    ReaderTheme.SEPIA -> Icons.Filled.LightMode
                                    ReaderTheme.DARK -> Icons.Filled.DarkMode
                                },
                                contentDescription = "စာဖတ် နောက်ခံအရောင်",
                                tint = AmberPrimary
                            )
                        }

                        // Bookmark Button
                        IconButton(
                            onClick = {
                                saveCurrentPosition()
                                Toast.makeText(
                                    context,
                                    "စာမျက်နှာ $currentPage တွင် မှတ်သားပြီးပါပြီ",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier.testTag("pdf_bookmark_btn")
                        ) {
                            Icon(
                                imageVector = if (isBookmarked) Icons.Filled.BookmarkAdded else Icons.Filled.Bookmark,
                                contentDescription = "မှတ်သားရန်",
                                tint = if (isBookmarked) AmberPrimary else TextSecondary
                            )
                        }

                        // Share Details Button (No Website Link)
                        IconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(
                                        Intent.EXTRA_SUBJECT,
                                        "${book.cleanTitle} - Yatal Digital Library"
                                    )
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "«${book.cleanTitle}» (${book.category})\nစာရေးဆရာ: ${if (book.author.isNotBlank()) book.author else "အမည်မသိ"}\nအရွယ်အစား: ${book.formattedSize}\n(Yatal Library အက်ပ်တွင် တိုက်ရိုက် ဖတ်ရှုနေပါသည်)"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "စာအုပ် ဝေမျှရန်"))
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Share,
                                contentDescription = "စာအုပ် ဝေမျှရန်",
                                tint = TextSecondary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DarkSurface.copy(alpha = 0.95f)
                    )
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = showControls && !isLoading && errorMessage == null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                SurfaceBottomBar(
                    currentPage = currentPage,
                    totalPages = totalPages,
                    zoomScale = zoomScale,
                    onZoomIn = { zoomScale = (zoomScale + 0.25f).coerceAtMost(2.5f) },
                    onZoomOut = { zoomScale = (zoomScale - 0.25f).coerceAtLeast(1.0f) },
                    onResetZoom = { zoomScale = 1.0f },
                    onPrevPage = {
                        if (currentPage > 1) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(currentPage - 2)
                            }
                        }
                    },
                    onNextPage = {
                        if (currentPage < totalPages) {
                            coroutineScope.launch {
                                listState.animateScrollToItem(currentPage)
                            }
                        }
                    },
                    onSliderChange = { newPage ->
                        currentPage = newPage
                        coroutineScope.launch {
                            listState.scrollToItem(newPage - 1)
                        }
                    },
                    onOpenJumpDialog = { showJumpDialog = true }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    // Direct in-app book loading screen
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(DarkCardBorder)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(AmberPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = AmberPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = book.cleanTitle,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "စာအုပ်ကို တိုက်ရိုက် ဖတ်ရှုရန် ပြင်ဆင်နေပါသည်…",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                if (downloadProgress > 0f) {
                                    LinearProgressIndicator(
                                        progress = { downloadProgress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp)),
                                        color = AmberPrimary,
                                        trackColor = DarkCardBorder
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "${(downloadProgress * 100).toInt()}% ပြီးစီးပါပြီ",
                                        color = AmberPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    CircularProgressIndicator(
                                        color = AmberPrimary,
                                        modifier = Modifier.size(32.dp),
                                        strokeWidth = 3.dp
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "ဒေါင်းလုဒ်လုပ်ရန် မလိုအပ်ပါ • အက်ပ်ထဲတွင် တိုက်ရိုက်ဖတ်ရှုနိုင်ပါသည်",
                                    color = TextMuted,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                errorMessage != null -> {
                    // In-app error screen with retry (No website redirect)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(DarkCardBorder)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.RestartAlt,
                                    contentDescription = null,
                                    tint = AmberPrimary,
                                    modifier = Modifier.size(42.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "စာအုပ် တိုက်ရိုက်ဖွင့်ရာတွင် အခက်အခဲရှိနေပါသည်",
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = errorMessage ?: "အင်တာနက်ချိတ်ဆက်မှုကို စစ်ဆေးပြီး ပြန်လည် ကြိုးစားကြည့်ပါ",
                                    color = TextSecondary,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = { loadBookPdf() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.testTag("pdf_retry_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.RestartAlt,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ပြန်လည် ကြိုးစားမည်", color = Color.Black, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                TextButton(onClick = onClose) {
                                    Text("စာအုပ်စင်သို့ ပြန်သွားမည်", color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                rendererHelper != null -> {
                    // Native PDF Page LazyColumn
                    val helper = rendererHelper!!
                    val horizontalScrollState = rememberScrollState()

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                showControls = !showControls
                            }
                    ) {
                        Box(
                            modifier = if (zoomScale > 1.05f) {
                                Modifier
                                    .fillMaxSize()
                                    .horizontalScroll(horizontalScrollState)
                            } else {
                                Modifier.fillMaxSize()
                            }
                        ) {
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("pdf_lazy_column"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                items(
                                    count = helper.pageCount,
                                    key = { index -> index }
                                ) { pageIndex ->
                                    PdfPageItem(
                                        pageIndex = pageIndex,
                                        rendererHelper = helper,
                                        zoomScale = zoomScale,
                                        theme = readerTheme
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Jump to Page Dialog
    if (showJumpDialog) {
        var pageInput by remember { mutableStateOf(currentPage.toString()) }
        var chapterInput by remember { mutableStateOf(locationDesc) }

        AlertDialog(
            onDismissRequest = { showJumpDialog = false },
            containerColor = DarkSurface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Bookmark,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "စာမျက်နှာသို့ သွားမည်",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "စုစုပေါင်း $totalPages မျက်နှာ ရှိပါသည်:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = pageInput,
                        onValueChange = { pageInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("စာမျက်နှာနံပါတ် (၁ မှ $totalPages)", fontSize = 11.sp) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("jump_page_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberPrimary,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = chapterInput,
                        onValueChange = { chapterInput = it },
                        label = { Text("မှတ်စု / အခန်းအမည် (ဥပမာ- အခန်း ၃)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberPrimary,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedPage = pageInput.toIntOrNull()?.coerceIn(1, totalPages) ?: currentPage
                        currentPage = parsedPage
                        locationDesc = chapterInput.trim()
                        saveCurrentPosition()
                        showJumpDialog = false
                        coroutineScope.launch {
                            listState.scrollToItem(parsedPage - 1)
                        }
                        Toast.makeText(
                            context,
                            "စာမျက်နှာ $currentPage သို့ ရောက်ရှိပါပြီ",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                    modifier = Modifier.testTag("jump_confirm_btn")
                ) {
                    Text("သွားမည်", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showJumpDialog = false }) {
                    Text("မလုပ်တော့ပါ", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun PdfPageItem(
    pageIndex: Int,
    rendererHelper: PdfRendererHelper,
    zoomScale: Float,
    theme: ReaderTheme,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.roundToPx() }
    val targetWidth = (screenWidthPx * zoomScale).toInt().coerceAtLeast(400)

    var pageBitmap by remember(pageIndex, zoomScale) { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember(pageIndex, zoomScale) { mutableStateOf(true) }

    LaunchedEffect(pageIndex, zoomScale) {
        isLoadingPage = true
        val bmp = rendererHelper.renderPage(pageIndex, targetWidth)
        pageBitmap = bmp
        isLoadingPage = false
    }

    val pageCardColor = when (theme) {
        ReaderTheme.LIGHT -> Color.White
        ReaderTheme.SEPIA -> Color(0xFFFBF0D9)
        ReaderTheme.DARK -> Color(0xFF222222)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = if (zoomScale <= 1.05f) 8.dp else 0.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = pageCardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.wrapContentSize()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (pageBitmap != null) {
                    Image(
                        bitmap = pageBitmap!!.asImageBitmap(),
                        contentDescription = "စာမျက်နှာ ${pageIndex + 1}",
                        modifier = Modifier.fillMaxWidth(),
                        contentScale = ContentScale.FillWidth
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(480.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoadingPage) {
                            CircularProgressIndicator(
                                color = AmberPrimary,
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "စာမျက်နှာ ${pageIndex + 1}",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Small page number footer on page
                Text(
                    text = "- ${pageIndex + 1} -",
                    color = when (theme) {
                        ReaderTheme.LIGHT -> Color.Gray
                        ReaderTheme.SEPIA -> Color(0xFF8D7B68)
                        ReaderTheme.DARK -> Color.DarkGray
                    },
                    fontSize = 10.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SurfaceBottomBar(
    currentPage: Int,
    totalPages: Int,
    zoomScale: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onPrevPage: () -> Unit,
    onNextPage: () -> Unit,
    onSliderChange: (Int) -> Unit,
    onOpenJumpDialog: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface.copy(alpha = 0.96f)
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(DarkCardBorder)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            // Page slider & info
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "စာမျက်နှာ $currentPage / $totalPages",
                    color = AmberPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                val percentage = if (totalPages > 1) {
                    ((currentPage - 1).toFloat() / (totalPages - 1).toFloat() * 100).toInt()
                } else 100
                Text(
                    text = "$percentage%",
                    color = TextSecondary,
                    fontSize = 11.sp
                )

                // Zoom control pills
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = onZoomOut,
                        modifier = Modifier.size(28.dp),
                        enabled = zoomScale > 1.05f
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = "ချုံ့မည်",
                            tint = if (zoomScale > 1.05f) TextPrimary else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "${(zoomScale * 100).toInt()}%",
                        color = TextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { onResetZoom() }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    IconButton(
                        onClick = onZoomIn,
                        modifier = Modifier.size(28.dp),
                        enabled = zoomScale < 2.45f
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "ချဲ့မည်",
                            tint = if (zoomScale < 2.45f) TextPrimary else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Slider
            if (totalPages > 1) {
                Slider(
                    value = currentPage.toFloat(),
                    onValueChange = { onSliderChange(it.toInt().coerceIn(1, totalPages)) },
                    valueRange = 1f..totalPages.toFloat(),
                    steps = if (totalPages > 2) totalPages - 2 else 0,
                    modifier = Modifier.fillMaxWidth(),
                    colors = SliderDefaults.colors(
                        thumbColor = AmberPrimary,
                        activeTrackColor = AmberPrimary,
                        inactiveTrackColor = DarkCardBorder
                    )
                )
            }

            // Prev / Jump / Next navigation buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onPrevPage,
                    enabled = currentPage > 1
                ) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowLeft,
                        contentDescription = null,
                        tint = if (currentPage > 1) AmberPrimary else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ရှေ့မျက်နှာ",
                        color = if (currentPage > 1) TextPrimary else TextMuted,
                        fontSize = 11.sp
                    )
                }

                OutlinedButton(
                    onClick = onOpenJumpDialog,
                    shape = RoundedCornerShape(8.dp),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(DarkCardBorder)
                    ),
                    modifier = Modifier.testTag("pdf_jump_page_btn")
                ) {
                    Text(
                        text = "စာမျက်နှာ ရွေးမည်",
                        color = AmberPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                TextButton(
                    onClick = onNextPage,
                    enabled = currentPage < totalPages
                ) {
                    Text(
                        text = "နောက်မျက်နှာ",
                        color = if (currentPage < totalPages) TextPrimary else TextMuted,
                        fontSize = 11.sp
                    )
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = if (currentPage < totalPages) AmberPrimary else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
