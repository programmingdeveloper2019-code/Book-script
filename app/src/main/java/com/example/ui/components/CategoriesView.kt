package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookGenre
import com.example.data.model.CategoryDto
import com.example.ui.theme.AmberPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CategoriesView(
    categories: List<CategoryDto>,
    selectedCategory: String,
    selectedGenre: String = "ALL",
    genreCounts: Map<String, Int> = emptyMap(),
    totalCachedCount: Int = 0,
    onGenreClick: (String) -> Unit = {},
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val numberFormat = NumberFormat.getNumberInstance(Locale.US)
    var categorySearchQuery by remember { mutableStateOf("") }

    val filteredCategories = remember(categories, categorySearchQuery) {
        if (categorySearchQuery.isBlank()) {
            categories
        } else {
            val q = categorySearchQuery.trim().lowercase()
            categories.filter { it.name.lowercase().contains(q) }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp)
            .testTag("categories_view"),
        contentPadding = PaddingValues(top = 12.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Primary Genres Section (Literature, History, Education, etc.)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Browse by Genre / ပင်မ ကဏ္ဍကြီးများ",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "စာပေ၊ သမိုင်း၊ ပညာရေး နှင့် အခြား ဘာသာရပ်အလိုက် ရှာဖွေပါ",
                            color = TextMuted,
                            fontSize = 11.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2-Column Grid for Primary Genres
                val genres = BookGenre.entries
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    genres.chunked(2).forEach { rowGenres ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowGenres.forEach { genre ->
                                val isSelected = (genre == BookGenre.ALL && (selectedGenre == "ALL" || selectedGenre == BookGenre.ALL.displayName)) ||
                                        (selectedGenre == genre.displayName)
                                val count = if (genre == BookGenre.ALL) {
                                    val totalInCategories = categories.sumOf { it.count }
                                    if (totalInCategories > 0) totalInCategories else (genreCounts[BookGenre.ALL.displayName] ?: totalCachedCount)
                                } else {
                                    genreCounts[genre.displayName] ?: 0
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { onGenreClick(genre.displayName) }
                                        .testTag("genre_card_${genre.id}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) AmberPrimary.copy(alpha = 0.15f) else DarkSurface
                                    ),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(
                                            if (isSelected) AmberPrimary else DarkCardBorder
                                        )
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(
                                                        if (isSelected) AmberPrimary.copy(alpha = 0.25f)
                                                        else DarkSurfaceVariant
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(text = genre.iconEmoji, fontSize = 18.sp)
                                            }

                                            if (count > 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            if (isSelected) AmberPrimary.copy(alpha = 0.3f)
                                                            else DarkBackground
                                                        )
                                                        .padding(horizontal = 7.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        text = "${numberFormat.format(count)} အုပ်",
                                                        color = if (isSelected) AmberPrimary else TextSecondary,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = if (genre == BookGenre.ALL) "All" else genre.displayName,
                                            color = if (isSelected) AmberPrimary else TextPrimary,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Text(
                                            text = genre.nameMm,
                                            color = TextMuted,
                                            fontSize = 10.5.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            if (rowGenres.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        // 2. Specific Subcategories Section
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Specific Topics / ခေါင်းစဉ်ခွဲများ (${categories.size} ခု)",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "စာအုပ်အမျိုးအစား သီးသန့်ခေါင်းစဉ်အလိုက် ရှာဖွေပါ",
                            color = TextMuted,
                            fontSize = 11.5.sp
                        )
                    }
                }

                if (categories.size > 5) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = categorySearchQuery,
                        onValueChange = { categorySearchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_search_input"),
                        placeholder = {
                            Text(
                                text = "ခေါင်းစဉ်အမည်ဖြင့် ရှာဖွေရန်...",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        trailingIcon = {
                            if (categorySearchQuery.isNotEmpty()) {
                                IconButton(onClick = { categorySearchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AmberPrimary,
                            unfocusedBorderColor = DarkCardBorder,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        )
                    )
                }
            }
        }

        // 3. Category List Items
        items(filteredCategories, key = { it.name }) { cat ->
            val isSelected = selectedCategory == cat.name

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onCategoryClick(cat.name) }
                    .testTag("category_row_${cat.name.hashCode()}"),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) DarkSurfaceVariant else DarkSurface
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isSelected) AmberPrimary else DarkCardBorder
                    )
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(AmberPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = null,
                                tint = AmberPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = cat.name,
                            color = if (isSelected) AmberPrimary else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBackground)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${numberFormat.format(cat.count)} အုပ်",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
