package dev.nora.pdfdrawr.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PDFViewer {
    fun loadPDF(context: Context, file: File): PDDocument {
        PDFBoxResourceLoader.init(context)
        return PDDocument.load(file)
    }

    suspend fun getFirstPage(file: File): Bitmap = withContext(Dispatchers.IO) {
        val input = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(input)
        val page = renderer.openPage(0)
        val bitmap = createBitmap(page.width, page.height)

        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

        page.close()
        renderer.close()
        return@withContext bitmap
    }
}