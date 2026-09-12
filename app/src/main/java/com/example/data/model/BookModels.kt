package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BookDto(
    @Json(name = "id") val id: Long,
    @Json(name = "category") val category: String = "",
    @Json(name = "raw_category") val rawCategory: String? = null,
    @Json(name = "title") val title: String = "",
    @Json(name = "raw_filename") val rawFilename: String? = null,
    @Json(name = "size") val size: Long = 0L
)

@JsonClass(generateAdapter = true)
data class CategoryDto(
    @Json(name = "name") val name: String,
    @Json(name = "count") val count: Int = 0
)

@JsonClass(generateAdapter = true)
data class BooksResponseDto(
    @Json(name = "total") val total: Int = 0,
    @Json(name = "page") val page: Int = 1,
    @Json(name = "limit") val limit: Int = 20,
    @Json(name = "totalPages") val totalPages: Int = 1,
    @Json(name = "books") val books: List<BookDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class QuotaDto(
    @Json(name = "ip") val ip: String? = null,
    @Json(name = "limit") val limit: Int = 999999,
    @Json(name = "used") val used: Int = 0,
    @Json(name = "remaining") val remaining: Int = 999999
)

enum class BookGenre(
    val id: String,
    val displayName: String,
    val nameMm: String,
    val iconEmoji: String,
    val keywords: List<String>
) {
    ALL("all", "All", "အားလုံး", "📚", emptyList()),
    LITERATURE(
        "literature",
        "Literature",
        "စာပေနှင့် ရသ",
        "📖",
        listOf("literature", "စာပေ", "ရသ", "ဝတ္ထု", "novel", "poetry", "poem", "ကဗျာ", "ဂျူး", "ပုညခင်", "မင်းလူ", "ချမ်းမြေ့ဝင်း", "လယ်တွင်းသား", "မိုးမိုး", "ဆရာတော်", "story", "fiction", "classic")
    ),
    HISTORY(
        "history",
        "History",
        "သမိုင်းနှင့် ရှေးဟောင်း",
        "🏛️",
        listOf("history", "သမိုင်း", "ရှေးဟောင်း", "ခေတ်", "ရာဇဝင်", "war", "စစ်ပွဲ", "historical", "revolution", "dynasty", "biography", "အထုပ္ပတ္တိ", "chronicle")
    ),
    EDUCATION(
        "education",
        "Education",
        "ပညာရေးနှင့် သင်ကြားရေး",
        "🎓",
        listOf("education", "ပညာရေး", "သင်ကြားရေး", "learning", "study", "cambridge", "ielts", "toefl", "grammar", "vocabulary", "english", "language", "japanese", "dictionary", "speaking", "headway", "oxford", "course", "ကျောင်း", "စာမေးပွဲ", "သင်ရိုး", "academic", "textbook")
    ),
    TECHNOLOGY(
        "tech",
        "Technology",
        "နည်းပညာနှင့် IT",
        "💻",
        listOf("technology", "tech", "ကွန်ပျူတာ", "နည်းပညာ", "ai", "programming", "software", "internet", "code", "python", "data", "developer", "computer", "web", "android", "kotlin")
    ),
    BUSINESS(
        "business",
        "Business",
        "စီးပွားရေးနှင့် စီမံခန့်ခွဲမှု",
        "💼",
        listOf("business", "စီးပွားရေး", "finance", "marketing", "management", "စီမံခန့်ခွဲမှု", "investment", "money", "အောင်မြင်ရေး", "leadership", "ခေါင်းဆောင်မှု", "economy", "startup")
    ),
    SCIENCE(
        "science",
        "Science",
        "သိပ္ပံနှင့် သုတ",
        "🔬",
        listOf("science", "သိပ္ပံ", "သုတ", "physics", "biology", "chemistry", "space", "nature", "ဗဟုသုတ", "ဒဿန", "အတွေးအခေါ်", "philosophy", "research")
    ),
    COMICS(
        "comics",
        "Comics & Art",
        "ရုပ်ပြနှင့် အနုပညာ",
        "🎨",
        listOf("comics", "comic", "art", "ရုပ်ပြ", "ကာတွန်း", "manga", "anime", "ကလေး", "drawing", "illustration")
    ),
    GENERAL(
        "general",
        "General",
        "အထွေထွေ",
        "⭐",
        listOf("general", "အထွေထွေ", "premium", "shwe note", "other", "magazine", "journal")
    );

    companion object {
        fun fromDisplayNameOrId(value: String): BookGenre {
            if (value.equals("ALL", ignoreCase = true) || value.equals("အားလုံး", ignoreCase = true)) {
                return ALL
            }
            return entries.firstOrNull {
                it.displayName.equals(value, ignoreCase = true) ||
                it.id.equals(value, ignoreCase = true) ||
                it.nameMm.equals(value, ignoreCase = true)
            } ?: when {
                value.contains("စာပေ") || value.contains("ရသ") || value.contains("literature", ignoreCase = true) -> LITERATURE
                value.contains("သမိုင်း") || value.contains("history", ignoreCase = true) -> HISTORY
                value.contains("ပညာရေး") || value.contains("education", ignoreCase = true) || value.contains("ဘာသာစကား") -> EDUCATION
                value.contains("နည်းပညာ") || value.contains("tech", ignoreCase = true) -> TECHNOLOGY
                value.contains("စီးပွားရေး") || value.contains("business", ignoreCase = true) -> BUSINESS
                value.contains("သိပ္ပံ") || value.contains("science", ignoreCase = true) -> SCIENCE
                value.contains("ရုပ်ပြ") || value.contains("comic", ignoreCase = true) -> COMICS
                else -> GENERAL
            }
        }
    }
}

fun resolveGenre(category: String, title: String): String {
    val catGenre = BookGenre.fromDisplayNameOrId(category)
    if (catGenre != BookGenre.GENERAL && catGenre != BookGenre.ALL) {
        return catGenre.displayName
    }

    val text = "$category $title".lowercase()
    val genresToCheck = listOf(
        BookGenre.HISTORY,
        BookGenre.EDUCATION,
        BookGenre.TECHNOLOGY,
        BookGenre.BUSINESS,
        BookGenre.SCIENCE,
        BookGenre.COMICS,
        BookGenre.LITERATURE
    )

    for (genre in genresToCheck) {
        for (keyword in genre.keywords) {
            val kw = keyword.lowercase()
            if (kw.length <= 2) continue
            if (kw.all { it in 'a'..'z' || it == ' ' }) {
                val pattern = Regex("\\b${Regex.escape(kw)}\\b", RegexOption.IGNORE_CASE)
                if (pattern.containsMatchIn(text)) {
                    return genre.displayName
                }
            } else {
                if (text.contains(kw)) {
                    return genre.displayName
                }
            }
        }
    }
    return BookGenre.GENERAL.displayName
}

fun normalizeGenre(raw: String?): String {
    if (raw.isNullOrBlank() || raw.equals("ALL", ignoreCase = true)) return "ALL"
    return BookGenre.fromDisplayNameOrId(raw).displayName
}

data class TitleAuthor(
    val title: String,
    val author: String
)

data class Book(
    val id: Long,
    val category: String,
    val rawCategory: String?,
    val title: String,
    val cleanTitle: String,
    val author: String = "",
    val rawFilename: String?,
    val size: Long,
    val formattedSize: String,
    val genre: String,
    val jacketIndex: Int,
    val previewUrl: String,
    val downloadUrl: String,
    val webUrl: String,
    val isFavorite: Boolean = false
) {
    val displayAuthor: String
        get() = author.ifBlank { "အမည်မသိ စာရေးဆရာ" }
}

fun BookDto.toBook(isFavorite: Boolean = false): Book {
    val extracted = extractAuthorAndTitle(this.title)
    val cat = this.category.ifBlank { "အထွေထွေ" }
    val assignedGenre = resolveGenre(cat, this.title)
    return Book(
        id = this.id,
        category = cat,
        rawCategory = this.rawCategory,
        title = this.title,
        cleanTitle = extracted.title.ifBlank { cleanBookTitle(this.title) },
        author = extracted.author,
        rawFilename = this.rawFilename,
        size = this.size,
        formattedSize = formatBytes(this.size),
        genre = assignedGenre,
        jacketIndex = (kotlin.math.abs(this.id) % 6).toInt(),
        previewUrl = "https://library.yatal.tech/api/preview/${this.id}",
        downloadUrl = "https://library.yatal.tech/api/download/${this.id}",
        webUrl = "https://library.yatal.tech/?book=${this.id}",
        isFavorite = isFavorite
    )
}

fun extractAuthorAndTitle(raw: String): TitleAuthor {
    val clean = cleanBookTitle(raw)

    val knownAuthors = listOf(
        "ဂျူး", "မစန္ဒာ", "ပုညခင်", "ချစ်ဦးညို", "မင်းလူ", "နတ်နွယ်", "လယ်တွင်းသားစောချစ်",
        "ကြည်အေး", "ဒဂုန်တာရာ", "သိန်းဖေမြင့်", "သော်တာဆွေ", "ရွှေဥဒေါင်း", "ဖေမြင့်",
        "နန္ဒာသိန်းဇံ", "ပါရဂူ", "အကြည်တော်", "မောင်သာနိုး", "ဗန်းမော်တင်အောင်", "မောင်ထွန်းသူ",
        "သိုက်ထွန်းသက်", "နီကိုရဲ", "တာရာမင်းဝေ", "မြသန်းတင့်", "အောင်သင်း", "လင်းယုန်မောင်မောင်",
        "ခင်နှင်းယု", "မောင်ဝံသ", "ဒေါက်တာမတင်ဝင်း", "ဒေါက်တာကျော်စိန်", "မောင်စူးစမ်း",
        "Dale Carnegie", "Robert Kiyosaki", "Napoleon Hill", "Stephen King", "James Clear",
        "Mark Manson", "Yuval Noah Harari", "George Orwell", "Arthur Conan Doyle", "Agatha Christie"
    )

    // 1. " by " separator (e.g. "How to Win Friends by Dale Carnegie")
    if (clean.contains(" by ", ignoreCase = true)) {
        val parts = clean.split(Regex(" by ", RegexOption.IGNORE_CASE), limit = 2)
        if (parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            return TitleAuthor(title = parts[0].trim(), author = parts[1].trim())
        }
    }

    // 2. Parentheses "(Author) Title" or "Title (Author)"
    val parenMatch = Regex("^(.+?)\\s*\\((.+?)\\)$").find(clean)
    if (parenMatch != null) {
        val p1 = parenMatch.groupValues[1].trim()
        val p2 = parenMatch.groupValues[2].trim()
        val authorKeywords = listOf("ဆရာ", "ဒေါက်တာ", "မောင်", "မင်း", "ဦး", "ဒေါ်", "ဂျူး", "author", "ရေး", "မြသန်းတင့်", "မြ", "နီကိုရဲ")
        if (authorKeywords.any { p2.contains(it, ignoreCase = true) } || knownAuthors.any { p2.contains(it, ignoreCase = true) }) {
            return TitleAuthor(title = p1, author = p2)
        }
    }

    // 3. Common author/title delimiters: " - ", " – ", " — ", " : ", " | ", " _ "
    val delimiters = listOf(" - ", " – ", " — ", " : ", " | ", " _ ")
    for (delim in delimiters) {
        if (clean.contains(delim)) {
            val parts = clean.split(delim, limit = 2)
            val p1 = parts[0].trim()
            val p2 = parts[1].trim()
            if (p1.isNotBlank() && p2.isNotBlank()) {
                val isP1KnownAuthor = knownAuthors.any { p1.contains(it, ignoreCase = true) } ||
                        Regex("^(ဆရာကြီး|ဆရာတော်|ဆရာမကြီး|ဆရာမ|ဆရာ|ဒေါက်တာ|အရှင်|ဦး|ဒေါ်)\\s*").containsMatchIn(p1)
                val isP2KnownAuthor = knownAuthors.any { p2.contains(it, ignoreCase = true) }

                if (isP2KnownAuthor && !isP1KnownAuthor) {
                    return TitleAuthor(title = p1, author = p2)
                }
                return TitleAuthor(title = p2, author = p1)
            }
        }
    }

    // 4. Known author detection in title
    for (author in knownAuthors) {
        if (clean.contains(author, ignoreCase = true)) {
            val leftover = clean.replace(author, "", ignoreCase = true)
                .replace(Regex("^[\\s\\-_/:]+"), "")
                .replace(Regex("[\\s\\-_/:]+$"), "")
                .trim()
            return TitleAuthor(
                title = if (leftover.length >= 3) leftover else clean,
                author = author
            )
        }
    }

    // 5. Check if starts with Burmese honorific title like "ဆရာမင်းလူ ၏ ..."
    val prefixMatch = Regex("^(ဆရာကြီး|ဆရာတော်|ဆရာမကြီး|ဆရာမ|ဆရာ|ဒေါက်တာ|အရှင်|ဦး|ဒေါ်)\\s*([^\\s]+)\\s+(.+)$").find(clean)
    if (prefixMatch != null) {
        val honorific = prefixMatch.groupValues[1]
        val name = prefixMatch.groupValues[2]
        val remaining = prefixMatch.groupValues[3]
        return TitleAuthor(title = remaining, author = "$honorific $name")
    }

    return TitleAuthor(title = clean, author = "")
}

fun cleanBookTitle(raw: String): String {
    return raw
        .replace(Regex("\\.(pdf|epub|mobi|docx)$", RegexOption.IGNORE_CASE), "")
        .replace('_', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val kb = bytes / 1024.0
    val mb = kb / 1024.0
    val gb = mb / 1024.0
    return when {
        gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
        mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
        kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB", kb)
        else -> "$bytes B"
    }
}

data class ReadingProgress(
    val bookId: Long,
    val title: String,
    val author: String = "",
    val category: String = "",
    val rawCategory: String? = null,
    val rawFilename: String? = null,
    val size: Long = 0L,
    val genre: String = "အထူးနှင့် အထွေထွေ",
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val scrollRatio: Float = 0f,
    val lastLocationDescription: String = "",
    val lastReadAt: Long = System.currentTimeMillis()
) {
    val progressPercent: Int
        get() = if (totalPages > 1) {
            ((currentPage.toFloat() / totalPages.toFloat()) * 100).toInt().coerceIn(1, 100)
        } else if (scrollRatio > 0f) {
            (scrollRatio * 100).toInt().coerceIn(1, 100)
        } else {
            1
        }

    val displayPageText: String
        get() = if (lastLocationDescription.isNotBlank()) {
            lastLocationDescription
        } else if (totalPages > 1) {
            "စာမျက်နှာ $currentPage / $totalPages"
        } else {
            "စာမျက်နှာ $currentPage"
        }

    val displayAuthor: String
        get() = author.ifBlank { "အမည်မသိ စာရေးဆရာ" }
}
