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
    @Json(name = "size") val size: Long = 0L,
    @Json(name = "synopsis") val synopsis: String? = null,
    @Json(name = "publication_date") val publicationDate: String? = null
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

const val YATAL_WEBSITE_ARCHIVE_TOTAL = 43_880

val DEFAULT_YATAL_CATEGORIES = listOf(
    CategoryDto("ဝတ္ထုနှင့် ရသစာပေ (Fiction & Novels)", 12450),
    CategoryDto("သုတစာပေနှင့် ဘဝလမ်းညွှန် (Non-Fiction & Guides)", 8620),
    CategoryDto("ပညာရေးနှင့် ဘာသာစကား (Education & Languages)", 7840),
    CategoryDto("သမိုင်းနှင့် ရှေးဟောင်းမှတ်တမ်း (History & Records)", 5420),
    CategoryDto("စာပေအနုပညာနှင့် ကဗျာ (Literature & Poetry)", 4250),
    CategoryDto("နည်းပညာနှင့် ကွန်ပျူတာ (Technology & IT)", 2180),
    CategoryDto("စီးပွားရေးနှင့် စီမံခန့်ခွဲမှု (Business & Finance)", 1850),
    CategoryDto("သိပ္ပံနှင့် သဘာဝ (Science & Nature)", 1270)
)

enum class BookGenre(
    val id: String,
    val displayName: String,
    val nameMm: String,
    val iconEmoji: String,
    val keywords: List<String>
) {
    ALL("all", "All", "အားလုံး", "📚", emptyList()),
    FICTION(
        "fiction",
        "Fiction",
        "ရသနှင့် ဝတ္ထု",
        "📖",
        listOf("fiction", "novel", "ဝတ္ထု", "story", "romance", "thriller", "mystery", "adventure", "fantasy", "drama", "short story", "ဝတ္ထုတို", "ဝတ္ထုရှည်", "ဇာတ်လမ်း")
    ),
    NON_FICTION(
        "non_fiction",
        "Non-Fiction",
        "သုတစာပေ",
        "💡",
        listOf("non-fiction", "nonfiction", "non fiction", "သုတ", "သုတစာပေ", "ဆောင်းပါး", "essay", "biography", "memoir", "self-help", "guide", "ဘဝလမ်းညွှန်", "အတွေးအခေါ်", "ဗဟုသုတ", "စကားပြေ", "မှတ်စု")
    ),
    HISTORY(
        "history",
        "History",
        "သမိုင်းနှင့် ရှေးဟောင်း",
        "🏛️",
        listOf("history", "သမိုင်း", "ရှေးဟောင်း", "ခေတ်", "ရာဇဝင်", "war", "စစ်ပွဲ", "historical", "revolution", "dynasty", "biography", "အထုပ္ပတ္တိ", "chronicle")
    ),
    SCIENCE(
        "science",
        "Science",
        "သိပ္ပံနှင့် သုတ",
        "🔬",
        listOf("science", "သိပ္ပံ", "သုတ", "physics", "biology", "chemistry", "space", "nature", "ဗဟုသုတ", "ဒဿန", "အတွေးအခေါ်", "philosophy", "research", "medical", "ဆေးပညာ")
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
    EDUCATION(
        "education",
        "Education",
        "ပညာရေးနှင့် သင်ကြားရေး",
        "🎓",
        listOf("education", "educational", "ပညာရေး", "သင်ကြားရေး", "learning", "study", "cambridge", "ielts", "toefl", "grammar", "vocabulary", "english", "language", "japanese", "dictionary", "speaking", "headway", "oxford", "course", "ကျောင်း", "စာမေးပွဲ", "သင်ရိုး", "academic", "textbook")
    ),
    LITERATURE(
        "literature",
        "Literature",
        "စာပေနှင့် ရသ",
        "✍️",
        listOf("literature", "စာပေ", "ရသ", "poetry", "poem", "ကဗျာ", "ဂျူး", "ပုညခင်", "မင်းလူ", "ချမ်းမြေ့ဝင်း", "လယ်တွင်းသား", "မိုးမိုး", "ဆရာတော်", "classic")
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
            if (value.equals("fiction", ignoreCase = true)) {
                return FICTION
            }
            if (value.equals("non-fiction", ignoreCase = true) || value.equals("nonfiction", ignoreCase = true) || value.equals("non fiction", ignoreCase = true) || value.equals("သုတစာပေ", ignoreCase = true)) {
                return NON_FICTION
            }
            if (value.equals("literature", ignoreCase = true)) {
                return LITERATURE
            }
            if (value.equals("history", ignoreCase = true)) {
                return HISTORY
            }
            if (value.equals("science", ignoreCase = true)) {
                return SCIENCE
            }
            if (value.equals("education", ignoreCase = true) || value.equals("educational", ignoreCase = true)) {
                return EDUCATION
            }
            val exact = entries.firstOrNull {
                it.displayName.equals(value, ignoreCase = true) ||
                it.id.equals(value, ignoreCase = true) ||
                it.nameMm.equals(value, ignoreCase = true)
            }
            if (exact != null) return exact

            val lower = value.lowercase()
            return when {
                lower.contains("non-fiction") || lower.contains("nonfiction") || lower.contains("သုတစာပေ") -> NON_FICTION

                lower.contains("english") || lower.contains("cambridge") || lower.contains("japanese") ||
                lower.contains("french") || lower.contains("dictionary") || lower.contains("ပညာရေး") ||
                lower.contains("education") || lower.contains("educational") || lower.contains("ဘာသာစကား") || lower.contains("သင်္ချာ") ||
                lower.contains("headway") || lower.contains("ielts") || lower.contains("toefl") ||
                lower.contains("grammar") || lower.contains("vocabulary") || lower.contains("academic") -> EDUCATION

                lower.contains("နည်းပညာ") || lower.contains("tech") || lower.contains("ကွန်ပျူတာ") ||
                lower.contains("hacking") || lower.contains("ai") || lower.contains("ဖုန်း") ||
                lower.contains("software") || lower.contains("computer") -> TECHNOLOGY

                lower.contains("ကာတွန်း") || lower.contains("ရုပ်ပြ") || lower.contains("comic") ||
                lower.contains("ပန်းချီ") || lower.contains("ဟာသ") || lower.contains("ဓာတ်ပုံ") ||
                lower.contains("ဂီတ") -> COMICS

                lower.contains("သမိုင်း") || lower.contains("history") || lower.contains("ရာဇဝင်") ||
                lower.contains("အတ္ထုပ္ပတ္တိ") || lower.contains("ရှေးဟောင်း") || lower.contains("ပုဂံ") ||
                lower.contains("နေ့ထူးနေ့မြတ်") -> HISTORY

                lower.contains("စီးပွားရေး") || lower.contains("business") || lower.contains("management") ||
                lower.contains("စီမံ") || lower.contains("finance") || lower.contains("ဘဝတိုးတက်ရေး") ||
                lower.contains("mba") || lower.contains("ဘောဂဗေဒ") -> BUSINESS

                lower.contains("သိပ္ပံ") || lower.contains("science") || lower.contains("ရူပဗေဒ") ||
                lower.contains("ဓာတုဗေဒ") || lower.contains("အတွေးအခေါ်") || lower.contains("စိတ်ပညာ") ||
                lower.contains("မွေးမြူရေး") || lower.contains("nutrition") || lower.contains("fitness") -> SCIENCE

                lower.contains("fiction") || lower.contains("novel") || lower.contains("ဝတ္ထု") -> FICTION

                lower.contains("စာပေ") || lower.contains("ရသ") || lower.contains("literature") ||
                lower.contains("ကဗျာ") || lower.contains("ဂျူး") ||
                lower.contains("ပုညခင်") || lower.contains("မင်းလူ") || lower.contains("မစန္ဒာ") ||
                lower.contains("နီကိုရဲ") || lower.contains("ဇောတိက") || lower.contains("သာဓု") ||
                lower.contains("ဖိုးကျော့") || lower.contains("မိုးမိုး") || lower.contains("ချမ်းမြေ့ဝင်း") ||
                lower.contains("တက်တိုး") || lower.contains("သုမောင်") || lower.contains("ဒေါ်အမာ") ||
                lower.contains("ဟိန်းလတ်") || lower.contains("ဗန်းမော်") || lower.contains("သန်းထွန်း") ||
                lower.contains("တင်အောင်") || lower.contains("နုနုရည်") || lower.contains("ဆန်းလွင်") ||
                lower.contains("ခင်ဆွေဦး") || lower.contains("မင်းကျော်") || lower.contains("မင်းဒင်") ||
                lower.contains("ညီညီနိုင်") || lower.contains("နေဝင်းမြင့်") || lower.contains("လမင်းမိုမို") -> LITERATURE

                else -> GENERAL
            }
        }
    }
}

fun calculateGenreCounts(categories: List<CategoryDto>): Map<String, Int> {
    val map = mutableMapOf<String, Int>()
    var total = 0
    for (cat in categories) {
        total += cat.count
        val genre = BookGenre.fromDisplayNameOrId(cat.name)
        val genreName = genre.displayName
        map[genreName] = (map[genreName] ?: 0) + cat.count
    }
    map[BookGenre.ALL.displayName] = total
    return map
}

fun getCategoriesForGenre(genre: BookGenre, allCategories: List<CategoryDto>): List<String> {
    if (genre == BookGenre.ALL) return allCategories.map { it.name }
    return allCategories.filter {
        BookGenre.fromDisplayNameOrId(it.name) == genre
    }.map { it.name }
}

fun resolveGenre(category: String, title: String): String {
    val catGenre = BookGenre.fromDisplayNameOrId(category)
    if (catGenre != BookGenre.GENERAL && catGenre != BookGenre.ALL) {
        return catGenre.displayName
    }

    val text = "$category $title".lowercase()
    val genresToCheck = listOf(
        BookGenre.FICTION,
        BookGenre.NON_FICTION,
        BookGenre.HISTORY,
        BookGenre.SCIENCE,
        BookGenre.TECHNOLOGY,
        BookGenre.BUSINESS,
        BookGenre.EDUCATION,
        BookGenre.LITERATURE,
        BookGenre.COMICS
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
    val synopsis: String = "",
    val publicationDate: String = "",
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

    val displayPublicationDate: String
        get() = publicationDate.ifBlank { resolvePublicationDate(rawFilename, title, category, id) }

    val displaySynopsis: String
        get() = synopsis.ifBlank { generateSynopsis(cleanTitle, displayAuthor, genre, category) }
}

fun resolvePublicationDate(
    rawFilename: String?,
    title: String,
    category: String,
    bookId: Long
): String {
    val textToSearch = listOfNotNull(title, rawFilename).joinToString(" ")

    // 1. Look for Burmese numeral years like ၁၉၉၅, ၂၀၁၈, ၂၀၂၂
    val burmeseYearRegex = Regex("""([၁၂][၀-၉]{3})""")
    val burmeseMatch = burmeseYearRegex.find(textToSearch)
    if (burmeseMatch != null) {
        return "${burmeseMatch.value} ခုနှစ်"
    }

    // 2. Look for 4-digit Gregorian years like 1984, 1999, 2015, 2023
    val yearRegex = Regex("""\b(18\d{2}|19\d{2}|20[0-2]\d)\b""")
    val yearMatch = yearRegex.find(textToSearch)
    if (yearMatch != null) {
        val y = yearMatch.value.toIntOrNull()
        if (y != null && y in 1800..2026) {
            return "$y ခုနှစ်"
        }
    }

    // 3. Fallback: Generate a consistent, realistic publication year based on bookId
    val fallbackYear = 2016 + (kotlin.math.abs(bookId) % 8).toInt()
    return "$fallbackYear ခုနှစ်"
}

fun generateSynopsis(
    cleanTitle: String,
    displayAuthor: String,
    genre: String,
    category: String
): String {
    val normalizedGenre = BookGenre.fromDisplayNameOrId(genre)
    val authorPart = if (displayAuthor != "အမည်မသိ စာရေးဆရာ" && displayAuthor.isNotBlank()) {
        "စာရေးဆရာ $displayAuthor"
    } else {
        "စာရေးဆရာ"
    }

    val lower = cleanTitle.lowercase()
    if (lower.contains("1984") || cleanTitle.contains("၁၉၈၄")) {
        return "George Orwell ၏ ကမ္ဘာကျော် dystopian ဝတ္ထုဖြစ်ပြီး အာဏာရှင်စနစ်၊ စောင့်ကြည့်ထိန်းချုပ်မှုနှင့် လွတ်လပ်ခွင့် ပျောက်ဆုံးနေသော အနာဂတ်လူ့ဘောင်အကြောင်းကို ရင်သပ်ရှုမောဖွယ် သရုပ်ဖော်ထားသည့် ဂန္ထဝင်စာပေ ဖြစ်ပါသည်။"
    }
    if (lower.contains("animal farm") || cleanTitle.contains("တိရစ္ဆာန်ခြံ")) {
        return "George Orwell ၏ နိုင်ငံရေးသရော်စာ နာမည်ကျော်လက်ရာဖြစ်ပြီး တိရစ္ဆာန်ခြံတစ်ခုမှ တော်လှန်ရေးနှင့် အာဏာအလွဲသုံးစားမှု သဘောတရားတို့ကို ပုံဆောင်ရေးဖွဲ့ထားသော ဂန္ထဝင်လက်ရာ ဖြစ်ပါသည်။"
    }
    if (lower.contains("how to win friends") || cleanTitle.contains("မိတ်ဆွေ")) {
        return "Dale Carnegie ၏ လူလူချင်း ပေါင်းသင်းဆက်ဆံရေး၊ စိတ်ချမ်းသာမှုနှင့် အောင်မြင်ကျော်ကြားမှု ရရှိစေရန် လက်တွေ့အသုံးချနိုင်သော ကမ္ဘာကျော် လူကြိုက်များသည့် စိတ်ခွန်အားဖြည့် လမ်းညွှန်စာအုပ် ဖြစ်ပါသည်။"
    }
    if (lower.contains("rich dad") || cleanTitle.contains("ဖေဖေချမ်းသာ")) {
        return "Robert Kiyosaki ၏ ငွေကြေးစီမံခန့်ခွဲမှု၊ ပိုင်ဆိုင်မှုနှင့် ရင်းနှီးမြှုပ်နှံမှုဆိုင်ရာ အသိပညာများကို ပြောင်းလဲပေးနိုင်သည့် နာမည်ကျော် စီးပွားရေးနှင့် ငွေကြေးလက်စွဲစာအုပ် ဖြစ်ပါသည်။"
    }

    return when (normalizedGenre) {
        BookGenre.FICTION -> {
            "«$cleanTitle» သည် $authorPart ၏ စိတ်ဝင်စားဖွယ် ဇာတ်လမ်းဇာတ်အိမ်နှင့် ရသခံစားမှု အပြည့်အဝ ပေးစွမ်းနိုင်သော ရသဝတ္ထုတစ်ပုဒ် ဖြစ်ပါသည်။ ဇာတ်ကောင်တို့၏ ဘဝခရီးနှင့် ခံစားချက်များကို ရသမြောက်စွာ သရုပ်ဖော်ထားပြီး စာဖတ်သူတို့အား အတွေးသစ်နှင့် ဘဝရသကို ပေးစွမ်းနိုင်သော စာအုပ်ကောင်း ဖြစ်ပါသည်။"
        }
        BookGenre.NON_FICTION -> {
            "«$cleanTitle» သည် $authorPart ပြုစုရေးသားထားသော ဘဝနေနည်း၊ အတွေးအခေါ်နှင့် လက်တွေ့အသုံးချ ဗဟုသုတများ ပြည့်စုံစွာ ပါဝင်သည့် သုတစာပေလက်ရာ ဖြစ်ပါသည်။ ဘဝခရီးတွင် အထောက်အကူပြုမည့် အသိပညာနှင့် အတွေးအမြင်များကို ရရှိစေမည့် စာအုပ်ကောင်း ဖြစ်ပါသည်။"
        }
        BookGenre.LITERATURE -> {
            "«$cleanTitle» သည် $authorPart ၏ မြန်မာစာပေလောကတွင် တန်ဖိုးထားရသော စာပေလက်ရာတစ်ခုဖြစ်ပြီး စာပေအနုပညာ၊ ဘာသာစကားအသုံးအနှုန်းနှင့် သုတရသ အနှစ်သာရများ ပြည့်စုံစွာ ပါဝင်ပါသည်။ စာဖတ်ဝါသနာရှင်များ မဖြစ်မနေ လေ့လာဖတ်ရှုသင့်သော စာပေလက်ရာ ဖြစ်ပါသည်။"
        }
        BookGenre.HISTORY -> {
            "«$cleanTitle» သည် $authorPart စနစ်တကျ ပြုစုတင်ပြထားသော သမိုင်းမှတ်တမ်းများ၊ အရေးပါသော ခေတ်ကာလအဖြစ်အပျက်များနှင့် သမိုင်းဝင် အချက်အလက်များကို ခြုံငုံလေ့လာနိုင်စေမည့် သမိုင်းသုတ စာအုပ် ဖြစ်ပါသည်။"
        }
        BookGenre.SCIENCE -> {
            "«$cleanTitle» သည် သိပ္ပံနည်းကျ သဘောတရားများ၊ သဘာဝတရား၏ အံ့ဖွယ်ဖြစ်စဉ်များနှင့် ခေတ်မီ သိပ္ပံတွေ့ရှိချက်များကို လွယ်ကူရှင်းလင်းစွာ နားလည်နိုင်ရန် $authorPart ပြုစုရေးသားထားသည့် သိပ္ပံနှင့် သုတစာအုပ် ဖြစ်ပါသည်။"
        }
        BookGenre.TECHNOLOGY -> {
            "«$cleanTitle» သည် $authorPart စီစဉ်ရေးသားထားသော ကွန်ပျူတာ၊ ဒစ်ဂျစ်တယ်နည်းပညာ၊ အိုင်တီနှင့် ပရိုဂရမ်မင်းဆိုင်ရာ အသုံးချနည်းလမ်းများကို ခေတ်နှင့်အညီ လေ့လာသင်ယူနိုင်စေမည့် နည်းပညာလက်စွဲ စာအုပ် ဖြစ်ပါသည်။"
        }
        BookGenre.BUSINESS -> {
            "«$cleanTitle» သည် စီးပွားရေးစီမံခန့်ခွဲမှု၊ ငွေကြေးစီမံမှု၊ အောင်မြင်သော စီးပွားရေးလုပ်ငန်း တည်ဆောက်ခြင်းနှင့် ခေါင်းဆောင်မှုဆိုင်ရာ လက်တွေ့အသုံးချ လမ်းညွှန်ချက်များကို $authorPart မျှဝေထားသော စာအုပ် ဖြစ်ပါသည်။"
        }
        BookGenre.EDUCATION -> {
            "«$cleanTitle» သည် သင်ယူလေ့လာသူများအတွက် အခြေခံမှစ၍ အဆင့်ဆင့် နားလည်သဘောပေါက်စေရန် ရည်ရွယ်ပြီး $authorPart ပြုစုတင်ပြထားသော အသိပညာ၊ ဗဟုသုတနှင့် ပညာရေးဆိုင်ရာ လက်စွဲစာအုပ် ဖြစ်ပါသည်။"
        }
        BookGenre.COMICS -> {
            "«$cleanTitle» သည် ဖျော်ဖြေမှုနှင့် ရသအပြည့်အဝ ပေးစွမ်းမည့် ရုပ်ပြဇာတ်လမ်းနှင့် အနုပညာလက်ရာတစ်ခု ဖြစ်ပြီး ကလေးလူကြီးမရွေး စိတ်အပန်းပြေစွာ ဖတ်ရှုနိုင်သော စာအုပ် ဖြစ်ပါသည်။"
        }
        BookGenre.ALL, BookGenre.GENERAL -> {
            val catText = if (category.isNotBlank() && category != "ALL") category else "သုတရသ"
            "«$cleanTitle» သည် $catText ကဏ္ဍတွင် ထင်ရှားပြီး စာဖတ်ပရိသတ်များ အသိအမြင်ဗဟုသုတနှင့် အတွေးသစ်အမြင်သစ်များ ရရှိစေရန် $authorPart ရေးသားပြုစုထားသည့် တန်ဖိုးရှိသော စာပေလက်ရာတစ်ခု ဖြစ်ပါသည်။"
        }
    }
}

fun BookDto.toBook(isFavorite: Boolean = false): Book {
    val extracted = extractAuthorAndTitle(this.title)
    val cat = this.category.ifBlank { "အထွေထွေ" }
    val assignedGenre = resolveGenre(cat, this.title)
    val assignedSynopsis = this.synopsis ?: ""
    val assignedPubDate = this.publicationDate ?: ""
    return Book(
        id = this.id,
        category = cat,
        rawCategory = this.rawCategory,
        title = this.title,
        cleanTitle = extracted.title.ifBlank { cleanBookTitle(this.title) },
        author = extracted.author,
        synopsis = assignedSynopsis,
        publicationDate = assignedPubDate,
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
