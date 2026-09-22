package com.joshfactory.qurantafseel.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.util.LruCache
import java.io.File
import java.util.concurrent.Executors

/**
 * Renders pages from the bundled PDFs (assets/pdfs/*.pdf) into bitmaps for
 * display in the flipbook, on a background thread.
 *
 * android.graphics.pdf.PdfRenderer needs a real file descriptor, so each PDF
 * is copied out of assets into the app's cache dir the first time it's
 * touched, then reused. Only a few PdfRenderer instances are kept open at
 * once (LRU) so memory stays bounded even though there are 200+ source
 * files; likewise only a handful of rendered page bitmaps are cached.
 */
class PdfPageRenderer(private val context: Context) {

    private val executor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    private val openRenderers = object : LruCache<String, PdfRenderer>(3) {
        override fun entryRemoved(
            evicted: Boolean,
            key: String,
            oldValue: PdfRenderer,
            newValue: PdfRenderer?
        ) {
            try {
                oldValue.close()
            } catch (_: Exception) {
            }
        }
    }

    private val bitmapCache = LruCache<Int, Bitmap>(6)

    private fun getLocalFile(fileName: String): File {
        val outFile = File(File(context.cacheDir, "pdfs"), fileName)
        if (!outFile.exists()) {
            outFile.parentFile?.mkdirs()
            context.assets.open("pdfs/$fileName").use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return outFile
    }

    @Synchronized
    private fun getRenderer(fileName: String): PdfRenderer {
        openRenderers.get(fileName)?.let { return it }
        val file = getLocalFile(fileName)
        val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(pfd)
        openRenderers.put(fileName, renderer)
        return renderer
    }

    /** Renders (or returns from cache) the page at [absoluteIndex], scaled to [targetWidthPx] wide. */
    fun renderPageAsync(absoluteIndex: Int, targetWidthPx: Int, callback: (Bitmap?) -> Unit) {
        bitmapCache.get(absoluteIndex)?.let {
            callback(it)
            return
        }
        executor.execute {
            val bmp = renderPageSync(absoluteIndex, targetWidthPx)
            if (bmp != null) bitmapCache.put(absoluteIndex, bmp)
            mainHandler.post { callback(bmp) }
        }
    }

    @Synchronized
    private fun renderPageSync(absoluteIndex: Int, targetWidthPx: Int): Bitmap? {
        val loc = PdfManifest.locate(absoluteIndex) ?: return null
        val (entry, localIndex) = loc
        return try {
            val renderer = getRenderer(entry.file)
            val page = renderer.openPage(localIndex)
            val w = targetWidthPx.coerceAtLeast(1)
            val scale = w.toFloat() / page.width
            val h = (page.height * scale).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun shutdown() {
        executor.shutdownNow()
    }
}
