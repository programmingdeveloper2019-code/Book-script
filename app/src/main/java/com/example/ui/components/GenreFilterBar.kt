package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookGenre
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun GenreFilterBar(
    selectedGenre: String,
    genreCounts: Map<String, Int>,
    totalCount: Int,
    onSelectGenre: (String) -> Unit,
    onOpenFilterSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier
                .weight(1f)
                .testTag("genre_filter_row"),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(end = 6.dp)
        ) {
            items(BookGenre.entries) { genre ->
                val isSelected = (genre == BookGenre.ALL && (selectedGenre == "ALL" || selectedGenre == BookGenre.ALL.displayName)) ||
                        (selectedGenre == genre.displayName)

                val count = if (genre == BookGenre.ALL) {
                    if (totalCount > 0) totalCount else null
                } else {
                    genreCounts[genre.displayName]
                }

                GenreChip(
                    emoji = genre.iconEmoji,
                    title = if (genre == BookGenre.ALL) "All" else genre.displayName,
                    count = count?.let { numberFormat.format(it) },
                    isSelected = isSelected,
                    onClick = { onSelectGenre(genre.displayName) },
                    testTag = "genre_chip_${genre.id}"
                )
            }
        }

        // Filter button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp))
                .clickable { onOpenFilterSheet() }
                .testTag("open_filter_sheet_btn"),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Filter & Sort",
                tint = AmberPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun GenreChip(
    emoji: String,
    title: String,
    count: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .testTag(testTag)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) AmberPrimary else DarkSurface)
            .border(
                1.dp,
                if (isSelected) AmberPrimary else DarkCardBorder,
                RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = title,
                color = if (isSelected) Color.Black else TextPrimary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            if (count != null) {
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) Color.Black.copy(alpha = 0.2f) else DarkSurfaceVariant
                        )
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = count,
                        color = if (isSelected) Color.Black else TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
