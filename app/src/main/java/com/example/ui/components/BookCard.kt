package com.example.ui.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Book
import com.example.data.model.ReadingProgress
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
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
fun BookCard(
    book: Book,
    readingProgress: ReadingProgress? = null,
    onBookClick: (Book) -> Unit,
    onReadClick: (Book) -> Unit,
    onDownloadClick: (Book) -> Unit,
    onToggleFavorite: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    val jacketBg = JacketColors[book.jacketIndex.coerceIn(0, JacketColors.lastIndex)]

    Card(
        modifier = modifier
            .testTag("book_card_${book.id}")
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onBookClick(book) },
        colors = CardDefaults.cardColors(
            containerColor = DarkSurface
        ),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.linearGradient(listOf(DarkCardBorder, DarkCardBorder.copy(alpha = 0.4f)))
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Editorial Book Jacket (Cover)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(jacketBg)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                    .clickable { onBookClick(book) }
                    .padding(8.dp)
            ) {
                // Subtle book inner margin frame
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                )

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Bar on Cover: Category & PDF badge & Favorite indicator
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = book.category,
                            color = AmberPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (book.isFavorite) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(AmberPrimary)
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bookmark,
                                        contentDescription = "Saved",
                                        tint = Color.Black,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.5f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "PDF",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Center: Title in Burmese/English with Golden dividing bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = book.cleanTitle,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 3,
                            lineHeight = 17.sp,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(32.dp)
                                .height(1.5.dp)
                                .background(AmberPrimary)
                        )
                    }

                    // Bottom Bar on Cover: File size & Read action
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = book.formattedSize,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { onReadClick(book) }
                        ) {
                            Text(
                                text = "အစမ်းဖတ်",
                                color = AmberPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Outlined.OpenInNew,
                                contentDescription = "ဖတ်ရှုရန်",
                                tint = AmberPrimary,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // External Book Title (Clean and full)
            Text(
                text = book.cleanTitle,
                color = TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                minLines = 2,
                lineHeight = 17.sp,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onBookClick(book) }
            )

            // Author display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "စာရေးဆရာ",
                    tint = if (book.author.isNotBlank()) AmberPrimary else TextMuted,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (book.author.isNotBlank()) book.author else "အမည်မသိ စာရေးဆရာ",
                    color = if (book.author.isNotBlank()) AmberPrimary.copy(alpha = 0.9f) else TextMuted,
                    fontSize = 10.5.sp,
                    fontWeight = if (book.author.isNotBlank()) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Genre badge
            Box(
                modifier = Modifier
                    .padding(top = 3.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(AmberPrimary.copy(alpha = 0.12f))
                    .padding(horizontal = 5.dp, vertical = 1.5.dp)
            ) {
                Text(
                    text = book.genre,
                    color = AmberPrimary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
            }

            // Continue Reading Progress Bar on BookCard
            if (readingProgress != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ဖတ်လက်စ: ${readingProgress.displayPageText}",
                            color = AmberPrimary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = "${readingProgress.progressPercent}%",
                            color = AmberPrimary,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    LinearProgressIndicator(
                        progress = { (readingProgress.progressPercent / 100f).coerceIn(0.01f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = AmberPrimary,
                        trackColor = DarkCardBorder
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons (Download, Read, Favorite)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Download Button
                OutlinedButton(
                    onClick = { onDownloadClick(book) },
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .testTag("download_btn_${book.id}"),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "ဒေါင်းလုဒ်",
                        tint = AmberPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ဒေါင်းလုဒ်",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                }

                // In-App Read Button (Shows active highlight when book is in progress)
                IconButton(
                    onClick = { onReadClick(book) },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (readingProgress != null) AmberPrimary.copy(alpha = 0.22f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .border(
                            1.dp,
                            if (readingProgress != null) AmberPrimary.copy(alpha = 0.7f)
                            else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .testTag("read_btn_${book.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MenuBook,
                        contentDescription = if (readingProgress != null) "ဆက်လက်ဖတ်ရှုရန်" else "ဖတ်ရှုရန်",
                        tint = AmberPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Favorite Toggle
                IconButton(
                    onClick = { onToggleFavorite(book) },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (book.isFavorite) AmberPrimary.copy(alpha = 0.2f)
                            else Color.White.copy(alpha = 0.05f)
                        )
                        .border(
                            1.dp,
                            if (book.isFavorite) AmberPrimary.copy(alpha = 0.6f)
                            else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .testTag("fav_btn_${book.id}")
                ) {
                    Icon(
                        imageVector = if (book.isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = if (book.isFavorite) "မှတ်သားထားသည် (ဖယ်ရှားရန်)" else "စာအုပ် သိမ်းဆည်းရန်",
                        tint = if (book.isFavorite) AmberPrimary else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
