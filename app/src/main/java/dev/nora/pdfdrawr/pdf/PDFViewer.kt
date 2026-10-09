package dev.nora.pdfdrawr.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.compose.ui.graphics.Path
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File
import androidx.core.graphics.createBitmap
import dev.nora.pdfdrawr.storage.SaveablePath
import dev.nora.pdfdrawr.storage.extractPathOps
import dev.nora.pdfdrawr.storage.loadFromJson
import dev.nora.pdfdrawr.storage.saveToJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class PDFViewer {

    fun loadPDF(context: Context, file: File): PDDocument {
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

    suspend fun saveCanvasData(paths: List<Path>, context: Context, filename: String) {
        val savedPaths = mutableListOf<SaveablePath>()

        paths.forEach { path ->
            val p = SaveablePath(path.fillType.toString(), extractPathOps(path))
            savedPaths.add(p)
        }
        saveToJson(savedPaths, context, filename)
        Log.d("DebugLog", "Saved ${savedPaths.count()} paths.")
    }

    suspend fun getSavedData(context: Context, filename: String): List<Path>?  = withContext(Dispatchers.IO){
        val savedPaths: List<SaveablePath> = loadFromJson(context, filename) ?: return@withContext null

        val restoredPaths = mutableListOf<Path>()
        savedPaths.forEach { p ->
            restoredPaths.add(p.toPath())
        }
        return@withContext restoredPaths
    }
}