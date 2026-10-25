package dev.nora.pdfdrawr.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import androidx.compose.ui.graphics.Path
import androidx.core.graphics.createBitmap
import java.io.File
import dev.nora.pdfdrawr.data.sync.WebDavRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.component1
import kotlin.collections.component2

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


    suspend fun saveCanvasData(pathsPerPage: MutableMap<Int, List<Path>>, context: Context, filename: String) {
        val savedPaths = pathsPerPage.flatMap { (page, paths) ->
            paths.map { path -> SaveablePath(path.fillType.toString(), extractPathOps(path), page) }
        }

        saveToJson(savedPaths, context, filename)
    }

    /**
     * Get and deserialize path save data.
     *
     * @param context Global context
     * @param filename PDF filename
     * @return list of paths
     */
    suspend fun getSavedData(context: Context, filename: String): Map<Int, List<Path>>? = withContext(Dispatchers.IO){
        val savedPaths: List<SaveablePath> = loadFromJson(context, filename) ?: return@withContext null

        // deserialize paths into Path objects
        val restoredPathsPerPage = mutableMapOf<Int, List<Path>>()
        savedPaths.groupBy { it.page }.forEach { (page, list) ->
            restoredPathsPerPage[page] = list.map { it.toPath() }
        }

        return@withContext restoredPathsPerPage
    }


    fun close() {
        renderer.close()
    }
}