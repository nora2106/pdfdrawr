package dev.nora.pdfdrawr.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import androidx.compose.ui.graphics.Path
import androidx.core.graphics.createBitmap
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import dev.nora.pdfdrawr.storage.SaveablePath
import dev.nora.pdfdrawr.storage.extractPathOps
import dev.nora.pdfdrawr.storage.loadFromJson
import dev.nora.pdfdrawr.storage.saveToJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PDFViewer(private val renderer: PdfRenderer) {

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

    /**
     * Save canvas data as a list of serializable path objects.
     *
     * @param paths List of paths
     * @param context Global context
     * @param filename PDF filename
     */
    suspend fun saveCanvasData(paths: List<Path>, context: Context, filename: String) {
        val savedPaths = mutableListOf<SaveablePath>()

        paths.forEach { path ->
            val p = SaveablePath(path.fillType.toString(), extractPathOps(path))
            savedPaths.add(p)
        }
        saveToJson(savedPaths, context, filename)
    }

    /**
     * Gets and deserializes path save data.
     *
     * @param context Global context
     * @param filename PDF filename
     * @return list of paths
     */
    suspend fun getSavedData(context: Context, filename: String): List<Path>?  = withContext(Dispatchers.IO){
        val savedPaths: List<SaveablePath> = loadFromJson(context, filename) ?: return@withContext null

        // deserialize paths into Path objects
        val restoredPaths = mutableListOf<Path>()
        savedPaths.forEach { p ->
            restoredPaths.add(p.toPath())
        }
        return@withContext restoredPaths
    }

    fun close() {
        renderer.close()
    }

    // currently unused
    fun loadPDF(context: Context, file: File): PDDocument {
        return PDDocument.load(file)
    }
}