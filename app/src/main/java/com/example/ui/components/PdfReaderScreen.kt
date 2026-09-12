package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.model.Book
import com.example.data.model.ReadingProgress
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.GreenOnline
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PdfReaderScreen(
    book: Book,
    savedProgress: ReadingProgress? = null,
    onClose: () -> Unit,
    onDownloadClick: (Book) -> Unit,
    onSaveProgress: (currentPage: Int, totalPages: Int, scrollRatio: Float, locationDescription: String) -> Unit
) {
    val context = LocalContext.current

    // State tracking reading position
    var currentPage by remember { mutableIntStateOf(savedProgress?.currentPage ?: 1) }
    var totalPages by remember { mutableIntStateOf(savedProgress?.totalPages?.coerceAtLeast(1) ?: 1) }
    var scrollRatio by remember { mutableFloatStateOf(savedProgress?.scrollRatio ?: 0f) }
    var locationDesc by remember { mutableStateOf(savedProgress?.lastLocationDescription.orEmpty()) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableFloatStateOf(0f) }
    var hasError by remember { mutableStateOf(false) }

    // Dialog and notification states
    var showJumpDialog by remember { mutableStateOf(false) }
    var showBookmarkFeedback by remember { mutableStateOf(false) }
    var showResumeBanner by remember {
        mutableStateOf(savedProgress != null && (savedProgress.currentPage > 1 || savedProgress.scrollRatio > 0.05f))
    }

    // Auto-save function
    val saveCurrentPosition: () -> Unit = {
        val desc = if (locationDesc.isNotBlank()) locationDesc else "စာမျက်နှာ $currentPage"
        onSaveProgress(currentPage, totalPages, scrollRatio, desc)
    }

    // Handle system back button and close
    val handleExit = {
        saveCurrentPosition()
        onClose()
    }

    BackHandler { handleExit() }

    // Save periodically and on disposal
    DisposableEffect(Unit) {
        onDispose {
            saveCurrentPosition()
        }
    }

    // Auto-hide resume banner after 5 seconds
    LaunchedEffect(showResumeBanner) {
        if (showResumeBanner) {
            delay(5000)
            showResumeBanner = false
        }
    }

    // PDF viewer URL
    val viewerUrl = remember(book.id) {
        val directUrl = "https://library.yatal.tech/api/preview/${book.id}"
        val encoded = URLEncoder.encode(directUrl, "UTF-8")
        "https://docs.google.com/gview?embedded=true&url=$encoded"
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("pdf_reader_screen"),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = book.cleanTitle,
                            color = TextPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (book.author.isNotBlank()) "${book.author} • " else "",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "စာမျက်နှာ $currentPage",
                                color = AmberPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = handleExit,
                        modifier = Modifier.testTag("reader_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "နောက်သို့ (မှတ်တမ်း သိမ်းဆည်းမည်)",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // Quick Bookmark / Save Button
                    IconButton(
                        onClick = {
                            saveCurrentPosition()
                            showBookmarkFeedback = true
                            Toast.makeText(context, "စာမျက်နှာ $currentPage တွင် မှတ်သားပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.testTag("reader_save_bookmark_btn")
                    ) {
                        Icon(
                            imageVector = if (showBookmarkFeedback) Icons.Filled.BookmarkAdded else Icons.Filled.Bookmark,
                            contentDescription = "စာမျက်နှာ မှတ်သားမည်",
                            tint = AmberPrimary
                        )
                    }

                    // Download
                    IconButton(
                        onClick = { onDownloadClick(book) },
                        modifier = Modifier.testTag("reader_download_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "ဒေါင်းလုဒ်",
                            tint = TextSecondary
                        )
                    }

                    // Open externally
                    IconButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://library.yatal.tech/api/preview/${book.id}"))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.testTag("reader_external_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "ပြင်ပတွင် ဖွင့်ဖတ်မည်",
                            tint = TextSecondary
                        )
                    }

                    // Share
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, book.cleanTitle)
                                putExtra(Intent.EXTRA_TEXT, "«${book.cleanTitle}» ကို Yatal Library တွင် ဖတ်ရှုပါ: https://library.yatal.tech/?book=${book.id}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "ဝေမျှရန်"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "ဝေမျှမည်",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            // Main WebView
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("pdf_webview"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewRef = this
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                        }

                        // JavaScript interface for scroll tracking
                        addJavascriptInterface(object {
                            @JavascriptInterface
                            fun onScroll(ratio: Float) {
                                scrollRatio = ratio.coerceIn(0f, 1f)
                                if (totalPages > 1) {
                                    val estimated = ((ratio * totalPages).toInt() + 1).coerceIn(1, totalPages)
                                    currentPage = estimated
                                }
                            }
                        }, "AndroidReader")

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadProgress = newProgress / 100f
                                if (newProgress >= 90) {
                                    isLoading = false
                                }
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                // Restore scroll position if previously saved
                                if (scrollRatio > 0.02f) {
                                    val script = "setTimeout(function() { window.scrollTo(0, (document.body.scrollHeight - window.innerHeight) * $scrollRatio); }, 600);"
                                    view?.evaluateJavascript(script, null)
                                }
                                // Inject scroll listener
                                val scrollListenerScript = """
                                    window.addEventListener('scroll', function() {
                                        var max = document.body.scrollHeight - window.innerHeight;
                                        if (max > 0) {
                                            var ratio = window.scrollY / max;
                                            if (window.AndroidReader) {
                                                window.AndroidReader.onScroll(ratio);
                                            }
                                        }
                                    });
                                """.trimIndent()
                                view?.evaluateJavascript(scrollListenerScript, null)
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                hasError = true
                                isLoading = false
                            }
                        }

                        loadUrl(viewerUrl)
                    }
                },
                update = { webView ->
                    webViewRef = webView
                }
            )

            // Top Linear Progress Indicator during loading
            if (isLoading) {
                LinearProgressIndicator(
                    progress = { loadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = AmberPrimary,
                    trackColor = DarkSurface
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = AmberPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "စာအုပ် ဖတ်ရှုရန် ပြင်ဆင်နေပါသည်…",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (savedProgress != null && savedProgress.currentPage > 1)
                                "ယခင်ဖတ်လက်စ စာမျက်နှာ ${savedProgress.currentPage} သို့ ဆက်လက်ပို့ဆောင်ပေးပါမည်"
                            else "ခဏ စောင့်ဆိုင်းပေးပါ",
                            color = TextMuted,
                            fontSize = 11.5.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Fallback error view
            if (hasError) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "စာအုပ် preview ကို တိုက်ရိုက်မဖော်ပြနိုင်သေးပါ",
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "ဒေါင်းလုဒ် ရယူပြီး သို့မဟုတ် ပြင်ပ browser/PDF reader တွင် တိုက်ရိုက်ဖတ်ရှုနိုင်ပါသည်",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = { onDownloadClick(book) },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary)
                            ) {
                                Text("PDF ဒေါင်းလုဒ် ရယူမည်", color = Color.Black)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://library.yatal.tech/api/preview/${book.id}"))
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurface)
                            ) {
                                Text("ပြင်ပတွင် ဖွင့်မည်", color = TextPrimary)
                            }
                        }
                    }
                }
            }

            // Resumed from Last Position Notification Banner
            AnimatedVisibility(
                visible = showResumeBanner,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopCenter)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.horizontalGradient(listOf(AmberPrimary, AmberPrimary.copy(alpha = 0.5f)))
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "ယခင်ဖတ်လက်စ စာမျက်နှာ $currentPage မှ ပြန်လည်စတင်ထားပါသည်",
                                color = TextPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    currentPage = 1
                                    scrollRatio = 0f
                                    saveCurrentPosition()
                                    webViewRef?.evaluateJavascript("window.scrollTo(0, 0);", null)
                                    showResumeBanner = false
                                },
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Restart",
                                    tint = AmberPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "အစမှ",
                                    color = AmberPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(
                                onClick = { showResumeBanner = false },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "ပိတ်မည်",
                                    tint = TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Sticky 'Continue Reading' Navigation Dock
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .align(Alignment.BottomCenter)
                    .testTag("reader_bottom_controls"),
                colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.95f)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.horizontalGradient(listOf(DarkCardBorder, AmberPrimary.copy(alpha = 0.3f), DarkCardBorder))
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Page Progress Bar & Position row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Previous Page Button
                        IconButton(
                            onClick = {
                                if (currentPage > 1) {
                                    currentPage--
                                    saveCurrentPosition()
                                    // Smooth scroll up in webview
                                    webViewRef?.evaluateJavascript("window.scrollBy(0, -window.innerHeight * 0.85);", null)
                                }
                            },
                            enabled = currentPage > 1,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .testTag("reader_prev_page_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "ယခင် စာမျက်နှာ",
                                tint = if (currentPage > 1) TextPrimary else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Center Page Jump Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .border(1.dp, AmberPrimary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .clickable { showJumpDialog = true }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("reader_page_indicator_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "စာမျက်နှာ ရွေးမည်",
                                    tint = AmberPrimary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (totalPages > 1) "စာမျက်နှာ $currentPage / $totalPages" else "စာမျက်နှာ $currentPage",
                                    color = TextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                if (locationDesc.isNotBlank() && locationDesc != "စာမျက်နှာ $currentPage") {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "($locationDesc)",
                                        color = AmberPrimary,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        // Next Page Button
                        IconButton(
                            onClick = {
                                currentPage++
                                if (currentPage > totalPages) {
                                    totalPages = currentPage
                                }
                                saveCurrentPosition()
                                // Smooth scroll down in webview
                                webViewRef?.evaluateJavascript("window.scrollBy(0, window.innerHeight * 0.85);", null)
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant)
                                .testTag("reader_next_page_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "နောက် စာမျက်နှာ",
                                tint = TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Reading Progress slider and save indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "ဆက်လက်ဖတ်ရှုရန် အလိုအလျောက် မှတ်သားထားသည်",
                            color = GreenOnline,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = if (totalPages > 1) "${((currentPage.toFloat() / totalPages.toFloat()) * 100).toInt()}%" else "စာဖတ်နေဆဲ",
                            color = AmberPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Jump to Page / Chapter Dialog
    if (showJumpDialog) {
        var pageInput by remember { mutableStateOf(currentPage.toString()) }
        var totalInput by remember { mutableStateOf(if (totalPages > 1) totalPages.toString() else "") }
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
                        text = "စာမျက်နှာ (သို့) အခန်း သတ်မှတ်မည်",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "ဖတ်ရှုနေသော စာမျက်နှာနံပါတ် သို့မဟုတ် အခန်းအမည်ကို သတ်မှတ်ပြီး သိမ်းဆည်းနိုင်ပါသည်:",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = pageInput,
                            onValueChange = { pageInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("လက်ရှိ စာမျက်နှာ", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("jump_page_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberPrimary,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = totalInput,
                            onValueChange = { totalInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("စုစုပေါင်း (ရှိလျှင်)", fontSize = 11.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("jump_total_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AmberPrimary,
                                unfocusedBorderColor = DarkCardBorder,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = chapterInput,
                        onValueChange = { chapterInput = it },
                        label = { Text("အခန်း / မှတ်ချက် (ဥပမာ- အခန်း ၃)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("jump_chapter_input"),
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
                        val parsedPage = pageInput.toIntOrNull()?.coerceAtLeast(1) ?: currentPage
                        val parsedTotal = totalInput.toIntOrNull()?.coerceAtLeast(parsedPage) ?: totalPages.coerceAtLeast(parsedPage)
                        currentPage = parsedPage
                        totalPages = parsedTotal
                        locationDesc = chapterInput.trim()

                        val ratio = if (totalPages > 1) {
                            ((currentPage - 1).toFloat() / (totalPages - 1).toFloat()).coerceIn(0f, 1f)
                        } else 0f
                        scrollRatio = ratio

                        saveCurrentPosition()
                        showJumpDialog = false

                        // Scroll webview to approximate ratio
                        if (ratio > 0f) {
                            webViewRef?.evaluateJavascript("window.scrollTo(0, (document.body.scrollHeight - window.innerHeight) * $ratio);", null)
                        }
                        Toast.makeText(context, "စာမျက်နှာ $currentPage တွင် မှတ်တမ်းတင်ပြီးပါပြီ", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmberPrimary),
                    modifier = Modifier.testTag("jump_confirm_btn")
                ) {
                    Text("မှတ်တမ်းတင်မည်", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showJumpDialog = false }
                ) {
                    Text("မလုပ်တော့ပါ", color = TextSecondary)
                }
            }
        )
    }
}
