package com.example.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PdfRendererHelper(val file: File) {

    private var pfd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    private val lock = Any()
    @Volatile
    private var isClosed = false

    // Cache up to 12 rendered page bitmaps in memory
    private val pageBitmapCache = object : LruCache<String, Bitmap>(12) {
        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            // Memory managed by GC
        }
    }

    private val parsedPageCountFromFile: Int by lazy {
        try {
            if (file.exists()) {
                val text = file.readText(Charsets.ISO_8859_1)
                val countMatch = Regex("/Type\\s*/Pages[^>]*?/Count\\s+(\\d+)").find(text)
                    ?: Regex("/Count\\s+(\\d+)").find(text)
                if (countMatch != null) {
                    countMatch.groupValues[1].toIntOrNull() ?: 0
                } else {
                    Regex("/Type\\s*/Page[^s]").findAll(text).count()
                }
            } else 0
        } catch (_: Throwable) {
            0
        }
    }

    val pageCount: Int
        get() = synchronized(lock) {
            if (isClosed) 0
            else {
                val nativeCount = renderer?.pageCount ?: 0
                if (nativeCount > 0) nativeCount else parsedPageCountFromFile
            }
        }

    init {
        try {
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pfd = descriptor
            renderer = PdfRenderer(descriptor)
        } catch (_: Throwable) {
            renderer = null
        }
    }

    /**
     * Returns the aspect ratio (height / width) of the given page, or a standard 1.414f (A4).
     */
    fun getPageAspectRatio(pageIndex: Int): Float = synchronized(lock) {
        if (isClosed) return 1.414f
        try {
            val r = renderer ?: return 1.414f
            if (pageIndex in 0 until r.pageCount) {
                val page = r.openPage(pageIndex)
                val ratio = if (page.width > 0) page.height.toFloat() / page.width.toFloat() else 1.414f
                page.close()
                return ratio
            }
        } catch (_: Throwable) {}
        return 1.414f
    }

    /**
     * Renders a specific page (0-indexed) into a high quality Bitmap at the requested target width.
     * Enforces safe bounds to prevent OutOfMemoryError and catches all Throwables.
     */
    suspend fun renderPage(pageIndex: Int, targetWidth: Int): Bitmap? = withContext(Dispatchers.IO) {
        if (isClosed) return@withContext null

        val safeWidth = targetWidth.coerceIn(360, 1400)
        val cacheKey = "$pageIndex-$safeWidth"

        synchronized(pageBitmapCache) {
            val cached = pageBitmapCache.get(cacheKey)
            if (cached != null && !cached.isRecycled) {
                return@withContext cached
            }
        }

        try {
            synchronized(lock) {
                if (isClosed) return@withContext null
                val currentRenderer = renderer ?: return@withContext null
                if (pageIndex < 0 || pageIndex >= currentRenderer.pageCount) {
                    return@withContext null
                }

                val page = currentRenderer.openPage(pageIndex)
                val scale = safeWidth.toFloat() / page.width.toFloat()
                val safeHeight = (page.height * scale).toInt().coerceIn(400, 2400)

                val bitmap = Bitmap.createBitmap(safeWidth, safeHeight, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(AndroidColor.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                synchronized(pageBitmapCache) {
                    pageBitmapCache.put(cacheKey, bitmap)
                }
                bitmap
            }
        } catch (t: Throwable) {
            // Trim cache if low on memory
            synchronized(pageBitmapCache) {
                pageBitmapCache.trimToSize(4)
            }
            null
        }
    }

    fun close() {
        synchronized(lock) {
            isClosed = true
            try {
                renderer?.close()
            } catch (_: Throwable) {}
            renderer = null

            try {
                pfd?.close()
            } catch (_: Throwable) {}
            pfd = null

            synchronized(pageBitmapCache) {
                pageBitmapCache.evictAll()
            }
        }
    }
}
