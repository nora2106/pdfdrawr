package dev.nora.pdfdrawr.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Path
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nora.pdfdrawr.data.pdf.AnnotationRepository
import dev.nora.pdfdrawr.data.pdf.PDFViewer
import dev.nora.pdfdrawr.data.pdf.SaveablePath
import dev.nora.pdfdrawr.data.sync.WebDavRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

class PdfViewModel(app: Application) : ViewModel() {
    var viewer by mutableStateOf<PDFViewer?>(null)
        private set
    private var currentFile: File? = null
    private val annotationRep = AnnotationRepository(app.applicationContext)
    private var canvasVm: DrawingCanvasViewModel? = null

    fun init(canvas: DrawingCanvasViewModel) {
        canvasVm = canvas
    }

    fun openFile(file: File) = viewModelScope.launch {
        if (file == currentFile) return@launch

        val saveData = loadSaveData()
        if(saveData != null) canvasVm?.redraw(saveData)

        val input = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(input)
        renderer.let { viewer = PDFViewer(it) }
    }

    suspend fun closeCurrentFile() = withContext(Dispatchers.IO) {
        saveData()
        viewer?.close()
    }

    suspend fun renderPage(index: Int): Bitmap? = viewer?.renderSinglePage(index)
    fun ratio(index: Int): Float = viewer?.ratio(index) ?: 1f
    fun getPageCount() = viewer?.getPageCount() ?: 0

    /**
     * Save canvas data as a list of serializable path objects.
     */
    suspend fun saveData() = withContext(Dispatchers.IO){
        if(currentFile == null || canvasVm == null) return@withContext

        val savedPaths = canvasVm!!.pathsPerPage.flatMap { (page, paths) ->
            paths.map { path -> SaveablePath(path.fillType.toString(), annotationRep.extractPathOps(path), page) }
        }

        annotationRep.saveToJson(savedPaths, currentFile!!.name)
    }

    /**
     * Get and deserialize path save data.
     *
     * @return List of paths per page
     */
    suspend fun loadSaveData(): Map<Int, List<Path>>? = withContext(Dispatchers.IO){
        if(currentFile == null) return@withContext null
        val savedPaths: List<SaveablePath> = annotationRep.loadFromJson( currentFile!!.name) ?: return@withContext null

        // deserialize paths into Path objects
        val restoredPathsPerPage = mutableMapOf<Int, List<Path>>()
        savedPaths.groupBy { it.page }.forEach { (page, list) ->
            restoredPathsPerPage[page] = list.map { it.toPath() }
        }

        return@withContext restoredPathsPerPage
    }

    /**
     * Save working file as PDF and upload to server.
     *
     * @param pathsPerPage Map containing all paths per page
     */
    suspend fun uploadAsPdf() = withContext(Dispatchers.IO) {
        if(currentFile == null || canvasVm == null) return@withContext
        val newFile = annotationRep.exportPdfWithAnnotations(currentFile!!, canvasVm!!.pathsPerPage, canvasVm!!.canvasWidth)
        // TODO add path
        WebDavRepository.uploadFile(newFile, "")
    }

    override fun onCleared() {
        runBlocking(Dispatchers.IO) {
            closeCurrentFile()
        }
    }
}