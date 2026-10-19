package com.example.util

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

object PdfCacheManager {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
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

    fun getCachedPdfFile(context: Context, bookId: Long): File? {
        val file = File(getCacheDir(context), "book_${bookId}.pdf")
        return if (file.exists() && file.length() > 2048) file else null
    }

    fun isBookCached(context: Context, bookId: Long): Boolean {
        return getCachedPdfFile(context, bookId) != null
    }

    /**
     * Downloads/caches the PDF file directly without requiring external browser or manual user download.
     * Tries preview endpoint first, then download endpoint.
     */
    suspend fun getOrFetchPdf(
        context: Context,
        bookId: Long,
        onProgress: (Float) -> Unit = {}
    ): File = withContext(Dispatchers.IO) {
        val existing = getCachedPdfFile(context, bookId)
        if (existing != null) {
            onProgress(1f)
            return@withContext existing
        }

        val targetFile = File(getCacheDir(context), "book_${bookId}.pdf")
        val tempFile = File(getCacheDir(context), "book_${bookId}.pdf.tmp")

        val urls = listOf(
            "https://library.yatal.tech/api/preview/$bookId",
            "https://library.yatal.tech/api/download/$bookId"
        )

        var lastException: Exception? = null

        for (url in urls) {
            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) YatalLibraryApp/1.0")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
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
                            val progress = (totalBytesRead.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
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

                // Check that file has reasonable PDF size (at least 2KB)
                if (tempFile.exists() && tempFile.length() > 2048) {
                    if (targetFile.exists()) {
                        targetFile.delete()
                    }
                    if (tempFile.renameTo(targetFile)) {
                        onProgress(1f)
                        return@withContext targetFile
                    }
                }
            } catch (e: Exception) {
                lastException = e
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
        }

        throw lastException ?: IOException("စာအုပ်ကို ဒေါင်းလုဒ် ဆာဗာမှ ရယူ၍ မရနိုင်သေးပါ")
    }
}
