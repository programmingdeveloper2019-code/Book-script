package com.example.ui.components

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.JacketBrown
import com.example.ui.theme.JacketBurgundy
import com.example.ui.theme.JacketCharcoal
import com.example.ui.theme.JacketGreen
import com.example.ui.theme.JacketNavy
import com.example.ui.theme.JacketPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private val JacketColors = listOf(
    JacketNavy,
    JacketGreen,
    JacketBrown,
    JacketPurple,
    JacketCharcoal,
    JacketBurgundy
)

@Composable
fun BookDetailView(
    book: Book,
    readingProgress: ReadingProgress? = null,
    onDismiss: () -> Unit,
    onReadClick: (Book) -> Unit,
    onDownloadClick: (Book) -> Unit,
    onToggleFavorite: (Book) -> Unit,
    onSearchByAuthor: ((String) -> Unit)? = null,
    onResetProgress: (() -> Unit)? = null
) {
    BookDetailModal(
        book = book,
        readingProgress = readingProgress,
        onDismiss = onDismiss,
        onReadClick = onReadClick,
        onDownloadClick = onDownloadClick,
        onToggleFavorite = onToggleFavorite,
        onSearchByAuthor = onSearchByAuthor,
        onResetProgress = onResetProgress
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailModal(
    book: Book,
    readingProgress: ReadingProgress? = null,
    onDismiss: () -> Unit,
    onReadClick: (Book) -> Unit,
    onDownloadClick: (Book) -> Unit,
    onToggleFavorite: (Book) -> Unit,
    onSearchByAuthor: ((String) -> Unit)? = null,
    onResetProgress: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val jacketBg = JacketColors[book.jacketIndex.coerceIn(0, JacketColors.lastIndex)]

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = null,
        modifier = Modifier
            .testTag("book_detail_view")
            .testTag("book_detail_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Drag handle pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(TextMuted.copy(alpha = 0.4f))
                )
            }

            // Header Row with Title and Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(AmberPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "စာအုပ် အသေးစိတ်",
                        color = AmberPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("detail_close_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "ပိတ်မည်",
                        tint = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Hero Book Overview Card (Jacket + Clean Title + Genre + Meta)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(DarkCardBorder)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Book Jacket Cover Thumbnail
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(125.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(jacketBg)
                                .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                .padding(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(113.dp)
                                    .border(0.7.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(5.dp))
                                    .padding(6.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = book.cleanTitle,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis,
                                        lineHeight = 13.sp
                                    )
                                    Text(
                                        text = book.displayAuthor,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 8.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Title & High-level details
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = book.cleanTitle,
                                color = TextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 23.sp,
                                modifier = Modifier.testTag("detail_title")
                            )

                            // Meta chips
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                MetaChip(label = book.genre, color = AmberPrimary)
                                MetaChip(label = "PDF", color = TextSecondary)
                                MetaChip(label = book.formattedSize, color = TextMuted)
                            }

                            MetaChip(label = book.category, color = AmberPrimary.copy(alpha = 0.85f))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Author & Publication Date Cards (Displayed prominently)
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(brush = SolidColor(DarkCardBorder)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Author Information Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_author"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmberPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "စာရေးဆရာ (Author)",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = book.displayAuthor,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Author Quick-Search Chip (if author is known)
                    if (book.author.isNotBlank() && onSearchByAuthor != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(AmberPrimary.copy(alpha = 0.12f))
                                .border(0.8.dp, AmberPrimary.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                                .clickable {
                                    onDismiss()
                                    onSearchByAuthor(book.author)
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                .testTag("search_by_author_btn")
                        ) {
                            Text(
                                text = "🔍 «${book.author}» ၏ အခြားစာအုပ်များ ရှာဖွေမည်",
                                color = AmberPrimary,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(
                        color = DarkCardBorder,
                        thickness = 0.8.dp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    // Publication Date Information Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("detail_publication_date"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmberPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Text(
                                text = "ထုတ်ဝေသည့် ခုနှစ် (Publication Date)",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = book.displayPublicationDate,
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Book Synopsis Section
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = SolidColor(AmberPrimary.copy(alpha = 0.35f))
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("detail_synopsis_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = AmberPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "စာအုပ် အကျဉ်းချုပ် (Synopsis)",
                            color = AmberPrimary,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = book.displaySynopsis,
                        color = TextPrimary.copy(alpha = 0.95f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        lineHeight = 21.sp,
                        modifier = Modifier.testTag("detail_synopsis")
                    )
                }
            }

            // Filename specification if present
            if (book.rawFilename != null && book.rawFilename.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "ဖိုင်အမည်: ${book.rawFilename}",
                    color = TextMuted,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            // Reading Progress Card (If previously read)
            if (readingProgress != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(AmberPrimary.copy(alpha = 0.5f))
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("detail_reading_progress_card")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Bookmark,
                                    contentDescription = null,
                                    tint = AmberPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ဖတ်လက်စ အခြေအနေ",
                                    color = AmberPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (onResetProgress != null) {
                                IconButton(
                                    onClick = onResetProgress,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DeleteOutline,
                                        contentDescription = "ဖတ်လက်စ ဖျက်မည်",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = readingProgress.displayPageText,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${readingProgress.progressPercent}% ဖတ်ပြီး",
                                color = AmberPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { (readingProgress.progressPercent / 100f).coerceIn(0.01f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = AmberPrimary,
                            trackColor = DarkCardBorder
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Primary Direct Read Action
            Button(
                onClick = {
                    onDismiss()
                    onReadClick(book)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("detail_read_btn"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AmberPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MenuBook,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (readingProgress != null) "ဆက်လက်ဖတ်ရှုမည် (${readingProgress.displayPageText})" else "စာအုပ်တစ်အုပ်လုံး တိုက်ရိုက် ဖတ်ရှုမည် (အပြည့်အစုံ)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Information note about complete full book reading
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "✓ အမှာစာ၊ အခန်း ၁ မှ ၁၂ အထိ၊ မာတိကာနှင့် အတ္ထုပ္ပတ္တိ (စာအုပ်တစ်အုပ်လုံး ၂၈ မျက်နှာ) ပြည့်စုံစွာ ဖတ်ရှုနိုင်ပါသည်",
                    color = AmberPrimary.copy(alpha = 0.9f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Secondary Actions: Favorite, Optional Download & Share
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Favorite Toggle
                OutlinedButton(
                    onClick = { onToggleFavorite(book) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("detail_favorite_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (book.isFavorite) AmberPrimary.copy(alpha = 0.18f) else DarkSurfaceVariant
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = SolidColor(
                            if (book.isFavorite) AmberPrimary else DarkCardBorder
                        )
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = if (book.isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = null,
                        tint = if (book.isFavorite) AmberPrimary else TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (book.isFavorite) "သိမ်းပြီး" else "မှတ်မည်",
                        color = if (book.isFavorite) AmberPrimary else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = if (book.isFavorite) FontWeight.Bold else FontWeight.Medium
                    )
                }

                // Optional Download Button
                OutlinedButton(
                    onClick = { onDownloadClick(book) },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("detail_download_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurfaceVariant),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = SolidColor(DarkCardBorder)
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ဒေါင်းလုဒ်",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Share Button
                OutlinedButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_SUBJECT,
                                "${book.cleanTitle} - Yatal Digital Library"
                            )
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "«${book.cleanTitle}» (${book.category})\nစာရေးဆရာ: ${book.displayAuthor}\nထုတ်ဝေသည့် ခုနှစ်: ${book.displayPublicationDate}\nအကျဉ်းချုပ်: ${book.displaySynopsis}\nအရွယ်အစား: ${book.formattedSize}\n(Yatal Library အက်ပ်တွင် တိုက်ရိုက် ဖတ်ရှုနိုင်ပါသည်)"
                            )
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "စာအုပ် အချက်အလက် ဝေမျှရန်"))
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("detail_share_btn"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DarkSurfaceVariant),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = SolidColor(DarkCardBorder)
                    ),
                    contentPadding = PaddingValues(horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        tint = AmberPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ဝေမျှမည်",
                        color = TextPrimary,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun MetaChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkBackground)
            .border(0.5.dp, DarkCardBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
