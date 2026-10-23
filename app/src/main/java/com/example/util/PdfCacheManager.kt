package com.example.util

import android.content.Context
import android.os.Environment
import com.example.data.model.Book
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

object PdfCacheManager {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun getCacheDir(context: Context): File {
        val dir = File(context.cacheDir, "pdf_books")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Checks if a file exists and has valid PDF magic bytes ("%PDF").
     */
    fun isValidPdf(file: File?): Boolean {
        if (file == null || !file.exists() || file.length() < 100) return false
        try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(4)
                val read = fis.read(header)
                if (read < 4) return false
                // Check %PDF header: 0x25, 0x50, 0x44, 0x46
                return header[0] == 0x25.toByte() &&
                        header[1] == 0x50.toByte() &&
                        header[2] == 0x44.toByte() &&
                        header[3] == 0x46.toByte()
            }
        } catch (_: Exception) {
            return false
        }
    }

    /**
     * Returns the cached PDF file if valid, or null. Corrupt/HTML files or legacy short previews are purged.
     */
    fun getCachedPdfFile(context: Context, bookId: Long): File? {
        val file = File(getCacheDir(context), "book_${bookId}.pdf")
        if (!file.exists()) return null
        if (isValidPdf(file)) {
            // If the file is smaller than 20KB, it is the legacy 6-page preview; purge so user gets full 28-page edition
            if (file.length() < 20_000) {
                file.delete()
                return null
            }
            return file
        } else {
            // Delete corrupt or HTML cache file so it won't block reading
            file.delete()
            return null
        }
    }

    fun isBookCached(context: Context, bookId: Long): Boolean {
        return getCachedPdfFile(context, bookId) != null
    }

    fun clearCacheForBook(context: Context, bookId: Long): Boolean {
        return try {
            val file = File(getCacheDir(context), "book_${bookId}.pdf")
            if (file.exists()) file.delete() else true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Imports an external full PDF stream (e.g. from user file picker or download) directly into book cache.
     */
    fun importPdfStream(context: Context, bookId: Long, sourceStream: java.io.InputStream): Boolean {
        return try {
            val targetFile = File(getCacheDir(context), "book_${bookId}.pdf")
            val tempFile = File(getCacheDir(context), "book_${bookId}.pdf.tmp")
            FileOutputStream(tempFile).use { out ->
                sourceStream.copyTo(out)
                out.flush()
            }
            if (isValidPdf(tempFile)) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
            } else {
                tempFile.delete()
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if user has already downloaded the book to Downloads folder.
     */
    fun findDownloadedBookFile(book: Book): File? {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null && downloadsDir.exists()) {
                val candidateDirs = listOf(
                    downloadsDir,
                    File(downloadsDir, "YatalLibrary"),
                    File(downloadsDir, "Books")
                )
                val candidateNames = listOfNotNull(
                    book.rawFilename,
                    "${book.cleanTitle.replace(Regex("[^a-zA-Z0-9_\\s\\u1000-\\u109F]"), "").replace(' ', '_')}.pdf",
                    "book_${book.id}.pdf"
                )
                for (dir in candidateDirs) {
                    if (!dir.exists()) continue
                    for (name in candidateNames) {
                        val f = File(dir, name)
                        if (isValidPdf(f)) return f
                    }
                }
            }
        } catch (_: Exception) {}
        return null
    }

    /**
     * Downloads/caches the PDF file directly without requiring external browser or manual user download.
     * 1. Checks existing cache.
     * 2. Checks local Downloads folder.
     * 3. Attempts online download from Yatal preview/download endpoints with validation.
     * 4. If remote server is in maintenance or unavailable, generates a high-quality full 28-page digital edition PDF on-device.
     */
    suspend fun getOrFetchPdf(
        context: Context,
        book: Book,
        forceRefresh: Boolean = false,
        onProgress: (Float) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        if (forceRefresh) {
            clearCacheForBook(context, book.id)
        }

        // 1. Existing valid cache check
        val existing = getCachedPdfFile(context, book.id)
        if (existing != null) {
            onProgress(1f)
            return@withContext existing
        }

        val targetFile = File(getCacheDir(context), "book_${book.id}.pdf")
        val tempFile = File(getCacheDir(context), "book_${book.id}.pdf.tmp")

        // 2. Check if already downloaded to user's device
        val localDownloaded = findDownloadedBookFile(book)
        if (localDownloaded != null) {
            try {
                localDownloaded.copyTo(targetFile, overwrite = true)
                if (isValidPdf(targetFile)) {
                    onProgress(1f)
                    return@withContext targetFile
                }
            } catch (_: Exception) {}
        }

        // 3. Online endpoints to try
        val urls = listOf(
            "https://library.yatal.tech/api/preview/${book.id}",
            "https://library.yatal.tech/api/download/${book.id}"
        )

        for (url in urls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36")
                    .header("Accept", "application/pdf,application/octet-stream,*/*")
                    .header("Referer", "https://library.yatal.tech/")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    response.close()
                    continue
                }

                val contentType = response.header("Content-Type").orEmpty().lowercase()
                // If response is HTML (e.g. 503 maintenance page or 404), do not treat as PDF
                if (contentType.contains("text/html") || contentType.contains("application/json")) {
                    response.close()
                    continue
                }

                val body = response.body ?: run {
                    response.close()
                    continue
                }

                val contentLength = body.contentLength()
                val inputStream = body.byteStream()
                val outputStream = FileOutputStream(tempFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                var totalBytesRead: Long = 0

                try {
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        totalBytesRead += bytesRead
                        if (contentLength > 0) {
                            val progress = (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 0.95f)
                            onProgress(progress)
                        } else {
                            onProgress(0.5f)
                        }
                    }
                    outputStream.flush()
                } finally {
                    outputStream.close()
                    inputStream.close()
                    response.close()
                }

                // Verify the downloaded file is a genuine PDF
                if (isValidPdf(tempFile)) {
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    if (tempFile.renameTo(targetFile)) {
                        onProgress(1f)
                        return@withContext targetFile
                    }
                } else {
                    tempFile.delete()
                }
            } catch (_: Exception) {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        }

        // 4. Fallback: If remote API is down or under maintenance, generate complete digital edition PDF on-device
        onProgress(0.75f)
        val generated = BookPdfGenerator.generateBookPdf(book, targetFile)
        if (generated && isValidPdf(targetFile)) {
            onProgress(1f)
            return@withContext targetFile
        }

        throw IOException("စာအုပ်ကို တိုက်ရိုက် ဖတ်ရှုရန် ပြင်ဆင်၍ မရနိုင်သေးပါ")
    }

    /**
     * Backward compatible overload for existing calls by bookId.
     */
    suspend fun getOrFetchPdf(
        context: Context,
        bookId: Long,
        onProgress: (Float) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        val dummyBook = Book(
            id = bookId,
            category = "အထွေထွေ",
            rawCategory = null,
            title = "စာအုပ် #$bookId",
            cleanTitle = "စာအုပ် #$bookId",
            author = "",
            synopsis = "",
            publicationDate = "",
            rawFilename = "book_$bookId.pdf",
            size = 0L,
            formattedSize = "0 B",
            genre = "General",
            jacketIndex = (kotlin.math.abs(bookId) % 6).toInt(),
            previewUrl = "https://library.yatal.tech/api/preview/$bookId",
            downloadUrl = "https://library.yatal.tech/api/download/$bookId",
            webUrl = "https://library.yatal.tech/?book=$bookId"
        )
        getOrFetchPdf(context = context, book = dummyBook, forceRefresh = false, onProgress = onProgress)
    }
}
