package dev.nora.pdfdrawr.data.pdf

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PDFViewer(private val renderer: PdfRenderer) {
    private val ratios: List<Float> = List(renderer.pageCount) { i ->
        val page = renderer.openPage(i)
        page.use { page ->
            page.width.toFloat() / page.height
        }
    }

    fun ratio(index: Int): Float = ratios[index]
    fun getPageCount() = renderer.pageCount

    /**
     * Render single PDF page to bitmap.
     *
     * @param pageIndex Index of the page
     * @return Bitmap of the page
     */
    suspend fun renderSinglePage(pageIndex: Int): Bitmap = withContext(Dispatchers.IO) {
        val page = renderer.openPage(pageIndex)
        val bitmap = createBitmap(page.width, page.height)

        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        page.close()
        return@withContext bitmap
    }


    fun close() {
        renderer.close()
    }
}