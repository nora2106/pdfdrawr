package dev.nora.pdfdrawr.viewmodel

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.nora.pdfdrawr.data.pdf.AnnotationRepository
import dev.nora.pdfdrawr.data.pdf.DrawingCanvasState
import dev.nora.pdfdrawr.data.pdf.DrawnPath
import dev.nora.pdfdrawr.data.pdf.PDFViewer
import dev.nora.pdfdrawr.data.pdf.SaveablePath
import dev.nora.pdfdrawr.data.sync.WebDavRepository
import dev.nora.pdfdrawr.ui.fileview.ToolState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File

class PdfViewModel(private val annotationRep: AnnotationRepository) : ViewModel() {
    var viewer by mutableStateOf<PDFViewer?>(null)
        private set
    var currentFile by mutableStateOf<File?>(null)
        private set

    val toolState: ToolState = ToolState()
    val canvas: DrawingCanvasState = DrawingCanvasState(toolState)

    fun openFile(file: File) = viewModelScope.launch {
        if (file == currentFile) return@launch

        // save and close current file
        viewer = null
        closeCurrentFile()
        canvas.clear()

        val saveData = loadSaveData(file)
        if(saveData != null) canvas.redraw(saveData)

        val newViewer = withContext(Dispatchers.IO) {
            PDFViewer(PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)))
        }

        currentFile = file
        viewer = newViewer
    }

    suspend fun closeCurrentFile() = withContext(Dispatchers.IO) {
        saveData()
        viewer?.close()
    }

    suspend fun renderPage(index: Int, pageWidth: Int): Bitmap? = viewer?.renderSinglePage(index, pageWidth)
    fun ratio(index: Int): Float = viewer?.ratio(index) ?: 1f
    fun getPageCount() = viewer?.getPageCount() ?: 0

    /**
     * Save canvas data as a list of serializable path objects.
     */
    fun saveData() = viewModelScope.launch{
        if(currentFile == null) return@launch

        val savedPaths = canvas.pathsPerPage.flatMap { (page, paths) ->
            paths.map { pathData -> SaveablePath(pathData.path.fillType.toString(), annotationRep.extractPathOps(pathData.path), page, pathData.style) }
        }

        annotationRep.saveToJson(savedPaths, currentFile!!.name)
    }

    /**
     * Get and deserialize path save data.
     *
     * @param file File
     * @return List of paths per page
     */
    suspend fun loadSaveData(file: File): Map<Int, List<DrawnPath>>? = withContext(Dispatchers.IO){
        val savedPaths: List<SaveablePath> = annotationRep.loadFromJson( file.name) ?: return@withContext null

        // deserialize paths into Path objects
        val restoredPathsPerPage = mutableMapOf<Int, List<DrawnPath>>()
        savedPaths.groupBy { it.page }.forEach { (page, list) ->
            restoredPathsPerPage[page] = list.map { saveable ->
                DrawnPath(
                    path = saveable.toPath(),
                    style = saveable.style
                )
            }
        }

        return@withContext restoredPathsPerPage
    }

    /**
     * Save current file as PDF and upload to server.
     */
    fun uploadAsPdf() = viewModelScope.launch {
        if(currentFile == null) return@launch
        val newFile = annotationRep.exportPdfWithAnnotations(currentFile!!, canvas.pathsPerPage, canvas.canvasWidth)
        // TODO add path
        WebDavRepository.uploadFile(newFile, "")
    }

    override fun onCleared() {
        runBlocking(Dispatchers.IO) {
            closeCurrentFile()
        }
    }
}