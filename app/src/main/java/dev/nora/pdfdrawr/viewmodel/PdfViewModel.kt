package dev.nora.pdfdrawr.viewmodel

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

class PdfViewModel(private val annotationRep: AnnotationRepository, private val canvasVm: DrawingCanvasViewModel) : ViewModel() {
    var viewer by mutableStateOf<PDFViewer?>(null)
        private set
    var currentFile by mutableStateOf<File?>(null)
        private set

    fun openFile(file: File) = viewModelScope.launch {
        if (file == currentFile) return@launch

        // save and close current file
        viewer = null
        closeCurrentFile()
        canvasVm.clear()

        val saveData = loadSaveData(file)
        if(saveData != null) canvasVm.redraw(saveData)

        val newViewer = withContext(Dispatchers.IO) {
            PDFViewer(PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)))
        }

        /*val input = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        val renderer = PdfRenderer(input)
        renderer.let { viewer = PDFViewer(it) }*/
        currentFile = file
        viewer = newViewer
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
        if(currentFile == null) return@withContext

        val savedPaths = canvasVm.pathsPerPage.flatMap { (page, paths) ->
            paths.map { path -> SaveablePath(path.fillType.toString(), annotationRep.extractPathOps(path), page) }
        }

        annotationRep.saveToJson(savedPaths, currentFile!!.name)
    }

    /**
     * Get and deserialize path save data.
     *
     * @param file File
     * @return List of paths per page
     */
    suspend fun loadSaveData(file: File): Map<Int, List<Path>>? = withContext(Dispatchers.IO){
        val savedPaths: List<SaveablePath> = annotationRep.loadFromJson( file.name) ?: return@withContext null

        // deserialize paths into Path objects
        val restoredPathsPerPage = mutableMapOf<Int, List<Path>>()
        savedPaths.groupBy { it.page }.forEach { (page, list) ->
            restoredPathsPerPage[page] = list.map { it.toPath() }
        }

        return@withContext restoredPathsPerPage
    }

    /**
     * Save current file as PDF and upload to server.
     */
    suspend fun uploadAsPdf() = withContext(Dispatchers.IO) {
        if(currentFile == null) return@withContext
        val newFile = annotationRep.exportPdfWithAnnotations(currentFile!!, canvasVm.pathsPerPage, canvasVm.canvasWidth)
        // TODO add path
        WebDavRepository.uploadFile(newFile, "")
    }

    override fun onCleared() {
        runBlocking(Dispatchers.IO) {
            closeCurrentFile()
        }
    }
}