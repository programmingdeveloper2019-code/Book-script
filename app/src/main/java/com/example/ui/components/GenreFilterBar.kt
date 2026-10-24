package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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

/**
 * Horizontal chip-based category filter list at the top of the home screen,
 * enabling users to swiftly browse books by genre (e.g., Fiction, History, Science, Technology, Business).
 */
@Composable
fun GenreFilterBar(
    selectedGenre: String,
    genreCounts: Map<String, Int>,
    totalCount: Int,
    onSelectGenre: (String) -> Unit,
    onOpenFilterSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }
    val displayGenres = remember {
        listOf(
            BookGenre.ALL,
            BookGenre.FICTION,
            BookGenre.NON_FICTION,
            BookGenre.EDUCATION,
            BookGenre.HISTORY,
            BookGenre.SCIENCE,
            BookGenre.TECHNOLOGY,
            BookGenre.BUSINESS,
            BookGenre.LITERATURE,
            BookGenre.COMICS,
            BookGenre.GENERAL
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LazyRow(
            modifier = Modifier
                .weight(1f)
                .testTag("category_filter_list")
                .testTag("genre_filter_row"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 8.dp)
        ) {
            items(
                items = displayGenres,
                key = { it.id }
            ) { genre ->
                val isSelected = (genre == BookGenre.ALL && (selectedGenre.equals("ALL", ignoreCase = true) || selectedGenre.equals(BookGenre.ALL.displayName, ignoreCase = true))) ||
                        (selectedGenre.equals(genre.displayName, ignoreCase = true))

                val count = if (genre == BookGenre.ALL) {
                    if (totalCount > 0) totalCount else null
                } else {
                    genreCounts[genre.displayName]
                }

                GenreChip(
                    emoji = genre.iconEmoji,
                    title = if (genre == BookGenre.ALL) "All" else genre.displayName,
                    count = count?.let { formatCount(it, numberFormat) },
                    isSelected = isSelected,
                    onClick = {
                        if (isSelected && genre != BookGenre.ALL) {
                            // Tapping selected genre toggles back to ALL
                            onSelectGenre(BookGenre.ALL.displayName)
                        } else {
                            onSelectGenre(genre.displayName)
                        }
                    },
                    testTagId = genre.id.lowercase(),
                    displayName = genre.displayName.lowercase(),
                    isEducation = genre == BookGenre.EDUCATION,
                    isNonFiction = genre == BookGenre.NON_FICTION
                )
            }
        }

        // Tune / Advanced Filter button
        Box(
            modifier = Modifier
                .padding(end = 12.dp)
                .minimumInteractiveComponentSize()
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                .clickable { onOpenFilterSheet() }
                .testTag("open_filter_sheet_btn")
                .semantics {
                    contentDescription = "Filter & Sort books"
                    role = Role.Button
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = "Filter & Sort",
                tint = AmberPrimary,
                modifier = Modifier.size(19.dp)
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
    testTagId: String,
    displayName: String,
    isEducation: Boolean = false,
    isNonFiction: Boolean = false
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) AmberPrimary else DarkSurfaceVariant,
        animationSpec = tween(durationMillis = 200),
        label = "chipBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFFFFD54F) else DarkCardBorder,
        animationSpec = tween(durationMillis = 200),
        label = "chipBorder"
    )

    Surface(
        onClick = onClick,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 40.dp)
            .testTag("genre_chip_$testTagId")
            .testTag("genre_chip_$displayName")
            .testTag("category_chip_$displayName")
            .then(if (isEducation) Modifier.testTag("genre_chip_educational").testTag("category_chip_educational") else Modifier)
            .then(if (isNonFiction) Modifier.testTag("genre_chip_nonfiction").testTag("category_chip_nonfiction").testTag("category_chip_non_fiction") else Modifier)
            .semantics {
                contentDescription = "$title genre filter, ${if (isSelected) "selected" else "not selected"}"
                role = Role.Tab
            },
        shape = RoundedCornerShape(20.dp),
        color = backgroundColor,
        border = androidx.compose.foundation.BorderStroke(1.2.dp, borderColor),
        tonalElevation = if (isSelected) 3.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon / Emoji badge
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color.Black.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = emoji,
                    fontSize = 12.sp,
                    lineHeight = 12.sp
                )
            }

            Spacer(modifier = Modifier.width(7.dp))

            // Genre Title
            Text(
                text = title,
                color = if (isSelected) Color(0xFF14120C) else TextPrimary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )

            // Optional Book Count Badge
            if (!count.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) Color(0xFF14120C).copy(alpha = 0.16f) else Color.White.copy(alpha = 0.08f)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = count,
                        color = if (isSelected) Color(0xFF14120C) else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatCount(count: Int, numberFormat: NumberFormat): String {
    return when {
        count >= 10_000 -> "${count / 1000}k"
        count >= 1000 -> "${numberFormat.format(count)}"
        else -> count.toString()
    }
}

