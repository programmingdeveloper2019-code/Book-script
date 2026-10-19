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

    // Cache up to 10 rendered page bitmaps in memory
    private val pageBitmapCache = object : LruCache<String, Bitmap>(10) {
        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            // Let GC handle recycling safely
        }
    }

    val pageCount: Int
        get() = synchronized(lock) { renderer?.pageCount ?: 0 }

    init {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        pfd = descriptor
        renderer = PdfRenderer(descriptor)
    }

    /**
     * Renders a specific page (0-indexed) into a high quality Bitmap at the requested target width.
     */
    suspend fun renderPage(pageIndex: Int, targetWidth: Int): Bitmap? = withContext(Dispatchers.IO) {
        val cacheKey = "$pageIndex-$targetWidth"
        synchronized(pageBitmapCache) {
            val cached = pageBitmapCache.get(cacheKey)
            if (cached != null && !cached.isRecycled) {
                return@withContext cached
            }
        }

        try {
            synchronized(lock) {
                val currentRenderer = renderer ?: return@withContext null
                if (pageIndex < 0 || pageIndex >= currentRenderer.pageCount) {
                    return@withContext null
                }

                val page = currentRenderer.openPage(pageIndex)
                val width = targetWidth.coerceAtLeast(300)
                val scale = width.toFloat() / page.width.toFloat()
                val height = (page.height * scale).toInt().coerceAtLeast(400)

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(AndroidColor.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                synchronized(pageBitmapCache) {
                    pageBitmapCache.put(cacheKey, bitmap)
                }
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    fun close() {
        synchronized(lock) {
            try {
                renderer?.close()
            } catch (_: Exception) {}
            renderer = null

            try {
                pfd?.close()
            } catch (_: Exception) {}
            pfd = null

            synchronized(pageBitmapCache) {
                pageBitmapCache.evictAll()
            }
        }
    }
}
